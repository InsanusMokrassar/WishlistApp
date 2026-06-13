package dev.inmo.wishlist.features.common.client.utils

import io.ktor.http.URLBuilder
import io.ktor.http.append
import io.ktor.http.clone
import io.ktor.util.appendAll

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

fun URLBuilder.appendOrSetPartsWith(defaultValues: URLBuilder) {
    val localCopyOfDefaults = defaultValues.clone()
    protocolOrNull = protocolOrNull ?: localCopyOfDefaults.protocolOrNull
    host = host.takeIf { it.isNotEmpty() } ?: localCopyOfDefaults.host
    port = port.takeIf { it != 0 } ?: localCopyOfDefaults.port
    encodedPathSegments = encodedPathSegments + localCopyOfDefaults.encodedPathSegments
    encodedUser = encodedUser ?: localCopyOfDefaults.encodedUser
    encodedPassword = encodedPassword ?: localCopyOfDefaults.encodedPassword
    val defaultEncodedParameters = localCopyOfDefaults.encodedParameters
    encodedParameters.entries().forEach {
        defaultEncodedParameters.appendAll(it.key, it.value)
    }
    encodedParameters = defaultEncodedParameters
    encodedFragment = encodedFragment.takeIf { it.isNotEmpty() } ?: localCopyOfDefaults.encodedFragment
    trailingQuery = trailingQuery || localCopyOfDefaults.trailingQuery
}

fun URLBuilder.set(from: URLBuilder) {
    protocolOrNull = from.protocolOrNull
    host = from.host
    port = from.port
    encodedPathSegments = from.encodedPathSegments
    encodedUser = from.encodedUser
    encodedPassword = from.encodedPassword
    encodedParameters = from.encodedParameters
    encodedFragment = from.encodedFragment
    trailingQuery = from.trailingQuery
}
