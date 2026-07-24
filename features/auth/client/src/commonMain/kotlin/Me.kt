package dev.inmo.wishlist.features.auth.client

import dev.inmo.wishlist.features.auth.common.models.AuthFeatureUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.Koin
import org.koin.core.definition.Definition
import org.koin.core.definition.KoinDefinition
import org.koin.core.module.Module
import org.koin.core.qualifier.Qualifier
import org.koin.core.qualifier.StringQualifier
import org.koin.core.qualifier.named
import org.koin.core.scope.Scope

/**
 * Koin qualifier under which the currently-authenticated-user ("me") [StateFlow] is registered
 * by [Plugin.setupDI].
 */
val meQualifier: StringQualifier = named("me")

internal fun Module.singleMeStateFlow(
    createdAtStart: Boolean = false,
    definition: Definition<StateFlow<AuthFeatureUser?>>,
): KoinDefinition<StateFlow<AuthFeatureUser?>> = single(meQualifier, createdAtStart, definition)


/**
 * State of the currently authenticated user: the caller's [AuthFeatureUser] when somebody is
 * logged in, `null` otherwise. Kept up to date by the [Plugin.startPlugin] subscription on
 * [AuthCredentialsStorage.userAuthorised].
 */
val Koin.meStateFlow: StateFlow<AuthFeatureUser?>
    get() = get(qualifier = meQualifier)

/**
 * [meStateFlow] accessor for Koin [Scope] receivers — e.g. inside `single { }` definitions of consumer
 * modules.
 */
val Scope.meStateFlow: StateFlow<AuthFeatureUser?>
    get() = get(qualifier = meQualifier)


/**
 * Koin qualifier under which the currently-authenticated-user ("me") [StateFlow] is registered
 * by [Plugin.setupDI].
 */
private val secretMeMutableStateFlowQualifier: StringQualifier = named("secret_me")

internal fun Module.singleSecretMeMutableStateFlow(
    createdAtStart: Boolean = false,
    definition: Definition<MutableStateFlow<AuthFeatureUser?>>,
): KoinDefinition<MutableStateFlow<AuthFeatureUser?>> = single(secretMeMutableStateFlowQualifier, createdAtStart, definition)

/**
 * State of the currently authenticated user: the caller's [AuthFeatureUser] when somebody is
 * logged in, `null` otherwise. Kept up to date by the [Plugin.startPlugin] subscription on
 * [AuthCredentialsStorage.userAuthorised].
 */
internal val Koin.secretMeMutableStateFlow: MutableStateFlow<AuthFeatureUser?>
    get() = get(qualifier = secretMeMutableStateFlowQualifier)

/**
 * [meStateFlow] accessor for Koin [Scope] receivers — e.g. inside `single { }` definitions of consumer
 * modules.
 */
internal val Scope.secretMeMutableStateFlow: MutableStateFlow<AuthFeatureUser?>
    get() = get(qualifier = secretMeMutableStateFlowQualifier)
