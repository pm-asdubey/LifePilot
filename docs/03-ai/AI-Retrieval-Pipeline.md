# AI Retrieval Pipeline

**Location:** `docs/03-ai/AI-Retrieval-Pipeline.md`

---

# Purpose

The AI Retrieval Pipeline converts natural language questions into accurate, explainable responses using the Life State Engine.

The LLM never has direct access to the entire database.

Instead, the pipeline retrieves only the relevant information required to answer the user's question.

This minimizes hallucinations, improves speed, reduces token usage, and ensures every response is grounded in structured data.

---

# Philosophy

The AI should never behave like a personal memory.

It should behave like an intelligent analyst.

It reasons over structured information rather than relying on conversation history.

Every answer should be reproducible.

If the same Life State exists tomorrow, the same question should produce the same factual answer.

---

# High-Level Pipeline

```text
User Question

↓

Intent Detection

↓

Entity Recognition

↓

Object Retrieval

↓

Relationship Expansion

↓

Timeline Retrieval

↓

Document Retrieval

↓

Metadata Retrieval

↓

Task Retrieval

↓

Context Assembly

↓

LLM Response

↓

Source Attribution
```

Every stage has a single responsibility.

---

# Step 1 — User Question

Examples

* Show my passport.
* What expires next month?
* Which interviews did I have in March?
* Do I still have active loans?
* What should I do after leaving my job?
* Show documents related to my house.

Questions may be:

* factual
* analytical
* historical
* comparative
* predictive
* procedural

---

# Step 2 — Intent Detection

The system determines what the user wants.

Supported intents include:

Retrieve Object

Example

Show my passport.

---

Find Documents

Example

Show all insurance documents.

---

Timeline Query

Example

What happened last week?

---

Task Query

Example

What should I do today?

---

Reminder Query

Example

What expires next?

---

Relationship Query

Example

Which documents belong to my property?

---

Summary Query

Example

Summarize my career.

---

Comparison Query

Example

Compare my current salary with my previous job.

---

Recommendation Query

Example

What should I update after renewing my passport?

---

# Step 3 — Entity Recognition

Extract referenced entities.

Example

"What expires next month?"

Entities

* Expiry
* Time Range

Example

"Show Dad's passport."

Entities

* Dad
* Passport

Example

"What happened after I joined Google?"

Entities

* Job
* Company
* Timeline

---

# Step 4 — Object Retrieval

Retrieve matching Objects.

Search order:

1. Exact match
2. Alias match
3. Semantic match
4. Relationship expansion

Returned data:

* Object ID
* Title
* Status
* Domain
* Confidence

---

# Step 5 — Relationship Expansion

Expand outward from retrieved Objects.

Example

Passport

↓

Visa

↓

Travel

↓

Insurance

↓

Tickets

The expansion depth should be configurable.

Default:

Depth = 2

The user may explicitly request broader context.

---

# Step 6 — Timeline Retrieval

Retrieve relevant historical events.

Examples

* Promotions
* Renewals
* Purchases
* Interviews
* Applications

Events should be returned chronologically.

---

# Step 7 — Document Retrieval

Retrieve supporting evidence.

Priority order:

1. Explicitly linked documents
2. Related documents through relationships
3. Semantically relevant documents

Original documents are never modified.

Only references are retrieved.

---

# Step 8 — Metadata Retrieval

Retrieve structured information.

Examples

Passport

* Number
* Expiry
* Country

Loan

* Principal
* Interest
* Balance

Property

* Address
* Purchase Date

Only relevant metadata should be included.

---

# Step 9 — Task Retrieval

Retrieve active tasks.

Example

User asks:

"I left my job."

Retrieve:

* Update Resume
* Update LinkedIn
* Review Insurance
* Apply to Companies

The AI should combine facts with actionable next steps.

---

# Step 10 — Context Assembly

The retrieval engine creates a structured context package.

Example

Question

↓

Relevant Objects

↓

Relevant Metadata

↓

Relevant Events

↓

Relevant Relationships

↓

Relevant Documents

↓

Relevant Tasks

↓

Relevant Reminders

↓

Final Context

Only this context is sent to the LLM.

The LLM should never receive the full database.

---

# Step 11 — Response Generation

The LLM produces:

* Natural language
* Bullet points
* Tables
* Recommendations
* Checklists

Responses must never invent facts.

Unknown information should be acknowledged rather than guessed.

---

# Step 12 — Source Attribution

Every factual statement should map back to its origin.

Possible sources:

* Object Metadata
* Timeline Event
* Uploaded Document
* User-entered Data
* AI Suggestion (if explicitly marked)

The UI should be able to display "Why am I seeing this?" for any AI-generated answer.

---

# Context Budget

The retrieval engine should minimize token usage.

Priority order:

1. Object Metadata
2. Active State
3. Relationships
4. Timeline
5. Tasks
6. Document Summaries
7. Raw OCR (only if required)

Large documents should be summarized before inclusion.

---

# Conversation Memory

The LLM's conversation memory is temporary.

The Life State Engine is permanent.

Conversation history should only provide conversational continuity.

Facts always come from the Life State Engine.

---

# Retrieval Modes

## Fast Mode

Optimized for quick answers.

* Minimal relationship expansion
* Metadata only
* No document parsing

Target latency: < 1 second (excluding LLM inference)

---

## Standard Mode

Default mode.

Includes:

* Objects
* Metadata
* Relationships
* Timeline
* Tasks

---

## Deep Analysis Mode

Used for complex questions.

Includes:

* Expanded relationship graph
* Multiple object comparison
* Document summaries
* Historical trends

Reserved for user-requested analysis.

---

# Privacy Rules

The retrieval engine should only retrieve information for the currently selected Profile unless the query explicitly references another Profile.

Example

"Show Dad's passport."

Only then should the retrieval scope include Dad's Profile.

---

# Error Handling

If retrieval returns no relevant information:

* Explain that no matching data exists.
* Suggest nearby Objects if appropriate.
* Never fabricate missing facts.

If multiple Objects match:

* Ask the user for clarification.
* Present likely matches.

---

# Future Compatibility

The retrieval pipeline should support future capabilities such as:

* Hybrid semantic + keyword search
* On-device embedding models
* Knowledge graph traversal
* Tool calling
* Multi-modal reasoning
* Federated cloud retrieval

These enhancements should replace individual stages without changing the overall pipeline.

---

# Summary

The AI Retrieval Pipeline ensures that every AI response is grounded in the Life State Engine.

The retrieval engine decides *what* information is relevant.

The LLM decides *how* to explain it.

This separation keeps AI accurate, efficient, explainable, and maintainable while allowing future improvements without redesigning the core architecture.
