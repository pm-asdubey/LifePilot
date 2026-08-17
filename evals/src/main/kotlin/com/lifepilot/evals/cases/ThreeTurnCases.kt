package com.lifepilot.evals.cases

import com.lifepilot.evals.model.EvalAssertion
import com.lifepilot.evals.model.EvalCase
import com.lifepilot.evals.model.EvalTurn

val threeTurnCases = listOf(

    EvalCase(
        id = "3T-01",
        description = "Job change Turn 1: must have [ASK], must NOT have any action block",
        turns = listOf(
            EvalTurn("user", "I accepted a job offer at Infosys starting August 1st as a Product Manager."),
        ),
        assertions = listOf(
            EvalAssertion.ContainsToken("[ASK]"),
            EvalAssertion.NotContainsToken("[LIFEPILOT_ACTION]"),
        ),
    ),

    EvalCase(
        id = "3T-02",
        description = "Job change Turn 2: after answers, must show plain text proposal, NO action block",
        turns = listOf(
            EvalTurn("user", "I accepted a job offer at Infosys starting August 1st as a Product Manager."),
            EvalTurn("user", "My role is Product Manager, CTC 18 LPA, joining Infosys Pune. Leaving TCS, last day July 25th. Salary account stays HDFC."),
        ),
        assertions = listOf(
            EvalAssertion.NotContainsToken("[LIFEPILOT_ACTION]"),
            EvalAssertion.ContainsToken("Would you like"),
            EvalAssertion.LlmJudge(
                rubric = "Does the response show a numbered list of specific tasks with due dates and priorities? Score 1 (generic/vague) to 5 (specific tasks, dates, priorities clearly listed).",
                minScore = 3,
            ),
        ),
    ),

    EvalCase(
        id = "3T-03",
        description = "Job change Turn 3: after confirmation, must emit ACTION_PLAN",
        turns = listOf(
            EvalTurn("user", "I accepted a job offer at Infosys starting August 1st as a Product Manager."),
            EvalTurn("user", "My role is Product Manager, CTC 18 LPA, joining Infosys Pune. Leaving TCS, last day July 25th. Salary account stays HDFC."),
            EvalTurn("user", "Yes, go ahead and create these."),
        ),
        assertions = listOf(
            EvalAssertion.ContainsActionType("ACTION_PLAN"),
            EvalAssertion.JsonParseable(),
            EvalAssertion.NoPiiLeak(),
        ),
    ),

    EvalCase(
        id = "3T-04",
        description = "Turn 1 with document attached: must have OBJECT_CREATION, may also have [ASK]",
        turns = listOf(
            EvalTurn(
                role = "user",
                content = "I just scanned my passport. [Document: Passport, OCR text: PASSPORT Republic of India, Surname: DUBEY, Given Names: ASHUTOSH, Passport No: Z1234567, Date of Birth: 10/05/1995, Date of Expiry: 09/05/2030]",
                hasAttachedDocument = true,
            ),
        ),
        assertions = listOf(
            EvalAssertion.ContainsActionType("OBJECT_CREATION"),
            EvalAssertion.NotContainsActionType("ACTION_PLAN"),
            EvalAssertion.NoPiiLeak(),
        ),
    ),

    EvalCase(
        id = "3T-05",
        description = "Marriage Turn 1: clarify only, no action blocks, warm acknowledgement",
        turns = listOf(
            EvalTurn("user", "I am getting married in November this year!"),
        ),
        assertions = listOf(
            EvalAssertion.ContainsToken("[ASK]"),
            EvalAssertion.NotContainsToken("[LIFEPILOT_ACTION]"),
            EvalAssertion.ContainsToken("Congratulations"),
        ),
    ),

    EvalCase(
        id = "3T-06",
        description = "Marriage Turn 3: confirmed plan must have ACTION_PLAN with planType MARRIAGE",
        turns = listOf(
            EvalTurn("user", "I am getting married in November this year!"),
            EvalTurn("user", "Partner is Priya Sharma, wedding on November 18th in Delhi, both registered and ceremony."),
            EvalTurn("user", "Looks great, go ahead."),
        ),
        assertions = listOf(
            EvalAssertion.ContainsActionType("ACTION_PLAN"),
            EvalAssertion.ContainsToken("MARRIAGE"),
            EvalAssertion.JsonParseable(),
            EvalAssertion.DomainsPresent(
                domains = listOf("Identity", "Finance", "Career", "Health", "Legal"),
                minMatched = 3,
            ),
        ),
    ),

    EvalCase(
        id = "3T-07",
        description = "Yes in Turn 1 answer is NOT approval — Turn 2 must still show proposal",
        turns = listOf(
            EvalTurn("user", "I need to file my income tax return this year."),
            EvalTurn("user", "Yes I have Form 16, TDS certificates, and some capital gains from mutual funds. I'm a salaried employee."),
        ),
        assertions = listOf(
            EvalAssertion.NotContainsToken("[LIFEPILOT_ACTION]"),
            EvalAssertion.ContainsToken("Would you like"),
        ),
    ),
)
