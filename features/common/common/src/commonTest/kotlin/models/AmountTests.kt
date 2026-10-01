package dev.inmo.wishlist.features.common.common.models

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


class AmountTests {
    @Test
    fun addsAmounts() {
        val result = Amount(12.25) + Amount(7.5)
        val result2 = Amount(73.39267099) + Amount(360.0)

        assertEquals(Amount(19u, 75uL, false), result)
        assertEquals(Amount(433u, 39267099uL, false), result2)
    }

    @Test
    fun addsAmountsWhenDecimalPartExceedsDoublePrecision() {
        val highPrecision = Amount(12u, 9_007_199_254_740_993uL, false)

        assertEquals(
            Amount(20u, 1_007_199_254_740_993uL, false),
            highPrecision + Amount(7u, 2uL, false)
        )
        assertEquals(
            Amount(5u, 7_007_199_254_740_993uL, false),
            highPrecision + Amount(7u, 2uL, true)
        )
    }

    @Test
    fun addsNumbers() {
        val result = Amount(12.25) + 2

        assertEquals(Amount(14u, 25uL, false), result)
    }

    @Test
    fun subtractsAmounts() {
        assertEquals(
            Amount(10u, 25uL, false),
            Amount(12.75) - Amount(2.5)
        )
        assertEquals(
            Amount(0u, 5uL, true),
            Amount(1.25) - Amount(1.75)
        )
    }

    @Test
    fun subtractsNumbers() {
        val result = Amount(12.75) - 2L

        assertEquals(Amount(10u, 75uL, false), result)
    }

    @Test
    fun multipliesByNumbers() {
        assertEquals(Amount(9u, 0uL, false), Amount(2.25) * 4)
        assertEquals(Amount(0u, 5uL, true), Amount(0.25) * -2)
        assertEquals(Amount(3u, 375uL, false), Amount(2.25) * 1.5)
    }

    @Test
    fun multipliesWithoutDoublePrecisionLoss() {
        val highPrecision = Amount(12u, 9_007_199_254_740_993uL, false)

        assertEquals(
            Amount(25u, 8_014_398_509_481_986uL, false),
            highPrecision * 2
        )
        assertEquals(
            Amount(25u, 8_014_398_509_481_986uL, true),
            highPrecision * -2
        )
    }

    @Test
    fun dividesByNumbers() {
        assertEquals(Amount(3u, 25uL, false), Amount(9.75) / 3.0)
        assertEquals(Amount(0u, 25uL, true), Amount(0.75) / -3)
    }

    @Test
    fun calculatesRemainder() {
        assertEquals(Amount(1u, 75uL, false), Amount(10.75) % 3)
        assertEquals(Amount(0u, 25uL, true), Amount(-0.75) % 0.5)
    }

    @Test
    fun preservesLeadingDecimalZeros() {
        val value = Amount(-1.0001)

        assertEquals(1uL, value.decimalPart)
        assertEquals(4, value.decimalPlaces)
        assertEquals(-1.0001, value.toDouble(), 0.00000000000001)
        assertEquals("-1.0001", value.toString())
        assertEquals(value, Amount.fromString(value.toString()))
    }

    @Test
    fun calculatesWithLeadingDecimalZeros() {
        assertEquals(Amount(1.001), Amount(1.0001) + Amount(0.0009))
        assertEquals(Amount(0.0009), Amount(0.001) - Amount(0.0001))
        assertEquals(Amount(0.0002), Amount(0.0001) * 2)
        assertEquals(Amount(0.0001), Amount(0.001) / 10)
        assertEquals(Amount(0.0001), Amount(0.0011) % 0.001)
    }

    @Test
    fun comparesAmounts() {
        assertTrue(Amount(0.0) <= Amount(0.0))
        assertTrue(Amount(1.0001) < Amount(1.001))
        assertTrue(Amount(1.1) < Amount(1.25))
        assertTrue(Amount(1.25) < Amount(1.5))
        assertTrue(Amount(1.5) < Amount(1.75))
        assertTrue(Amount(1.25) < Amount(1.75))
        assertTrue(Amount(1.25) < Amount(1.75))
        assertTrue(Amount(1.25) < Amount(1.75))
        assertTrue(Amount(1.75) < Amount(2.25))
        assertTrue(Amount(-1.25) > Amount(-1.75))
        assertTrue(Amount(-1.75) > Amount(-2.25))
        assertTrue(Amount(-1.25) >= Amount(-1.25))
        assertTrue(Amount(-1.75) >= Amount(-1.75))
        assertTrue(Amount(-1.25) <= Amount(-1.25))
        assertTrue(Amount(-1.75) <= Amount(-1.75))
        assertEquals(0, Amount(2.25).compareTo(Amount(2.25)))
    }
}

