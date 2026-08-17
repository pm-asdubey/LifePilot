package com.lifepilot.data.engine

import com.google.common.truth.Truth.assertThat
import com.lifepilot.data.ai.AiProviderFactory
import com.lifepilot.domain.ai.AiCompletionResult
import com.lifepilot.domain.ai.AiProvider
import com.lifepilot.domain.model.DomainLifeState
import com.lifepilot.domain.repository.DomainRepository
import com.lifepilot.domain.repository.ObjectRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.Instant

class DomainLifeStateEngineImplTest {

    private val domainRepository = mockk<DomainRepository>(relaxed = true)
    private val objectRepository = mockk<ObjectRepository>(relaxed = true)
    private val aiProviderFactory = mockk<AiProviderFactory>(relaxed = true)
    // Non-relaxed: a relaxed mock would intercept the `complete$default` dispatch (the engine
    // calls complete() without the defaulted readTimeoutSeconds arg) and return a dummy result
    // instead of our stubbed Success, so the write path would never run.
    private val aiProvider = mockk<AiProvider>()

    private lateinit var engine: DomainLifeStateEngineImpl

    private val validUpdateResponse = """
        {
          "shouldUpdate": true,
          "currentSituation": "Looking for a new job in Bangalore.",
          "priorities": ["Update resume", "Apply to 5 companies this week"],
          "risks": ["Current contract ends in 2 months"],
          "openQuestions": ["Should I target product or service companies?"],
          "recommendations": ["Focus on companies hiring remote"],
          "recentChanges": ["User mentioned they are actively job-hunting"]
        }
    """.trimIndent()

    private val noUpdateResponse = """{"shouldUpdate": false}"""

    @Before
    fun setUp() {
        coEvery { aiProviderFactory.getProvider() } returns aiProvider
        every { objectRepository.observeObjectsByDomain(any(), any()) } returns flowOf(emptyList())
        coEvery { domainRepository.getDomainLifeState(any(), any()) } returns null
        coEvery { aiProvider.complete(any(), any(), any()) } returns AiCompletionResult.Success(validUpdateResponse, "test-model")
        engine = DomainLifeStateEngineImpl(domainRepository, objectRepository, aiProviderFactory)
    }

    // ── Empty domain list ─────────────────────────────────────────────────────

    @Test
    fun `evaluateAndUpdate with empty domains makes no AI call`() = runTest {
        engine.evaluateAndUpdate("profile-1", "hi", "hello", emptyList())
        coVerify(exactly = 0) { aiProvider.complete(any(), any(), any()) }
    }

    @Test
    fun `evaluateAndUpdate with empty domains makes no repository write`() = runTest {
        engine.evaluateAndUpdate("profile-1", "hi", "hello", emptyList())
        coVerify(exactly = 0) { domainRepository.upsertDomainLifeState(any()) }
    }

    // ── Domain cap ────────────────────────────────────────────────────────────

    @Test
    fun `evaluateAndUpdate caps evaluation at 3 domains`() = runTest {
        val manyDomains = listOf("Career", "Finance", "Health", "Travel", "Legal")
        engine.evaluateAndUpdate("profile-1", "msg", "resp", manyDomains)
        // At most 3 AI calls (one per domain, capped at 3)
        coVerify(atMost = 3) { aiProvider.complete(any(), any(), any()) }
    }

    @Test
    fun `evaluateAndUpdate deduplicates repeated domains`() = runTest {
        val duplicatedDomains = listOf("Career", "Career", "Career")
        engine.evaluateAndUpdate("profile-1", "msg", "resp", duplicatedDomains)
        // Only 1 unique domain → only 1 AI call
        coVerify(exactly = 1) { aiProvider.complete(any(), any(), any()) }
    }

    // ── Happy path — update written ───────────────────────────────────────────

    @Test
    fun `evaluateAndUpdate with valid AI response writes domain life state`() = runTest {
        engine.evaluateAndUpdate("profile-1", "I am looking for a new job", "I can help", listOf("Career"))
        coVerify { domainRepository.upsertDomainLifeState(any()) }
    }

    @Test
    fun `evaluateAndUpdate sets correct profileId on written state`() = runTest {
        val captured = slot<DomainLifeState>()
        coEvery { domainRepository.upsertDomainLifeState(capture(captured)) } returns Unit
        engine.evaluateAndUpdate("profile-abc", "job hunting", "I can help", listOf("Career"))
        assertThat(captured.captured.profileId).isEqualTo("profile-abc")
    }

    @Test
    fun `evaluateAndUpdate sets correct domain on written state`() = runTest {
        val captured = slot<DomainLifeState>()
        coEvery { domainRepository.upsertDomainLifeState(capture(captured)) } returns Unit
        engine.evaluateAndUpdate("profile-1", "job hunting", "I can help", listOf("Career"))
        assertThat(captured.captured.domain).isEqualTo("Career")
    }

