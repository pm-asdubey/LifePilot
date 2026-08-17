package com.lifepilot.evals

import com.lifepilot.evals.api.AnthropicAdapter
import com.lifepilot.evals.api.EvalAiClient
import com.lifepilot.evals.api.OllamaAdapter
import com.lifepilot.evals.api.OllamaAiClient
import com.lifepilot.evals.cases.actionClassificationCases
import com.lifepilot.evals.cases.agenticEvalCases
import com.lifepilot.evals.cases.piiAndQualityCases
import com.lifepilot.evals.cases.retrievalEvalCases
import com.lifepilot.evals.cases.threeTurnCases
import com.lifepilot.evals.model.EvalSuiteReport
import com.lifepilot.evals.prompt.EvalSystemPrompt
import com.lifepilot.evals.runner.EvalRunner

/**
 * Usage:
 *
 *   Anthropic (default):
 *     export ANTHROPIC_API_KEY=sk-ant-...
 *     ./gradlew :evals:run
 *
 *   Anthropic with Sonnet as judge:
 *     export ANTHROPIC_API_KEY=sk-ant-...
 *     ./gradlew :evals:run --args="--judge-model claude-sonnet-4-6"
 *
 *   NVIDIA NIM:
 *     export NVIDIA_API_KEY=nvapi-...
 *     ./gradlew :evals:run --args="--provider nvidia --model meta/llama-3.1-70b-instruct"
 *
 *   Local Ollama (no API key needed):
 *     ollama serve  (in a separate terminal)
 *     ollama pull gemma2:2b
 *     ./gradlew :evals:run --args="--provider ollama --model gemma2:2b"
 *
 *   Ollama answers + Anthropic judge (recommended):
 *     export ANTHROPIC_API_KEY=sk-ant-...
 *     ./gradlew :evals:run --args="--provider ollama --model gemma2:2b --judge anthropic"
 */
fun main(args: Array<String>) {
    val argMap = parseArgs(args)
    val provider = argMap["provider"] ?: "anthropic"
    val model = argMap["model"]
    val judgeProvider = argMap["judge"]
    val judgeModel = argMap["judge-model"] ?: "claude-haiku-4-5-20251001"

    println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    println("LifePilot Eval Suite")
    println("Provider: $provider${if (model != null) " / $model" else ""}")
    println("Judge: ${judgeProvider ?: provider}${if (judgeModel != judgeModel) " / $judgeModel" else ""}")
    println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

    val answerClient = when (provider) {
        "ollama" -> {
            val ollamaClient = OllamaAiClient(model = model ?: "gemma2:2b")
            if (!ollamaClient.isAvailable()) {
                println("✗ Cannot connect to Ollama. Start it first: ollama serve")
                println("  Available models after starting: ollama list")
                System.exit(1)
            }
            val available = ollamaClient.listModels()
            val requested = model ?: "gemma2:2b"
            if (available.none { it.startsWith(requested.substringBefore(":")) }) {
                println("✗ Model '$requested' not found. Pull it first: ollama pull $requested")
                println("  Available: $available")
                System.exit(1)
            }
            val judgeApiClient = if (judgeProvider == "anthropic") {
                val key = System.getenv("ANTHROPIC_API_KEY")
                    ?: error("ANTHROPIC_API_KEY required for Anthropic judge")
                EvalAiClient(apiKey = key, model = judgeModel)
            } else null
            OllamaAdapter(ollamaClient, judgeApiClient)
        }
        "nvidia" -> {
            val apiKey = System.getenv("NVIDIA_API_KEY")
                ?: error("NVIDIA_API_KEY env var not set. Export it: export NVIDIA_API_KEY=nvapi-...")
            AnthropicAdapter(
                EvalAiClient(
                    apiKey = apiKey,
                    model = model ?: "meta/llama-3.1-70b-instruct",
                    baseUrl = "https://integrate.api.nvidia.com/v1",
                    useOpenAiFormat = true,
                )
            )
        }
        else -> {
            val apiKey = System.getenv("ANTHROPIC_API_KEY")
                ?: error("ANTHROPIC_API_KEY env var not set. Export it: export ANTHROPIC_API_KEY=sk-ant-...")
            AnthropicAdapter(
                EvalAiClient(
                    apiKey = apiKey,
                    model = model ?: "claude-haiku-4-5-20251001",
                    judgeModel = judgeModel,
                )
            )
        }
    }

    val systemPrompt = EvalSystemPrompt.minimal()
    val runner = EvalRunner(aiClient = answerClient, systemPrompt = systemPrompt)

    val suites = mapOf(
        "Action Classification" to actionClassificationCases,
        "3-Turn Conversation Structure" to threeTurnCases,
        "PII, Quality & Cascade" to piiAndQualityCases,
        "Retrieval Quality (Recall, Precision, Hit Rate)" to retrievalEvalCases,
        "Agentic Behaviour (Tool Accuracy, Planning, Recovery, Latency)" to agenticEvalCases,
    )

    val allResults = mutableListOf<EvalSuiteReport>()
    for ((suiteName, cases) in suites) {
        println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("Suite: $suiteName (${cases.size} cases)")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        val report = runner.runSuite(cases)
        allResults.add(report)
        printSuiteReport(suiteName, report)
    }

    printFinalReport(allResults)
}

private fun parseArgs(args: Array<String>): Map<String, String> {
    val map = mutableMapOf<String, String>()
    var i = 0
    while (i < args.size) {
        if (args[i].startsWith("--") && i + 1 < args.size) {
            map[args[i].removePrefix("--")] = args[i + 1]
            i += 2
        } else i++
    }
    return map
}

private fun printSuiteReport(name: String, report: EvalSuiteReport) {
    val pct = "%.0f".format(report.passRate * 100)
    println("\n  Suite: $name — ${report.passed}/${report.totalCases} ($pct%) in ${report.durationMs}ms")
    if (report.hardBlocked) println("  ⛔ HARD BLOCK — PII leak detected.")
    report.results.filter { !it.passed }.forEach { result ->
        println("\n  ✗ [${result.caseId}] ${result.description}")
        result.assertionResults.filter { !it.passed }.forEach { println("      → ${it.reason}") }
    }
}

private fun printFinalReport(reports: List<EvalSuiteReport>) {
    val total = reports.sumOf { it.totalCases }
    val passed = reports.sumOf { it.passed }
    val duration = reports.sumOf { it.durationMs }
    val hardBlocked = reports.any { it.hardBlocked }
    val pct = if (total == 0) 0.0 else passed.toDouble() / total * 100

    println("\n╔══════════════════════════════════════════╗")
    println("║           EVAL SUITE COMPLETE            ║")
    println("╠══════════════════════════════════════════╣")
    println("║  Total:    $passed / $total (${"%.0f".format(pct)}%)")
    println("║  Duration: ${duration}ms")
    if (hardBlocked) {
        println("║  ⛔ HARD BLOCK: PII LEAK — do not ship")
    } else if (pct >= 90) {
        println("║  ✓ PASS — above 90% threshold")
    } else {
        println("║  ✗ FAIL — below 90% threshold")
    }
    println("╚══════════════════════════════════════════╝")

    if (hardBlocked || pct < 90) System.exit(1)
}
