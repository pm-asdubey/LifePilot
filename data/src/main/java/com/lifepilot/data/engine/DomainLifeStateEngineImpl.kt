package com.lifepilot.data.engine

import com.lifepilot.data.ai.AiProviderFactory
import com.lifepilot.domain.engine.DomainLifeStateEngine
import com.lifepilot.domain.model.DomainLifeState
import com.lifepilot.domain.repository.DomainRepository
import com.lifepilot.domain.repository.ObjectRepository
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import org.json.JSONObject
import timber.log.Timber
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DomainLifeStateEngineImpl @Inject constructor(
    private val domainRepository: DomainRepository,
    private val objectRepository: ObjectRepository,
    private val aiProviderFactory: AiProviderFactory,
) : DomainLifeStateEngine {

    override suspend fun evaluateAndUpdate(
        profileId: String,
        userMessage: String,
        aiResponse: String,
        affectedDomains: List<String>,
    ) {
        if (affectedDomains.isEmpty()) return
        val provider = runCatching { aiProviderFactory.getProvider() }.getOrElse {
            Timber.w("DomainLifeStateEngine: no AI provider available")
            return
        }

        // Deduplicate and cap to avoid too many sequential AI calls per turn.
        val domainsToEvaluate = affectedDomains.distinct().take(3)

        for (domain in domainsToEvaluate) {
            runCatching {
                updateDomain(profileId, domain, userMessage, aiResponse, provider)
            }.onFailure { Timber.e(it, "DomainLifeStateEngine: failed updating domain=$domain") }
        }
    }

    private suspend fun updateDomain(
        profileId: String,
        domain: String,
        userMessage: String,
        aiResponse: String,
        provider: com.lifepilot.domain.ai.AiProvider,
    ) {
        val current = domainRepository.getDomainLifeState(profileId, domain)

        // Load object titles/types for this domain to give the AI structural context.
        val domainObjects = runCatching {
            objectRepository.observeObjectsByDomain(profileId, domain)
                .catch {}
                .firstOrNull() ?: emptyList()
        }.getOrElse { emptyList() }

        val objectSummary = if (domainObjects.isEmpty()) {
            "No tracked objects yet."
        } else {
            domainObjects.take(10).joinToString(", ") { "${it.title} (${it.objectType})" }
        }

        val currentStateBlock = if (current == null || current.currentSituation.isBlank()) {
            "(No existing understanding — create a fresh assessment.)"
        } else {
            buildString {
                appendLine("Current Situation: ${current.currentSituation}")
                if (current.currentPriorities.isNotEmpty())
                    appendLine("Priorities: ${current.currentPriorities.joinToString("; ")}")
                if (current.openQuestions.isNotEmpty())
                    appendLine("Open Questions: ${current.openQuestions.joinToString("; ")}")
                if (current.recommendations.isNotEmpty())
                    appendLine("Recommendations: ${current.recommendations.joinToString("; ")}")
            }
        }

        val systemPrompt = """
You are a life-management AI maintaining a structured understanding of a person's $domain domain.

Your job: decide if the recent conversation contains information that meaningfully updates the $domain understanding. If it does, produce an updated assessment. If it does not, respond with {"shouldUpdate": false}.

Tracked objects in this domain: $objectSummary

Current $domain understanding:
$currentStateBlock

RECENT CONVERSATION:
User: $userMessage
Assistant: $aiResponse

INSTRUCTIONS:
- If the conversation reveals a new situation, decision, plan, concern, milestone, or open question in the $domain domain, update the understanding.
- If the conversation is a simple factual question with no new life context, respond {"shouldUpdate": false}.
- Keep each section concise and grounded in real information the user has shared.
- recentChanges should capture what specifically changed in this conversation (1-2 items max).

Respond with ONLY valid JSON — no other text:
{
  "shouldUpdate": true,
  "currentSituation": "<1-3 sentence factual summary of current state>",
  "priorities": ["<item>", "..."],
  "risks": ["<item>", "..."],
  "openQuestions": ["<item>", "..."],
  "recommendations": ["<item>", "..."],
  "recentChanges": ["<what changed in this conversation>"]
}
        """.trimIndent()

        val result = provider.complete(
            systemPrompt = systemPrompt,
            userMessage = "Update the $domain life state based on the conversation above.",
            conversationHistory = emptyList(),
        )

        val responseText = when (result) {
            is com.lifepilot.domain.ai.AiCompletionResult.Success -> result.content
            else -> return
        }

        parseDomainUpdate(responseText, current, profileId, domain)?.let { updated ->
            domainRepository.upsertDomainLifeState(updated)
            Timber.d("DomainLifeStateEngine: updated domain=$domain")
        }
    }

    private fun parseDomainUpdate(
        responseText: String,
        current: DomainLifeState?,
        profileId: String,
        domain: String,
    ): DomainLifeState? {
        // Extract the JSON block. Models frequently wrap the object in markdown fences
        // (```json … ```) or add prose around it, so isolate the outermost {…} span rather
        // than relying on fence delimiters, which silently dropped fenced responses before.
        val trimmed = responseText.trim()
        val start = trimmed.indexOf('{')
        val end = trimmed.lastIndexOf('}')
        val jsonText = if (start >= 0 && end > start) trimmed.substring(start, end + 1) else trimmed

        return runCatching {
            val json = JSONObject(jsonText)
            if (!json.optBoolean("shouldUpdate", true)) return null

            val version = (current?.version ?: 0) + 1
            DomainLifeState(
                profileId = profileId,
                domain = domain,
                currentSituation = json.optString("currentSituation", current?.currentSituation ?: ""),
                currentPriorities = json.optJSONArray("priorities")?.toStringList()
                    ?: current?.currentPriorities ?: emptyList(),
                knownRisks = json.optJSONArray("risks")?.toStringList()
                    ?: current?.knownRisks ?: emptyList(),
                openQuestions = json.optJSONArray("openQuestions")?.toStringList()
                    ?: current?.openQuestions ?: emptyList(),
                recommendations = json.optJSONArray("recommendations")?.toStringList()
                    ?: current?.recommendations ?: emptyList(),
                recentChanges = json.optJSONArray("recentChanges")?.toStringList()
                    ?: emptyList(),
                lastUpdated = Instant.now(),
                version = version,
            )
        }.onFailure { Timber.w(it, "DomainLifeStateEngine: failed parsing JSON for domain=$domain") }
            .getOrNull()
    }

    private fun org.json.JSONArray.toStringList(): List<String> =
        (0 until length()).map { getString(it) }.filter { it.isNotBlank() }
}
