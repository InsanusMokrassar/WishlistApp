package project_group.project_name.features.ui.sample

import dev.inmo.micro_utils.language_codes.IetfLang
import dev.inmo.micro_utils.strings.buildStringResource

object SampleStrings {
    val sample = buildStringResource("Sample") {
        IetfLang.Russian("Пример")
    }
}