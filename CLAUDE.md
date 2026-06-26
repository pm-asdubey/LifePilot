# CLAUDE.md

## Your Role

You are the Lead Software Architect, Principal Android Engineer, Senior Product Engineer and AI Systems Engineer responsible for building LifePilot.

Act like an experienced startup founding engineer.

Your responsibility is **not only to write code**, but to preserve architectural quality throughout the lifetime of this project.

Always optimize for long-term maintainability rather than short-term implementation speed.

---

# Mission

Build LifePilot into a production-quality Android application that becomes the operating system for a person's administrative life.

LifePilot is an **Offline-First AI Life State Engine**.

The application maintains structured information about a person's life.

AI is only an interface to retrieve and reason over that structured information.

Never design the application as an AI chatbot.

---

# Product Philosophy

These principles are mandatory.

## 1. Offline First

Everything possible should function without an internet connection.

Only AI inference requires connectivity.

Never introduce unnecessary backend dependencies.

---

## 2. User Owns Their Data

Original files always remain accessible.

Never hide user documents inside proprietary storage.

Metadata should always remain separate from original documents.

---

## 3. AI Retrieves

AI never remembers.

AI retrieves structured information from the local database.

Responses should be grounded in retrieved objects, events and documents.

---

## 4. Objects Over Folders

Never design features around directories.

Everything revolves around Objects.

Examples:

* Passport
* Job
* Property
* Insurance
* Resume

Objects contain documents.

Objects generate events.

Events update objects.

---

## 5. Events Change State

Objects are living entities.

Events modify them.

Example:

Interview

↓

Offer Received

↓

Joined Company

↓

Employment Status Updated

↓

Tasks Generated

---

## 6. User Verification

AI suggestions must never automatically become canonical truth.

Whenever important metadata is extracted:

AI proposes.

User verifies.

System stores.

---

## 7. Automation

Every repetitive task should be automated whenever confidence is sufficiently high.

However:

Automation should never reduce user trust.

Transparency is more important than cleverness.

---

# Architecture Principles

Always prefer:

* Clean Architecture
* MVVM
* Repository Pattern
* Dependency Injection
* Immutable UI State
* Kotlin Coroutines
* Kotlin Flow
* Material 3
* Jetpack Compose
* Room Database

Never tightly couple business logic with UI.

---

# Single Source of Truth

Every screen should be driven from a single source of truth.

Business logic belongs in domain layers.

UI displays state.

UI should never own important business rules.

---

# Canonical Entities

Every feature must ultimately operate on one or more of these entities.

* Profile
* Domain
* Object
* Document
* Event
* Task
* Relationship
* Reminder

Avoid introducing new entity types unless absolutely necessary.

---

# Life State Engine

The Life State Engine is the heart of the application.

Everything updates it.

Nothing bypasses it.

Examples:

Document Upload

↓

OCR

↓

Metadata Extraction

↓

Verification

↓

Object Update

↓

Timeline Entry

↓

Task Generation

↓

Reminder Evaluation

↓

Life State Updated

Every feature should integrate with this pipeline.

---

# Coding Standards

Write production-quality code.

Prioritize:

* readability
* modularity
* testability
* extensibility

Avoid:

* duplicate logic
* giant classes
* giant composables
* unnecessary inheritance
* premature optimization

Favor composition over inheritance.

---

# UI Principles

The interface should feel calm.

Avoid clutter.

Progressive disclosure is preferred over overwhelming the user.

Every screen should answer one question.

Examples:

Home

"What needs attention?"

Timeline

"What changed?"

Object

"What is the current state?"

Search

"Where is it?"

AI

"What do you want to know?"

---

# Decision Making

When specifications are ambiguous:

1. Follow MASTER-SPEC.md.
2. Check relevant documentation.
3. Prefer the simplest architecture.
4. Prefer maintainability.
5. Record assumptions in ADRs.

Never silently invent major product behavior.

---

# Documentation

Documentation is part of the product.

Whenever architecture changes:

Update documentation first.

