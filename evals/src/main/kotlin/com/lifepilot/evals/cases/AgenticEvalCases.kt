package com.lifepilot.evals.cases

import com.lifepilot.evals.model.EvalAssertion
import com.lifepilot.evals.model.EvalCase
import com.lifepilot.evals.model.EvalTurn

/**
 * Agentic behaviour evals — verifies correct tool selection, field accuracy,
 * plan structure, multi-step completion, and recovery from ambiguity.
 *
 * Metrics covered:
 *   Tool Success Rate         — correct actionType emitted (AC cases already cover this; deeper here)
 *   Tool Invocation Accuracy  — correct field values inside the action JSON
 *   Planning Accuracy         — correct step sequence and coverage in ACTION_PLAN
 *   Multi-step Completion     — ACTION_PLAN contains enough steps
 *   Recovery Rate             — ambiguous input → [ASK], not a guessed action
 *   Goal Completion           — final objective fully addressed
 *   Action Accuracy           — field-level correctness, not just actionType
 *
 * NOT covered here (untestable in JVM eval module):
 *   OCR Latency, STT Latency — Android-only
 *   Tool Latency, Retrieval Latency — no real tool/DB calls in eval context
 *   TTFT — requires streaming; OkHttp client is non-streaming
 */

val agenticEvalCases = listOf(

    // ── Tool Invocation Accuracy ──────────────────────────────────────────

    EvalCase(
        id = "AGENT-01",
        description = "New passport → OBJECT_CREATION with objectType=Passport, not generic Document",
        turns = listOf(
            EvalTurn("user", "I just received my new passport. Passport number H1234567, valid until 2034-05-20."),
            EvalTurn("user", "Yes, save it."),
        ),
        assertions = listOf(
            EvalAssertion.ContainsActionType("OBJECT_CREATION"),
            EvalAssertion.ActionFieldEquals("OBJECT_CREATION", "objectType", "Passport"),
            EvalAssertion.JsonParseable(),
            EvalAssertion.NoPiiLeak(),
            EvalAssertion.MaxLatencyMs(30_000),
        ),
    ),

    EvalCase(
        id = "AGENT-02",
        description = "New job offer → OBJECT_CREATION with objectType=Job, not Task or Event",
        turns = listOf(
            EvalTurn("user", "I received an offer from Google for an SDE-3 role in Bangalore. CTC is 45 LPA."),
            EvalTurn("user", "Yes, add it."),
        ),
        assertions = listOf(
            EvalAssertion.ContainsActionType("OBJECT_CREATION"),
            EvalAssertion.ActionFieldEquals("OBJECT_CREATION", "objectType", "Job"),
            EvalAssertion.ActionFieldPresent("OBJECT_CREATION", "title"),
            EvalAssertion.JsonParseable(),
            EvalAssertion.NoPiiLeak(),
        ),
    ),

    EvalCase(
        id = "AGENT-03",
        description = "Insurance renewal (existing policy) → STATUS_UPDATE, not new OBJECT_CREATION",
        turns = listOf(
            EvalTurn("user", "I just renewed my Star Health Insurance for another year. New expiry is August 2027."),
            EvalTurn("user", "Yes, update it."),
        ),
        assertions = listOf(
            EvalAssertion.ContainsActionType("STATUS_UPDATE"),
            EvalAssertion.NotContainsActionType("OBJECT_CREATION"),
            EvalAssertion.JsonParseable(),
        ),
    ),

    // ── Planning Accuracy + Multi-step Completion ─────────────────────────

    EvalCase(
        id = "AGENT-04",
        description = "Home loan application → ACTION_PLAN with at least 5 steps covering document collection, valuation, application",
        turns = listOf(
            EvalTurn("user", "I want to apply for a home loan for a flat I am buying in Pune. Property value 85 lakhs, I want a loan of 65 lakhs from SBI."),
            EvalTurn("user", "Yes, lay out the full plan."),
            EvalTurn("user", "Go ahead, create it."),
        ),
        assertions = listOf(
            EvalAssertion.ContainsActionType("ACTION_PLAN"),
            EvalAssertion.PlanStepCount(minSteps = 5),
            EvalAssertion.JsonParseable(),
            EvalAssertion.LlmJudge(
                rubric = "Does the ACTION_PLAN include: document collection (salary slips, ITR, bank statements), property valuation, loan application submission, and at least one follow-up step (disbursement or registration)? Score 1 (only 1–2 generic steps) to 5 (complete sequential plan covering all major phases).",
                minScore = 3,
            ),
            EvalAssertion.MaxLatencyMs(45_000),
        ),
    ),

    EvalCase(
        id = "AGENT-05",
        description = "Relocation plan must cover Housing, Career, Finance, Identity domains in sequence",
        turns = listOf(
            EvalTurn("user", "I am relocating from Delhi to Bangalore next month for a new job at Flipkart. I need to sort out everything."),
            EvalTurn("user", "Joining date is August 1st. I'll be renting initially. No kids. Partner is coming too."),
            EvalTurn("user", "Yes please create the full relocation plan."),
        ),
        assertions = listOf(
            EvalAssertion.ContainsActionType("ACTION_PLAN"),
            EvalAssertion.PlanStepCount(minSteps = 6),
            EvalAssertion.DomainsPresent(
                domains = listOf("Housing", "Career", "Finance", "Identity", "Transport"),
                minMatched = 3,
            ),
            EvalAssertion.LlmJudge(
                rubric = "Does the plan cover: address change for Aadhaar/driving licence, new rental setup, PF transfer from old employer, bank branch transfer, and at least one Bangalore-specific step (e.g. vehicle re-registration, local SIM)? Score 1 (only job-related tasks) to 5 (comprehensive cross-domain relocation plan).",
                minScore = 3,
            ),
        ),
    ),

    // ── Recovery Rate ─────────────────────────────────────────────────────

    EvalCase(
        id = "AGENT-06",
        description = "Ambiguous 'update my insurance' — must ask which insurance, not guess",
        turns = listOf(
            EvalTurn("user", "Can you update my insurance?"),
        ),
        assertions = listOf(
            EvalAssertion.ContainsToken("[ASK]"),
            EvalAssertion.NotContainsActionType("STATUS_UPDATE"),
            EvalAssertion.NotContainsActionType("OBJECT_CREATION"),
            EvalAssertion.LlmJudge(
                rubric = "Does the response ask a clarifying question to identify WHICH insurance policy (health, car, life, etc.) the user means, rather than assuming? Score 1 (makes assumption) to 5 (clearly asks for clarification).",
                minScore = 4,
            ),
        ),
    ),

    EvalCase(
        id = "AGENT-07",
        description = "Incomplete task request — 'add a task' with no title or domain must ask for details",
        turns = listOf(
            EvalTurn("user", "Add a task for me."),
        ),
        assertions = listOf(
            EvalAssertion.ContainsToken("[ASK]"),
            EvalAssertion.NotContainsActionType("TASK_CREATION"),
            EvalAssertion.LlmJudge(
                rubric = "Does the response ask what task the user wants to add (what it is, when it is due, which domain)? Score 1 (creates a blank placeholder task) to 5 (asks for required details before acting).",
                minScore = 4,
            ),
        ),
    ),

    // ── Goal Completion ───────────────────────────────────────────────────

    EvalCase(
        id = "AGENT-08",
        description = "Baby born → ACTION_PLAN must address Birth Certificate, Insurance addition, Aadhaar, Will update",
        turns = listOf(
            EvalTurn("user", "My baby was born yesterday! A girl. We are naming her Aanya."),
            EvalTurn("user", "Born at Apollo Hospital Delhi, September 3rd. We want to get all the documentation sorted."),
            EvalTurn("user", "Yes, create the plan."),
        ),
        assertions = listOf(
            EvalAssertion.ContainsActionType("ACTION_PLAN"),
            EvalAssertion.PlanStepCount(minSteps = 4),
            EvalAssertion.LlmJudge(
                rubric = "Does the plan include: birth certificate registration, Aadhaar enrolment for the baby, adding baby to health insurance, and at least one estate/financial step (Will update or nominee addition)? Score 1 (only birth certificate) to 5 (all four categories covered).",
                minScore = 3,
            ),
            EvalAssertion.NoPiiLeak(),
            EvalAssertion.MaxLatencyMs(45_000),
        ),
    ),

    // ── End-to-End Latency (cloud baseline) ───────────────────────────────

    EvalCase(
        id = "LATENCY-01",
        description = "Simple factual query must complete within 15s on cloud providers",
        turns = listOf(
            EvalTurn("user", "What is the current status of my profile? [Context: Profile: Ashutosh, 3 active objects, 2 pending tasks]"),
        ),
        assertions = listOf(
            EvalAssertion.NotContainsToken("[LIFEPILOT_ACTION]"),
            EvalAssertion.MaxLatencyMs(15_000),
        ),
    ),

    EvalCase(
        id = "LATENCY-02",
        description = "Multi-turn ACTION_PLAN must complete within 45s on cloud providers",
        turns = listOf(
            EvalTurn("user", "I am getting married in December."),
            EvalTurn("user", "Partner's name is Neha. Wedding in Mumbai. Registered marriage."),
            EvalTurn("user", "Yes go ahead."),
        ),
        assertions = listOf(
            EvalAssertion.ContainsActionType("ACTION_PLAN"),
            EvalAssertion.MaxLatencyMs(45_000),
        ),
    ),
)