    @Test
    fun `evaluateAndUpdate writes currentSituation from AI response`() = runTest {
        val captured = slot<DomainLifeState>()
        coEvery { domainRepository.upsertDomainLifeState(capture(captured)) } returns Unit
        engine.evaluateAndUpdate("profile-1", "job hunting", "I can help", listOf("Career"))
        assertThat(captured.captured.currentSituation).isEqualTo("Looking for a new job in Bangalore.")
    }

    @Test
    fun `evaluateAndUpdate starts version at 1 when no prior state exists`() = runTest {
        val captured = slot<DomainLifeState>()
        coEvery { domainRepository.getDomainLifeState(any(), any()) } returns null
        coEvery { domainRepository.upsertDomainLifeState(capture(captured)) } returns Unit
        engine.evaluateAndUpdate("profile-1", "job hunting", "I can help", listOf("Career"))
        assertThat(captured.captured.version).isEqualTo(1)
    }

    @Test
    fun `evaluateAndUpdate increments version when prior state exists`() = runTest {
        val existingState = DomainLifeState(
            profileId = "profile-1",
            domain = "Career",
            currentSituation = "Employed at TCS",
            currentPriorities = emptyList(),
            knownRisks = emptyList(),
            openQuestions = emptyList(),
            recommendations = emptyList(),
            recentChanges = emptyList(),
            lastUpdated = Instant.EPOCH,
            version = 5,
        )
        coEvery { domainRepository.getDomainLifeState("profile-1", "Career") } returns existingState
        val captured = slot<DomainLifeState>()
        coEvery { domainRepository.upsertDomainLifeState(capture(captured)) } returns Unit
        engine.evaluateAndUpdate("profile-1", "resigned", "I can help", listOf("Career"))
        assertThat(captured.captured.version).isEqualTo(6)
    }

    // ── shouldUpdate: false — no write ────────────────────────────────────────

    @Test
    fun `evaluateAndUpdate does not write state when AI returns shouldUpdate false`() = runTest {
        coEvery { aiProvider.complete(any(), any(), any()) } returns AiCompletionResult.Success(noUpdateResponse, "test-model")
        engine.evaluateAndUpdate("profile-1", "what is 2+2", "4", listOf("Finance"))
        coVerify(exactly = 0) { domainRepository.upsertDomainLifeState(any()) }
    }

    // ── AI unavailable ────────────────────────────────────────────────────────

    @Test
    fun `evaluateAndUpdate does not crash when AI provider throws`() = runTest {
        coEvery { aiProviderFactory.getProvider() } throws IllegalStateException("No provider configured")
        // Should not throw — engine must be resilient to missing AI configuration
        engine.evaluateAndUpdate("profile-1", "msg", "resp", listOf("Career"))
        coVerify(exactly = 0) { domainRepository.upsertDomainLifeState(any()) }
    }

    @Test
    fun `evaluateAndUpdate does not crash when AI call returns error`() = runTest {
        coEvery { aiProvider.complete(any(), any(), any()) } returns AiCompletionResult.Error("API timeout")
        engine.evaluateAndUpdate("profile-1", "msg", "resp", listOf("Career"))
        coVerify(exactly = 0) { domainRepository.upsertDomainLifeState(any()) }
    }

    // ── Malformed AI response ─────────────────────────────────────────────────

    @Test
    fun `evaluateAndUpdate does not crash or write when AI returns invalid JSON`() = runTest {
        coEvery { aiProvider.complete(any(), any(), any()) } returns AiCompletionResult.Success("{not valid json", "test-model")
        engine.evaluateAndUpdate("profile-1", "msg", "resp", listOf("Career"))
        coVerify(exactly = 0) { domainRepository.upsertDomainLifeState(any()) }
    }

    @Test
    fun `evaluateAndUpdate handles markdown-wrapped JSON from AI`() = runTest {
        val markdownWrapped = "```json\n$validUpdateResponse\n```"
        coEvery { aiProvider.complete(any(), any(), any()) } returns AiCompletionResult.Success(markdownWrapped, "test-model")
        val captured = slot<DomainLifeState>()
        coEvery { domainRepository.upsertDomainLifeState(capture(captured)) } returns Unit
        engine.evaluateAndUpdate("profile-1", "msg", "resp", listOf("Career"))
        // Should successfully parse through markdown wrapper
        assertThat(captured.isCaptured).isTrue()
        assertThat(captured.captured.currentSituation).isEqualTo("Looking for a new job in Bangalore.")
    }
}
