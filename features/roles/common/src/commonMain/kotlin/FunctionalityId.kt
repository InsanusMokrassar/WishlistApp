package dev.inmo.wishlist.features.roles.common

import kotlin.jvm.JvmInline

/**
 * Strongly-typed identifier of a role-gated functionality (capability). Wraps the symbolic string id
 * so a functionality id can never be silently confused with an arbitrary [String] at a call site.
 * Concrete ids live in [RoleGatedFeatureIds]; each is paired with a required role through a
 * [FeatureRolesRegistry.Requirement].
 *
 * @property string Raw symbolic id (e.g. `admin.panel`).
 */
@JvmInline
value class FunctionalityId(val string: String)
