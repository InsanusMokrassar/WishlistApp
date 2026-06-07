package dev.inmo.wishlist.features.auth.client

import dev.inmo.wishlist.features.users.common.models.RegisteredUser
import kotlinx.coroutines.flow.MutableStateFlow
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
val Koin.meStateFlow: StateFlow<RegisteredUser?>
    get() = get(qualifier = meQualifier)

/**
 * [meStateFlow] accessor for Koin [Scope] receivers — e.g. inside `single { }` definitions of consumer
 * modules.
 */
val Scope.meStateFlow: StateFlow<RegisteredUser?>
    get() = get(qualifier = meQualifier)


/**
 * Koin qualifier under which the currently-authenticated-user ("me") [StateFlow] is registered
 * by [Plugin.setupDI].
 */
internal val secretMeMutablemeStateFlowQualifier: StringQualifier = named("secret_me")

/**
 * State of the currently authenticated user: the caller's [RegisteredUser] when somebody is
 * logged in, `null` otherwise. Kept up to date by the [Plugin.startPlugin] subscription on
 * [AuthCredentialsStorage.userAuthorised].
 */
internal val Koin.secretMeMutableStateFlow: MutableStateFlow<RegisteredUser?>
    get() = get(qualifier = secretMeMutablemeStateFlowQualifier)

/**
 * [meStateFlow] accessor for Koin [Scope] receivers — e.g. inside `single { }` definitions of consumer
 * modules.
 */
internal val Scope.secretMeMutableStateFlow: MutableStateFlow<RegisteredUser?>
    get() = get(qualifier = secretMeMutablemeStateFlowQualifier)