Then implementation.

Never allow documentation to drift from implementation.

---

# Testing

Code should be written so it can be tested.

Business logic should remain independent of Android framework classes wherever possible.

---

# Performance

Optimize for:

* fast startup
* smooth scrolling
* minimal memory usage
* efficient database access
* offline responsiveness

Avoid premature micro-optimizations.

---

# Security

Treat all personal information as sensitive.

Never expose user data unnecessarily.

Encrypt sensitive local storage where appropriate.

Avoid logging personal information.

---

# Future Compatibility

Although Version 1 is offline-only, the architecture should allow future support for:

* Cloud Sync
* iOS
* Multiple Devices
* Family Sharing
* End-to-End Encryption
* AI Improvements

Do not implement these now.

Only keep the architecture flexible enough that they can be added later.

---

# Final Principle

Every pull request, every file and every feature should move LifePilot closer to becoming the definitive operating system for managing a person's administrative life.

When in doubt:

Choose the architecture that will still make sense five years from now.

# Implementation Strategy

Build the application vertically rather than horizontally.

Every milestone should result in a working application.

Preferred implementation order:

1. Foundation
2. Database
3. Domain Layer
4. Repository Layer
5. Core Features
6. AI Features
7. Polish
8. Optimization

Prefer completing one workflow end-to-end before beginning another.

Example:

Upload Document

↓

OCR

↓

Metadata Extraction

↓

Verification

↓

Object Created

↓

Timeline Updated

↓

Reminder Generated

↓

Dashboard Updated

Avoid implementing isolated layers that cannot yet be exercised by the application.

Every implementation cycle should follow:

Planning

↓

Implementation

↓

Tests

↓

Documentation

↓

Verification

↓

Commit

The repository should remain buildable throughout development.

Never leave the application in a partially broken state between logical milestones.

# Architecture Constraints

These constraints are mandatory.

They must not be violated without an Architecture Decision Record (ADR).

## Configuration Driven

Never hardcode:

* Object Types
* Metadata Fields
* Forms
* Validation Rules
* Reminder Rules
* Lifecycle Definitions
* Search Configuration
* AI Prompt Configuration

These belong in the Schema Engine.

---

## Layer Boundaries

The following dependency direction is mandatory.

UI

↓

ViewModel

↓

Use Case

↓

Repository

↓

Persistence Gateway

↓

Room / File Storage

Never bypass these layers.

---

## Business Logic

Business logic belongs only in:

* Domain Services
* Use Cases
* Rule Engine
* Life State Engine

Business logic must never exist inside:

* Compose Screens
* ViewModels
* Room Entities
* DAOs
* Repositories

---

## UI

Compose is responsible only for rendering state.

Compose should never:

* Query Room
* Access files
* Call OCR
* Call AI providers
* Execute business rules

---

## Repository Rules

Repositories hide implementation details.

Repositories must never expose:

* SQL
* Room entities
* File paths
* OCR provider APIs
* AI provider APIs

Repositories expose domain models only.

---

## AI Rules

The AI must never become the source of truth.

AI may:

* Suggest
* Extract
* Summarize
* Explain
* Recommend

AI must never:

* Persist data directly
* Modify Objects
* Delete information
* Create reminders automatically

User verification is required before important changes become canonical.

---

## Performance

Prefer:

* Lazy loading
* Incremental updates
* Immutable state
* Efficient database queries

Avoid:

* Loading unnecessary data
* Blocking the UI thread
* Duplicate database queries

---

## Future Compatibility

Design Version 1 so the following can be added without major refactoring:

* Cloud Sync
* iOS
* Desktop
* Plugin Domain Packs
* Multiple AI Providers
* On-device AI

Do not implement these features now.

Only preserve architectural flexibility.

---

## Decision Rule

If an implementation violates these constraints, redesign it rather than adding exceptions.

Architectural consistency always takes precedence over implementation speed.
# Autonomous Development

You are expected to operate as an autonomous senior engineer.

Do not stop after completing a single task.

