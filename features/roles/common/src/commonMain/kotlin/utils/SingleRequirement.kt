package dev.inmo.wishlist.features.roles.common.utils

import dev.inmo.micro_utils.koin.singleWithRandomQualifier
import dev.inmo.wishlist.features.roles.common.FeatureRolesRegistry
import org.koin.core.definition.Definition
import org.koin.core.module.Module

/**
 * Registers one [dev.inmo.wishlist.features.roles.common.FeatureRolesRegistry.Requirement] into the Koin module so [dev.inmo.wishlist.features.roles.common.MapFeatureRolesRegistry]
 * collects it via `getAllDistinct`. Uses a random qualifier under the hood, so any number of
 * requirements may be contributed — from any feature's `setupDI` — without qualifier collisions,
 * mirroring how `ApplicationRoutingConfigurator.Element`s are contributed with
 * `singleWithRandomQualifier` and aggregated with `getAllDistinct` in `features/common/server`.
 *
 * @param createdAtStart Whether the requirement is instantiated eagerly at Koin start (default `false`).
 * @param block Definition producing the [dev.inmo.wishlist.features.roles.common.FeatureRolesRegistry.Requirement] to contribute.
 * @return Koin definition for the registered requirement.
 */
fun Module.singleRequirement(
    createdAtStart: Boolean = false,
    block: Definition<FeatureRolesRegistry.Requirement>
) = singleWithRandomQualifier(createdAtStart, block)
