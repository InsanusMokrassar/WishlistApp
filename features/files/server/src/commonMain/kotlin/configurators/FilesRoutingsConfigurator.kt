package dev.inmo.wishlist.features.files.server.configurators

import dev.inmo.micro_utils.ktor.server.configurators.ApplicationRoutingConfigurator
import dev.inmo.wishlist.features.auth.server.utils.getCallerUserIdOrAnswerUnauthorized
import dev.inmo.wishlist.features.files.common.Constants
import dev.inmo.wishlist.features.files.common.models.FileId
import dev.inmo.wishlist.features.files.common.models.FinalizeFileRequest
import dev.inmo.wishlist.features.files.server.services.FilesService
import dev.inmo.wishlist.features.simpleRoles.server.SimpleRolesFeature
import dev.inmo.wishlist.features.users.common.models.UserId
import io.ktor.http.ContentDisposition
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route

/**
 * Ktor routing configurator registering the files endpoints under `/files`.
 *
 * **Public routes** (no auth):
 * - `GET /files/{id}` — streams the raw payload with its stored `Content-Type`
 * - `GET /files/meta/{id}` — returns the [dev.inmo.wishlist.features.files.common.models.RegisteredFileMetaInfo] as JSON
 *
 * **Auth-required route** (valid bearer token):
 * - `POST /files/finalize` — promotes a temporal upload into permanent storage; body: [FinalizeFileRequest]
 *
 * Status codes: `200 OK` on success, `400 Bad Request` on malformed id / rejected finalize
 * (missing temp file or non-image MIME), `404 Not Found` when a file id is unknown,
 * `401 Unauthorized` when the bearer token is absent on finalize.
 *
 * The shared temporal upload endpoint (`POST /temp_upload`) is registered separately via the
 * MicroUtils `TemporalFilesRoutingConfigurator` in the feature [dev.inmo.wishlist.features.files.server.Plugin].
 *
 * @param filesService Service performing storage, metadata and ownership work.
 * @param simpleRolesFeature Used to resolve whether the authenticated caller holds the SuperAdmin role
 * when authorizing avatar changes for another user.
 */
class FilesRoutingsConfigurator(
    private val filesService: FilesService,
    private val simpleRolesFeature: SimpleRolesFeature
) : ApplicationRoutingConfigurator.Element {
    override fun Route.invoke() {
        route(Constants.filesPrefixPathPart) {
            get("${Constants.metaPathPart}/{id}") {
                val id = call.parameters["id"]?.let(::FileId) ?: run {
                    call.respond(HttpStatusCode.BadRequest)
                    return@get
                }
                val meta = filesService.getMeta(id) ?: run {
                    call.respond(HttpStatusCode.NotFound)
                    return@get
                }
                call.respond(meta)
            }
            get("{id}") {
                val id = call.parameters["id"]?.let(::FileId) ?: run {
                    call.respond(HttpStatusCode.BadRequest)
                    return@get
                }
                val meta = filesService.getMeta(id) ?: run {
                    call.respond(HttpStatusCode.NotFound)
                    return@get
                }
                val bytes = filesService.getBytes(id) ?: run {
                    call.respond(HttpStatusCode.NotFound)
                    return@get
                }
                call.response.headers.append("X-Content-Type-Options", "nosniff")
                call.response.headers.append(
                    HttpHeaders.ContentDisposition,
                    ContentDisposition.Attachment
                        .withParameter(ContentDisposition.Parameters.FileName, meta.fileName.string)
                        .toString()
                )
                call.respondBytes(bytes, ContentType.parse(meta.mimeType))
            }
            get("${Constants.avatarPathPart}/{userId}") {
                val userId = call.parameters["userId"]?.toLongOrNull()?.let(::UserId) ?: run {
                    call.respond(HttpStatusCode.BadRequest)
                    return@get
                }
                val fileId = filesService.getAvatar(userId) ?: run {
                    call.respond(HttpStatusCode.NotFound)
                    return@get
                }
                call.respond(fileId)
            }
            authenticate {
                post(Constants.finalizePathPart) {
                    val callerId = getCallerUserIdOrAnswerUnauthorized() ?: return@post
                    val request = call.receive<FinalizeFileRequest>()
                    val result = filesService.finalize(request, callerId)
                    if (result == null) {
                        call.respond(HttpStatusCode.BadRequest)
                    } else {
                        call.respond(result)
                    }
                }
                put("${Constants.avatarPathPart}/{userId}") {
                    val callerId = getCallerUserIdOrAnswerUnauthorized() ?: return@put
                    val userId = call.parameters["userId"]?.toLongOrNull()?.let(::UserId) ?: run {
                        call.respond(HttpStatusCode.BadRequest)
                        return@put
                    }
                    if (callerId != userId && !simpleRolesFeature.isSuperAdmin(callerId)) {
                        call.respond(HttpStatusCode.Forbidden)
                        return@put
                    }
                    val fileId = call.receive<FileId>()
                    if (filesService.setAvatar(userId, fileId)) {
                        call.respond(HttpStatusCode.OK)
                    } else {
                        call.respond(HttpStatusCode.BadRequest)
                    }
                }
            }
        }
    }
}
