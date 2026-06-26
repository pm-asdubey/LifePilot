# AI Prompt Specification

**Location:** `docs/03-ai/AI-Prompt-Specification.md`

---

# Purpose

This document defines every prompt used by LifePilot.

Prompt definitions are treated as versioned application assets.

No prompt should be embedded directly in application code.

Prompts should be:

* Version controlled
* Testable
* Reusable
* Configurable
* Observable

---

# Design Philosophy

The AI does not own the application.

The AI performs well-defined tasks.

Every prompt should have:

* One responsibility
* One expected output format
* One validation strategy

Prompts should never perform multiple unrelated tasks.

---

# Prompt Categories

Version 1 defines the following prompt types.

* Document Classification
* Metadata Extraction
* Object Matching
* Relationship Suggestion
* AI Summary
* AI Insights
* Question Answering
* Timeline Summary
* Duplicate Detection
* OCR Cleanup

Each prompt has its own specification.

---

# Prompt Structure

Every prompt contains:

```text
Prompt ID

Purpose

Input

Context

Instructions

Output Schema

Validation Rules

Fallback Strategy

Version
```

Prompts are immutable once released.

Changes require version increments.

---

# Prompt Registry

Every prompt receives a unique identifier.

Examples

```text
DOC_CLASSIFY_V1

PASSPORT_EXTRACT_V1

LOAN_EXTRACT_V1

OBJECT_MATCH_V1

RELATIONSHIP_V1

SUMMARY_V1

QA_V1
```

The Prompt Registry is the single source of truth.

---

# System Prompt

Every AI request begins with the same system instructions.

Goals

The model must:

* Remain factual.
* Never invent metadata.
* Never modify user data.
* Clearly distinguish facts from suggestions.
* Prefer structured metadata over OCR.
* Prefer verified metadata over AI-generated metadata.

Unknown information should be acknowledged rather than guessed.

---

# Document Classification Prompt

Purpose

Determine the uploaded document type.

Input

* OCR Text
* File Name
* MIME Type

Output

```json
{
  "documentType": "",
  "objectType": "",
  "confidence": 0.98,
  "reasoning": ""
}
```

The reasoning field is used for logging only.

---

# Metadata Extraction Prompt

Purpose

Extract structured metadata from a document.

Example

Passport

Expected Output

```json
{
  "holderName": "",
  "passportNumber": "",
  "issueDate": "",
  "expiryDate": "",
  "nationality": ""
}
```

Requirements

* Return only requested fields.
* Use null for missing values.
* Do not infer absent data.

---

# Object Matching Prompt

Purpose

Determine whether a document belongs to an existing Object.

Inputs

* Existing Objects
* Candidate Metadata
* OCR Summary

Output

```json
{
  "matched": true,
  "objectId": "OBJ_001",
  "confidence": 0.96,
  "reason": ""
}
```

The application always asks the user before merging.

---

# Relationship Suggestion Prompt

Purpose

Identify likely relationships.

Example

Passport

↓

Visa

↓

Suggest relationship

Output

```json
{
  "relationshipType": "",
  "source": "",
  "target": "",
  "confidence": 0.91
}
```

Suggestions require user confirmation.

---

# Summary Prompt

Purpose

Produce a concise summary of an Object.

Example

Passport

Output

* Current Status
* Expiry
* Important Notes
* Linked Objects

The summary should fit within one card.

---

# AI Insight Prompt

Purpose

Generate proactive recommendations.

Examples

* Passport expires before planned travel.
* Resume has not been updated after job change.
* Vehicle lacks insurance documentation.

Insights must always include supporting evidence.

---

# Question Answering Prompt

Purpose

Answer user questions using Life State context.

Inputs

* Retrieved Objects
* Metadata
* Timeline
* Tasks
* Relationships

The prompt must instruct the model:

* Never answer beyond supplied context.
* Never invent missing information.
* Cite the supporting Object IDs internally.

---

# Timeline Summary Prompt

Purpose

Summarize historical events.

Example

"What happened this month?"

Output

* Chronological bullets
* Important milestones
* Pending follow-ups

---

# OCR Cleanup Prompt

Purpose

Normalize noisy OCR.

Tasks

* Remove duplicates
* Correct spacing
* Preserve formatting
* Standardize dates

Never invent missing text.

---

# Output Format

Every prompt must return structured JSON.

Natural-language responses are generated only after validation.

Malformed responses should be rejected.

---

# Validation

Every prompt result is validated.

Checks include:

* JSON syntax
* Required fields
* Data types
* Enum values
* Date formats
* Confidence ranges

Invalid outputs trigger retries or fallback behavior.

---

# Prompt Versioning

Every prompt includes:

```text
Prompt ID

Version

Last Updated

Compatible Schema Version
```

Prompt changes should be backward compatible where practical.

---

# Prompt Testing

Every prompt requires:

* Golden test cases
* Edge cases
* Empty input tests
* Invalid document tests
* Regression tests

Prompt quality should be measured over time.

---

# Prompt Metrics

Track:

* Extraction accuracy
* Validation failures
* Retry rate
* Average latency
* Token usage
* User correction rate

These metrics help improve prompts without changing application logic.

---

# Provider Independence

Prompts must remain independent of any LLM vendor.

The application should support:

* Claude
* GPT
* Gemini
* Local models

Prompt definitions should require only minor provider-specific adapters.

---

# Security

Prompts must never include:

* Hidden application secrets
* API keys
* Local file paths
* Unnecessary personal data

Only the minimum context required for the task should be sent.

---

# Design Rules

Every prompt should:

* Solve one problem.
* Produce structured output.
* Be deterministic where possible.
* Minimize token usage.
* Prefer facts over inference.

Prompts are part of the application architecture, not implementation details.

---

# Summary

The AI Prompt Specification centralizes every interaction between LifePilot and an LLM.

By treating prompts as versioned, testable assets rather than scattered strings, the application remains maintainable, measurable and adaptable as AI models evolve.
