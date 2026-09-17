package dev.inmo.wishlist.detekt

import dev.detekt.api.Config
import dev.detekt.api.RuleSetId
import dev.detekt.api.RuleSetProvider
import dev.detekt.test.lint
import java.util.ServiceLoader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Verifies the rule provider is discoverable through its service descriptor.
 */
class WishlistRuleSetProviderTest {
    /** Verifies the service-loaded provider creates exactly the custom rule. */
    @Test
    fun loadsWishlistRuleSet() {
        val provider = ServiceLoader.load(RuleSetProvider::class.java).iterator().asSequence()
            .single { it.ruleSetId == RuleSetId("wishlist") }
        val ruleSet = provider.instance()
        val rule = ruleSet.rules.values.single().invoke(Config.empty)

        assertEquals(RuleSetId("wishlist"), ruleSet.id)
        assertIs<NoElseIf>(rule)
        assertEquals(1, rule.lint("fun select(value: Boolean) = if (value) 1 else if (!value) 2 else 3").size)
    }
}
