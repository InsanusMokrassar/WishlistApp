package dev.inmo.wishlist.features.common.server.utils

import io.ktor.server.application.ApplicationCall
import io.ktor.server.plugins.statuspages.StatusPagesConfig
import io.ktor.server.request.httpMethod
import io.ktor.server.response.respond
import io.ktor.server.plugins.statuspages.exception
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException
import org.slf4j.LoggerFactory

/**
 * Formats an HTTP request log line without path, query, headers, body, exception text, or Location.
 *
 * @param call Completed server call.
 * @return Method and final numeric status only.
 */
fun safeCallLogLine(call: ApplicationCall): String =
    "${call.request.httpMethod.value} ${call.response.status()?.value ?: 0}"

/**
 * Converts unexpected application failures into a status-only public response and application log.
 *
 * Cancellation remains a control-flow signal and is deliberately rethrown. Ordinary failures are
 * neither exposed in the response nor passed to SLF4J, preventing URL, request-body, and repository
 * detail from reaching the application error logger.
 */
fun StatusPagesConfig.installSanitizedUnhandledErrorBoundary() {
    exception<Throwable> { call, cause ->
        if (cause is CancellationException) throw cause
        call.respond(HttpStatusCode.InternalServerError)
        LoggerFactory.getLogger("Ktor").error(safeCallLogLine(call))
    }
}
