package dev.inmo.wishlist.features.email.common.models

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * A validated email address represented as an inline value class.
 *
 * Construction via [invoke] trims the input and validates it against RFC-ish rules.
 * Use [parse] for a non-throwing alternative that returns a [Result].
 *
 * Serialization uses [EmailSerializer], which round-trips the address as a bare JSON string
 * and re-validates on deserialization — malformed wire values fail fast.
 *
 * @property string Trimmed, validated email address string.
 */
@Serializable(with = EmailSerializer::class)
@JvmInline
value class Email private constructor(val string: String) {

    companion object {
        /** Maximum allowed email length per RFC 5321. */
        private const val MAX_LENGTH = 254

        /**
         * Permissive RFC-ish email pattern.
         * Local part: alphanumeric + `!#$%&'*+/=?^_`{|}~.-`
         * Domain: label(s) separated by dots, each containing alphanumeric and hyphens.
         */
        private val REGEX: Regex = Regex(
            """^[A-Za-z0-9!#${'$'}%&'*+/=?^_`{|}~.\-]+@[A-Za-z0-9\-]+(\.[A-Za-z0-9\-]+)+$"""
        )

        /**
         * Checks whether [value] is a valid email string (trimmed, non-blank, within [MAX_LENGTH],
         * matching [REGEX]).
         *
         * @param value Raw input to validate.
         * @return `true` if valid; `false` otherwise.
         */
        fun isValid(value: String): Boolean {
            val trimmed = value.trim()
            return trimmed.isNotBlank() && trimmed.length <= MAX_LENGTH && REGEX.matches(trimmed)
        }

        /**
         * Constructs a validated [Email], trimming whitespace from [value].
         *
         * @param value Raw email string to validate and wrap.
         * @return Validated [Email] instance.
         * @throws IllegalArgumentException when [value] fails validation.
         */
        operator fun invoke(value: String): Email {
            val trimmed = value.trim()
            require(isValid(trimmed)) { "Invalid email address: $trimmed" }
            return Email(trimmed)
        }

        /**
         * Non-throwing alternative to [invoke].
         *
         * @param value Raw email string to parse.
         * @return [Result.success] wrapping the [Email], or [Result.failure] with an
         *   [IllegalArgumentException] when invalid.
         */
        fun parse(value: String): Result<Email> = runCatching { invoke(value) }
    }
}

/**
 * KSerializer for [Email] that encodes/decodes a plain JSON string and re-validates on decode.
 *
 * Ensures that any malformed value reaching the wire layer fails fast at deserialization rather than
 * silently producing an invalid [Email].
 */
object EmailSerializer : KSerializer<Email> {
    /** Serial descriptor identifying this as a primitive STRING with the Email FQN. */
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor(
            "dev.inmo.wishlist.features.email.common.models.Email",
            PrimitiveKind.STRING
        )

    /**
     * Encodes [value] as its raw string representation.
     *
     * @param encoder Target encoder.
     * @param value [Email] to serialize.
     */
    override fun serialize(encoder: Encoder, value: Email) {
        encoder.encodeString(value.string)
    }

    /**
     * Decodes a string from [decoder] and constructs an [Email], validating on the way.
     *
     * @param decoder Source decoder.
     * @return Validated [Email].
     * @throws IllegalArgumentException when the decoded string is not a valid email.
     */
    override fun deserialize(decoder: Decoder): Email = Email(decoder.decodeString())
}
