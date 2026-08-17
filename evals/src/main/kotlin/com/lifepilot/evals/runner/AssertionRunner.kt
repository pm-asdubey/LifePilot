package com.lifepilot.evals.runner

import com.lifepilot.evals.api.AiClientAdapter
import com.lifepilot.evals.model.AssertionResult
import com.lifepilot.evals.model.EvalAssertion
import com.lifepilot.evals.domain.SensitiveFieldRegistry
import org.json.JSONObject

class AssertionRunner(private val judgeClient: AiClientAdapter) {

    private val actionBlockPattern = Regex("""\[LIFEPILOT_ACTION](.*?)\[/LIFEPILOT_ACTION]""", RegexOption.DOT_MATCHES_ALL)
    private val askBlockPattern = Regex("""\[ASK](.*?)\[/ASK]""", RegexOption.DOT_MATCHES_ALL)

    fun run(assertion: EvalAssertion, response: String, durationMs: Long = 0L): AssertionResult {
        return when (assertion) {

            is EvalAssertion.ContainsActionType -> {
                val found = actionBlockPattern.findAll(response).any { match ->
                    runCatching {
                        val json = JSONObject(match.groupValues[1].trim())
                        json.optString("actionType").equals(assertion.actionType, ignoreCase = true)
                    }.getOrDefault(false)
                }
                AssertionResult(
                    assertion = assertion,
                    passed = found,
                    reason = if (found) "Found actionType=${assertion.actionType}" else "Missing actionType=${assertion.actionType}",
                )
            }

            is EvalAssertion.NotContainsActionType -> {
                val found = actionBlockPattern.findAll(response).any { match ->
                    runCatching {
                        val json = JSONObject(match.groupValues[1].trim())
                        json.optString("actionType").equals(assertion.actionType, ignoreCase = true)
                    }.getOrDefault(false)
                }
                AssertionResult(
                    assertion = assertion,
                    passed = !found,
                    reason = if (!found) "Correctly absent: actionType=${assertion.actionType}" else "Unexpected actionType=${assertion.actionType} found",
                )
            }

            is EvalAssertion.ContainsToken -> {
                val found = response.contains(assertion.token, ignoreCase = true)
                AssertionResult(
                    assertion = assertion,
                    passed = found,
                    reason = if (found) "Token '${assertion.token}' present" else "Token '${assertion.token}' missing",
                )
            }

            is EvalAssertion.NotContainsToken -> {
                val found = response.contains(assertion.token, ignoreCase = true)
                AssertionResult(
                    assertion = assertion,
                    passed = !found,
                    reason = if (!found) "Token '${assertion.token}' correctly absent" else "Forbidden token '${assertion.token}' found",
                )
            }

            is EvalAssertion.JsonParseable -> {
                val blocks = actionBlockPattern.findAll(response).toList()
                if (blocks.isEmpty()) {
                    return AssertionResult(assertion, passed = false, reason = "No [LIFEPILOT_ACTION] blocks found")
                }
                val failed = blocks.filter { match ->
                    runCatching { JSONObject(match.groupValues[1].trim()) }.isFailure
                }
                AssertionResult(
                    assertion = assertion,
                    passed = failed.isEmpty(),
                    reason = if (failed.isEmpty()) "All ${blocks.size} action blocks parse cleanly"
                    else "${failed.size}/${blocks.size} blocks failed JSON parse",
                )
            }

            is EvalAssertion.NoPiiLeak -> {
                // Scrub and check if anything changed — if scrub changes the string, PII was present
                val scrubbed = SensitiveFieldRegistry.scrub(response)
                val leaked = scrubbed.contains("[REDACTED]")
                AssertionResult(
                    assertion = assertion,
                    passed = !leaked,
                    reason = if (!leaked) "No PII detected in response" else "PII DETECTED — scrubber found sensitive values",
                    isHardBlock = leaked,
                )
            }

            is EvalAssertion.DomainsPresent -> {
                val responseLower = response.lowercase()
                val matched = assertion.domains.filter { domain ->
                    responseLower.contains(domain.lowercase())
                }
                val passed = matched.size >= assertion.minMatched
                AssertionResult(
                    assertion = assertion,
                    passed = passed,
                    reason = "Matched ${matched.size}/${assertion.domains.size} domains (min=${assertion.minMatched}): $matched",
                )
            }

            is EvalAssertion.LlmJudge -> {
                val score = judgeClient.judge(assertion.rubric, response)
                val passed = score >= assertion.minScore
                AssertionResult(
                    assertion = assertion,
                    passed = passed,
                    reason = "Judge score: $score/${assertion.maxScore} (min=${assertion.minScore})",
                )
            }

            is EvalAssertion.HitRateAtK -> {
                val responseLower = response.lowercase()
                val hit = assertion.expectedObjects.any { obj ->
                    responseLower.contains(obj.lowercase())
                }
                AssertionResult(
                    assertion = assertion,
                    passed = hit,
                    reason = if (hit) {
                        val matched = assertion.expectedObjects.first { responseLower.contains(it.lowercase()) }
                        "Hit@${assertion.k}: found '$matched' in response"
                    } else {
                        "Hit@${assertion.k}: none of ${assertion.expectedObjects} appeared in response"
                    },
                )
            }

            is EvalAssertion.RecallAtK -> {
                val responseLower = response.lowercase()
                val cited = assertion.expectedObjects.filter { responseLower.contains(it.lowercase()) }
                val recall = if (assertion.expectedObjects.isEmpty()) 1f
                             else cited.size.toFloat() / assertion.expectedObjects.size
                val passed = recall >= assertion.minRecall
                AssertionResult(
                    assertion = assertion,
                    passed = passed,
                    reason = "Recall@${assertion.k}: ${cited.size}/${assertion.expectedObjects.size} expected objects cited " +
                             "(${(recall * 100).toInt()}% >= min ${(assertion.minRecall * 100).toInt()}%) — cited: $cited",
                )
            }

            is EvalAssertion.PrecisionAtK -> {
                val responseLower = response.lowercase()
                val cited = assertion.allContextObjects.filter { responseLower.contains(it.lowercase()) }
                val relevantCited = cited.filter { obj ->
                    assertion.expectedObjects.any { it.equals(obj, ignoreCase = true) }
                }
                val precision = if (cited.isEmpty()) 1f
                                else relevantCited.size.toFloat() / cited.size
                val passed = precision >= assertion.minPrecision
                AssertionResult(
                    assertion = assertion,
                    passed = passed,
                    reason = "Precision@${assertion.k}: ${relevantCited.size}/${cited.size} cited objects were relevant " +
                             "(${(precision * 100).toInt()}% >= min ${(assertion.minPrecision * 100).toInt()}%) " +
                             "— cited: $cited, relevant: $relevantCited",
                )
            }

            // ── Agentic metrics ──────────────────────────────────────────────

            is EvalAssertion.ActionFieldEquals -> {
                val block = actionBlockPattern.findAll(response).firstOrNull { match ->
                    runCatching {
                        JSONObject(match.groupValues[1].trim())
                            .optString("actionType")
                            .equals(assertion.actionType, ignoreCase = true)
                    }.getOrDefault(false)
                }
                if (block == null) {
                    return AssertionResult(
                        assertion = assertion,
                        passed = false,
                        reason = "No ${assertion.actionType} action block found",
                    )
                }
                val json = runCatching { JSONObject(block.groupValues[1].trim()) }.getOrNull()
                val actual = json?.optString(assertion.fieldName)
                val passed = actual?.equals(assertion.expectedValue, ignoreCase = true) == true
                AssertionResult(
                    assertion = assertion,
                    passed = passed,
                    reason = if (passed) "${assertion.fieldName}='${assertion.expectedValue}' ✓"
                             else "${assertion.fieldName}: expected='${assertion.expectedValue}' actual='$actual'",
                )
            }

            is EvalAssertion.ActionFieldPresent -> {
                val block = actionBlockPattern.findAll(response).firstOrNull { match ->
                    runCatching {
                        JSONObject(match.groupValues[1].trim())
                            .optString("actionType")
                            .equals(assertion.actionType, ignoreCase = true)
                    }.getOrDefault(false)
                }
                if (block == null) {
                    return AssertionResult(
                        assertion = assertion,
                        passed = false,
                        reason = "No ${assertion.actionType} action block found",
                    )
                }
                val json = runCatching { JSONObject(block.groupValues[1].trim()) }.getOrNull()
                val value = json?.optString(assertion.fieldName)
                val passed = !value.isNullOrBlank()
                AssertionResult(
                    assertion = assertion,
                    passed = passed,
                    reason = if (passed) "${assertion.fieldName} present: '$value'"
                             else "${assertion.fieldName} missing or blank in ${assertion.actionType} block",
                )
            }

            is EvalAssertion.PlanStepCount -> {
                val block = actionBlockPattern.find(response)
                if (block == null) {
                    return AssertionResult(
                        assertion = assertion,
                        passed = false,
                        reason = "No ACTION_PLAN block found",
                    )
                }
                val json = runCatching { JSONObject(block.groupValues[1].trim()) }.getOrNull()
                // Steps live in "items" array or at top-level "tasks" array
                val items = json?.optJSONArray("items") ?: json?.optJSONArray("tasks")
                val count = items?.length() ?: 0
                val passed = count >= assertion.minSteps &&
                             (assertion.maxSteps == null || count <= assertion.maxSteps)
                val range = if (assertion.maxSteps != null) "${assertion.minSteps}–${assertion.maxSteps}" else "≥${assertion.minSteps}"
                AssertionResult(
                    assertion = assertion,
                    passed = passed,
                    reason = "Plan step count: $count (expected $range)",
                )
            }

            // ── Latency ──────────────────────────────────────────────────────

            is EvalAssertion.MaxLatencyMs -> {
                val passed = durationMs <= assertion.maxMs
                AssertionResult(
                    assertion = assertion,
                    passed = passed,
                    reason = "Latency: ${durationMs}ms (max=${assertion.maxMs}ms)",
                )
            }
        }
    }
}
