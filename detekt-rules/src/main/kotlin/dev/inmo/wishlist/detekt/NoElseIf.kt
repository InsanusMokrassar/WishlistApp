package dev.inmo.wishlist.detekt

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import org.jetbrains.kotlin.psi.KtIfExpression

/**
 * Reports conditionals whose direct `else` branch is another conditional.
 */
class NoElseIf(config: Config) : Rule(config, DESCRIPTION) {
    /**
     * Visits every conditional and reports each structural `else if` link.
     *
     * @param expression conditional PSI node under analysis.
     */
    override fun visitIfExpression(expression: KtIfExpression) {
        super.visitIfExpression(expression)

        if (expression.`else` is KtIfExpression) {
            report(Finding(Entity.from(expression), MESSAGE))
        }
    }

    private companion object {
        private const val DESCRIPTION = "Disallows chained else-if conditionals."
        private const val MESSAGE = "Replace an else-if chain with when or another non-chained form."
    }
}
