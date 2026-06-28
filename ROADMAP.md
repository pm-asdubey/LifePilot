# LifePilot Product Roadmap

This document describes the long-term evolution of LifePilot.

Unlike `TASKS.md`, which tracks implementation work, this roadmap defines product direction over multiple releases.

Features may move between releases as priorities evolve.

---

# Product Vision

LifePilot aims to become the operating system for managing a person's administrative life.

Every release should strengthen one or more of these pillars:

* Structured Life State
* AI Assistance
* Automation
* Privacy
* Reliability
* Extensibility

---

# Version 1.0 — Foundation

**Status: Release Candidate — Architecture Stabilization complete**

## Goal

Build a stable offline-first platform capable of managing a person's administrative life.

### Core Platform

* ✅ Offline-first architecture
* ✅ Local SQLite database (Room, version 3)
* ✅ File Storage Service
* ✅ Schema Engine (JSON-driven, 17 schemas)
* ✅ Rule Engine (reminder/task evaluation)
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
* ✅ Universal Search (Objects, Metadata, Documents, Goals, Tasks)
* ✅ Dashboard (attention items, domain distribution)
* ✅ Object Relationships
* ✅ Library: Domain-grouped Object Tree with sticky section headers
* ✅ Metadata Provenance (source, verification status, ProvenanceBadge)

### AI Features

* ✅ OCR (ML Kit)
* ✅ Metadata Extraction (AI-powered with user verification)
* ✅ AI Workspace (Home screen, dual-mode: Daily Brief / AI Workspace)
* ✅ Structured AI proposals (MetadataUpdate, GoalProposal, TaskCreation, TaskCompletion, ObjectCreation)
* ✅ AiObjectContext (per-object structured AI analysis, stored in metadata)
* ✅ MorningBrief daily notification (9 AM, pending tasks + active goals)
* ⬜ Document Classification (deferred to 1.1)
* ⬜ AI Summaries on demand (deferred to 1.1)

### Infrastructure

* ✅ Export (JSON with full payload)
* ✅ Import (with manifest validation)
* ✅ Security (biometric lock, Keystore encryption)
* ✅ Notifications (WorkManager: ReminderEvaluation every 6h, MorningBrief daily 9 AM)
* ✅ Animated screen transitions (slide+fade)
* ✅ Pull-to-refresh on list screens
* ✅ Multi-select bulk operations (archive/delete)
* ✅ Haptic feedback on key actions

### Remaining for 1.0 GA

* ⬜ Play Store listing assets

---

# Version 1.1 — Better Intelligence

## Goal

Reduce manual work.

Planned improvements:

* Better metadata extraction
* Improved reminder suggestions
* Relationship discovery
* Better duplicate detection
* Faster search
* Better dashboard insights
* Improved OCR quality

---

# Version 1.2 — Better User Experience

## Goal

Make daily usage effortless.

Planned improvements:

* Widgets
* Quick Actions
* Improved onboarding
* Bulk document import
* Better scanning
* Rich timeline
* Faster navigation
* Better accessibility

---

# Version 2.0 — Personal Cloud

## Goal

Allow secure backup and synchronization.

Planned features:

* Encrypted cloud backup
* Multiple devices
* Sync engine
* Conflict resolution
* Incremental synchronization

Privacy remains the primary design goal.

---

# Version 3.0 — Shared Life

## Goal

Support families while preserving privacy.

Planned features:

* Family Profiles
* Shared Objects
* Shared Documents
* Permission Management
* Shared Timelines
* Shared Reminders

---

# Version 4.0 — Domain Packs

## Goal

Transform LifePilot into an extensible platform.

Planned Domain Packs:

* Immigration
* Property
* Healthcare
* Tax
* Estate Planning
* Business
* Education
* Travel

New packs should be installable without modifying the core application.

---

# Version 5.0 — Intelligence Platform

## Goal

Make LifePilot proactively useful.

Possible features:

* Predictive reminders
* Life health score
* Administrative recommendations
* Financial document insights
* Relationship insights
* Long-term planning assistance

AI should remain explainable and grounded in structured data.

---

# Long-Term Principles

The following principles should remain unchanged across all releases.

* Offline First
* User Ownership of Data
* AI Retrieves Rather Than Owns Truth
* Objects Over Folders
* User Verification Before Canonical Updates
* Configuration-Driven Architecture

Future releases should extend these principles rather than replace them.

---

# Success Criteria

LifePilot succeeds when a user can:

* Find any important document in seconds.
* Understand the current state of their administrative life.
* Receive timely reminders without manual configuration.
* Trust that their information remains private and under their control.
* Confidently rely on the application for everyday life administration.

---

# Living Document

This roadmap is expected to evolve.

Features may move between releases as implementation experience, user feedback and product priorities change.

Architectural principles, however, should remain stable.
