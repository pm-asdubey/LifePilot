# ADR-009: Attachment Queue Flow & Mandatory Projects

**Date:** 2026-07-04
**Status:** Accepted

## Context

Two recurring, regression-prone behaviours surfaced during on-device testing of the AI conversation:

1. **Attachment handling stored documents on scan.** `HomeViewModel.processAttachment` ran OCR +
   AI classification and then **persisted** the document (as a placeholder object or a pending
   proposal) the moment the user scanned/picked it — before any message was sent. Users experienced
   "the screen blanked for a long time, then the document was saved instantly without me sending
   anything." It also coupled storage to a classification step that could fail (see below), so a
   failed classify still wrote a placeholder. This violated the product principle that **AI proposes,
   the user verifies, the system stores** — storage was happening pre-verification.

2. **Grouped work did not reliably become a Project.** Projects are the core organizing abstraction
   (ADR-001), but the AI, when asked to do something multi-step (e.g. file taxes), sometimes emitted
   several standalone tasks with no Project. Relying on prompt instructions alone is not deterministic
   — model compliance varies.

Separately, the confirmed root cause of classification failures was `AiActionParser.parseAction`
throwing `JSONException` when the model wrote the action type as a bare token before the JSON
(`OBJECT_CREATION {…}`). That is a parser-robustness fix, documented in `CLAUDE.md`, not an ADR.

## Decision

### 1. Queue-on-scan, persist-on-send

`processAttachment(uri)` becomes a **queue** operation:

- Copy the file, set `pendingAttachment{Path,DisplayName,MimeType}` (renders a chip in the chat).
- Run OCR + classification **in the background**, holding the result in `pendingClassifiedAction`
  and `attachedDocumentContext` — **without storing anything**.

`sendMessage()` performs the persistence:

- The queued document (its OCR text + any pre-computed classification) is attached to the turn, so
  the AI answers grounded in it.
- After the answer, the classification is presented for **save via the normal approval path**
  (proposal card → object + PDF via `UploadDocumentUseCase` → `LifeStateEngine.processObjectEvent`).
- If classification failed, a general document record is saved so the file is never lost.
- `clearPendingAttachment()` resets the queue on send/dismiss.

Camera returns to a **normal camera** (`ActivityResultContracts.TakePicture`); only "Scan a document"
uses the ML Kit document scanner. A photographed document is still OCR'd/classified downstream.

### 2. Deterministic Project fallback

`ActionPlanNormalizer.ensureProject(plan)` (pure function, `domain/model/`) runs inside
`AiActionParser.parseActionPlan`. When a plan has **2+ `CreateTask` items and no `CreateProject`**, it
synthesizes a `CreateProject` (title/domain/emoji derived from the plan type, else the plan summary)
and links every otherwise-unlinked task/record to it. Applied at **parse time**, so the injected
Project appears in the proposal the user approves — no silent writes. A prompt-level MULTIPLE-TASK
RULE in `PromptBuilderImpl` remains as the first line of defence; the normalizer is the safety net.

## Consequences

**Positive**

- Storage now respects "AI proposes → user verifies → system stores"; nothing is written before the
  user sends/approves.
- The attachment flow is resilient: OCR/classification can fail without losing the file or blocking
  the chat.
- Projects are guaranteed for grouped work regardless of model behaviour — deterministic, no extra
  AI round-trip, fast.

**Negative / trade-offs**

- The synthesized Project's name is derived deterministically, so it can be less descriptive than an
  AI-authored title for `CUSTOM` plans (it uses the plan summary). A follow-up could add an optional
  AI naming call with the deterministic name as fallback.
- Classification now happens twice in some paths (background on scan, and again on send if the
  context wasn't set) — acceptable, the background result is reused when available.

## Testing

- `ActionPlanNormalizerTest` (6) — injection, single-task skip, existing-project skip, type-based
  title/domain, summary-based title, preserving explicit `projectItemId`.
- `AiActionParserTest` — stray action-type prefix + markdown-fence recovery.
- `ThinkingMessagesTest` (3) — the always-cycling indicator copy.

## Related

- ADR-001 (Projects as a first-class concept)
- ADR-002 (Offline-first AI interface — AI retrieves, never the source of truth)
- ADR-008 (Architecture stabilization pass)
- `CLAUDE.md` → "Attachment → chat flow", "Projects are mandatory for grouped work", "AI Action Block Parsing"
