package dev.inmo.wishlist.features.common.server.utils

import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.httpMethod

/**
 * Formats an HTTP request log line without path, query, headers, body, exception text, or Location.
 *
 * @param call Completed server call.
 * @return Method and final numeric status only.
 */
fun safeCallLogLine(call: ApplicationCall): String =
    "${call.request.httpMethod.value} ${call.response.status()?.value ?: 0}"
