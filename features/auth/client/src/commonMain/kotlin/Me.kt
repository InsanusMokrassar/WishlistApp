package dev.inmo.wishlist.features.auth.client

import dev.inmo.wishlist.features.users.common.models.RegisteredUser
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.Koin
import org.koin.core.qualifier.StringQualifier
import org.koin.core.qualifier.named
import org.koin.core.scope.Scope

/**
 * Koin qualifier under which the currently-authenticated-user ("me") [StateFlow] is registered
 * by [Plugin.setupDI].
 */
val meQualifier: StringQualifier = named("me")

/**
 * State of the currently authenticated user: the caller's [RegisteredUser] when somebody is
 * logged in, `null` otherwise. Kept up to date by the [Plugin.startPlugin] subscription on
 * [AuthCredentialsStorage.userAuthorised].
 */
val Koin.me: StateFlow<RegisteredUser?>
    get() = get(qualifier = meQualifier)

/**
 * [me] accessor for Koin [Scope] receivers — e.g. inside `single { }` definitions of consumer
 * modules.
 */
val Scope.me: StateFlow<RegisteredUser?>
    get() = get(qualifier = meQualifier)
