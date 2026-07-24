# Feature: Files

## Operator Notes

<!-- Human operator writes here. Agents MUST read and respect before making any changes. Agents MUST NOT modify this section. -->

## Overview

Generic binary file storage with metadata. Stores file payloads on disk and metadata in Postgres. Provides a single shared temporal-upload endpoint (`POST /temp_upload`) reusable by any feature. Wishlist items consume the feature to attach images. File finalization is constrained to image MIME types only. Follows a two-step upload pattern: clients upload raw bytes to the shared temporal endpoint, receive a `TemporalFileId`, then call `/files/finalize` to move the temp file into permanent storage and record ownership.

## Routes

> All paths below are served under the global `/api` prefix applied by `features/common/server` (e.g. `/api/files/{id}`, `/api/temp_upload`). The client adds the prefix by appending `/api` to the configured server base URL (`DefaultUrlHttpClientConfigurator`), except browser-loaded image `<img src>` URLs (which bypass the shared client), for which `FilesClientService.apiFileUrl` bakes in `/api` directly.

| Method | Path | Auth | Body / Response | Description |
|--------|------|------|-----------------|-------------|
| POST | `/temp_upload` | None | multipart binary → `TemporalFileId` | Shared temporal upload endpoint (MicroUtils `TemporalFilesRoutingConfigurator`); single reusable entry point for all features |
| POST | `/files/finalize` | Bearer | `FinalizeFileRequest` → `FilesFeatureMetaInfo \| 400` | Promote temporal upload to permanent storage; returns `FilesFeatureMetaInfo` (200) or `400` when the temp file is missing/expired or the payload is not an allowlisted raster image (see Architecture Notes); owner recorded from bearer principal |
| GET | `/files/{id}` | None | → raw bytes with `Content-Type` \| 404 | Stream persisted file payload; no auth required. Sent with `X-Content-Type-Options: nosniff` and `Content-Disposition: attachment` so a direct hit cannot be MIME-sniffed or rendered inline as active content |
| GET | `/files/meta/{id}` | None | → `FilesFeatureMetaInfo` JSON \| 404 | Fetch file metadata; no auth required; 404 when unknown |
| GET | `/files/avatar/{userId}` | None | → `FileId` JSON \| 404 | Resolve a user's avatar file id; no auth required; 404 when the user has no avatar |
| PUT | `/files/avatar/{userId}` | Bearer | `FileId` → `200` \| `400` \| `403` | Associate an already-finalized file as the user's avatar; allowed only for the user themselves or a SuperAdmin; `400` when the file is unknown, `403` for any other caller |

## Models

| Type | Package | Description |
|------|---------|-------------|
| `FileId` | common | `@JvmInline value class(val string: String)` — server-generated UUID, primary key for binary and metadata stores |
| `FileMetaInfo` | common | Sealed interface with `fileName: FileName`, `mimeType: String`, `size: Long`, `uploaderId: UserId`; variants `NewFileMetaInfo` and `RegisteredFileMetaInfo(id, ...)` |
| `NewFileMetaInfo` | common | Temporary metadata before persistence (fileName, mimeType, size, uploaderId) |
| `RegisteredFileMetaInfo` | common | Persisted metadata with `id: FileId`; stored by `FilesMetaInfoRepo` — no longer returned directly by `FilesFeature`/`FilesService` (see `FilesFeatureMetaInfo`) |
| `FilesFeatureMetaInfo` | common | `@Serializable` feature model mirroring `RegisteredFileMetaInfo` verbatim, returned by `FilesFeature.finalize`/`getMeta` and the server-side `FilesService` equivalents instead of the persistence entity directly, per the Feature Interface Return Model Rule |
| `FinalizeFileRequest` | common | JSON body for `POST /files/finalize`: `temporalFileId: TemporalFileId`, `fileName: FileName`, `mimeType: String` |
| `FilesRepo` | common | Interface: `put(FileId, ByteArray)`, `get(FileId): ByteArray?`, `remove(FileId)` |
| `FilesMetaInfoRepo` | common | Interface: `KeyValueRepo<FileId, RegisteredFileMetaInfo>` |
| `UserAvatarsRepo` | common | Interface: `KeyValueRepo<UserId, FileId>` — maps a user to that user's avatar file; Exposed impl `ExposedUserAvatarsRepo` (`user_avatars` table: `user_id` long PK, `file_id` text) |
| `FilesFeature` | client | Client contract: `finalize(FinalizeFileRequest): FilesFeatureMetaInfo?`, `getMeta(FileId): FilesFeatureMetaInfo?`, `getAvatar(UserId)`, `setAvatar(UserId, FileId)` |
| `FilesClientService` | client | High-level client service: `uploadFile(MPPFile): FilesFeatureMetaInfo?`, `apiFileUrl(FileId)` (browser-ready `/api/files/{id}`), `fileUrl(FileId)` (bare `files/{id}` for the shared client), `downloadBytes(FileId)`, `getAvatar(UserId)`, `uploadAvatar(UserId, MPPFile)` |
| `KtorFilesFeature` | client | HTTP implementation of `FilesFeature` |

