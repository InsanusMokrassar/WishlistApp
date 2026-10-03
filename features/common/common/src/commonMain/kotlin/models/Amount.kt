package dev.inmo.wishlist.features.common.common.models

import dev.inmo.micro_utils.common.fixed
import dev.inmo.wishlist.features.common.common.utils.toDecimalString
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.jsonPrimitive
import kotlin.jvm.JvmInline
import kotlin.math.absoluteValue
import kotlin.math.pow
import kotlin.math.truncate

/**
 * Fixed-point decimal value backed by unsigned integer and fractional magnitudes plus sign and scale.
 *
 * The fractional magnitude is a raw digit sequence whose [decimalPlaces] retains leading zeroes, so
 * `0.05` has decimalPart `5uL` and scale `2`. Exact fixed-point operations preserve that scale.
 *
 * @property integerPart The integer portion of the amount.
 * @property decimalPart The fractional portion as an unsigned raw digit sequence.
 */
@Serializable
data class Amount(
    val integerPart: ULong,
    val decimalPart: ULong,
    /** Whether the represented amount is negative. */
    val negative: Boolean,
    /** Number of fractional decimal digits retained by [decimalPart]. */
    val decimalPlaces: Int = decimalPart.decimalDigitCount()
) : Comparable<Amount> {
    init {
        require(decimalPlaces >= decimalPart.decimalDigitCount()) {
            "decimalPlaces must fit decimalPart"
        }
    }

    /** Multiplier that applies the stored sign to numeric conversions. */
    val signMultiplier = if (negative) -1 else 1

    /** Prefix used by [toString] to render a negative amount. */
    val signStringPrefix = if (negative) "-" else ""

    /**
     * Constructs an [Amount] from a [Double], extracting integer and fractional parts.
     *
     * @param amount Source double value.
     */
    constructor(amount: Double) : this(amount.toAmountParts())

    private constructor(parts: AmountParts) : this(
        integerPart = parts.integerPart,
        decimalPart = parts.decimalPart,
        negative = parts.negative,
        decimalPlaces = parts.decimalPlaces
    )

    /** Constructs an [Amount] from a [Float]. */
    constructor(amount: Float) : this(amount.toDouble())

    /** Constructs an [Amount] from a [Long]. */
    constructor(amount: Long) : this(amount.toDouble())

    /** Constructs an [Amount] from an [Int]. */
    constructor(amount: Int) : this(amount.toDouble())

    /** Constructs an [Amount] from a [Short]. */
    constructor(amount: Short) : this(amount.toDouble())

    /** Constructs an [Amount] from a [Byte]. */
    constructor(amount: Byte) : this(amount.toDouble())

    /** Returns this amount as a [Double]. */
    fun toDouble(): Double = signMultiplier * (
            integerPart.toDouble() + decimalPart.toDouble() / 10.0.pow(decimalPlaces)
            )

    /** Returns this amount as a [Float]. */
    fun toFloat(): Float = toDouble().toFloat()

    /** Returns this amount truncated to [Long]. */
    fun toLong(): Long = toDouble().toLong()

    /** Returns this amount truncated to [Int]. */
    fun toInt(): Int = toDouble().toInt()

    /** Returns this amount truncated to [Short]. */
    fun toShort(): Short = toInt().toShort()

    /** Returns this amount truncated to [Byte]. */
    fun toByte(): Byte = toInt().toByte()

    /**
     * Compares by integer part first, then decimal part.
     *
     * @param other Amount to compare against.
     * @return Negative, zero, or positive as per [Comparable] contract.
     */
    override fun compareTo(other: Amount): Int {
        if (negative != other.negative) {
            return if (negative) -1 else 1
        }

        val magnitudeComparison = integerPart.compareTo(other.integerPart).takeIf { it != 0 }
            ?: compareFractionalParts(decimalPart, decimalPlaces, other.decimalPart, other.decimalPlaces)

        return if (negative) -magnitudeComparison else magnitudeComparison
    }

    /** Returns sum of this and [other] amount. */
    operator fun plus(other: Amount): Amount {
        val resultDecimalPlaces = maxOf(decimalPlaces, other.decimalPlaces)
        val decimalScale = powerOfTen(resultDecimalPlaces)
        val thisDecimalScale = powerOfTen(resultDecimalPlaces - decimalPlaces)
        val otherDecimalScale = powerOfTen(resultDecimalPlaces - other.decimalPlaces)
        val thisMagnitude = integerPart.toULong() * decimalScale + decimalPart * thisDecimalScale
        val otherMagnitude = other.integerPart.toULong() * decimalScale + other.decimalPart * otherDecimalScale

        val (resultMagnitude, resultNegative) = when {
            negative == other.negative -> thisMagnitude + otherMagnitude to negative
            thisMagnitude >= otherMagnitude -> thisMagnitude - otherMagnitude to negative
            else -> otherMagnitude - thisMagnitude to other.negative
        }

        return amountFromMagnitude(resultMagnitude, resultDecimalPlaces, resultNegative)
    }

    /** Returns sum of this amount and a [Double]. */
    operator fun plus(other: Number): Amount = plus(Amount(other.toDouble()))

    /** Returns difference of this and [other] amount. */
    operator fun minus(other: Amount): Amount = plus(-other)

    /** Returns difference of this amount and a [Double]. */
    operator fun minus(other: Number): Amount = plus(-Amount(other.toDouble()))

    /** Returns this amount multiplied by [other]. */
    operator fun times(other: Number): Amount {
        val otherAsString = when (other) {
            is Byte, is Short, is Int, is Long -> other.toString()
            else -> other.toDouble().toDecimalString(14)
        }
        val resultNegative = negative != otherAsString.startsWith('-')
        val otherMagnitudeString = otherAsString.removePrefix("-")
        val otherIntegerPart = otherMagnitudeString.substringBefore('.').toULong()
        val otherDecimalPartString = otherMagnitudeString.substringAfter('.', "")
        val otherDecimalPart = otherDecimalPartString.toULongOrNull() ?: 0uL

        val otherDecimalPlaces = otherDecimalPartString.length
        val thisDecimalScale = powerOfTen(decimalPlaces)
        val otherDecimalScale = powerOfTen(otherDecimalPlaces)
        val resultDecimalPlaces = decimalPlaces + otherDecimalPlaces
        val thisMagnitude = integerPart.toULong() * thisDecimalScale + decimalPart
        val otherMagnitude = otherIntegerPart * otherDecimalScale + otherDecimalPart
        val resultMagnitude = thisMagnitude * otherMagnitude

        return amountFromMagnitude(resultMagnitude, resultDecimalPlaces, resultNegative)
    }

    /** Returns this amount divided by [other]. */
    operator fun div(other: Number): Amount = Amount(toDouble() / other.toDouble())

    /** Returns the remainder after dividing this amount by [i]. */
    operator fun rem(i: Number): Amount = Amount((toDouble() % i.toDouble()))

    override fun equals(other: Any?): Boolean {
        return this === other || (
                other is Amount &&
                        negative == other.negative &&
                        integerPart == other.integerPart &&
                        decimalPart == other.decimalPart &&
                        decimalPlaces == other.decimalPlaces
                )
    }

    /** Returns the same magnitude with the opposite sign. */
    operator fun unaryMinus(): Amount = Amount(
        integerPart = integerPart,
        decimalPart = decimalPart,
        negative = !negative,
        decimalPlaces = decimalPlaces
    )

    override fun hashCode(): Int {
        var result = integerPart.hashCode()
        result = 31 * result + decimalPart.hashCode()
        result = 31 * result + negative.hashCode()
        result = 31 * result + decimalPlaces
        return result
    }


    /** Returns decimal string representation, omitting `.0` fractional part when [decimalPart] is zero. */
    override fun toString(): String = if (decimalPart == 0uL) {
        "$signStringPrefix$integerPart"
    } else {
        "$signStringPrefix$integerPart.${decimalPart.toString().padStart(decimalPlaces, '0')}"
    }

    /** Provides Amount parsing, serialization, and the zero constant. */
    companion object : KSerializer<Amount> {
        /** Amount representing zero. */
        val ZERO = Amount(0)

        /** Parses a signed fixed-point decimal string while retaining its fractional scale. */
        fun fromString(string: String): Amount {
            val negative = string.startsWith('-')
            val magnitudeString = string.removePrefix("-")
            val decimalPartString = magnitudeString.substringAfter('.', "")

            return Amount(
                integerPart = magnitudeString.substringBefore('.').toULong(),
                decimalPart = if (decimalPartString.isEmpty()) 0uL else decimalPartString.toULong(),
                negative = negative,
                decimalPlaces = decimalPartString.length
            )
        }

        override val descriptor: SerialDescriptor
            get() = PrimitiveSerialDescriptor("Amount", PrimitiveKind.STRING)

        override fun serialize(
            encoder: Encoder,
            value: Amount
        ) {
            encoder.encodeString(value.toString())
        }

        override fun deserialize(decoder: Decoder): Amount {
            return when (decoder) {
                is JsonDecoder -> {
                    fromString(
                        decoder.decodeJsonElement().jsonPrimitive.content
                    )
                }
                else -> fromString(
                    decoder.decodeString()
                )
            }
        }
    }
}

