package dev.inmo.wishlist.features.common.common.models

import dev.inmo.micro_utils.common.fixed
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline
import kotlin.math.truncate

/**
 * Fixed-point decimal value backed by separate integer and fractional parts.
 *
 * Stored as [Pair] of [Long] (integer part) and [ULong] (fractional digits after the decimal point,
 * treated as a raw digit string, e.g. `0.05` → decimalPart = `5uL`).
 * Arithmetic is performed via [Double] conversion, so results may not be exact for very large values.
 *
 * @property integerPart The integer portion of the amount.
 * @property decimalPart The fractional portion as an unsigned raw digit sequence.
 */
@Serializable
@JvmInline
value class Amount(
    private val intDecimalPair: Pair<Long, ULong>
) : Comparable<Amount> {
    /** Integer portion of this amount. */
    val integerPart: Long
        get() = intDecimalPair.first

    /** Fractional portion as raw digit sequence (e.g. `5` represents `.05` when width is 2). */
    val decimalPart: ULong
        get() = intDecimalPair.second

    /**
     * Constructs an [Amount] from explicit integer and decimal parts.
     *
     * @param integerPart Integer portion.
     * @param decimalPart Fractional digit sequence.
     */
    constructor(
        integerPart: Long,
        decimalPart: ULong
    ) : this(
        integerPart to decimalPart
    )

    /**
     * Constructs an [Amount] from a [Double], extracting integer and fractional parts.
     *
     * @param amount Source double value.
     */
    constructor(amount: Double) : this(
        truncate(amount).toLong(),
        (amount % 1).fixed(14).toString().dropWhile { it != '.' }.drop(1).takeIf { it.isNotEmpty() } ?.toULong() ?: 0uL
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
    fun toDouble(): Double = "$integerPart.$decimalPart".toDouble()

    /** Returns this amount as a [Float]. */
    fun toFloat(): Float = "$integerPart.$decimalPart".toFloat()

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
    override fun compareTo(other: Amount): Int = integerPart.compareTo(other.integerPart).takeIf {
        it != 0
    } ?: decimalPart.compareTo(other.decimalPart)

    /** Returns sum of this and [other] amount. */
    operator fun plus(other: Amount): Amount = Amount(toDouble() + other.toDouble())

    /** Returns sum of this amount and a [Double]. */
    operator fun plus(other: Double): Amount = Amount(toDouble() + other.toDouble())

    /** Returns difference of this and [other] amount. */
    operator fun minus(other: Amount): Amount = Amount(toDouble() - other.toDouble())

    /** Returns difference of this amount and a [Double]. */
    operator fun minus(other: Double): Amount = Amount(toDouble() - other)

    /** Returns this amount multiplied by [other]. */
    operator fun times(other: Double): Amount = Amount(toDouble() * other)

    /** Returns this amount divided by [other]. */
    operator fun div(other: Double): Amount = Amount(toDouble() / other)

    /** Returns decimal string representation, omitting `.0` fractional part when [decimalPart] is zero. */
    override fun toString(): String = if (decimalPart == 0uL) {
        integerPart.toString()
    } else {
        "$integerPart.$decimalPart"
    }

    companion object {
        /** Amount representing zero. */
        val ZERO = Amount(0)
    }
}
