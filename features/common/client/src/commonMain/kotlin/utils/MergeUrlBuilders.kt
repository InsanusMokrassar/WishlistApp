package dev.inmo.wishlist.features.common.client.utils

import io.ktor.http.URLBuilder

/**
 * Updates the properties of the current [URLBuilder] instance with values from the specified `defaultValues`
 * instance if those properties are not already set or are in an invalid initial state.
 *
 * @param defaultValues The [URLBuilder] instance containing default values to fill in for missing or invalid
 *                       parts of the [this] [URLBuilder] instance.
 */
fun URLBuilder.fillAbsentPartsWith(defaultValues: URLBuilder) {
    protocolOrNull = protocolOrNull ?: defaultValues.protocolOrNull
    host = host.takeIf { it.isNotEmpty() } ?: defaultValues.host
    port = port.takeIf { it != 0 } ?: defaultValues.port
    encodedPathSegments = encodedPathSegments.takeIf { it.isNotEmpty() } ?: defaultValues.encodedPathSegments
    encodedUser = encodedUser ?: defaultValues.encodedUser
    encodedPassword = encodedPassword ?: defaultValues.encodedPassword
    encodedParameters = encodedParameters.takeIf { it.isEmpty() == false } ?: defaultValues.encodedParameters
    encodedFragment = encodedFragment.takeIf { it.isNotEmpty() } ?: defaultValues.encodedFragment
    trailingQuery = trailingQuery || defaultValues.trailingQuery
}
