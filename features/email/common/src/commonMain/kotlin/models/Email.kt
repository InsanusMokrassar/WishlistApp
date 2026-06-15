package dev.inmo.wishlist.features.email.common.models

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.jvm.JvmInline

/**
 * Validated e-mail address.
 *
 * A value class that guarantees, by construction, that the wrapped [raw] string is a non-blank,
 * RFC-ish well-formed e-mail address. Instances can only be obtained through the validating factories
 * on the companion object ([invoke] / [parse]) — the primary constructor is `private`, so an [Email]
 * carrying a malformed address can never exist.
 *
 * Serialized as a plain JSON string (via [EmailSerializer]) so the wire format is a bare address and any
 * received value is re-validated on decode.
 *
 * @property raw The validated, trimmed e-mail address string.
 */
@Serializable(with = EmailSerializer::class)
@JvmInline
value class Email private constructor(val raw: String) {
    companion object {
        /**
         * Upper bound on the total length of an e-mail address per the SMTP/RFC 5321 path limit.
         */
        private const val MAX_LENGTH = 254

        /**
         * Basic RFC-ish e-mail shape: a non-empty local part, a single `@`, and a dotted domain with at
         * least one dot. Intentionally permissive on the local part while rejecting whitespace, missing
         * `@`, and dot-less domains.
         */
        private val REGEX = Regex(
            "^[A-Za-z0-9!#\$%&'*+/=?^_`{|}~.-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)+\$"
        )

        /**
         * Checks whether [value] (after trimming) is a structurally valid e-mail address.
         *
         * @param value Candidate address string.
         * @return `true` when [value] is non-blank, within [MAX_LENGTH], and matches [REGEX].
         */
        fun isValid(value: String): Boolean {
            val trimmed = value.trim()
            return trimmed.isNotBlank() && trimmed.length <= MAX_LENGTH && REGEX.matches(trimmed)
        }

        /**
         * Validating factory.
         *
         * @param value Candidate address string; leading/trailing whitespace is trimmed.
         * @return The constructed [Email].
         * @throws IllegalArgumentException When [value] is blank or malformed.
         */
        operator fun invoke(value: String): Email {
            val trimmed = value.trim()
            require(isValid(trimmed)) { "Invalid email address: \"$value\"" }
            return Email(trimmed)
        }

        /**
         * Non-throwing validating factory.
         *
         * @param value Candidate address string; leading/trailing whitespace is trimmed.
         * @return [Result.success] with the [Email] when valid, otherwise [Result.failure] holding the
         * thrown [IllegalArgumentException].
         */
        fun parse(value: String): Result<Email> = runCatching { invoke(value) }
    }
}

/**
 * Serializes [Email] as a primitive JSON string and re-validates on decode.
 *
 * Encoding writes [Email.raw] directly; decoding routes the incoming string through the validating
 * [Email] factory, so deserializing a malformed address fails fast with an [IllegalArgumentException].
 */
object EmailSerializer : KSerializer<Email> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("dev.inmo.wishlist.features.email.common.models.Email", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Email) {
        encoder.encodeString(value.raw)
    }

    override fun deserialize(decoder: Decoder): Email = Email(decoder.decodeString())
}
