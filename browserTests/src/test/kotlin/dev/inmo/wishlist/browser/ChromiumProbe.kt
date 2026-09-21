package dev.inmo.wishlist.browser

import com.microsoft.playwright.BrowserType
import com.microsoft.playwright.Playwright
import java.nio.file.Files
import java.nio.file.Path

/** Verifies that the Chromium binaries pinned by the Playwright JVM dependency can launch headlessly. */
internal object ChromiumProbe {
    /**
     * Prints the pinned executable path or launches the browser to validate the private Playwright cache.
     *
     * @param args accepts `--print-executable` to query the pinned path without launching Chromium.
     */
    @JvmStatic
    fun main(args: Array<String>) {
        Playwright.create().use { playwright ->
            val executable = playwright.chromium().executablePath()
            println("PLAYWRIGHT_CHROMIUM_EXECUTABLE=$executable")
            if (args.singleOrNull() == "--print-executable") return
            val executablePath = Path.of(executable)
            check(Files.isRegularFile(executablePath) && Files.isExecutable(executablePath)) {
                "Pinned Chromium executable is unavailable: $executable"
            }
            playwright.chromium().launch(BrowserType.LaunchOptions().setHeadless(true)).use { }
        }
    }
}
