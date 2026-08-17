package com.lifepilot.evals.cases

import com.lifepilot.evals.model.EvalAssertion
import com.lifepilot.evals.model.EvalCase
import com.lifepilot.evals.model.EvalTurn

/**
 * Retrieval quality evals — measure whether the AI surfaces the correct Objects
 * from the mock life graph injected into each prompt as [Context: ...].
 *
 * Metrics:
 *   HitRateAtK  — binary: at least one expected object appeared
 *   RecallAtK   — fraction of expected objects cited (|cited ∩ expected| / |expected|)
 *   PrecisionAtK — fraction of cited objects that were relevant (|cited ∩ expected| / |cited|)
 *
 * allContextObjects represents the full mock life graph (everything retrievable).
 * expectedObjects represents what SHOULD have been surfaced for this query.
 */

// Shared mock life graph — same across cases so metrics are comparable
private val fullLifeGraph = listOf(
    "Indian Passport",
    "Star Health Insurance",
    "LIC Jeevan Anand Policy",
    "HDFC Savings Account",
    "TCS Job",
    "Wipro Job",
    "Maruti Swift RC",
    "Bajaj Allianz Car Insurance",
    "PAN Card",
    "Aadhaar Card",
    "Delhi Flat",
    "Home Loan - SBI",
    "Zerodha Demat Account",
    "ITR FY2024",
    "Amazon Job",
)

