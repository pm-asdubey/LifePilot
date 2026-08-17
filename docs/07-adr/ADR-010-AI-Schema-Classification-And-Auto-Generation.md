# ADR-010: AI-Driven Schema Classification and Auto-Generation on OCR Completion

**Status:** Accepted — Not Yet Implemented
**Date:** 2026-07-16

---

## Context

LifePilot currently ships with 32 hand-authored JSON schemas bundled as assets. When a user
uploads a document, OCR runs via ML Kit and the extracted text is attached to the conversation
for the AI to reason over.

The current flow has two problems:

1. **Unknown document types are unclassified.** If the user scans a document whose type does
   not match any of the 32 bundled schemas, the system has no structured representation for it.
   The AI sees the OCR text but cannot associate it with a schema, so no typed metadata is
   extracted and the document is stored as a generic file.

2. **Schema suggestion requires user schema review.** An earlier proposal required the user to
   review and approve the AI-suggested schema structure before the document could be stored.
   This is the wrong granularity — schema design is not a user-facing concept. The user should
   only ever authorise what data is saved, not how the data type is defined.

---

## Decision

### Phase 1 — Classification (always runs)

Immediately after OCR completes (inside the existing OCR WorkManager worker, before any
conversation message is sent), fire a **lightweight AI classification call**:

- **Input:** OCR extracted text + the list of all currently registered schema `objectType`
  names and their `displayName` and `description` fields (not the full schema — just enough
  to identify them).
- **Output:** The best-matching `objectType` from the existing schema list, or `UNKNOWN` if
  no schema fits with sufficient confidence.
- **Timeout:** 8 seconds. On failure or timeout, fall back to `UNKNOWN` and continue.
- **Cost:** Minimal — small input, single-token-class output.

This call happens **in the background**, invisible to the user. No UI is shown. The result is
stored on the pending attachment state (`pendingClassifiedAction`) as already exists in
`HomeViewModel`.

### Phase 2 — Schema Auto-Generation (only when classification returns UNKNOWN)

If Phase 1 returns `UNKNOWN`, fire a second AI call immediately:

- **Input:** OCR extracted text + the JSON structure of one representative existing schema
  (e.g. `job.json`) as a template showing the expected format (fields, fieldTypes,
  aiExtractable, sensitivityLevel, validationRules).
- **Output:** A complete `ObjectSchema`-compatible JSON for the new document type, including:
  - `objectType` (PascalCase, no spaces)
  - `domain` (must match one of the canonical domains from `SchemaEngine.getAllDomains()`)
  - `displayName`
  - `fields` array with `fieldId`, `fieldType`, `aiExtractable`, `sensitivityLevel`
  - At minimum one `required: true` field
- **System accepts the schema immediately.** No user review of the schema structure.
- The schema is registered into `SchemaEngine`'s `registeredSchemas` StateFlow at runtime
  and persisted to a user-schemas directory (separate from the bundled assets folder) so it
  survives restarts.

### Phase 3 — Metadata Extraction and User Authorisation

Once a schema exists (either matched in Phase 1 or generated in Phase 2), the existing
metadata extraction flow runs:

- AI extracts field values from the OCR text using the schema's `aiExtractable` fields.
- The result is presented to the user as an `OBJECT_CREATION` proposal card in the chat —
  exactly as today.
- **The user authorises only the data save** (object + metadata), not the schema.
- On approval: object is created, metadata stored, Life State Engine triggered.

---

## What the User Sees

```
User scans unknown document
    ↓ [background, invisible]
OCR completes
    ↓ [background, invisible]
Classification call → UNKNOWN
    ↓ [background, invisible]
Schema generation call → new schema registered silently
    ↓ [background, invisible]
Metadata extraction using new schema
    ↓ [shown to user]
"I found a [new type]. Here's what I extracted — shall I save it?"
User taps Save
    ↓
Object created, metadata stored, Life State Engine runs
```

The schema generation step is entirely invisible. From the user's perspective, the app
understood their document and asked to save it — same experience as a known document type.

---

## Key Constraints

- **Schema auto-generation is a fallback only.** If Phase 1 classifies successfully,
  Phase 2 never runs. Avoid unnecessary API calls.
- **Generated schemas are additive.** They do not modify or replace bundled schemas.
- **Domain must be canonical.** The AI must be constrained to pick a `domain` from
  `SchemaEngine.getAllDomains()`. Reject and retry once if the returned domain is not in
  the canonical list.
- **Generated schemas are persisted to a user-schemas directory** (e.g.
  `data/user_schemas/` in internal storage), not written into the assets folder. The
  `SchemaEngineImpl.loadSchemas()` must be extended to load from both locations.
- **Sensitive field detection.** After schema generation, run each `fieldId` through
  `SensitiveFieldRegistry.isSensitive()`. Any field that matches must have its
  `sensitivityLevel` set to `SENSITIVE` regardless of what the AI returned.
- **No schema review screen.** Do not introduce a SchemaVerificationScreen or any
  intermediate UI for schema approval. This is a deliberate product decision — schema
  structure is not a user-facing concept.

---

## Implementation Touchpoints

| Component | Change Required |
|---|---|
| `OCR WorkManager Worker` | Trigger classification call on OCR completion |
| `SchemaEngineImpl` | Load from both assets and user-schemas directory |
| `SchemaEngine` interface | Add `registerSchema(schema: ObjectSchema)` method |
| New: `SchemaClassifier` (domain interface) | Phase 1 classification call |
| New: `SchemaGenerator` (domain interface) | Phase 2 schema generation call |
| New: `SchemaClassifierImpl` / `SchemaGeneratorImpl` (data) | AI call implementations |
| `SensitiveFieldRegistry` | Post-process generated schema field sensitivity |
| `HomeViewModel` / `pendingClassifiedAction` | Already receives the classification result — no structural change needed |

---

## What Is Not Changing

- The user authorisation step for data saving is unchanged.
- `MetadataVerificationScreen` flow is unchanged.
- `SensitiveFieldRegistry` scrubbing of prompts is unchanged.
- Bundled schemas are unchanged — auto-generated schemas are additive only.
- The `aiExtractable` flag per field remains the mechanism that controls which fields the
  AI attempts to extract from documents.

---

## Rationale

Schema structure is an implementation detail. The user cares that their data is saved
correctly — not that the app invented a new data type to hold it. Asking the user to
review a JSON schema structure is the wrong abstraction level and adds friction to a flow
that should feel effortless.

The authorisation boundary is: **what data is saved = user decision. How the data is
typed internally = system decision.**

This mirrors how every other intelligent system works: the database schema is never
shown to the end user.
