/**
 * https://gist.github.com/InsanusMokrassar/3c5f0b0137ecc6948bc374aa31e3cf2d
 *
 * Generates files and folders as the have been put in the folder. Envs uses common syntax, but
 * values may contains ${'$'}{variable} parts, where ${'$'}{variable} will be replaced with variable value.
 * Example:
 *
 * .env:
 * sampleVariable=${'$'}prompt # require request from command line
 *         sampleVariable2=just some value
 * sampleVariable3=${'$'}{sampleVariable}.${'$'}{sampleVariable2}
 *
 * Result variables:
 * sampleVariable=your input in console # lets imagine you typed it
 *         sampleVariable2=just some value
 * sampleVariable3=your input in console.just some value
 *
 *         To use these variables in template, you will need to write {{${'$'}sampleVariable}}.
 * You may use it in text of files as well as in files/folders names.
 *
 * Usage: kotlin generator.kts [args] folders...
 * Args:
 * -e, --env: Path to file with args for generation; Use "${'$'}prompt" as values to read variable value from console
 * -o, --outputFolder: Folder where templates should be used. Folder of calling by default
 *         folders: Folders-templates
 */
import java.io.File

fun requestVariable(variableName: String): String {
    println("Enter value for variable $variableName: ")
    return readLine()!!
}

fun readEnvs(content: String): Map<String, String> {
    val initialEnvs = mutableMapOf<String, String>()
    content.split("\n").forEach {
        val withoutComment = it.replace(Regex("\\#.*"), "")
        if (withoutComment.isBlank()) return@forEach
        
        runCatching {
            val (key, value) = withoutComment.split("=")
            if (value == "\$prompt") {
                initialEnvs[key] = requestVariable(key)
            } else {
                initialEnvs[key] = value
            }
        }
    }
    var i = 0
    var readEnvs = initialEnvs.toMap()
    while (i < readEnvs.size) {
        val key = readEnvs.keys.elementAt(i)
        val currentValue = readEnvs.getValue(key)
        var changed = false
        readEnvs = readEnvs.mapValues { (k, v) ->
            val withReplaced = v.replace("\${${key}}", currentValue)
            if (withReplaced == v) {
                v
            } else {
                changed = true
                withReplaced
            }
        }
        if (changed) {
            i = 0
        } else {
            i++
        }
    }
    return readEnvs
}

var envFile: File? = null
var outputFolder: File = File("./") // current folder by default
val templatesFolders = mutableListOf<File>()
var extensions: List<String>? = null

fun readParameters() {
    var i = 0
    while (i < args.size) {
        val arg = args[i]
        when (arg) {
            "--env",
            "-e" -> {
                i++
                envFile = File(args[i])
            }
            "--extensions",
            "-ex" -> {
                i++
                extensions = args[i].split(",")
            }
            "--outputFolder",
            "-o" -> {
                i++
                outputFolder = File(args[i])
            }
            "--help",
            "-h" -> {
                println("""
                    Generates files and folders as the have been put in the folder. Envs uses common syntax, but
                    values may contains ${'$'}{variable} parts, where ${'$'}{variable} will be replaced with variable value.
                    Example:
                    
                    .env:
                    sampleVariable=${'$'}prompt # require request from command line
                    sampleVariable2=just some value
                    sampleVariable3=${'$'}{sampleVariable}.${'$'}{sampleVariable2}
                    
                    Result variables:
                    sampleVariable=your input in console # lets imagine you typed it
                    sampleVariable2=just some value
                    sampleVariable3=your input in console.just some value
                    
                    To use these variables in template, you will need to write {{${'$'}sampleVariable}}.
                    You may use it in text of files as well as in files/folders names.
                    
                    Usage: kotlin generator.kts [args] folders...
                    Args:
                        -e, --env: Path to file with args for generation; Use "${'$'}prompt" as values to read variable value from console
                        -o, --outputFolder: Folder where templates should be used. Folder of calling by default
                        folders: Folders-templates
                """.trimIndent())
                Runtime.getRuntime().exit(0)
            }
            else -> {
                val potentialFile = File(arg)
                println("Potential file/folder as template: ${potentialFile.absolutePath}")
                runCatching {
                    if (potentialFile.exists()) {
                        println("Adding file/folder as template: ${potentialFile.absolutePath}")
                        templatesFolders.add(potentialFile)
                    }
                }.onFailure { e ->
                    println("Unable to use folder $arg as template folder")
                    e.printStackTrace()
                }
            }
        }
        i++
    }
}

readParameters()

val envs = envFile ?.let { readEnvs(it.readText()) } ?.toMutableMap() ?: mutableMapOf()

println(
    """
    Result environments:
        ${envs.toList().joinToString("\n        ") { (k, v) -> "$k=$v" }}
    Result extensions:
        ${extensions ?.joinToString()}
    Input folders:
        ${templatesFolders.joinToString("\n        ") { it.absolutePath }}
    Output folder:
        ${outputFolder.absolutePath}
    """.trimIndent()
)

fun String.replaceWithVariables(): String {
    var currentString = this
    var changed = false

    do {
        changed = false
        envs.forEach { (k, v) ->
            val previousString = currentString
            currentString = currentString.replace("{{$${k}}}", v)
            changed = changed || currentString != previousString
        }
    } while (changed)
    
    return currentString
}

fun File.handleTemplate(targetFolder: File) {
    println("Handling $absolutePath")
    val newName = name.replaceWithVariables()
    println("New name $newName")
    when {
        !exists() -> return
        isFile -> {
            val content = useLines {
                it.map { it.replaceWithVariables() }.toList()
            }.joinToString("\n")
            val targetFile = File(targetFolder, newName)
            targetFile.writeText(content)
            println("Target file: ${targetFile.absolutePath}")
        }
        else -> {
            val folder = File(targetFolder, newName)
            println("Target folder: ${folder.absolutePath}")
            folder.mkdirs()
            listFiles() ?.forEach { fileOrFolder ->
                fileOrFolder.handleTemplate(folder)
            }
        }
    }
}

templatesFolders.forEach { folderOrFile ->
    folderOrFile.handleTemplate(outputFolder)
}

