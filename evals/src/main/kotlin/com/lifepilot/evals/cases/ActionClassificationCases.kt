package com.lifepilot.evals.cases

import com.lifepilot.evals.model.EvalAssertion
import com.lifepilot.evals.model.EvalCase
import com.lifepilot.evals.model.EvalTurn

val actionClassificationCases = listOf(

    EvalCase(
        id = "AC-01",
        description = "New job must trigger ACTION_PLAN, never OBJECT_CREATION",
        turns = listOf(
            EvalTurn("user", "I got a new job at Google as a Senior Engineer starting next month."),
        ),
        assertions = listOf(
            EvalAssertion.ContainsToken("[ASK]"),
            EvalAssertion.NotContainsActionType("OBJECT_CREATION"),
            EvalAssertion.NotContainsActionType("TASK_CREATION"),
            EvalAssertion.NotContainsActionType("ACTION_PLAN"),
        ),
    ),

    EvalCase(
        id = "AC-02",
        description = "Buying a car = OBJECT_CREATION not ACTION_PLAN",
        turns = listOf(
            EvalTurn("user", "I just bought a new Honda City."),
        ),
        assertions = listOf(
            EvalAssertion.ContainsActionType("OBJECT_CREATION"),
            EvalAssertion.NotContainsActionType("ACTION_PLAN"),
        ),
    ),

    EvalCase(
        id = "AC-03",
        description = "Single reminder = TASK_CREATION",
        turns = listOf(
            EvalTurn("user", "Remind me to renew my gym membership next month."),
        ),
        assertions = listOf(
            EvalAssertion.ContainsActionType("TASK_CREATION"),
            EvalAssertion.NotContainsActionType("ACTION_PLAN"),
        ),
    ),

    EvalCase(
        id = "AC-04",
        description = "Expired passport = STATUS_UPDATE",
        turns = listOf(
            EvalTurn("user", "My passport expired last week."),
        ),
        assertions = listOf(
            EvalAssertion.ContainsActionType("STATUS_UPDATE"),
        ),
    ),

    EvalCase(
        id = "AC-05",
        description = "Marriage = ACTION_PLAN with clarifying questions first",
        turns = listOf(
            EvalTurn("user", "I am getting married in December."),
        ),
        assertions = listOf(
            EvalAssertion.ContainsToken("[ASK]"),
            EvalAssertion.NotContainsActionType("ACTION_PLAN"),
            EvalAssertion.NotContainsActionType("TASK_CREATION"),
        ),
    ),

    EvalCase(
        id = "AC-06",
        description = "Relocation = ACTION_PLAN not OBJECT_CREATION",
        turns = listOf(
            EvalTurn("user", "I am relocating to Bangalore next month for a new job."),
        ),
        assertions = listOf(
            EvalAssertion.ContainsToken("[ASK]"),
            EvalAssertion.NotContainsActionType("OBJECT_CREATION"),
        ),
    ),

    EvalCase(
        id = "AC-07",
        description = "New baby = ACTION_PLAN with clarifying questions",
        turns = listOf(
            EvalTurn("user", "We just had a baby boy!"),
        ),
        assertions = listOf(
            EvalAssertion.ContainsToken("[ASK]"),
            EvalAssertion.NotContainsActionType("ACTION_PLAN"),
            EvalAssertion.ContainsToken("Congratulations"),
        ),
    ),

    EvalCase(
        id = "AC-08",
        description = "Death of relative = ACTION_PLAN with empathy first",
        turns = listOf(
            EvalTurn("user", "My father passed away last week."),
        ),
        assertions = listOf(
            EvalAssertion.ContainsToken("[ASK]"),
            EvalAssertion.NotContainsActionType("ACTION_PLAN"),
            EvalAssertion.LlmJudge(
                rubric = "Does the response acknowledge the loss with empathy before asking questions? Score 1 (no empathy) to 5 (warm and human).",
                minScore = 4,
            ),
        ),
    ),

    EvalCase(
        id = "AC-09",
        description = "Opening a bank account = OBJECT_CREATION",
        turns = listOf(
            EvalTurn("user", "I opened a new savings account at HDFC Bank."),
        ),
        assertions = listOf(
            EvalAssertion.ContainsActionType("OBJECT_CREATION"),
            EvalAssertion.NotContainsActionType("ACTION_PLAN"),
        ),
    ),

    EvalCase(
        id = "AC-10",
        description = "Tax filing = ACTION_PLAN (multiple steps)",
        turns = listOf(
            EvalTurn("user", "I need to file my income tax return."),
        ),
        assertions = listOf(
            EvalAssertion.ContainsToken("[ASK]"),
            EvalAssertion.NotContainsActionType("TASK_CREATION"),
        ),
    ),
)
