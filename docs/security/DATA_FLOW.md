# LifePilot Data Flow Diagram

This document maps every significant data path in LifePilot and classifies what
leaves the device, what stays local, and what protections apply.

It supports the P0 security audit tracked in `ISSUE-055`.

---

## Legend

- **Local only** — never leaves the device under normal operation.
- **To AI provider** — transmitted over HTTPS to the configured provider
  (NVIDIA NIM or Anthropic Claude) as part of a chat completion request.
- **To internet search** — future Layer 3 path; not implemented in V1.
- **To cloud storage** — future Drive backup path; not implemented in V1.

---

## 1. Document ingestion

```
User file (content:// URI)
        │
        ▼
DocumentUploadSheet / DocumentUploadViewModel
        │
        ▼
UploadDocumentUseCase
        │
        ▼
DocumentRepositoryImpl ──► FileStorageManager
        │                       │
        │                       ▼
        │              App-private filesDir/documents/<objectId>/
        │
        ▼
DocumentEntity + DocumentVersionEntity  ──►  Room (local only)
        │
        ▼
DocumentOcrWorker (WorkManager)
        │
        ▼
MlKitOcrService (on-device ML Kit)
        │
        ▼
OCR text stored in DocumentVersionEntity  ──►  Room (local only)
```

**Boundary:** Original file bytes and raw OCR text stay local.

---

## 2. AI metadata extraction

```
DocumentViewerScreen ──► MetadataVerificationViewModel
        │
        ▼
ExtractMetadataUseCase
        │
        ├─► SchemaEngine (local)
        │
        ▼
AiProvider.complete(systemPrompt, ocrText, history)
        │
        ▼
NVIDIA NIM / Anthropic API over HTTPS
        │
        ▼
JSON / regex parsing of suggested fields
        │
        ▼
MetadataVerificationScreen (user review)
        │
        ▼
metadataRepository.upsertMetadata(...) ──► Room (local only)
```

**Boundary:** The full OCR text is sent to the AI provider during extraction.
Sensitive values extracted from the OCR are shown to the user for verification
before they become canonical metadata.

---

## 3. AI chat / retrieval

```
HomeViewModel.sendMessage(userMessage)
        │
        ├─► ConversationRepository.saveMessage(userMessage)  ──► Room (local)
        │
        ├─► RetrievalEngine.retrieve(profileId, userMessage)
        │       │
        │       ├─► ObjectRepository / MetadataRepository / TaskRepository / ReminderRepository
        │       │
        │       ▼
        │   RetrievalContext (local objects, tasks, reminders, domain life states)
        │
        ├─► ObjectReasoner.buildSnapshot(...) (local)
        │
        ├─► PromptBuilderImpl.build(context, userMessage)
        │       │
        │       ▼
        │   System prompt with scrubbed metadata values
        │   (SensitiveFieldRegistry redacts known sensitive fields)
        │
        ▼
AiProvider.complete(systemPrompt, userMessage, history)
        │
        ▼
NVIDIA NIM / Anthropic API over HTTPS
        │
        ▼
parseAiResponse(...) ──► AiProposal
        │
        ▼
User approval ──► executeProposal ──► Repository write  ──► Room (local only)
```

**Boundary:**
- The system prompt is scrubbed before transmission.
- The user's current message and conversation history are transmitted as-is.
- AI proposals returned from the provider may contain values the user explicitly
  shared; those proposals are surfaced for approval before persistence.

---

## 4. Biometric lock

```
MainActivity.onCreate
        │
        ▼
BiometricLockScreen
        │
        ▼
BiometricPrompt
        │
        ▼
BiometricAuthManager (local only)
        │
        ▼
Android BiometricManager / Keystore-backed crypto
```

**Boundary:** Biometric data never leaves the device; Android handles matching
inside secure hardware / TEE.

---

## 5. API key storage

```
SettingsViewModel.saveAiConfig(apiKey)
        │
        ▼
EncryptedKeyStorage
        │
        ▼
Android Keystore (AES-256-GCM) + SharedPreferences ciphertext
```

**Boundary:** API keys are encrypted before disk storage and never logged.

---

## 6. Export / backup (V1)

```
SettingsViewModel.exportData
        │
        ▼
ExportDataUseCase
        │
        ▼
JSON manifest + payload written to filesDir/exports/
        │
        ▼
Android share sheet / file picker (user-controlled egress)
```

**Boundary:** V1 export is plaintext JSON. The user chooses the destination.
Encrypted export is planned for a future release.

---

## 7. Reminder evaluation

```
ReminderEvaluationWorker (WorkManager, local)
        │
        ▼
RuleEngineImpl
        │
        ├─► ObjectRepository / MetadataRepository (local)
        │
        ▼
ReminderEntity ──► Room (local only)
        │
        ▼
NotificationHelper (local notification, no network)
```

**Boundary:** Rule evaluation is entirely local.

---

## Outbound HTTP calls

| Caller | Destination | Payload contents |
|--------|-------------|------------------|
| `NvidiaAiProvider` | `https://integrate.api.nvidia.com/v1/chat/completions` | Scrubbed system prompt, user message, conversation history, model name, API key header. |
| `AnthropicAiProvider` | `https://api.anthropic.com/v1/messages` | Scrubbed system prompt, user message, conversation history, model name, API key header. |
| `UpdateRepositoryImpl` | `https://api.github.com/repos/<githubRepo>/releases/latest` | Repository slug only; no user data. |

No other network calls exist in V1.

---

## Review checklist

- [x] `PromptBuilderImpl` scrubs structured metadata before transmission.
- [x] Timber logs are scrubbed by `ScrubbingTree`.
- [x] `FLAG_SECURE` is set on `MainActivity`.
- [x] API keys are encrypted with Android Keystore.
- [ ] Room database encryption (SQLCipher) — deferred to P1.
- [ ] Export bundle encryption — deferred to P2.
- [ ] Onboarding biometric prompt — deferred to P2.
