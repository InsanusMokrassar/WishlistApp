package dev.inmo.wishlist.features.files.client.utils

/**
 * Full request path for the shared temporal-upload endpoint, as used by [dev.inmo.micro_utils.ktor.client.tempUpload].
 *
 * Platform-specific because `tempUpload` reaches the server differently per platform:
 * - JS uses a raw `XMLHttpRequest` that bypasses the Ktor client plugins, so the `/api` prefix must be
 *   baked into the path here (the request resolves against the page origin).
 * - JVM/Android send through the shared Ktor [io.ktor.client.HttpClient], where
 *   `DefaultUrlHttpClientConfigurator` already appends `/api` to the base URL, so the path stays bare
 *   to avoid a doubled `/api/api/...`.
 *
 * @return The endpoint path to pass to `tempUpload`.
 */
expect fun temporalUploadFullPath(): String
