package com.lifepilot.app.onboarding

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifepilot.data.ai.AiProviderFactory
import com.lifepilot.data.repository.PreferenceManager
import com.lifepilot.domain.ai.AiCompletionResult
import com.lifepilot.domain.engine.SchemaEngine
import com.lifepilot.domain.model.DomainLifeState
import com.lifepilot.domain.ocr.OcrResult
import com.lifepilot.domain.ocr.OcrService
import com.lifepilot.domain.repository.DomainRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.io.File
import java.time.Instant
import javax.inject.Inject

/**
 * Drives the lightweight two-path onboarding (paste-prompt + scan-document).
 *
 * Both paths feed text to the configured AI provider, which classifies it into per-life-domain
 * understanding that is written straight into [DomainLifeState] — no per-item approval — so the AI
 * can immediately retrieve it. The classification runs on [viewModelScope]; the UI stays on the
 * onboarding screen showing progress until it finishes (the AI may take as long as it needs).
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferenceManager: PreferenceManager,
    private val aiProviderFactory: AiProviderFactory,
    private val domainRepository: DomainRepository,
    private val ocrService: OcrService,
    private val schemaEngine: SchemaEngine,
) : ViewModel() {

    enum class Status { IDLE, PROCESSING, DONE, ERROR }

    data class UiState(
        val status: Status = Status.IDLE,
        val message: String? = null,
        val importedDomains: Int = 0,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    /** null = still loading the flag; true/false = whether onboarding is complete. */
    val completed: StateFlow<Boolean?> = preferenceManager.hasCompletedOnboarding
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Copyable prompt shown to the user to run in their existing AI assistant. */
    val promptTemplate: String = """
        List everything you confidently know about me, organised by life area:
        Career, Finance, Health, Identity, Travel, Property, Education, People, Legal, Home,
        Transport, Major Life Events.
        For each area, give specific facts you are confident about (not guesses) as short bullets.
    """.trimIndent()

    fun ingestPastedText(text: String) {
        if (text.isBlank() || _state.value.status == Status.PROCESSING) return
        _state.update { it.copy(status = Status.PROCESSING, message = "Organising what you shared…") }
        viewModelScope.launch {
            val imported = runCatching { classifyAndStore(text) }.getOrElse {
                Timber.e(it, "Onboarding paste classification failed"); 0
            }
            finish(imported)
        }
    }

    fun ingestScannedPdf(uri: Uri) {
        if (_state.value.status == Status.PROCESSING) return
        _state.update { it.copy(status = Status.PROCESSING, message = "Reading your document…") }
        viewModelScope.launch {
            val imported = runCatching {
                val path = resolveToFile(uri) ?: return@runCatching 0
                val ocr = ocrService.extractText(path, "application/pdf")
                val text = (ocr as? OcrResult.Success)?.text.orEmpty()
                if (text.isBlank()) 0 else classifyAndStore(text)
            }.getOrElse { Timber.e(it, "Onboarding scan classification failed"); 0 }
            finish(imported)
        }
    }

    fun skip() {
        viewModelScope.launch { preferenceManager.setOnboardingComplete() }
    }

    private suspend fun finish(importedDomains: Int) {
        preferenceManager.setOnboardingComplete()
        _state.update {
            it.copy(
                status = Status.DONE,
                importedDomains = importedDomains,
                message = if (importedDomains > 0) {
                    "Set up $importedDomains life ${if (importedDomains == 1) "area" else "areas"}."
                } else {
                    "You're all set."
                },
            )
        }
    }

    // ── AI classification ──────────────────────────────────────────────────────

    private suspend fun classifyAndStore(text: String): Int {
        val profileId = preferenceManager.getActiveProfileId() ?: return 0
        val provider = runCatching { aiProviderFactory.getProvider() }.getOrNull() ?: return 0
        val domains = schemaEngine.getAllDomains()
        val result = runCatching {
            provider.complete(
                systemPrompt = buildSystemPrompt(domains),
                userMessage = text.take(12000),
                conversationHistory = emptyList(),
            )
        }.getOrNull()
        val content = (result as? AiCompletionResult.Success)?.content ?: return 0
        return parseAndStore(content, profileId)
    }

    private fun buildSystemPrompt(domains: List<String>): String = """
You are organising a person's life information into structured "understanding" per life domain.

Valid domains: ${domains.joinToString(", ")}

From the user's text, extract what is confidently stated for each RELEVANT domain. Omit domains with
no information. Do not invent facts.

Respond with ONLY valid JSON — an object keyed by domain name, each value an object:
{
  "<Domain>": {
    "currentSituation": "<1-3 sentence factual summary>",
    "priorities": ["..."],
    "risks": ["..."],
    "openQuestions": ["..."],
    "recommendations": ["..."]
  }
}
    """.trimIndent()

    private suspend fun parseAndStore(content: String, profileId: String): Int {
        val jsonText = extractJsonObject(content) ?: return 0
        val obj = runCatching { JSONObject(jsonText) }.getOrNull() ?: return 0
        val now = Instant.now()
        var count = 0
        val keys = obj.keys()
        while (keys.hasNext()) {
            val domain = keys.next()
            val d = obj.optJSONObject(domain) ?: continue
            val situation = d.optString("currentSituation", "").trim()
            if (situation.isBlank()) continue
            val existing = domainRepository.getDomainLifeState(profileId, domain)
            domainRepository.upsertDomainLifeState(
                DomainLifeState(
                    profileId = profileId,
                    domain = domain,
                    currentSituation = situation,
                    currentPriorities = d.optJSONArray("priorities").toStringList(),
                    knownRisks = d.optJSONArray("risks").toStringList(),
                    openQuestions = d.optJSONArray("openQuestions").toStringList(),
                    recommendations = d.optJSONArray("recommendations").toStringList(),
                    recentChanges = listOf("Imported during onboarding"),
                    lastUpdated = now,
                    version = (existing?.version ?: 0) + 1,
                ),
            )
            count++
        }
        return count
    }

    /** Isolates the outermost {…} so markdown-fenced or prose-wrapped model output still parses. */
    private fun extractJsonObject(raw: String): String? {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        return if (start >= 0 && end > start) raw.substring(start, end + 1) else null
    }

    private fun JSONArray?.toStringList(): List<String> =
        if (this == null) emptyList()
        else (0 until length()).map { optString(it) }.filter { it.isNotBlank() }

    /** ML Kit scanner returns a file/content URI; give the OCR service a readable file path. */
    private fun resolveToFile(uri: Uri): String? {
        if (uri.scheme == "file") return uri.path
        return try {
            val out = File(context.cacheDir, "onboarding_scan_${System.currentTimeMillis()}.pdf")
            context.contentResolver.openInputStream(uri)?.use { input ->
                out.outputStream().use { input.copyTo(it) }
            }
            out.absolutePath
        } catch (e: Exception) {
            Timber.e(e, "Failed to resolve scanned document URI")
            null
        }
    }
}
