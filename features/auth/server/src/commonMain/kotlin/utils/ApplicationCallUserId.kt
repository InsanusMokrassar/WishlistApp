package dev.inmo.wishlist.features.auth.server.utils

import dev.inmo.wishlist.features.users.common.models.UserId
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.RoutingContext

fun ApplicationCall.getCallerUserId(): UserId? {
    return principal<UserId>()
}

fun RoutingContext.getCallerUserId(): UserId? {
    return call.getCallerUserId()
}

suspend fun ApplicationCall.getCallerUserIdOrAnswerUnauthorized(): UserId? {
    return principal<UserId>() ?: null.also { respond(HttpStatusCode.Unauthorized) }
}

suspend fun RoutingContext.getCallerUserIdOrAnswerUnauthorized(): UserId? {
    return call.getCallerUserIdOrAnswerUnauthorized()
}
