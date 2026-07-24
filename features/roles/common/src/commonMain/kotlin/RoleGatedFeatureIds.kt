package dev.inmo.wishlist.features.roles.common

/**
 * The concrete [FunctionalityId]s this app gates behind a role. One `val` per gated capability, named
 * after the capability rather than the class that enforces it today, so the id survives future
 * refactors of the enforcing code. Each is paired with a required role through a
 * [FeatureRolesRegistry.Requirement] at registration (see `roles/common` `Plugin.setupDI`).
 */
object RoleGatedFeatureIds {
    /** The whole `/admin/...` route surface (`AdminRoutingsConfigurator`). */
    val adminPanel = FunctionalityId("admin.panel")

    /** Changing another user's avatar via `PUT /files/avatar/{userId}` (`FilesRoutingsConfigurator`). */
    val filesAvatarChangeForOthers = FunctionalityId("files.avatarChangeForOthers")

    /** Sending a test email via `POST /email/sendTest` (`EmailFeatureService.sendTestEmail`). */
    val emailSendTest = FunctionalityId("email.sendTest")
}
