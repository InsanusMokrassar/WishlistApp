package dev.inmo.wishlist.features.roles.common.utils

import dev.inmo.kroles.repos.BaseRoleSubject
import dev.inmo.kroles.roles.BaseRole
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object RolesSubjectsSerializer : KSerializer<BaseRoleSubject> {
    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor("role_subject", PrimitiveKind.STRING)

    override fun serialize(
        encoder: Encoder,
        value: BaseRoleSubject
    ) {
        val prefix = when (value) {
            is BaseRoleSubject.Direct -> "d"
            is BaseRoleSubject.OtherRole -> "r"
        }
        encoder.encodeString("$prefix:${value.rawValue}")
    }

    override fun deserialize(decoder: Decoder): BaseRoleSubject {
        val rawValueWithPrefix = decoder.decodeString()
        val prefix = rawValueWithPrefix.takeWhile { it != ':' }
        val rawValue = rawValueWithPrefix.takeLastWhile { it != ':' }
        return when (prefix) {
            "d" -> BaseRoleSubject.Direct(rawValue)
            "r" -> BaseRoleSubject.OtherRole(BaseRole(rawValue))
            else -> error("Unknown prefix for role: '$prefix'")
        }
    }

}