private data class AmountParts(
    val integerPart: ULong,
    val decimalPart: ULong,
    val negative: Boolean,
    val decimalPlaces: Int
)

private fun Double.toAmountParts(): AmountParts {
    val decimalPartString = (this % 1)
        .absoluteValue
        .fixed(14)
        .toDecimalString(14)
        .substringAfter('.', "")

    return AmountParts(
        integerPart = truncate(this).toInt().absoluteValue.toULong(),
        decimalPart = decimalPartString.toULongOrNull() ?: 0uL,
        negative = this < 0.0,
        decimalPlaces = decimalPartString.length
    )
}

private fun ULong.decimalDigitCount(): Int {
    var remaining = this
    var result = 0
    while (remaining != 0uL) {
        remaining /= 10uL
        result++
    }
    return result
}

private fun compareFractionalParts(
    first: ULong,
    firstDecimalPlaces: Int,
    second: ULong,
    secondDecimalPlaces: Int
): Int {
    if (first == 0uL || second == 0uL) return first.compareTo(second)

    val firstLeadingZeros = firstDecimalPlaces - first.decimalDigitCount()
    val secondLeadingZeros = secondDecimalPlaces - second.decimalDigitCount()
    secondLeadingZeros.compareTo(firstLeadingZeros).let { if (it != 0) return it }

    var firstPlace = first.highestDecimalPlace()
    var secondPlace = second.highestDecimalPlace()

    while (firstPlace != 0uL || secondPlace != 0uL) {
        val firstDigit = if (firstPlace == 0uL) 0uL else first / firstPlace % 10uL
        val secondDigit = if (secondPlace == 0uL) 0uL else second / secondPlace % 10uL
        firstDigit.compareTo(secondDigit).let { if (it != 0) return it }

        firstPlace /= 10uL
        secondPlace /= 10uL
    }

    return 0
}

private fun ULong.highestDecimalPlace(): ULong {
    if (this == 0uL) return 0uL

    var remaining = this
    var place = 1uL
    while (remaining >= 10uL) {
        remaining /= 10uL
        place *= 10uL
    }
    return place
}

private fun powerOfTen(power: Int): ULong {
    var result = 1uL
    repeat(power) {
        result *= 10uL
    }
    return result
}

private fun amountFromMagnitude(
    magnitude: ULong,
    decimalPlaces: Int,
    negative: Boolean
): Amount {
    val decimalScale = powerOfTen(decimalPlaces)
    var resultDecimalPart = magnitude % decimalScale
    var resultDecimalPlaces = decimalPlaces
    while (resultDecimalPart != 0uL && resultDecimalPart % 10uL == 0uL) {
        resultDecimalPart /= 10uL
        resultDecimalPlaces--
    }
    if (resultDecimalPart == 0uL) {
        resultDecimalPlaces = 0
    }

    return Amount(
        integerPart = (magnitude / decimalScale),
        decimalPart = resultDecimalPart,
        negative = negative && magnitude != 0uL,
        decimalPlaces = resultDecimalPlaces
    )
}

