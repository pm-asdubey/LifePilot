# Search Architecture

**Location:** `docs/04-architecture/Search-Architecture.md`

---

# Purpose

The Search Engine provides a unified mechanism for discovering information throughout LifePilot.

Search is not limited to documents.

It indexes and retrieves every entity within the Life State Engine.

The same search infrastructure powers:

* Universal Search
* AI Retrieval
* Duplicate Detection
* Relationship Suggestions
* Object Linking
* Metadata Lookup
* Dashboard Suggestions

---

# Philosophy

The user should never think about where something is stored.

Instead, they should think about what they are looking for.

Examples

"I need my passport."

"My SBI loan."

"The interview with Google."

"My health insurance."

The Search Engine is responsible for finding the correct information regardless of where it exists.

---

# Search Scope

Version 1 indexes:

Profiles

Domains

Objects

Metadata

Documents

OCR Text

Events

Tasks

Relationships

Reminders

Timeline Entries

Everything searchable should be searchable through one engine.

---

# Search Pipeline

```text
User Query

↓

Normalization

↓

Intent Detection

↓

Search Strategy Selection

↓

Candidate Retrieval

↓

Ranking

↓

Grouping

↓

Presentation
```

---

# Step 1 — Query Normalization

Convert input into a standard representation.

Examples

"passport"

↓

Passport

"pass port"

↓

Passport

"HDFC Bank"

↓

HDFC

Operations include:

* Lowercasing
* Whitespace normalization
* Stemming (future)
* Stop-word removal (where appropriate)

---

# Step 2 — Intent Detection

Determine what the user is searching for.

Supported intents

* Object
* Document
* Event
* Task
* Timeline
* Profile
* Metadata
* Relationship

Example

"passport"

↓

Object Search

Example

"documents for my house"

↓

Relationship Search

Example

"interviews last month"

↓

Timeline Search

---

# Step 3 — Search Strategy

Different queries require different retrieval methods.

## Exact Search

Used for:

* Passport Number
* PAN
* Aadhaar
* Registration Number
* Policy Number

---

## Prefix Search

Used for:

* Names
* Companies
* Cities

---

## Full Text Search

Used for:

* OCR
* Notes
* Descriptions

---

## Relationship Search

Used for:

* Objects connected to another Object

---

## Semantic Search (Future)

Embedding-based similarity search.

Not included in Version 1.

---

# Candidate Retrieval

Search retrieves potential matches.

Each candidate includes:

* Entity Type
* Identifier
* Match Score
* Match Reason

The engine should retrieve broadly before ranking.

---

# Ranking

Ranking determines the final order.

Ranking signals include:

* Exact match
* Prefix match
* Metadata match
* Recent activity
* Object status
* Relationship distance
* User interaction frequency

Higher relevance always appears first.

---

# Result Grouping

Results are grouped by entity.

Example

Search

"Google"

Results

Objects

* Job
* Interview

Documents

* Offer Letter
* Resume

Timeline

* Joined Google
* Promotion

Tasks

* Update Resume

Relationships

* Salary Account

Grouping improves discoverability.

---

# Search Result Card

Every result contains:

* Title
* Entity Type
* Domain
* Related Object
* Last Updated
* Highlighted Match

Selecting a result opens the corresponding screen.

---

# Live Search

Search updates continuously as the user types.

Target latency:

<100 ms

Search should remain responsive even with large datasets.

---

# Indexing

Indexes update automatically when:

* Object created
* Object updated
* Metadata changed
* Document uploaded
* OCR completed
* Event created
* Relationship added
* Task updated

The user should never manually rebuild indexes.

---

# Duplicate Detection

Search supports duplicate detection.

Signals include:

* File checksum
* OCR similarity
* Metadata similarity
* Object similarity

Possible duplicates are surfaced for review rather than merged automatically.

---

# Relationship Discovery

Search also identifies potential relationships.

Example

Vehicle Insurance

↓

Registration Number

↓

Existing Vehicle

↓

Suggest Relationship

Suggestions require user approval.

---

# AI Integration

AI does not query the database directly.

Instead:

Question

↓

Search Engine

↓

Relevant Objects

↓

Relevant Documents

↓

Relevant Timeline

↓

Relevant Tasks

↓

Context Builder

↓

LLM

Search is therefore the foundation of AI retrieval.

---

# Performance Targets

Autocomplete

<50 ms

Search Results

<200 ms

Object Retrieval

<100 ms

Full OCR Search

<300 ms

Performance should remain stable as data grows.

---

# Security

Search respects Profile boundaries.

Only data belonging to the active Profile is returned unless the query explicitly references another Profile.

Sensitive metadata should remain masked in previews where appropriate.

---

# Future Compatibility

Future versions may introduce:

* Semantic embeddings
* Voice search
* OCR handwriting search
* Multilingual search
* Fuzzy phonetic search
* Cloud-assisted indexing

These enhancements should plug into the existing Search Engine without changing higher layers.

---

# Design Principles

The Search Engine must be:

* Fast
* Predictable
* Explainable
* Extensible
* Offline-first
* Shared across the application

Search is infrastructure, not a feature.

Every subsystem should rely on the same search implementation.

---

# Summary

The Search Engine is the discovery layer of LifePilot.

Rather than indexing only files, it indexes the user's administrative life.

By serving Home, AI, OCR, Relationships and Universal Search through one architecture, the application maintains consistency, avoids duplication, and provides a single authoritative mechanism for locating information.
