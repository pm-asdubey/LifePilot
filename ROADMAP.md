# LifePilot Product Roadmap

This document defines the product direction across all releases.

`ISSUES.md` tracks specific bugs and feature requests. `TASKS.md` tracks active implementation work. This roadmap defines the shape of each release: what changes, why, and what it unlocks.

Features may move between releases as priorities evolve. Architectural principles do not change.

---

# Product Vision

LifePilot is an **AI Administrative Operating System**.

It maintains a continuously evolving, structured understanding of a person's life — and uses that understanding to help them plan and complete the administrative work that life requires.

The four layers of the platform:

```
Knowledge     →  Documents + Records
Life State    →  Domain Understanding
Planning      →  Goals + Tasks
Execution     →  Life Actions
```

Every release strengthens one or more of: **Knowledge · Life State · Planning · Execution · Privacy · Reliability**

---

# Version 1.0 — Foundation

**Status: Release Candidate — Stability hardening in progress**

## What was built

The core platform is complete. The Life State Engine, Schema Engine, Rule Engine, RetrievalEngine, and all primary features (Library, Timeline, Planner, AI Workspace, Search, Documents) are functional.

### Core Platform

* ✅ Offline-first architecture
* ✅ Local SQLite database (Room, version 3)
* ✅ File Storage Service
* ✅ Schema Engine (JSON-driven, 17 schemas)
* ✅ Rule Engine (reminder and task evaluation)
* ✅ Search Engine (Objects, Metadata, Documents, Goals, Tasks)
* ✅ Life State Engine (full pipeline)
* ✅ RetrievalEngine (keyword scoring, max 5 objects per AI request)
* ✅ PromptBuilder (pure formatting, no I/O)
* ✅ ObjectReasoner (per-object canonical AI snapshot)
* ✅ PlanningEngine (single mutation path for all goal and task changes)

### User Features

* ✅ Profiles
* ✅ Objects (create, view, archive, delete)
* ✅ Documents (upload, view, OCR, PDF rendering)
* ✅ Timeline (grouped, filterable)
* ✅ Tasks (generated, completable, goal-linked)
* ✅ Goals (created manually or via AI proposal, with status lifecycle)
* ✅ Reminders (rule-driven, notifications)
* ✅ Universal Search
* ✅ Dashboard (attention items, domain distribution)
* ✅ Object Relationships
* ✅ Library (domain-grouped object tree)
* ✅ Metadata Provenance (source, verification status)

### AI Features

* ✅ OCR (ML Kit)
* ✅ Metadata Extraction with user verification
* ✅ AI Workspace (dual-mode: Daily Brief / AI Chat)
* ✅ Structured AI proposals (MetadataUpdate, GoalProposal, TaskCreation, TaskCompletion, ObjectCreation)
* ✅ Morning Brief daily notification (9 AM)

### Remaining for 1.0 GA

* ⬜ Play Store listing assets
* ⬜ Critical bug fixes (see Version 1.0.x below)

---

# Version 1.0.x — Stability

**Goal: make the existing product reliable before expanding it**

The 1.0 platform has architectural gaps that cause incorrect behaviour under normal use. These must be resolved before any new features are added.

### Critical fixes (ISSUE references)

* ⬜ Search screen crash on certain devices — Material3 SearchBar instability (ISSUE-004)
* ⬜ All AI conversations append to the first conversation — `currentConversationId` never cleared (ISSUE-001)
* ⬜ Object status not updated on cancellation — missing STATUS_UPDATE action type (ISSUE-022)
* ⬜ Document re-upload appends duplicate metadata instead of updating (ISSUE-025)
* ⬜ Security gaps: no SQLCipher, FLAG_SECURE not set, sensitive data not redacted before AI (ISSUE-018)
* ⬜ Formal security audit: data flow diagram mapping what stays local vs what leaves the device (ISSUE-055)

---

# Version 1.1 — Domain Life State

**Goal: establish Domain Life State as the canonical source of understanding**

