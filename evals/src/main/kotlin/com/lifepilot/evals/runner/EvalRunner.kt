package com.lifepilot.evals.runner

import com.lifepilot.evals.domain.AiCompletionResult
import com.lifepilot.evals.domain.AiMessage
import com.lifepilot.evals.domain.AiMessageRole
import com.lifepilot.evals.api.AiClientAdapter
import com.lifepilot.evals.model.EvalCase
import com.lifepilot.evals.model.EvalResult
import com.lifepilot.evals.model.EvalSuiteReport

class EvalRunner(
    private val aiClient: AiClientAdapter,
    private val systemPrompt: String,
) {
    private val assertionRunner = AssertionRunner(aiClient)

    fun runSuite(cases: List<EvalCase>): EvalSuiteReport {
        val start = System.currentTimeMillis()
        val results = cases.map { runCase(it) }
        val duration = System.currentTimeMillis() - start

        val hardBlocked = results.any { result ->
            result.assertionResults.any { it.isHardBlock && !it.passed }
        }

        return EvalSuiteReport(
            totalCases = cases.size,
            passed = results.count { it.passed },
            failed = results.count { !it.passed },
            hardBlocked = hardBlocked,
            results = results,
            durationMs = duration,
        )
    }

    private fun runCase(case: EvalCase): EvalResult {
        val start = System.currentTimeMillis()
        println("  Running: [${case.id}] ${case.description}")

        val history = mutableListOf<AiMessage>()
        var lastResponse = ""

        for ((index, turn) in case.turns.withIndex()) {
            if (turn.role == "user") {
                val result = aiClient.complete(systemPrompt, turn.content, history)
                when (result) {
                    is AiCompletionResult.Success -> {
                        lastResponse = result.content
                        history.add(AiMessage(AiMessageRole.USER, turn.content))
                        history.add(AiMessage(AiMessageRole.ASSISTANT, result.content))
                    }
                    else -> {
                        return EvalResult(
                            caseId = case.id,
                            description = case.description,
                            passed = false,
                            assertionResults = emptyList(),
                            rawResponse = "AI call failed at turn $index: $result",
                            durationMs = System.currentTimeMillis() - start,
                        )
                    }
                }
            }
        }

        val duration = System.currentTimeMillis() - start
        val assertionResults = case.assertions.map { assertion ->
            assertionRunner.run(assertion, lastResponse, duration)
        }

        val passed = assertionResults.all { it.passed }

        println("    ${if (passed) "✓ PASS" else "✗ FAIL"} (${duration}ms)")
        assertionResults.filter { !it.passed }.forEach { println("      ✗ ${it.reason}") }

        return EvalResult(
            caseId = case.id,
            description = case.description,
            passed = passed,
            assertionResults = assertionResults,
            rawResponse = lastResponse,
            durationMs = duration,
        )
    }
}
