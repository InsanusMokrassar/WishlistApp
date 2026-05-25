package project_group.project_name.features.auth.common.models

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
@JvmInline
value class Password(val string: String)