This is the most significant architectural upgrade since launch. Today, AI context is stored at the object level. Version 1.1 moves understanding up one level — to the domain — and wires every life event into keeping that understanding current.

After 1.1, LifePilot does not just hold records. It holds a continuously evolving understanding of each area of a person's life.

### Architecture changes

* ⬜ Domain Life State becomes canonical — understanding lives at the domain, not the object (ISSUE-050)
* ⬜ Domain Life State maintenance pipeline: every life event (OCR, conversation, task completion, status update, goal achievement) triggers domain re-evaluation (ISSUE-051)
* ⬜ AI reasoning uses domain life states as primary context; objects are supporting evidence (ISSUE-050)

### AI improvements

* ⬜ AI gathers details before creating records — no more shell objects (ISSUE-007)
* ⬜ AI follow-up interview: high-value events (new job, medical event, property, visa) trigger a structured 3–5 question sequence before anything is persisted (ISSUE-052)
* ⬜ Task completion updates domain life state (ISSUE-002)
* ⬜ Health symptoms routed to domain life state, not object records (ISSUE-010)
* ⬜ Cross-domain travel queries check documents across domains (ISSUE-015)
* ⬜ AI timeout retry: 3-attempt loop with 0s/5s/15s backoff (ISSUE-021)
* ⬜ Domain life state visible on home screen without requiring domain filter selection (ISSUE-011)

### Library improvements

* ⬜ Library types sourced from SchemaEngine, not hardcoded (ISSUE-027)
* ⬜ Proposal cards fully editable before approval — user modifies AI-suggested values inline (ISSUE-044)
* ⬜ Edit capability for all objects and metadata after creation (ISSUE-036)

### Chat improvements

* ⬜ Camera + file attachment in AI chat (ISSUE-026)

---

# Version 1.2 — Life Event Action Plan

**Goal: when a significant life event occurs, LifePilot understands every downstream implication and presents a complete, interactive action plan in a single step**

This is the product's most important upgrade. A job change does not create one record — it closes the old job, creates leaving tasks, opens the new record, asks about relocation, generates moving tasks, updates Career and Finance understanding, and queues every downstream change as an interactive checklist the user approves in one step.

### Life Event Action Plan (ISSUE-057)

* ⬜ `LifeEventClassifier` — detects high-impact event types (job change, relocation, medical, visa, property, financial)
* ⬜ `CascadingUpdateEngine` — configuration-driven templates defining every downstream record update, task, and domain re-evaluation per event type
* ⬜ `LifeEventActionPlanCard` — interactive inline UI in the chat: a checklist of all proposed updates, grouped by type, editable before confirmation
* ⬜ Conditional branches: "Are you relocating?" gates additional tasks (property search, booking tickets, address update tasks)
* ⬜ Sequential execution: each item executes in order, each completion updates the relevant domain life state
* ⬜ Conversation continues naturally while the plan is live — the plan card and the chat are two parallel threads in the same screen

### Plain-language and UX audit (ISSUE-053)

* ⬜ Remove all internal technical terminology from user-facing strings
* ⬜ "Object" → Record, "Domain" → Life Area, "Metadata" → Details, "Verification Status" → Confirmed/Unconfirmed
* ⬜ `CONTENT_GUIDELINES.md` created as the permanent standard for all UI copy

### Conversation improvements (ISSUE-056, ISSUE-059)

* ⬜ AI conversations auto-named from first message (5-word AI-generated title)
* ⬜ Manual conversation rename
* ⬜ Long-press copy, share, add-to-record on any message
* ⬜ AI message regenerate and feedback (thumbs up/down)
* ⬜ User message edit-and-resend
* ⬜ Select all / copy full conversation

### Record and task management fixes (ISSUE-037, ISSUE-038, ISSUE-039, ISSUE-029, ISSUE-028)

* ⬜ Tasks and goals editable after creation
* ⬜ Planner filters: Today / This Week / This Month + priority chips
* ⬜ Task priority field with colour coding
* ⬜ Verified / Unconfirmed status clearly visible on all record detail screens
* ⬜ Blank chat bubble no longer appears after record saved

