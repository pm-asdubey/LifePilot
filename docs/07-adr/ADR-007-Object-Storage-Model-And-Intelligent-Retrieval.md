# ADR-007: Object Storage Model and Intelligent Context Retrieval

**Status:** Accepted  
**Date:** 2026-06-28

---

## Context

LifePilot needs to handle an ever-growing set of Objects across many domains. Sending the entire Life State to the AI on every request is expensive, hallucination-prone, and unnecessary. The architecture must scale to hundreds of Objects without degrading AI response quality or exceeding model context limits.

Additionally, a clearer model of what an Object *contains* is needed to support export, object-level AI context, and document surfacing inside AI conversations.

---

## Decisions

### 1. Objects Are Self-Contained Life State Units

Each Object is independently complete. It owns:
- Original documents (file references)
- OCR output (structured text per document)
- Structured metadata (typed fields with provenance)
- AI-generated context (continuously updated narrative)
- User notes
- Events
- Goals
- Tasks
- Relationships
- History

The entire collection above is the Life State for that Object. No Object requires another Object to be complete.

### 2. Object Tree (Logical Hierarchy)

Objects are organised into Domain categories for UI display. Categories are presentational — they do not change the canonical Object entity. The `domain` field on `LifePilotObject` already encodes this.

The Library UI will present objects grouped by domain in a collapsible tree. Internally, no folder-based storage is used.

### 3. AI Context per Object

Each Object stores an `aiContext` string — a continuously-updated AI-generated narrative of the object's current state, open questions, and suggested actions. This replaces the need for the AI to re-derive context from raw metadata on every request.

`aiContext` is stored as a special metadata field with `fieldId = "ai_context"` and `source = AI_GENERATED`. This avoids schema changes.

### 4. Intelligent Context Retrieval (Decision 7)

Before every AI request, the system must:

1. **Detect intent** — identify which Object types are relevant to the user's query.
2. **Retrieve relevant Objects only** — using keyword matching against object titles, types, and domains.
3. **Retrieve related metadata** — for the matched objects only.
4. **Retrieve relevant Planner items** — goals and tasks linked to matched objects.
5. **Construct a scoped prompt** — never send all objects.

Maximum objects sent to AI: **5** (configurable).  
Maximum metadata entries per object: **10** (most recently updated).

This reduces token usage, improves response focus, and prevents cross-domain hallucination.

### 5. Automatic Object Creation via AI

The AI may propose creating a new Object when the user mentions a trackable life entity that does not yet exist. This is implemented as `AiProposal.ObjectCreation` and routed through `PlanningEngine.createObject()` after user approval.

### 6. Export (Future)

Every Object must be independently exportable as a ZIP containing:
- Life State JSON
- Original documents
- Metadata provenance
- AI context

A human-readable PDF summary is a future enhancement.

---

## Consequences

- `HomeViewModel.buildSystemPrompt()` is updated to use intelligent retrieval instead of loading all objects.
- `AiProposal` sealed class gains `ObjectCreation` subtype.
- `PlanningEngine` gains `createObject()` method.
- `ai_context` metadata field is reserved across all schema types.
- Library UI is updated to show domain-grouped Object Tree.
- No Room migrations required (all changes are additive or use existing schema).
