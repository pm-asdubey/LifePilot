package com.lifepilot.evals.model

data class EvalCase(
    val id: String,
    val description: String,
    val turns: List<EvalTurn>,
    val assertions: List<EvalAssertion>,
)

data class EvalTurn(
    val role: String,
    val content: String,
    val hasAttachedDocument: Boolean = false,
)

sealed class EvalAssertion {
    data class ContainsActionType(val actionType: String) : EvalAssertion()
    data class NotContainsActionType(val actionType: String) : EvalAssertion()
    data class ContainsToken(val token: String) : EvalAssertion()
    data class NotContainsToken(val token: String) : EvalAssertion()
    data class JsonParseable(val dummy: Unit = Unit) : EvalAssertion()
    data class NoPiiLeak(val dummy: Unit = Unit) : EvalAssertion()
    data class DomainsPresent(val domains: List<String>, val minMatched: Int) : EvalAssertion()
    data class LlmJudge(val rubric: String, val minScore: Int, val maxScore: Int = 5) : EvalAssertion()

    /**
     * Binary: did the response cite at least one expected object?
     * Pass threshold is always 1 — any hit counts.
     *
     * @param expectedObjects objects that SHOULD appear (e.g. ["Passport", "Star Health Insurance"])
     * @param k informational — how many retrieved slots we're measuring against
     */
    data class HitRateAtK(
        val expectedObjects: List<String>,
        val k: Int = 5,
    ) : EvalAssertion()

    /**
     * |cited ∩ expected| / |expected|
     * Measures how many of the objects the system SHOULD have surfaced actually appeared.
     *
     * @param expectedObjects objects that SHOULD be cited
     * @param allContextObjects full mock life graph passed in the prompt — used to identify what was cited
     * @param minRecall minimum acceptable fraction (0.0–1.0)
     */
    data class RecallAtK(
        val expectedObjects: List<String>,
        val allContextObjects: List<String>,
        val k: Int = 5,
        val minRecall: Float = 0.5f,
    ) : EvalAssertion()

    /**
     * |cited ∩ expected| / |cited|
     * Measures how many of the objects the response surfaced were actually relevant.
     */
    data class PrecisionAtK(
        val expectedObjects: List<String>,
        val allContextObjects: List<String>,
        val k: Int = 5,
        val minPrecision: Float = 0.5f,
    ) : EvalAssertion()

    // ── Agentic metrics ────────────────────────────────────────────────────

    /** Checks a specific field value inside a [LIFEPILOT_ACTION] block of the given actionType. */
    data class ActionFieldEquals(
        val actionType: String,
        val fieldName: String,
        val expectedValue: String,
    ) : EvalAssertion()

    /** Checks that a field exists (non-blank) inside a [LIFEPILOT_ACTION] block of the given actionType. */
    data class ActionFieldPresent(
        val actionType: String,
        val fieldName: String,
    ) : EvalAssertion()

    /** Checks that an ACTION_PLAN block contains at least minSteps items. */
    data class PlanStepCount(
        val minSteps: Int,
        val maxSteps: Int? = null,
    ) : EvalAssertion()

    // ── Latency ────────────────────────────────────────────────────────────

    /**
     * Asserts end-to-end response time is below a threshold.
     * Evaluated in EvalRunner after the AI call completes — AssertionRunner receives durationMs.
     * Use as a soft signal — set generous bounds so cloud variance doesn't cause false fails.
     */
    data class MaxLatencyMs(val maxMs: Long) : EvalAssertion()
}

data class EvalResult(
    val caseId: String,
    val description: String,
    val passed: Boolean,
    val assertionResults: List<AssertionResult>,
    val rawResponse: String,
    val durationMs: Long,
)

data class AssertionResult(
    val assertion: EvalAssertion,
    val passed: Boolean,
    val reason: String,
    val isHardBlock: Boolean = false,
)

data class EvalSuiteReport(
    val totalCases: Int,
    val passed: Int,
    val failed: Int,
    val hardBlocked: Boolean,
    val results: List<EvalResult>,
    val durationMs: Long,
) {
    val passRate: Double get() = if (totalCases == 0) 0.0 else passed.toDouble() / totalCases
}