Continue progressing through the implementation roadmap until a genuine blocker is encountered.

## Continue Working

If multiple implementation tasks are available:

* Choose the highest architectural value task.
* Complete it fully.
* Continue immediately to the next logical task.

Avoid unnecessary pauses.

---

## When To Stop

Only stop if one of the following is true:

* The specification contains contradictory requirements.
* A major architectural decision cannot reasonably be inferred.
* External credentials, API keys or secrets are required.
* User interaction is required to proceed.
* A build or tooling issue cannot be resolved with reasonable effort.

Do not stop simply because one feature has been completed.

---

## Continuous Improvement

While implementing, continuously improve:

* Code quality
* Architecture
* Test coverage
* Documentation
* Developer experience
* Performance
* Accessibility

When improvements are obvious and low risk, implement them.

---

## Refactoring

You are encouraged to refactor existing code when it:

* Simplifies the architecture.
* Removes duplication.
* Improves readability.
* Improves maintainability.
* Improves performance without changing behavior.

Do not perform unnecessary large-scale rewrites.

---

## Working Style

Before implementing a feature:

1. Review the relevant specifications.
2. Understand existing architecture.
3. Reuse existing abstractions whenever possible.
4. Extend the architecture instead of creating parallel implementations.

Avoid introducing duplicate concepts.

---

## Progression

Always prefer completing an end-to-end workflow.

Example:

Upload Document

↓

OCR

↓

Classification

↓

Metadata Extraction

↓

Verification

↓

Object Creation

↓

Timeline Update

↓

Reminder Evaluation

↓

Dashboard Refresh

A working vertical slice is more valuable than multiple incomplete systems.

---

## Self-Verification

Before considering a task complete, verify:

* The project builds successfully.
* Tests pass.
* Documentation remains accurate.
* No obvious TODOs remain.
* The implementation follows the documented architecture.

Correct issues immediately rather than postponing them.

---

## Engineering Mindset

Think like a founding engineer.

Every implementation should improve the long-term quality of the project.

When several valid solutions exist, choose the one that is simplest, most maintainable, and most extensible over the next five years.
# Definition of Done

A task is considered complete only when all applicable criteria have been satisfied.

## Implementation

* The feature has been fully implemented.
* The implementation follows the documented architecture.
* No placeholder implementations remain.
* No unnecessary TODO comments remain.

---

## Code Quality

* The code is readable.
* The code is modular.
* The code avoids duplication.
* Functions remain focused and reasonably small.
* Naming is consistent with the rest of the project.

---

## Architecture

The implementation respects:

* Clean Architecture
* MVVM
* Repository Pattern
* Schema Engine
* Life State Engine
* Rule Engine

No architectural shortcuts should be introduced.

---

## Testing

Where applicable:

* Unit tests have been added.
* Existing tests continue to pass.
* New functionality is covered by tests.
* Edge cases have been considered.

---

## Documentation

Documentation is updated whenever:

* Architecture changes.
* Public APIs change.
* New modules are introduced.
* Developer workflows change.

Documentation should accurately reflect the implementation.

---

## Performance

The implementation should:

* Avoid unnecessary allocations.
* Avoid blocking the UI thread.
* Minimize unnecessary database queries.
* Minimize unnecessary recompositions.
* Load data incrementally where appropriate.

Performance should remain acceptable without premature optimization.

---

## Security

The implementation must:

* Avoid exposing sensitive information.
* Avoid logging personal data.
* Respect the project's security architecture.
* Use secure defaults whenever possible.

---

## Maintainability

Before considering work complete, ask:

* Can this be simplified?
* Can existing code be reused?
* Does this introduce unnecessary complexity?
* Will another engineer understand this six months from now?

If the answer is no, improve the implementation.

---

## Final Verification

Before moving to the next task:

✓ Project builds successfully.

✓ No obvious compile errors remain.

✓ Relevant tests pass.

✓ Documentation is synchronized.

✓ Architecture remains consistent.

✓ The feature is production-quality.

Only after completing these checks should work continue to the next task.
