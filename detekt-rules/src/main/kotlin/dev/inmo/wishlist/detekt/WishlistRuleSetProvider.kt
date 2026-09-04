package dev.inmo.wishlist.detekt

import dev.detekt.api.Config
import dev.detekt.api.RuleSet
import dev.detekt.api.RuleSetId
import dev.detekt.api.RuleSetProvider

/**
 * Supplies WishlistApp-specific Detekt rules through Java service loading.
 */
class WishlistRuleSetProvider : RuleSetProvider {
    /** Identifier used by Detekt configuration to activate the rule set. */
    override val ruleSetId = RuleSetId("wishlist")

    /**
     * Creates the configured rules supplied by the WishlistApp rule set.
     *
     * @return rule set containing the structural conditional rule.
     */
    override fun instance() = RuleSet(ruleSetId, listOf(::NoElseIf))
}