val retrievalEvalCases = listOf(

    // ── RET-01: Single-domain single-object query ───────────────────────────
    EvalCase(
        id = "RET-01",
        description = "Passport expiry query — should surface passport, not health/finance objects",
        turns = listOf(
            EvalTurn(
                "user",
                "When does my passport expire? " +
                "[Context: ${fullLifeGraph.joinToString(", ")}. " +
                "Indian Passport: expiry_date=2029-03-15, status=ACTIVE]",
            ),
        ),
        assertions = listOf(
            EvalAssertion.HitRateAtK(
                expectedObjects = listOf("Indian Passport", "Passport"),
                k = 5,
            ),
            EvalAssertion.RecallAtK(
                expectedObjects = listOf("Indian Passport"),
                allContextObjects = fullLifeGraph,
                k = 5,
                minRecall = 1.0f,
            ),
            EvalAssertion.PrecisionAtK(
                expectedObjects = listOf("Indian Passport"),
                allContextObjects = fullLifeGraph,
                k = 5,
                minPrecision = 0.5f,
            ),
            EvalAssertion.ContainsToken("2029"),
            EvalAssertion.NotContainsToken("[LIFEPILOT_ACTION]"),
        ),
    ),

    // ── RET-02: Multi-object query — insurance + job ────────────────────────
    EvalCase(
        id = "RET-02",
        description = "Job change query — should surface active job and health insurance, not passport or car",
        turns = listOf(
            EvalTurn(
                "user",
                "I am joining Amazon next month. What records will I need to update? " +
                "[Context: ${fullLifeGraph.joinToString(", ")}. " +
                "TCS Job: status=ACTIVE, joined=2021-06-01. " +
                "Star Health Insurance: employer_group_policy=true, linked_employer=TCS. " +
                "Amazon Job: status=OFFER_ACCEPTED, joining=2026-08-15]",
            ),
        ),
        assertions = listOf(
            EvalAssertion.HitRateAtK(
                expectedObjects = listOf("TCS Job", "Star Health Insurance", "Amazon Job"),
                k = 5,
            ),
            EvalAssertion.RecallAtK(
                expectedObjects = listOf("TCS Job", "Star Health Insurance"),
                allContextObjects = fullLifeGraph,
                k = 5,
                minRecall = 0.5f,
            ),
            EvalAssertion.PrecisionAtK(
                expectedObjects = listOf("TCS Job", "Star Health Insurance", "Amazon Job"),
                allContextObjects = fullLifeGraph,
                k = 5,
                minPrecision = 0.5f,
            ),
        ),
    ),

    // ── RET-03: Finance domain — should not spill into identity/travel ──────
    EvalCase(
        id = "RET-03",
        description = "Tax filing query — should surface ITR, Zerodha demat, HDFC account; not passport or car",
        turns = listOf(
            EvalTurn(
                "user",
                "What do I need to sort out before the ITR deadline? " +
                "[Context: ${fullLifeGraph.joinToString(", ")}. " +
                "ITR FY2024: status=PENDING, deadline=2026-07-31. " +
                "Zerodha Demat Account: has_capital_gains=true. " +
                "HDFC Savings Account: linked_to_pan=true]",
            ),
        ),
        assertions = listOf(
            EvalAssertion.HitRateAtK(
                expectedObjects = listOf("ITR FY2024", "Zerodha Demat Account", "HDFC Savings Account"),
                k = 5,
            ),
            EvalAssertion.RecallAtK(
                expectedObjects = listOf("ITR FY2024", "Zerodha Demat Account"),
                allContextObjects = fullLifeGraph,
                k = 5,
                minRecall = 0.5f,
            ),
            EvalAssertion.PrecisionAtK(
                expectedObjects = listOf("ITR FY2024", "Zerodha Demat Account", "HDFC Savings Account"),
                allContextObjects = fullLifeGraph,
                k = 5,
                minPrecision = 0.4f,
            ),
        ),
    ),

    // ── RET-04: Cross-domain query — should pull from multiple domains ───────
    EvalCase(
        id = "RET-04",
        description = "Home loan query — should surface Delhi Flat and Home Loan together",
        turns = listOf(
            EvalTurn(
                "user",
                "Tell me about my property situation. " +
                "[Context: ${fullLifeGraph.joinToString(", ")}. " +
                "Delhi Flat: status=OWNED, purchase_value=8500000, purchase_year=2022. " +
                "Home Loan - SBI: outstanding=6200000, emi=52000, next_due=2026-08-05]",
            ),
        ),
        assertions = listOf(
            EvalAssertion.HitRateAtK(
                expectedObjects = listOf("Delhi Flat", "Home Loan"),
                k = 5,
            ),
            EvalAssertion.RecallAtK(
                expectedObjects = listOf("Delhi Flat", "Home Loan - SBI"),
                allContextObjects = fullLifeGraph,
                k = 5,
                minRecall = 1.0f,
            ),
            EvalAssertion.PrecisionAtK(
                expectedObjects = listOf("Delhi Flat", "Home Loan - SBI"),
                allContextObjects = fullLifeGraph,
                k = 5,
                minPrecision = 0.5f,
            ),
        ),
    ),

    // ── RET-05: Negative — query with no matching object ────────────────────
    EvalCase(
        id = "RET-05",
        description = "Query about an object not in the life graph — response must not hallucinate a record",
        turns = listOf(
            EvalTurn(
                "user",
                "What does my term life insurance cover? " +
                "[Context: ${fullLifeGraph.joinToString(", ")}. No term life insurance record found.]",
            ),
        ),
        assertions = listOf(
            EvalAssertion.HitRateAtK(
                expectedObjects = listOf("LIC Jeevan Anand"),
                k = 5,
            ),
            EvalAssertion.LlmJudge(
                rubric = "The context explicitly says there is no term life insurance. Does the response " +
                         "correctly acknowledge this WITHOUT inventing coverage details? " +
                         "Score 1 (hallucinates a policy) to 5 (correctly says no term life record found).",
                minScore = 4,
            ),
            EvalAssertion.NoPiiLeak(),
        ),
    ),

    // ── RET-06: Precision stress — ambiguous query touching many domains ─────
    EvalCase(
        id = "RET-06",
        description = "Vague 'what should I renew?' query — should surface expiring objects only, not full graph",
        turns = listOf(
            EvalTurn(
                "user",
                "What do I need to renew soon? " +
                "[Context: ${fullLifeGraph.joinToString(", ")}. " +
                "Indian Passport: expiry=2029-03-15. " +
                "Star Health Insurance: renewal_due=2026-09-01. " +
                "Bajaj Allianz Car Insurance: renewal_due=2026-08-20. " +
                "LIC Jeevan Anand Policy: next_premium_due=2026-10-01. " +
                "Maruti Swift RC: renewal_due=2028-11-01. " +
                "PAN Card: no_expiry=true]",
            ),
        ),
        assertions = listOf(
            EvalAssertion.HitRateAtK(
                expectedObjects = listOf("Star Health Insurance", "Bajaj Allianz Car Insurance"),
                k = 5,
            ),
            EvalAssertion.RecallAtK(
                expectedObjects = listOf("Star Health Insurance", "Bajaj Allianz Car Insurance"),
                allContextObjects = fullLifeGraph,
                k = 5,
                minRecall = 0.5f,
            ),
            EvalAssertion.PrecisionAtK(
                expectedObjects = listOf("Star Health Insurance", "Bajaj Allianz Car Insurance", "LIC Jeevan Anand Policy"),
                allContextObjects = fullLifeGraph,
                k = 5,
                minPrecision = 0.4f,
            ),
            EvalAssertion.NotContainsToken("PAN Card"),
        ),
    ),
)
