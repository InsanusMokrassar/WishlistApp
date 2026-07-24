package dev.inmo.wishlist.features.roles.common

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/**
 * Strongly-typed identifier of a role-gated functionality (capability). Wraps the symbolic string id
 * so a functionality id can never be silently confused with an arbitrary [String] at a call site.
 * Each concrete id is declared in its owning feature's `Constants` file (see `roles/README.md`
 * Architecture Notes) and paired with a required role through a [FeatureRolesRegistry.Requirement].
 *
 * Serializes transparently as its underlying [string].
 *
 * @property string Raw symbolic id (e.g. `admin.panel`).
 */
@Serializable
@JvmInline
value class FunctionalityId(val string: String)
