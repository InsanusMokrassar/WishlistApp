package dev.inmo.wishlist.features.auth.common.models

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
@JvmInline
value class Password(val string: String)
