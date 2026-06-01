# Feature: Files

## Operator Notes

<!-- Human operator writes here. Agents MUST read and respect before making any changes. Agents MUST NOT modify this section. -->

## Overview

Generic binary file storage with metadata. Stores file payloads on disk and metadata in Postgres. Provides a single shared temporal-upload endpoint (`POST /temp_upload`) reusable by any feature. Wishlist items consume the feature to attach images. File finalization is constrained to image MIME types only. Follows a two-step upload pattern: clients upload raw bytes to the shared temporal endpoint, receive a `TemporalFileId`, then call `/files/finalize` to move the temp file into permanent storage and record ownership.

## Routes

| Method | Path | Auth | Body / Response | Description |
|--------|------|------|-----------------|-------------|
| POST | `/temp_upload` | None | multipart binary → `TemporalFileId` | Shared temporal upload endpoint (MicroUtils `TemporalFilesRoutingConfigurator`); single reusable entry point for all features |
| POST | `/files/finalize` | Bearer | `FinalizeFileRequest` → `RegisteredFileMetaInfo \| 400` | Promote temporal upload to permanent storage; returns `RegisteredFileMetaInfo` (200) or `400` when temp file missing/expired or MIME is not an image; owner recorded from bearer principal |
| GET | `/files/{id}` | None | → raw bytes with `Content-Type` \| 404 | Stream persisted file payload; no auth required |
| GET | `/files/meta/{id}` | None | → `RegisteredFileMetaInfo` JSON \| 404 | Fetch file metadata; no auth required; 404 when unknown |
| GET | `/files/avatar/{userId}` | None | → `FileId` JSON \| 404 | Resolve a user's avatar file id; no auth required; 404 when the user has no avatar |
| PUT | `/files/avatar/{userId}` | Bearer | `FileId` → `200` \| `400` \| `403` | Associate an already-finalized file as the user's avatar; allowed only for the user themselves or `root`; `400` when the file is unknown, `403` for any other caller |

## Models

| Type | Package | Description |
|------|---------|-------------|
| `FileId` | common | `@JvmInline value class(val string: String)` — server-generated UUID, primary key for binary and metadata stores |
| `FileMetaInfo` | common | Sealed interface with `fileName: FileName`, `mimeType: String`, `size: Long`, `uploaderId: UserId`; variants `NewFileMetaInfo` and `RegisteredFileMetaInfo(id, ...)` |
| `NewFileMetaInfo` | common | Temporary metadata before persistence (fileName, mimeType, size, uploaderId) |
| `RegisteredFileMetaInfo` | common | Persisted metadata with `id: FileId`; returned by finalize and metadata routes |
| `FinalizeFileRequest` | common | JSON body for `POST /files/finalize`: `temporalFileId: TemporalFileId`, `fileName: FileName`, `mimeType: String` |
| `FilesRepo` | common | Interface: `put(FileId, ByteArray)`, `get(FileId): ByteArray?`, `remove(FileId)` |
| `FilesMetaInfoRepo` | common | Interface: `KeyValueRepo<FileId, RegisteredFileMetaInfo>` |
| `UserAvatarsRepo` | common | Interface: `KeyValueRepo<UserId, FileId>` — maps a user to that user's avatar file; Exposed impl `ExposedUserAvatarsRepo` (`user_avatars` table: `user_id` long PK, `file_id` text) |
| `FilesFeature` | client | Client contract: `finalize(FinalizeFileRequest)`, `getMeta(FileId)`, `getAvatar(UserId)`, `setAvatar(UserId, FileId)` |
| `FilesClientService` | client | High-level client service: `uploadFile(MPPFile)`, `fileUrl(FileId)`, `downloadBytes(FileId)`, `getAvatar(UserId)`, `uploadAvatar(UserId, MPPFile)` |
| `KtorFilesFeature` | client | HTTP implementation of `FilesFeature` |

## Architecture Notes

- **Two-step upload**: Client uploads bytes to `/temp_upload` (shared `TemporalFilesRoutingConfigurator` from MicroUtils; JS implementation uses XMLHttpRequest for large-file support), receives `TemporalFileId`, then calls `/files/finalize` referencing that id.
- **Binary storage**: `DiskFilesRepo` persists payloads on disk under a directory from `FilesConfig.filesFolder` (decoded from root server config JSON; key `filesFolder`, default `./uploaded_files`). Guarded by `SmartRWLocker`.
- **Metadata storage**: `ExposedFilesMetaInfoRepo` (Exposed-backed `KeyValueRepo`) persists `RegisteredFileMetaInfo` as JSON text in the `files_meta` table (`file_id` text PK, `meta_json` text); mirrors `ExposedPasswordsRepo` pattern.
- **Finalize constraints**: `FilesService.finalize` rejects non-image MIME types (checked with `startsWith("image/")`); deletes the temp file before returning `null` on rejection. Records the uploader from the authenticated `UserId` parameter.
- **Uploader field rename**: `FileMetaInfo.ownerId` was renamed to `uploaderId` (the field captures *who uploaded* a file, not domain ownership). NOTE: persisted `files_meta` JSON written before the rename uses the `ownerId` key and will not decode after the rename — wipe/migrate the `files_meta` table on existing dev databases.
- **User avatars**: `UserAvatarsRepo : KeyValueRepo<UserId, FileId>` (Exposed `ExposedUserAvatarsRepo`, table `user_avatars`) maps each user to one avatar file. `FilesService.getAvatar(userId)` / `setAvatar(userId, fileId)` back the routes; `setAvatar` rejects (`false`/`400`) a `FileId` with no finalized metadata. The `PUT /files/avatar/{userId}` route authorizes the caller as the user themselves or `root` (root resolved via `ReadUsersRepo` injected into `FilesRoutingsConfigurator`, username `"root"`); `GET /files/avatar/{userId}` is public so any profile view can render the avatar. Avatar upload reuses the normal finalize flow then associates the resulting id, so avatars are ordinary finalized files plus a mapping row.
- **Server-only service**: `FilesService` is server-only (caller `UserId` parameter on finalize) — not bound to a client-facing interface, mirroring `WishlistItemService` pattern.
- **Temporal-file management**: The shared `TemporalFilesRoutingConfigurator` is registered as both a Koin singleton (so `FilesService` can retrieve pending temp files) and an `ApplicationRoutingConfigurator.Element` (installs the `/temp_upload` route).
- **Client service composition**: `FilesClientService` wraps the two-step flow (tempUpload via MicroUtils `tempUpload`, then finalize via `FilesFeature`). Higher-level concerns (MIME type inference, URL building, byte downloads) live in the service, not in the HTTP feature interface.
- **Dependencies**: Server module depends on `features/auth/server` (bearer caller resolution) and `micro_utils.ktor.server`. Client module depends on `micro_utils.ktor.client`.
