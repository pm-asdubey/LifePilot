# Product Requirements Document — LifePilot

**Version:** 2.0 — Reflects built state  
**Status:** V1.0 Release Candidate. Stability pass in progress.  
**Last Updated:** July 2026  
**Audience:** Lead PM, Engineering Lead

---

## What This Is

LifePilot is an offline-first Android application that organizes the administrative layer of a person's life. Not a document manager. Not a to-do app. Not a chatbot. The closest analogy is an operating system for your personal records — a place where your passport, job, insurance, property, loan, and health records exist as structured, living entities with metadata, timelines, expiry dates, and tasks. Not files in folders. Actual things you can reason about.

You tell it "I joined a new job." It understands every downstream implication. You ask it "are my documents ready for my US trip?" It checks your passport expiry, notices you're missing a US visa, and generates the tasks. That's the gap we're trying to close.

---

## Table of Contents

1. [The Problem We're Solving](#1-the-problem-were-solving)
2. [The Strategic Bet](#2-the-strategic-bet)
3. [Who This Is For](#3-who-this-is-for)
4. [The Principles That Actually Constrain Us](#4-the-principles-that-actually-constrain-us)
5. [What's Live — V1.0](#5-whats-live--v10)
6. [How The Product Works](#6-how-the-product-works)
7. [Feature Specs](#7-feature-specs) — Document Ingestion, AI Workspace, Record Library, Projects, Onboarding, Biometric Lock, Life Event Action Plan, Life Completeness Score, Universal Search, Backup and Export
8. [The AI Layer](#8-the-ai-layer)
9. [Architecture — What a PM Needs to Know](#9-architecture--what-a-pm-needs-to-know)
10. [Roadmap](#10-roadmap)
11. [Open Questions](#11-open-questions)

---

## 1. The Problem We're Solving

Everyone I've spoken to about this has the same version of the same story. You're sitting across from a bank manager, a visa officer, an HR executive — and they ask for a document you know you have somewhere. Is it in WhatsApp? Your email? A folder you made when you moved? Did you scan it? Did a family member send it to you at some point? You open your phone and start scrolling through Photos.

That's embarrassing and avoidable. But the deeper problem is structural. Modern adult life requires managing dozens of important records across a dozen incompatible apps — your passport in your photos, your insurance PDF on Google Drive, your offer letter in Gmail, your property documents at your parents' house. None of these tools know what a passport actually is. They store the file. They don't know it expires. They don't know renewal takes 45 days and you're travelling in 60.

The result is that most people manage their administrative life reactively — scrambling when something expires, guessing when deadlines are, losing documents that cost thousands to replace. It's one of the highest-stress, lowest-intelligence parts of being an adult. And there's no good software for it.

Current tools manage files. LifePilot manages life state.

---

## 2. The Strategic Bet

The obvious way to build this is cloud-native. Backend, accounts, email parsing, calendar sync, Salesforce-style ingestion. That's eventually where this product could go.

We're not building that first, and I want to be clear about why — because it's a deliberate choice, not a resource constraint.

Cloud-native requires trust before utility. A user needs to hand over their passport number, Aadhaar, PAN, health records, and salary details before the product proves it's worth trusting. That's a high bar to clear. We'd spend the first year earning trust we could have baked in architecturally.

The offline-first path earns trust by design. Everything is on the device. No server holds your data. No breach exposes your passport number. The export is a ZIP you hold. The AI never sees your raw documents — only the structured metadata we construct from them, and only after you've verified each field. That's a promise we can make with a straight face from day one.

The second reason I believe in this path: it forces the data model to be genuinely good. When you can't rely on cloud sync and server-side intelligence, the on-device schema has to carry real weight. The schema engine, the structured metadata, domain life states — these are what make LifePilot a life manager rather than a fancy folder system. A cloud product can paper over a shallow model with integrations. We can't.

The corollary that matters for every product decision: AI is an interface to the data, not a replacement for it. If AI is wrong about your passport expiry date, you need to be able to see that, correct it, and trust the correction stuck. That's the verification-before-canonical-update principle. It's not a safety feature. It's the trust model.

---

## 3. Who This Is For

**The primary user is someone in their late 20s or 30s who has real administrative complexity and is tired of feeling disorganized about it.**

She has a passport, a few insurance policies, a job she may have changed recently, a flat she rents or owns, investments she doesn't fully track, and a low-grade anxiety that she's missing something somewhere. She's not a spreadsheet person but she's not completely analog either. She has WhatsApp, Google Drive, and Gmail all functioning as accidental document storage. She would genuinely pay for something that made her feel on top of her administrative life.

What she needs isn't another place to store files. She needs something that understands what those files mean — and tells her what to do about them.

**The secondary user is someone managing another person's administrative life alongside their own.**

Parents tracking elderly parents' healthcare and document renewals. A person handling a sibling's paperwork during a medical event. A partner who ends up being the "organized one" in a household. Multi-profile support covers this use case. It's not a separate audience — it's the same user, managing more people.

**Who we are explicitly not designing for right now:** enterprise buyers, document management administrators, or data scientists. We're building a consumer product for individuals. The complexity of the data model can serve sophisticated users, but the UX should never require sophistication to use.

---

## 4. The Principles That Actually Constrain Us

I'm not listing these because they look good in a document. These are the principles we've used to make real decisions, and any future decision that contradicts one of them needs to explain itself.

**Offline first. Everything works without internet except AI completions.**

OCR, rule evaluation, reminders, search, record management — all local. If we ever design a feature that requires connectivity as a baseline, we've broken the model. AI is an enhancement you get when you're online. It's not the core product.

**User owns their data. Structurally.**

The files are on the device. The export is theirs. `android:allowBackup="false"` so even Android's cloud backup doesn't touch it without explicit user action. We should be able to say to any user: your data is on your phone, here's the button to take it with you. If that's ever untrue, we've made a wrong turn.

**AI retrieves, never owns truth.**

Every AI suggestion is a proposal. The user verifies. Only approval triggers a database write. This is non-negotiable — not because AI makes mistakes (though it does) but because we are asking people to store their most sensitive documents with us. The verification model is the product's credibility.

**Records over folders.**

A passport is not a PDF. It's an entity: expiry date, issuing country, linked visas, upcoming tasks, renewal reminders. Everything in LifePilot is a structured record, not a file path. The folder metaphor is a concession to legacy behaviour we're specifically not making.

**Events change state; state is always current.**

Every significant action — uploading a document, completing a task, telling the AI about a new job — flows through the Life State Engine and produces an updated, accurate current state. The timeline is an immutable log. The current state is always the authoritative answer to "what's my situation right now?"

**Configuration over code.**

New record types are JSON files, not code changes. A new visa category, a new financial product, a country-specific document schema — these are config additions. This is what makes future domain packs possible. If we violate this principle and start hardcoding record types, we create a ceiling on the product's scope.

---

## 5. What's Live — V1.0

The core platform is complete and at release candidate stage.

**Platform foundations:**
- Offline-first SQLite database via Room, version 3
- Private file storage with SHA-256 checksums for deduplication
- Schema Engine — 17 bundled record type schemas (passport, driving licence, insurance, property, vehicle, will, job, bank account, health, tax, investment, education, loan, pension, subscription, travel, utilities)
- Life State Engine — full pipeline from document upload through OCR through metadata extraction through task generation through reminder evaluation
- Rule Engine — evaluates schema-defined reminder rules every 6 hours via WorkManager
- RetrievalEngine — keyword scoring to select up to 5 relevant records per AI request
- PlanningEngine — single mutation path for all goal and task state changes

**User-facing features:**
- Record Library — domain-grouped tree with sticky headers and domain filter tabs
- Record Detail — overview, details with provenance badges, documents, timeline, tasks, relationships
- Document upload — file picker and camera, ML Kit OCR in background
- AI Metadata Extraction — field suggestions from OCR text, user verifies before saving
- Verification status on every metadata field (Confirmed / Unconfirmed / Rejected)
- Timeline — immutable activity log, filterable by source type
- Universal Search — records, metadata values, documents, goals, tasks
- Planner — goals and tasks with status lifecycle, linked to records
- Rule-driven reminders with WorkManager notifications
- Home screen — dual mode: Daily Brief (attention items, active goals, recent conversations) and AI Workspace (conversation + proposal cards)
- Structured AI proposals — MetadataUpdate, GoalProposal, TaskCreation, TaskCompletion, ObjectCreation, StatusUpdate
- AI providers — NVIDIA NIM (default), Anthropic, Offline graceful fallback
- API key encryption — Android Keystore AES-256-GCM
- Biometric lock
- Record relationships
- Multi-profile support
- Data export (JSON — documents not yet included, see known gaps)
- Morning Brief daily notification at 9 AM

**Pending before GA:**
- Play Store listing assets
- Stability pass (search, conversation threading, document re-upload deduplication, security hardening)

---

## 6. How The Product Works

There are a few core concepts that matter for every feature decision.

**Record** (internally called "Object" — we haven't finished the terminology migration)  
A structured entity: a passport, a job, an insurance policy, a property. The primary organizational unit. Records have typed metadata fields, attached documents, an event timeline, generated tasks, and relationships to other records. Not files. Structured things.

**Life Area** (internally "Domain")  
The category a record belongs to — Identity, Career, Finance, Health, Travel, Property, Legal, Education. Life Areas are also the level at which LifePilot maintains a current understanding of the user's situation. This "Domain Life State" concept is architecturally important and is the core work of V1.1.

**Metadata**  
The structured fields of a record. For a passport: passport number, expiry date, nationality, country of issue. For a job: employer, role, start date, salary. Every field has a verification status. The Schema Engine defines what fields exist for each record type and whether they can be extracted by AI.

**Life State Engine**  
The central pipeline. Every significant user action flows through it — document uploads, metadata verifications, AI proposal approvals, task completions, reminder evaluations. Nothing bypasses it. This is how the data stays consistent. When something doesn't update correctly, it's almost always a gap in the Life State Engine pipeline, not a one-off bug.

**Domain Life State**  
A structured, continuously-updated understanding of the user's situation within a Life Area. For Career: current employer, recent changes, open questions. For Finance: current situation, risks, recommendations. Right now this model exists and is populated with sample data, but it's not wired to life events — the real understanding stays at the object level. Fixing this is V1.1's core work.

**Proposal Cards**  
AI never writes to the database. It proposes. The user reviews a structured card showing exactly what would change, approves or rejects it. Only approval triggers the write. This is the trust model made tangible in the UI.

---

## 7. Feature Specs

### Document Ingestion

1. User uploads a file (PDF, image) or takes a camera photo
2. File copied to internal private storage; SHA-256 checksum computed
3. OCR runs in the background via WorkManager (ML Kit)
4. User can trigger AI metadata extraction from the document viewer
5. AI suggests field values from OCR text; user reviews each field on the Verification screen
6. Accepted fields written to the database with `verificationStatus = VERIFIED`
7. Life State Engine fires: record status updated, timeline entry created, tasks generated from schema, reminder rules evaluated

Rejected or unextracted fields are stored as `UNVERIFIED`. The UI should visually distinguish these.

### AI Workspace

The Home screen has two modes. Without an active conversation, it shows the Daily Brief — attention items, active goals, recent conversations. The first message transitions it to the AI Workspace.

The pipeline: user message → `HomeViewModel.sendMessage()` → RetrievalEngine selects up to 5 relevant records → ObjectReasoner builds per-record snapshots → PromptBuilder formats the system prompt → AI provider generates a response → response parsed for a structured proposal → proposal shown as an interactive card → user approves → repository write.

What AI can currently propose: update a metadata field, create a new record, propose a goal with tasks, create a task, mark a task complete, update a record's lifecycle status.

What AI cannot do: delete records, access raw documents, transmit data externally, or make any change without user approval.

### Record Library

Domain-grouped tree with sticky section headers. Each domain shows a "Current Understanding" card at the top (the Domain Life State — currently populated with sample data, not live). Below it, all records in that domain as object cards.

Multi-select: long-press to enter selection mode, bulk archive or delete. Sorting by name, type, or last updated.

The type picker in "Add new" needs to be sourced from SchemaEngine, not hardcoded. Right now if you want to add a record type that isn't on the preset list, you can't.

### Projects (replacing Goals)

Goals are being retired as a concept. The replacement is Projects — scoped workspaces that bring tasks, documents, knowledge, and AI into one place for a specific life objective. Examples: Japan Trip 2026, Senior PM Job Search, Buy First House, PhD Completion.

Projects live in the Tasks tab as a third subtab alongside Today and Upcoming. Each Project card shows an emoji, title, task count, deadline, and a life state confidence chip. Home shows the top 3 active Projects for quick-resume.

Each Project is a 7-tab workspace: Overview (status, AI recommendations, blockers, milestones), Tasks (the same tasks visible globally — not duplicated), Documents (Library objects surfaced here, not copied), Timeline (events for this project), Knowledge (cached relevant context like visa requirements or salary benchmarks), AI (a dedicated chat scoped only to this project's data), and Life State (AI's current structured understanding of the project).

The AI scoping is the architectural differentiator. When inside a Project's AI tab, `RetrievalEngine` filters to that project's linked documents, tasks, timeline, knowledge, and conversations. This produces faster responses, better SLM performance, and less noise than querying the entire life graph.

The `Goal` domain model is extended with Project fields (`coverEmoji`, `projectLifeState`, `linkedObjectIds`, `linkedKnowledgeIds`). Existing Goals migrate automatically to Projects in the database. All user-facing strings change from "Goal" to "Project."

### Planner

Tasks. Filter chips (Today / This Week / This Month), task priorities, and swipe gestures are in the next cycle. The Planner renders tasks linked to Projects with a project badge so global and project-scoped task views stay consistent.

### Biometric Lock

The app requires biometric authentication (fingerprint or face unlock via Android BiometricPrompt) on every cold launch and after a configurable inactivity timeout. Authentication is handled entirely on-device — no credential leaves the device. The lock screen is a minimal full-screen prompt with no information visible behind it; `FLAG_SECURE` is set on the Activity so the app content is excluded from the system's recent apps preview and cannot be screenshotted by other apps while locked.

Biometric setup is prompted during onboarding (Step 1 or as a post-onboarding suggestion, depending on device capability). If the device has no enrolled biometric, the app falls back to device PIN/password via `DEVICE_CREDENTIAL` fallback. Biometric lock is on by default and can be disabled in Settings, but doing so surfaces a friction warning. The timeout duration is also user-configurable (1 minute / 5 minutes / on app close).

### Life Event Action Plan

When the user describes a significant life event in the AI chat — a job change, relocation, marriage, new property, major medical event — the AI does not create a single record and stop. It reasons about every downstream implication across the user's life and presents a single interactive checklist in the chat: which records to close, which to create, which tasks to generate, which domains to update.

The plan renders inline as a special `LifeEventActionPlanCard` message type with checkable, editable rows. The user can confirm the whole plan at once or step through items individually. Execution is sequential and wired to domain life state updates. One conversation, one coherent outcome.

The underlying machinery: a `LifeEventClassifier` detects high-impact event types. Each type has a cascading update template in the Schema Engine (JSON config, not code) defining which records to touch, which tasks to generate, which clarifying questions determine whether additional cascades apply. Before presenting the plan, the AI asks 3–5 clarifying questions so the plan is specific, not generic.

This is the clearest demonstration of what makes LifePilot a life operating system rather than a records manager. A records manager creates one record. LifePilot understands what that event means for everything else.

### Life Completeness Score

After the initial onboarding session ends, there is no persistent signal to the user about how complete their profile is or what would make LifePilot more useful to them. A user with 2 records and a user with 50 see the same Home screen. Life Completeness solves this.

A `CompletenessEngine` computes a per-domain score (0–100%) and a weighted overall score, stored in `DomainLifeState.completenessScore`. It refreshes whenever a record is added, a field is verified, or a domain life state updates. The Home screen shows a "Life Completeness" row below the greeting — e.g. `72% complete` — with a linear progress bar. Tapping it opens a `CompletenessDetailScreen` showing per-domain breakdown: progress bar, key record count, and a "What to add next" suggestion list for each domain that navigates directly to the relevant add flow.

The score only ever increases — it does not decrease when records are removed, to avoid discouraging cleanup. Onboarding step completion contributes: Step 1 bumps Identity, Step 5 document import bumps the relevant domains. The schema engine defines `completenessWeights` per domain — what percentage of each domain's score each field category contributes.

### Universal Search

SQLite `LIKE` queries across record titles, metadata values, document names, goal titles, and task titles. Results ranked: exact → prefix → contains → metadata match → document match. Correct for V1 where datasets are personal-scale.

### Onboarding

**First Launch — Welcome Screen**

On first install, the app shows a Welcome Screen before any profile is created. Two paths: "Start fresh" proceeds to the onboarding flow; "Restore from backup" opens a file picker, runs `ImportDataUseCase` to restore the user's data, then navigates to Home. Profile creation is deferred until onboarding Step 1 completes — `AppInitializer` no longer creates a profile unconditionally on launch. A `hasCompletedWelcome` flag in `PreferenceManager` controls which path the app takes on subsequent launches.

**The 7-Step Journey (designed for 3–5 minute completion)**

The onboarding is a product experience, not a data-collection form. The goal of the first session is a single, specific outcome: the user sends one AI query about their own real data and gets a useful answer. Everything in the flow is in service of reaching that moment.

*Step 1 — Name and Profession.*  
Collects `displayName` and `profession` (free-text with suggested chips: Software Engineer, Product Manager, Doctor, Student, Entrepreneur, Other). These two fields unlock Step 2 and are the minimum required input. Everything else is skippable.

*Step 2 — Domain Packs.*  
Based on profession, LifePilot automatically pre-selects relevant domain packs and shows a full selection grid so the user can add or remove. Example: "Product Manager" pre-selects Career, Finance, Travel, Health. Downloading runs in the background while the user proceeds. Domain packs contain pre-loaded schema types, domain-specific AI prompt templates, and suggested starter tasks (e.g. Career pack suggests "Upload your latest offer letter").

*Step 3 — Platform Demo (50 seconds, always skippable).*  
Five full-screen swipeable cards demonstrating one core use case each: AI answering a natural-language document question; the Library with a multi-document domain; a Life Event Action Plan for a job change; a visa expiry reminder; and the privacy proposition ("Everything stays on your device"). Each card has a title, a one-sentence description, and an animated illustration. Auto-advances to Step 4 after Card 5 or on "Get started" tap.

*Step 4 — AI Prompt Generator.*  
For users who already use ChatGPT, Gemini, or Claude: a copyable prompt that asks those tools to share everything they know about the user, organised by life area. Below the copyable box, an optional paste field. If the user pastes a response, LifePilot's NLP pipeline parses it and creates draft `AiProposal.MetadataUpdate` entries for review. If skipped, the user proceeds directly to Step 5. This is the fastest path to importing a meaningful volume of structured data in one session.

*Step 5 — Bulk Document Scan.*  
With explicit permission, LifePilot scans WhatsApp Documents, Telegram Documents, Downloads, and Documents folders. It does not scan Photos or personal media. A progress bar shows scan status. After scanning, a thumbnail grid lets the user select or deselect found PDFs and document-like images. Tapping "Import selected" runs them through `DocumentProcessingPipeline` → OCR → classification → draft record creation. "Skip for now" is always available.

*Step 6 — Wow Moment.*  
Immediately after import (or after a skip), LifePilot generates one suggested AI query based on whatever was just imported — pre-filled in the chat input, ready to send. Examples: "When does your [document type] expire?", "What documents do you need for your next trip?". The user taps to send or edits it first. This is the designed moment of first value — the first AI answer about the user's actual data.

*Step 7 — Completion.*  
A brief "You're all set" confirmation showing the user's name and a summary: "X records imported, Y domains active." Transitions to Home.

**Profile Completeness Ring**

After onboarding, the Home top bar shows a completion ring representing the percentage of profile fields that have been filled. Tapping the ring navigates to the first incomplete onboarding step. The ring is also the re-entry point if the user skipped steps on first run. The `Profile` model is extended with `fullName`, `dateOfBirth`, `profession`, `employer`, `city`, `country`, `phoneNumber`, `emergencyContactName`, and `emergencyContactPhone`; `profession`, `city`, and `country` are passed into `RetrievalContext` so the AI has personal context without the user having to repeat themselves.

---

### Backup and Export

Currently produces a JSON file. It does not include the actual document files. This means the current export is not a real backup — you can restore your records and metadata but not your documents. The full export needs to be a ZIP that includes document files, which is a prerequisite for any meaningful cloud backup story.

---

## 8. The AI Layer

### The one constraint that shapes everything

AI never writes to the database. It proposes. The user confirms. The system stores.

This sounds like a limitation. It's actually the product's core trust mechanism. The moment AI can silently update your passport number or mark your insurance as cancelled without you seeing it, the app becomes something you can't fully trust — and an app you can't trust with your most sensitive documents is worthless regardless of how smart the AI is.

So the pipeline ends with an `AiProposal` — a proposal card the user reviews, potentially edits inline, and approves. Approval triggers the write. Rejection does nothing. The AI's suggestion lives in memory until the user acts on it.

Not every proposal will always deserve the same level of review. "Noted that you prefer window seats" is not the same as "your passport number is Z4921835." The next iteration of the approval model will tier by sensitivity: full card for sensitive fields, lightweight chip for standard fields, auto-apply for preferences. Same principle, appropriately calibrated friction.

### What AI currently knows

For each request, the RetrievalEngine selects up to 5 relevant records via keyword scoring and passes their structured snapshots to the PromptBuilder. The AI sees: record titles, types, metadata field values with verification status, task counts, document counts, and a stored AI context summary per record.

It does not see raw document text, OCR output, file contents, or any field the user hasn't already accepted into metadata.

### The current weakness

AI context is record-level. When a user asks "how is my career going?", the AI assembles an answer by looking at individual Career records — job title here, start date there — rather than consulting a single authoritative understanding of the user's career situation. The answer is fragmented and shallow.

The fix is V1.1: Domain Life State becomes the canonical layer. One persistent, continuously-updated understanding per Life Area. Objects become evidence; domains become the source of truth. The AI reasons from domain life states first, cites records as supporting context.

### Providers

Three providers implement the same `AiProvider` interface: NVIDIA NIM (OpenAI-compatible, the production default), Anthropic Claude, and an Offline provider that gracefully degrades when no key is configured. API keys are encrypted with Android Keystore AES-256-GCM and never appear in logs, bundles, or exports. Provider selection is a user setting.

---

## 9. Architecture — What a PM Needs to Know

**The stack:** Kotlin, Jetpack Compose, Material Design 3, Clean Architecture, MVVM, Room for SQLite, WorkManager for background tasks, Hilt for dependency injection. Multi-module Gradle project.

**Why this architecture matters for the roadmap:**

The `domain/` module is pure Kotlin — zero Android framework dependencies. This is intentional and important. When we build iOS, the shared business logic is already there. Kotlin Multiplatform lets the domain layer compile for both platforms. We write SwiftUI on top for iOS. The foundation is already laid.

The Life State Engine pipeline is the most critical thing to understand. Every significant user action flows through it. Document upload, metadata verification, AI proposal approval, task completion, reminder evaluation — all of them enter the pipeline and exit with consistent, updated state. When something doesn't update correctly, it's almost always a pipeline gap.

**Database:** Room with SQLite, version 3. Soft deletes — records marked `deleted = 1`, not physically removed. Migration history: v1 → v2 added goals, conversations, chat messages. v2 → v3 added verification_status to metadata.

**Security model:** API keys are in Android Keystore ✓. Biometric lock exists ✓. `android:allowBackup="false"` ✓. HTTPS only ✓. Database encryption (SQLCipher), FLAG_SECURE on sensitive screens, and PII redaction before AI prompt construction are all required before public launch.

---

## 10. Roadmap

### V1.1 — Domain Life State as Canonical Understanding

This is the most architecturally significant upgrade since launch and the one I care most about getting right.

Right now, AI context lives at the record level. V1.1 moves it up to the domain. Each Life Area gets a single `DomainLifeState` with fields like `currentSituation`, `momentum`, `openQuestions`, `knownRisks`, `nextActions`, `confidenceScore`. Individual records become evidence that feeds into domain understanding, not the understanding itself.

The second part is wiring every life event to domain state updates. Task completion, document scan completion, conversation turns, status changes, goal achievement — all of them trigger a re-evaluation of the relevant domain's understanding. Some updates are deterministic (status change → patch domain state directly). Some need AI (unstructured conversation → queue async domain re-evaluation).

After this, "how is my career going?" doesn't mean assembling fragments from individual records. It means consulting a single, maintained, structured answer about the user's career situation — with records as cited evidence.

V1.1 also includes the AI follow-up interview pattern — for high-value life events, the AI asks 3-5 questions before creating anything, and synthesises the answers into a single proposal the user reviews before anything is persisted.

### V1.2 — Life Event Action Plan

The product's most important user-facing upgrade, and the clearest demonstration of what separates a life operating system from a records manager.

When a significant life event occurs, LifePilot reasons about every downstream implication — which records to close, which tasks to create, which domains to update — and presents everything as a single interactive checklist. One approval completes the cascade.

Here's the contrast with today:

*What happens today:* "I joined Zepto in Bengaluru starting August 1st." One Career record created. Done.

*What should happen:*

```
User: I joined Zepto in Bengaluru starting August 1st.

LifePilot: That's a significant change. Here's everything I can 
           update — review and confirm what applies:

┌─────────────────────────────────────────────────────────┐
│  LIFE EVENT ACTION PLAN                    4 of 7 ready │
├─────────────────────────────────────────────────────────┤
│  ✓  Close current job at Accenture                      │
│     Left date: 31 July                                  │
│                                                         │
│  ✓  Add task: Collect experience letter                 │
│     Due: 31 July · High priority                        │
│                                                         │
│  ✓  Add task: Initiate PF transfer (EPFO)               │
│     Due: 15 August · High priority                      │
│                                                         │
│  ✓  Create new job record: Zepto                        │
│     Role: [confirm] · Salary: [confirm] · From: Aug 1   │
│                                                         │
│  ✓  Update Career understanding                         │
│  ✓  Update Finance understanding (salary change)        │
├─────────────────────────────────────────────────────────┤
│  [ Confirm all ]          [ Review individually ]       │
└─────────────────────────────────────────────────────────┘

Before I apply these — what role and salary should I record 
for the Zepto position?
```

The AI continues the conversation naturally while the plan is live. The user can confirm everything at once or step through items individually to edit. The plan executes sequentially, each completed step checked off. One conversation, one coherent outcome.

The underlying machinery: a `LifeEventClassifier` detects high-impact event types (job change, relocation, marriage, new property, major medical event, immigration). Each type has a cascading update template — JSON config in the Schema Engine, not hardcoded — defining which records to update, which tasks to generate, which domains to re-evaluate, and which clarifying questions determine whether additional cascades apply. The `LifeEventActionPlanCard` renders inline in the chat as a special message type with checkable, editable rows. Execution is sequential and wired to domain life state updates.

V1.2 also includes the plain-language terminology audit (removing "Object," "Domain," "Metadata" from all user-facing strings), chat message interactions (copy, regenerate, edit-and-resend), task priorities, planner filters, notification deep links, and the in-app document viewer.

### V1.3 — Voice, Semantic Search, and Onboarding

**Onboarding that actually works.** The current onboarding collects a name and does nothing else. V1.3 introduces domain pack selection based on profession, a 50-second product demo, an AI prompt generator (a copyable prompt you run in ChatGPT/Gemini to pull out what those tools already know about you and paste back into LifePilot), and a bulk document scanner that finds PDFs in your Downloads and WhatsApp folders and runs them through the classification pipeline. The goal is for the first session to produce the "wow" moment — the first useful AI answer about the user's actual data.

**Voice input.** A microphone button in the AI chat. Recognised text lands in the input field as an editable draft before sending. Holds for continuous dictation mode. On-device SpeechRecognizer — no cloud dependency. This matters because voice is genuinely the fastest input for complex, unstructured information. Describing a doctor's visit or a job interview verbally is much faster than typing it.

**Semantic search.** Right now, "health cover" doesn't find "Star Health Insurance" because there's no token overlap. An on-device embedding model (all-MiniLM-L6-v2, 22 MB ONNX) would enable conceptual matching. Two-stage retrieval: keyword pre-filter, semantic re-ranking. The search UI doesn't change — results just get meaningfully better.

**Full backup ZIP.** Export that actually includes document files, not just metadata JSON. Incremental backup (delta exports using a manifest file). Google Drive auto-backup scheduling. Without this, the export promise isn't real.

### V2.0 — Hybrid Intelligence and On-Device AI

This is a large architectural upgrade. The short version: LifePilot gains an on-device AI layer that works entirely offline, and the intelligence model becomes a four-layer stack.

Layer 1 is deterministic — expiry checks, date calculations, task filtering, reminder lookups. No model invoked. This answers roughly 40% of queries.

Layer 2 is the personal knowledge layer — the local Life Graph, retrieved by the on-device model. Never leaves the device.

Layer 3 is internet intelligence — Brave Search or Tavily for current information like visa requirements, processing times, or medication regulations. Search queries contain only generic procedural questions, never personal field values. "Passport renewal procedure India 2025" is a safe query. A query containing your actual passport number is not. This boundary is enforced in the orchestration layer.

Layer 4 is the reasoning layer — personal context combined with internet results, passed to the on-device or cloud model depending on query complexity.

The default on-device model is Google AICore + Gemma 4 E2B (~1.8 GB, fits Snapdragon 778G+, 6 GB RAM devices). April 2026's Gemma 4 ships with native agentic capabilities and function calling — directly applicable to the structured proposal pipeline. MediaPipe + Gemma 2B Q4 as fallback for older devices.

The model abstraction is designed so switching to any future model (Qwen, Phi, Mistral) requires only a new implementation class, never a change to orchestration, prompts, or UI.

V2.0 also introduces multi-device sync — the encrypted backup ZIP as the sync unit, Drive as the relay, phone as the authoritative source.

### V3.0 — Life Actions: Doing, Not Just Tracking

The most ambitious version and the one that turns LifePilot into something that has no direct competitor.

LifePilot today understands your life and helps you plan it. V3.0 adds execution. A Life Action is a user-approved administrative workflow that LifePilot can complete using the verified information it already holds. Applying for a Japan tourist visa. Filling in a job application form. Completing a KYC update. Initiating an insurance claim.

The user doesn't see "browser automation." They tap "Execute this task" in the Planner. LifePilot opens the official form, pre-fills every field it has verified data for, asks only for what's genuinely missing, presents a full review screen, and submits after explicit approval. The receipt is stored as a document on the relevant record. The domain life state updates.

The guiding constraint: no irreversible action without explicit, unambiguous user confirmation. Verified metadata only — no inferred or guessed form field values. If execution fails partway through, a recovery task is created automatically.

Initial templates: tourist visa applications for Japan, UK, Schengen, USA; passport renewal via Passport Seva; job application pre-fill for standard ATS platforms (Workday, Greenhouse, Lever); insurance claim initiation; bank KYC update; government form submission for common Indian government portals.

The product's four layers after this: Knowledge → Life State → Planning → Execution. That's the full arc.

### V3.5 and Beyond

**Platform expansion.** Desktop app (Tauri) with QR-code sign-in — phone generates the QR, desktop scans it, Drive credentials passed via a stateless relay, no LifePilot server ever holds credentials. iOS with Kotlin Multiplatform shared domain layer and SwiftUI UI. Web with Next.js and Dexie.js for offline storage.

**Shared Life (V4.0).** Family member profiles, shared records, per-member permissions. The person who manages the family's administrative life gets a proper first-class experience.

**Domain Packs (V5.0).** The schema engine reads bundled JSON. Domain Packs are downloadable extensions — new record types, rules, Life Action templates, and AI prompt adjustments for a specific life domain — without an app update. First packs: Immigration, Healthcare, Tax, Estate Planning, Business, Travel.

**Intelligence Platform (V6.0).** A Life Health Score — a single number representing how well-organized your administrative life is. Predictive reminders before you think to ask. Multi-year planning assistance.

**Monetization (V7.0).** Freemium: on-device AI + single device + manual backup free. Cloud AI credits + multi-device sync + automated backup as a paid tier. RevenueCat for Android/iOS billing. Offline-redeemable access codes for referrals — HMAC-signed, no server call for redemption.

---

## 11. Open Questions

These are real, not rhetorical. They need owners and answers before we hit the respective milestones.

**The verification friction question.** The "user verifies everything" model is right for sensitive data. But we're already seeing the friction for preference-level data — "you prefer Italian food" generating an approval card nobody needs. The tiered approval direction is right, but the specific thresholds are genuinely unknown. Too much friction and users stop using the AI. Too little and they can't trust the data. We need usage data to find the right line.

**What happens when the schema doesn't cover it?** 17 schemas will still leave gaps. Every user has records we haven't thought of. A "Custom / Not listed" path is necessary. The deeper question is whether LifePilot needs AI-driven schema inference — upload something we've never seen, it detects the document type, proposes fields. More powerful than a manual custom record, but significantly more complex to get right.

**The onboarding trust gap.** We're asking for sensitive data very early — passport numbers, health details, financial records. The UX needs to earn that trust before asking for it. The V1.3 onboarding (bulk document scan, domain packs, demo) helps, but I'm genuinely uncertain about the right order of operations: show value first, then ask for sensitive input? Or lighter data first, sensitive fields only after a few sessions? Getting this wrong loses a meaningful percentage of installs in the first session.

**The iOS timeline.** The domain layer being pure Kotlin makes iOS more tractable than it would be otherwise. But the WorkManager and Android Keystore dependencies in the data layer need clean abstractions before we can share logic on iOS. The question is: do we start iOS work in parallel with V2.0/V3.0 Android work, or is iOS a post-V3.0 effort? A significant portion of the target demographic uses iPhone, but doing iOS half-baked is worse than not doing it at all.

**What does monetization actually look like?** The freemium structure in V7.0 is directionally correct — free for on-device, paid for cloud AI and sync. But the exact price point, the right trial length, and whether AI credits or a flat subscription are more effective are all genuinely unknown. I don't want to design a billing system before we have enough user behaviour data to know what people value enough to pay for.

---

*Architecture decisions with rationale are in `docs/07-adr/`. Full issue log with root causes and fix approaches is in `ISSUES.md`. The feature-level implementation roadmap is in `ROADMAP.md`. The canonical product spec is in `MASTER-SPEC.md`.*
