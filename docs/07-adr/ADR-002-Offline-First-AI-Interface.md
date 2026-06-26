# ADR-002: Offline-First AI Interface

## Status
Accepted

## Date
2026-06-27

## Context

LifePilot must provide AI-powered features (document extraction, natural language queries, intelligent summaries) while remaining fully functional without internet connectivity.

The core product promise is that personal data is never dependent on a remote service. AI is an enhancement, not the foundation.

## Decision

AI is treated as a **pluggable inference layer** on top of a fully functional offline application:

1. **AI never owns data** — all data lives in Room; AI only reads from it
2. **Offline fallback** — all AI features have a usable offline mode (local context queries, empty states with explanations)
3. **Provider independence** — the AI interface is defined in the domain layer as an abstraction; implementations for Anthropic, OpenAI, Gemini are swappable via DI binding
4. **User verification gate** — AI suggestions are never automatically committed to the database; they go through a user-verification UI step
5. **Local context building** — before any AI call, the `AiChatViewModel` builds a structured context string from local Room data; this becomes the system prompt for the AI

## Architecture

```
AiChatViewModel
  └─ builds context from ObjectRepository, TimelineRepository
  └─ calls AiProvider (interface)
      └─ AnthropicAiProvider (future impl)
      └─ OpenAiProvider (future impl)
      └─ LocalOfflineProvider (current impl — rule-based responses)
```

The `PreferenceManager` stores the configured provider name, API key, and model. On startup the DI graph selects the appropriate provider.

## Consequences

**Positive:**
- Core app works 100% offline
- Users control which AI provider (or none) powers the features
- No vendor lock-in; provider can be swapped in a single DI binding change
- Privacy-preserving: data leaves the device only when the user explicitly queries AI

**Negative:**
- Responses in offline mode are limited to keyword-based logic
- Context building must be kept concise to fit LLM context windows
- API key management in DataStore is basic (not encrypted in v1)

## Alternatives Considered

1. **Cloud-hosted AI with mandatory connectivity** — breaks offline-first promise
2. **On-device LLM (llama.cpp)** — too large for v1 distribution; planned for v3
3. **Single hardcoded provider** — limits user choice and creates vendor dependency
