package dev.inmo.wishlist.features.wishlist.common.models

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.jvm.JvmInline

/**
 * Relative importance of a [WishlistItem], expressed as a numeric [weight].
 *
 * Higher [weight] means higher priority. Three named presets are provided —
 * [Small] (`0`), [Medium] (`50`) and [High] (`100`) — plus a [Custom] variant
 * that carries an arbitrary caller-supplied weight.
 *
 * Serialized as its [weight] alone (a single signed 64-bit integer / [Long]) via
 * [PrioritySerializer]; on decode the weight is mapped back to a named preset when it matches
 * one, otherwise to [Custom]. As a consequence a [Custom] holding a preset weight (e.g. `0`)
 * round-trips to the matching preset ([Small]).
 */
@Serializable(with = PrioritySerializer::class)
sealed interface Priority {
    /** Numeric importance; higher means more important. */
    val weight: UInt

    /** Lowest priority preset; [weight] is `0`. */
    @Serializable
    data object Small : Priority {
        override val weight: UInt get() = 0u
    }

    /** Middle priority preset; [weight] is `50`. */
    @Serializable
    data object Medium : Priority {
        override val weight: UInt get() = 50u
    }

    /** Highest named priority preset; [weight] is `100`. */
    @Serializable
    data object High : Priority {
        override val weight: UInt get() = 100u
    }

    /**
     * Arbitrary-weight priority.
     *
     * @property weight Caller-provided importance value.
     */
    @JvmInline
    @Serializable
    value class Custom(override val weight: UInt) : Priority

    companion object {
        /** All named presets, ordered from lowest to highest [weight]. */
        val presets: List<Priority> = listOf(Small, Medium, High)

        /**
         * Resolves a [Priority] from a raw [weight], preferring a named preset when the
         * weight matches one, otherwise wrapping it in [Custom].
         *
         * @param weight Raw importance value.
         * @return Matching preset, or a [Custom] holding [weight].
         */
        fun fromWeight(weight: UInt): Priority = when (weight) {
            Small.weight -> Small
            Medium.weight -> Medium
            High.weight -> High
            else -> Custom(weight)
        }
    }
}

/**
 * Serializes [Priority] as its raw [Priority.weight] reduced to a single [Long], reconstructing
 * the concrete variant through [Priority.fromWeight] on decode.
 *
 * The [UInt] weight is widened to [Long] on encode and narrowed back with [Long.toUInt] on
 * decode; weights always fit losslessly because [UInt.MAX_VALUE] is within [Long] range.
 */
object PrioritySerializer : KSerializer<Priority> {
    /** Primitive [PrimitiveKind.LONG] descriptor — [Priority] is wire-encoded as a plain number. */
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
        "wishlist_item_priority",
        PrimitiveKind.LONG
    )

    /**
     * Writes [value]'s [Priority.weight] as a [Long] via [Encoder.encodeLong].
     *
     * @param encoder Target encoder.
     * @param value Priority to encode.
     */
    override fun serialize(encoder: Encoder, value: Priority) {
        encoder.encodeLong(value.weight.toLong())
    }

    /**
     * Reads a [Long] weight via [Decoder.decodeLong] and resolves it to a [Priority]
     * through [Priority.fromWeight] (after narrowing to [UInt]).
     *
     * @param decoder Source decoder.
     * @return Decoded priority.
     */
    override fun deserialize(decoder: Decoder): Priority =
        Priority.fromWeight(decoder.decodeLong().toUInt())
}