### Notifications and documents (ISSUE-032, ISSUE-033, ISSUE-045)

* ⬜ Notification deep-links open the relevant record or task directly
* ⬜ In-app document viewer (no leaving the app to open a PDF)
* ⬜ Download as PDF + direct share for any document

---

# Version 1.3 — Polish and Schema

**Goal: make the product feel enterprise-quality and cover the full breadth of a person's administrative life**

### Onboarding and first run (ISSUE-014, ISSUE-017)

* ⬜ Onboarding flow with compass mascot (ISSUE-060)
* ⬜ Welcome screen with import/restore from previous export

### Compass mascot and motion identity (ISSUE-060, ISSUE-054)

* ⬜ Compass animation: pure Compose Canvas, settle animation with spring physics
* ⬜ Appears on: empty chat, splash, onboarding, morning brief card, search empty state
* ⬜ Full UI animation pass: navigation transitions, staggered list entrances, shared element transitions Library→Record detail, proposal card spring entrance, shimmer skeleton, press feedback, success/error states
* ⬜ All animations respect system reduced-motion setting

### Schema expansion (ISSUE-061)

* ⬜ Research and validate full object type set across all life domains
* ⬜ Finance: Bank Account, Credit Card, FD, SIP/Mutual Fund, Loan, PPF/NPS, Tax Filing
* ⬜ Career: Freelance Contract, Offer Letter, Appraisal, Professional Certification
* ⬜ Health: Prescription, Vaccination, Lab Result, Hospital Visit, Chronic Condition
* ⬜ Property: Rental Agreement, Utility Account, Vehicle, Vehicle Insurance, RC
* ⬜ Legal: Contract, NDA, Power of Attorney, Notarised Document
* ⬜ Subscriptions: Software, Streaming, Gym, Club Memberships
* ⬜ Education: Degree, Course, Scholarship, Student Loan
* ⬜ Identity: Voter ID, Birth Certificate, Marriage Certificate, Will/Testament

### Voice input (ISSUE-058)

* ⬜ Microphone button in AI chat — on-device `SpeechRecognizer`, text lands in field as editable draft before sending
* ⬜ Hold for continuous dictation mode
* ⬜ `RECORD_AUDIO` permission with clear rationale

### Search and retrieval improvements (ISSUE-041, ISSUE-020, ISSUE-005)

* ⬜ Vector/semantic search using all-MiniLM-L6-v2 ONNX — "health cover" finds "Star Health Insurance"
* ⬜ Two-stage retrieval: keyword pre-filter then semantic re-ranking
* ⬜ Contextual search placeholders based on recent records

### Backup and export (ISSUE-040, ISSUE-047, ISSUE-016)

* ⬜ Export ZIP includes documents, tasks, search history, conversations (not JSON-only)
* ⬜ Delta/incremental backup: only changed files re-exported using `manifest.json` + `contentHash`
* ⬜ Google Drive backup integration

### Other P3 quality improvements (ISSUE-008, ISSUE-030, ISSUE-034, ISSUE-019, ISSUE-023)

* ⬜ Proactive AI — LifePilot surfaces expiring documents, upcoming renewals without being asked
* ⬜ AI responses no longer open with "Based on Provided data" — natural, direct tone
* ⬜ Timeline canonical entity fully implemented
* ⬜ PII redaction layer before AI prompt construction
* ⬜ AI permission settings (per-domain opt-in for AI access)

---

# Version 2.0 — Hybrid Intelligence

**Goal: LifePilot gains a persistent, private intelligence layer that works fully offline and augments cloud AI with internet-aware reasoning**

This version introduces the Hybrid Intelligence Architecture — a four-layer model that separates deterministic rules from personal knowledge from internet intelligence from reasoning.

### Hybrid Intelligence Architecture (ISSUE-024)

* ⬜ Layer 1 — Deterministic Engine: rule-based answers, expiry checks, calendar logic — no model invoked
* ⬜ Layer 2 — Personal Knowledge Layer: encrypted local Life Graph, never sent to search engines
* ⬜ Layer 3 — Internet Intelligence Layer: Brave Search / Tavily abstraction for current information (visa requirements, processing times, market rates) — personal data never included in search queries
* ⬜ Layer 4 — Reasoning Layer: personal context + internet results + conversation history → response