## Architecture Notes

- **Two-step upload**: Client uploads bytes to `/temp_upload` (shared `TemporalFilesRoutingConfigurator` from MicroUtils; JS implementation uses XMLHttpRequest for large-file support), receives `TemporalFileId`, then calls `/files/finalize` referencing that id.
- **Binary storage**: `DiskFilesRepo` persists payloads on disk under a directory from `FilesConfig.filesFolder` (decoded from root server config JSON; key `filesFolder`, default `./uploaded_files`). Guarded by `SmartRWLocker`.
- **Metadata storage**: `ExposedFilesMetaInfoRepo` (Exposed-backed `KeyValueRepo`) persists `RegisteredFileMetaInfo` as JSON text in the `files_meta` table (`file_id` text PK, `meta_json` text); mirrors `ExposedPasswordsRepo` pattern.
- **Finalize constraints**: `FilesService.finalize` accepts only an allowlist of raster image types (`image/png`, `image/jpeg`, `image/gif`, `image/bmp`, `image/webp`) AND verifies the payload's leading magic bytes match the declared type. Vector formats such as `image/svg+xml` (which can carry active script) and any byte/MIME mismatch are rejected. The temp file is always read then deleted before validation, so a rejected upload leaves nothing behind. Records the uploader from the authenticated `UserId` parameter.
- **Uploader field rename**: `FileMetaInfo.ownerId` was renamed to `uploaderId` (the field captures *who uploaded* a file, not domain ownership). NOTE: persisted `files_meta` JSON written before the rename uses the `ownerId` key and will not decode after the rename — wipe/migrate the `files_meta` table on existing dev databases.
- **User avatars**: `UserAvatarsRepo : KeyValueRepo<UserId, FileId>` (Exposed `ExposedUserAvatarsRepo`, table `user_avatars`) maps each user to one avatar file. `FilesService.getAvatar(userId)` / `setAvatar(userId, fileId)` back the routes; `setAvatar` rejects (`false`/`400`) a `FileId` with no finalized metadata. The `PUT /files/avatar/{userId}` route authorizes the caller as the user themselves or a SuperAdmin (resolved via `simpleRoles.server`'s `SimpleRolesFeature.isSuperAdmin`, injected into `FilesRoutingsConfigurator` — issue #68; the constructor no longer takes `ReadUsersRepo`, which this was its only use of); `GET /files/avatar/{userId}` is public so any profile view can render the avatar. Avatar upload reuses the normal finalize flow then associates the resulting id, so avatars are ordinary finalized files plus a mapping row.
- **Server-only service**: `FilesService` is server-only (caller `UserId` parameter on finalize) — not bound to a client-facing interface, mirroring `WishlistItemService` pattern.
- **Feature Interface Return Model Rule**: `FilesFeature.finalize`/`getMeta` and server-side `FilesService.finalize`/`getMeta` return `FilesFeatureMetaInfo` instead of the persistence entity `RegisteredFileMetaInfo` directly. `FilesMetaInfoRepo` keeps storing `RegisteredFileMetaInfo` unchanged — only the two methods' **return** values are retyped. `FilesRoutingsConfigurator` needed no textual change (the retype propagates through `call.respond(...)`'s inferred type, and the raw-byte route's `meta.fileName`/`meta.mimeType` field reads are unaffected). `FilesClientService.uploadFile` (wraps `finalize`) is retyped to stay call-site-compatible.
- **Temporal-file management**: The shared `TemporalFilesRoutingConfigurator` is registered as both a Koin singleton (so `FilesService` can retrieve pending temp files) and an `ApplicationRoutingConfigurator.Element` (installs the `/temp_upload` route). It is built with `TimedTemporalFilesUtilizer` (in `server/.../services/`), which purges any temporal upload not finalized within a one-hour TTL from the in-memory map and from disk — without it, abandoned uploads would survive until process exit and grow unbounded. (`/temp_upload` itself stays unauthenticated: the JS web client uploads it via a raw `XMLHttpRequest` that does not carry the bearer token, so requiring auth there would break web uploads.)
- **Client service composition**: `FilesClientService` wraps the two-step flow (tempUpload via MicroUtils `tempUpload`, then finalize via `FilesFeature`). Higher-level concerns (MIME type inference, URL building, byte downloads) live in the service, not in the HTTP feature interface.
- **`/api` prefix split**: HTTP calls made through the shared client (`downloadBytes`, `tempUpload`, finalize/meta/avatar) use bare paths (`files/{id}`, `temp_upload`); `/api` is added once by appending it to the server base URL (`DefaultUrlHttpClientConfigurator`). Browser `<img>` requests bypass the client, so `apiFileUrl(id)` returns the already-prefixed absolute `/api/files/{id}`. The two paths must never both add the prefix (would yield `/api/api/...`).
- **Dependencies**: Server module depends on `features/auth/server` (bearer caller resolution) and `micro_utils.ktor.server`. Client module depends on `micro_utils.ktor.client`.
