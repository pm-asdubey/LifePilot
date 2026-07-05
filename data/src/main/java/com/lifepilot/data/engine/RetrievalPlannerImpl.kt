package com.lifepilot.data.engine

import com.lifepilot.data.ai.AiProviderFactory
import com.lifepilot.domain.ai.AiCompletionResult
import com.lifepilot.domain.engine.RetrievalPlanner
import com.lifepilot.domain.model.LifeStateSummary
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RetrievalPlannerImpl @Inject constructor(
    private val aiProviderFactory: AiProviderFactory,
) : RetrievalPlanner {

    override suspend fun planDomains(
        userQuery: String,
        documentType: String?,
        summary: LifeStateSummary,
    ): List<String> {
        if (summary.domainObjectCounts.isEmpty()) return emptyList()

        val provider = runCatching { aiProviderFactory.getProvider() }.getOrNull()
            ?: return emptyList()

        val domainList = summary.domainObjectCounts.entries
            .sortedByDescending { it.value }
            .joinToString("\n") { (domain, count) -> "- $domain ($count objects)" }

        val docLine = if (!documentType.isNullOrBlank())
            "Attached document type: $documentType\n" else ""

        val systemPrompt = """
            You are a retrieval planner for a personal life management assistant.
            Your only job is to decide which life domains are relevant to a given query.
            Reply with ONLY a comma-separated list of domain names from the available list. No explanation, no punctuation beyond commas.
        """.trimIndent()

        val userMessage = """
            Available life domains:
            $domainList

            ${docLine}User query: "$userQuery"

            Which domains should be loaded to answer this query comprehensively? Be generous — include any domain that may provide useful context (e.g. Finance for a job change, Legal for a property, Health for insurance).
            Reply with domain names only, comma-separated. Example: Career, Finance, Legal
        """.trimIndent()

        return runCatching {
            val result = provider.complete(
                systemPrompt = systemPrompt,
                userMessage = userMessage,
                conversationHistory = emptyList(),
                readTimeoutSeconds = 10,
            )
            when (result) {
                is AiCompletionResult.Success -> parseDomainList(result.content, summary.domainObjectCounts.keys)
                else -> emptyList()
            }
        }.getOrElse {
            Timber.w(it, "RetrievalPlanner: planning call failed, falling back to all domains")
            emptyList()
        }
    }

    private fun parseDomainList(raw: String, validDomains: Set<String>): List<String> {
        val validLower = validDomains.associateBy { it.lowercase() }
        return raw.split(",")
            .map { it.trim() }
            .mapNotNull { token -> validLower[token.lowercase()] }
            .distinct()
    }
}
