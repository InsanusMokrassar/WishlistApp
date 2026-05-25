package project_group.project_name.{{$module_package}}

import dev.inmo.micro_utils.language_codes.IetfLang
import dev.inmo.micro_utils.strings.buildStringResource

object {{$module_ui_name}}Strings {
    val sample = buildStringResource("Sample") {
        IetfLang.Russian("Пример")
    }
}
