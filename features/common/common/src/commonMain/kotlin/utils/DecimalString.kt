package dev.inmo.wishlist.features.common.common.utils

import dev.inmo.micro_utils.common.fixed
import kotlin.math.absoluteValue
import kotlin.math.pow

fun Double.toDecimalString(decimals: Int = 14, dropTrailingZeros: Boolean = true): String {
    val signPrefix = if (this < 0) "-" else ""
    val additionalFracPartMultiplier = 10.0.pow(decimals)
    val fracPart = (this % 1).fixed(decimals)
    val intPart = (this - fracPart).toLong().absoluteValue
    val intPartString = "$signPrefix$intPart"

    if (fracPart == 0.0) return intPartString
    val resultFracPart = (fracPart * additionalFracPartMultiplier).toLong().absoluteValue

    val fracPartAsString = resultFracPart.toString()
    val fracPartWithLeadingZeros = fracPartAsString.padStart(decimals, '0')
    val fracPartWithoutTrailingZeros = if (dropTrailingZeros) fracPartWithLeadingZeros.dropLastWhile { it == '0' } else fracPartWithLeadingZeros

    return "$intPartString.$fracPartWithoutTrailingZeros"
}