### On-device SLM (ISSUE-024)

* ⬜ `EmbeddedModelInterface` — model-agnostic abstraction over any on-device inference backend
* ⬜ Default: Google AICore + Gemma 4 E2B (~1.8 GB, fully offline, agentic + function calling natively)
* ⬜ Fallback: MediaPipe LLM Inference API + Gemma 2B Q4 for devices where AICore is unavailable
* ⬜ Swappable via config to any model — no code change required
* ⬜ Offline mode routes all queries through Layers 1 and 2 only; Layer 3 and cloud AI disabled gracefully

### Multi-device sync

* ⬜ Encrypted backup ZIP as the sync unit (ISSUE-040 + ISSUE-047)
* ⬜ Drive-based sync: phone is authoritative, other devices pull from Drive
* ⬜ Conflict resolution: last-write-wins on metadata fields; documents are additive

---

# Version 2.1 — Voice and Multimodal

**Goal: hands-free LifePilot — voice in, voice out, the compass as the primary UI during voice interaction**

### AI Voicebot (ISSUE-062)

* ⬜ Full voice pipeline: on-device STT → LifeStateEngine/AI → on-device TTS (Android `TextToSpeech`)
* ⬜ Voice mode expands compass to fullscreen; needle animates during processing
* ⬜ User can interrupt mid-response by speaking
* ⬜ Subtitle scrolls below compass as response is read aloud
* ⬜ No cloud TTS in V1 — fully offline voice experience

---

# Version 3.0 — Life Actions

**Goal: LifePilot can complete administrative workflows — not just track them**

This is the most significant product expansion in LifePilot's history. The architecture gains a fourth layer: Execution. LifePilot does not just understand and plan — it acts.

A **Life Action** is a high-confidence, user-approved administrative workflow that LifePilot executes using the verified information it already holds. The user never sees "browser automation." They ask "help me complete this" and LifePilot prepares everything, asks only for what is missing, and executes after explicit approval at every irreversible step.

### Core architecture (ISSUE-063)

* ⬜ `LifeAction` domain model — action type, status, target URL, pre-fill field map, execution log, outcome
* ⬜ `LifeActionTemplate` — configuration-driven workflow definitions (fields, target, eligibility rules, confirmation requirements)
* ⬜ `LifeActionEngine` — reads from Domain Life State, builds pre-fill map, identifies missing fields, manages execution state
* ⬜ `WebViewExecutor` — managed WebView with JavaScript field injection; user reviews before submission
* ⬜ `ExecutionLog` — timestamped record of every step; recovery tasks if execution fails partway

### Planner integration

* ⬜ Planner distinguishes manual tasks from Life Action tasks
* ⬜ Life Action tasks show an "Execute" button
* ⬜ After successful execution: domain life state updated, task marked complete, outcome document stored

### Initial Life Action templates

* ⬜ Tourist visa application (Japan, UK, Schengen, USA)
* ⬜ Passport renewal (India — Passport Seva)
* ⬜ Job application form fill (standard ATS forms: Workday, Greenhouse, Lever)
* ⬜ Insurance claim initiation
* ⬜ Bank KYC update
* ⬜ Government form submission (PAN update, Aadhaar address change)

### Guiding principles (non-negotiable)

* No irreversible action without explicit, unambiguous user confirmation
* Verified metadata only — no inferred or guessed field values
* Execution layer never visible to the user as a concept — the product is completing a Life Action, not running browser automation
* Recovery task created automatically if execution fails mid-way

---

# Version 3.5 — Platform Expansion

**Goal: LifePilot on every device the user owns**

### Desktop app (ISSUE-046)

* ⬜ Tauri (Rust + WebView) — small binary, native performance
* ⬜ QR code sign-in: phone generates QR, desktop scans it, one-time stateless relay handshake — no LifePilot server holds credentials
* ⬜ Drive-direct from desktop after first connection
* ⬜ Manifest-driven on-demand document fetch with LRU cache (default 500 MB)
* ⬜ Same design tokens as web app

