package dev.inmo.wishlist.features.roles.common

import dev.inmo.kroles.roles.BaseRole

/**
 * The single, hardcoded, root-only administrative role (issue #68 point 5). Never grantable through
 * any UI/route in this app — SuperAdmin access is architecturally fixed to the `root` account; see
 * `roles/README.md` Architecture Notes and `features/admin/README.md` Operator Notes.
 */
val SuperAdminRole = BaseRole("SuperAdmin")

/**
 * The single, hardcoded role every registered user holds (issue #68 point 6). Granted automatically
 * on user creation and backfilled once for pre-existing users — see
 * [dev.inmo.wishlist.features.roles.server.JVMPlugin].
 */
val UserRole = BaseRole("User")
