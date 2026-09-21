package dev.inmo.wishlist.detekt

import dev.detekt.api.Config
import dev.detekt.api.RuleName
import dev.detekt.test.lint
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Verifies syntax-only detection of chained conditional branches.
 */
class NoElseIfTest {
    /** Verifies a statement-body chain reports the outer conditional. */
    @Test
    fun reportsBracedChain() {
        assertSingleFinding(
            code = """
                fun select(value: Boolean) {
                    if (value) {
                        work()
                    } else if (!value) {
                        work()
                    }
                }
            """.trimIndent(),
            expectedLine = 2,
        )
    }

    /** Verifies an expression-body chain reports the outer conditional. */
    @Test
    fun reportsExpressionChain() {
        assertSingleFinding(
            code = """
                fun select(value: Boolean) = if (value) 1 else if (!value) 2 else 3
            """.trimIndent(),
            expectedLine = 1,
        )
    }

    /** Verifies line breaks between `else` and `if` do not change detection. */
    @Test
    fun reportsMultilineChain() {
        assertSingleFinding(
            code = """
                fun select(value: Boolean) {
                    if (value) 1 else
                    if (!value) 2 else 3
                }
            """.trimIndent(),
            expectedLine = 2,
        )
    }

    /** Verifies comments between `else` and `if` do not change detection. */
    @Test
    fun reportsCommentSeparatedChain() {
        assertSingleFinding(
            code = """
                fun select(value: Boolean) {
                    if (value) 1 else /* separator */ if (!value) 2 else 3
                }
            """.trimIndent(),
            expectedLine = 2,
        )
    }

    /** Verifies every direct link of a multi-branch chain is reported. */
    @Test
    fun reportsEachChainLink() {
        val findings = findingsFor(
            """
                fun select(first: Boolean, second: Boolean) {
                    if (first) {
                        1
                    } else if (second) {
                        2
                    } else if (!first) {
                        3
                    } else {
                        4
                    }
                }
            """.trimIndent(),
        )

        assertEquals(2, findings.size)
        assertEquals(
            setOf("2:5", "4:12"),
            findings.map { finding ->
                val source = finding.entity.location.source
                "${source.line}:${source.column}"
            }.toSet(),
        )
        findings.forEach(::assertFindingContract)
    }

    /** Verifies a binary conditional is not treated as a chain. */
    @Test
    fun acceptsBinaryElse() {
        assertEquals(0, findingsFor("fun select(value: Boolean) = if (value) 1 else 2").size)
    }

    /** Verifies a conditional without an `else` branch is accepted. */
    @Test
    fun acceptsIfWithoutElse() {
        assertEquals(0, findingsFor("fun select(value: Boolean) { if (value) work() }").size)
    }

    /** Verifies an explicitly braced else block isolates an independent conditional. */
    @Test
    fun acceptsNestedIfInElseBlock() {
        assertEquals(
            0,
            findingsFor(
                """
                    fun select(value: Boolean) {
                        val text = "else if"
                        // else if is text, not a chained conditional.
                        if (value) work() else { if (!value) work() }
                    }
                """.trimIndent(),
            ).size,
        )
    }

    /**
     * Lints one source fixture with a fresh rule instance.
     *
     * @param code Kotlin source fixture.
     * @return findings reported for the fixture.
     */
    private fun findingsFor(code: String) = NoElseIf(Config.empty).lint(code)

    /**
     * Checks the full contract for a fixture containing one direct chain link.
     *
     * @param code Kotlin source fixture.
     * @param expectedLine one-based line containing the outer conditional.
     */
    private fun assertSingleFinding(code: String, expectedLine: Int) {
        val finding = findingsFor(code).single()
        assertEquals(expectedLine, finding.entity.location.source.line)
        assertFindingContract(finding)
    }

    /**
     * Checks the stable name and remediation message of a reported finding.
     *
     * @param finding Detekt finding produced by the rule.
     */
    private fun assertFindingContract(finding: dev.detekt.api.Finding) {
        assertEquals(RuleName("NoElseIf"), NoElseIf(Config.empty).ruleName)
        assertEquals("Replace an else-if chain with when or another non-chained form.", finding.message)
    }
}