### iOS app (ISSUE-042)

* ⬜ Kotlin Multiplatform shared domain/use case layer
* ⬜ SwiftUI native UI
* ⬜ Core ML for on-device inference
* ⬜ iCloud Drive or Google Drive as sync backend

### Web app (ISSUE-043)

* ⬜ Next.js + TypeScript
* ⬜ Dexie.js for local-first offline storage
* ⬜ Read access + search + task management for V1
* ⬜ Drive OAuth for sync

---

# Version 4.0 — Shared Life

**Goal: LifePilot for families, not just individuals**

* ⬜ Family member profiles (ISSUE-031)
* ⬜ Shared objects (property, insurance, joint accounts)
* ⬜ Per-member permission model — granular visibility control
* ⬜ Shared timeline
* ⬜ Shared reminders and tasks

---

# Version 5.0 — Domain Packs

**Goal: LifePilot as an extensible platform — domain-specific capability installable without modifying the core app**

Domain Packs extend the schema, rules, Life Action templates, and AI prompts for a specific life area. Installing a Domain Pack never requires an app update.

Planned Domain Packs:

* Immigration (visa tracking, work permit renewals, citizenship applications)
* Property (mortgage management, rental income, maintenance scheduling)
* Healthcare (chronic condition management, care coordination, insurance claims)
* Tax (filing, deductions, audit trail)
* Estate Planning (will, power of attorney, asset inventory)
* Business (freelance contracts, invoicing, GST, company filings)
* Education (course tracking, certifications, children's school records)
* Travel (multi-trip planning, loyalty programmes, travel insurance)

---

# Version 6.0 — Intelligence Platform

**Goal: proactive, predictive, and explainable AI assistance grounded entirely in structured life data**

* ⬜ Life Health Score — a single number representing how well-organised a person's administrative life is, with specific improvement recommendations
* ⬜ Predictive reminders — surface "you should renew X before Y happens" before the user thinks to ask
* ⬜ Administrative recommendations grounded in domain life state comparisons
* ⬜ Long-term planning assistance — multi-year goal modelling

AI remains explainable and grounded in structured data at every step.

---

# Version 7.0 — Monetisation

**Goal: sustainable business model that aligns with the product's privacy principles**

* ⬜ Freemium model: on-device AI + single device + manual backup free; cloud AI credits + multi-device sync + automated backup as paid tier (ISSUE-048)
* ⬜ RevenueCat for Android/iOS in-app purchase; Stripe for web/desktop
* ⬜ Signed local entitlement token — no server call for basic feature gating
* ⬜ Referral/access code system: HMAC-signed offline codes, device-local redemption, CDN-hosted revocation list (ISSUE-049)

---

# Long-Term Principles

These principles remain unchanged across all releases.

* **Offline First** — everything possible works without connectivity
* **User Owns Their Data** — original files always accessible, never hidden
* **AI Retrieves, Never Owns** — AI suggestions require user verification before becoming canonical
* **Objects Over Folders** — everything revolves around structured records, not directories
* **User Verification Before Canonical Updates** — AI proposes, user confirms, system stores
* **Configuration-Driven Architecture** — schemas, rules, actions, and prompts are config, not code
* **Execution is a Last Step** — automation is always preceded by understanding, planning, and explicit approval

---

# Success Criteria

LifePilot succeeds when a user can:

* Find any important document in seconds.
* Understand the current state of their administrative life without being asked.
* Receive timely, contextual reminders without any manual configuration.
* Report a major life event — a new job, a move, a visa trip — and have LifePilot understand every implication and prepare everything needed.
* Complete a visa application, job application, or government form with a single approval.
* Trust that their most sensitive personal information never leaves their control.

---

# Living Document

This roadmap evolves as implementation experience, user feedback, and product priorities change.

Feature placement between versions is a current best estimate — it will shift as the product matures.

Architectural principles, however, do not change.
