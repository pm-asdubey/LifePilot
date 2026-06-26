# LifePilot

> **An Offline-First AI Life State Engine for managing a person's administrative life.**

LifePilot is an Android application that continuously maintains the structured state of a person's life by organizing documents, objects, events, tasks and reminders into a single coherent system.

Rather than functioning as a chatbot, document manager or reminder application, LifePilot maintains an evolving representation of the user's administrative life. AI serves as an interface for retrieval, reasoning and assistance—not as the source of truth.

---

# Vision

People manage important information across dozens of disconnected systems.

Documents live in:

* Downloads
* WhatsApp
* Gmail
* Google Drive
* Photos
* File Managers

Tasks live in:

* Notes
* Todo Apps
* Calendars
* Memory

LifePilot unifies these into a structured Life State Engine.

Every uploaded document, event or user action updates the current state of life.

AI simply helps users interact with that structured state.

---

# Core Principles

LifePilot is built around the following principles.

* Offline First
* User Owns Their Data
* Objects Over Folders
* AI Retrieves, Never Owns Truth
* Events Change Object State
* User Verification Before Canonical Updates
* Configuration-Driven Architecture
* Long-Term Maintainability

These principles guide every architectural decision.

---

# Core Concepts

LifePilot revolves around nine canonical entities.

* Profile
* Object
* Document
* Metadata
* Event
* Task
* Reminder
* Relationship
* Timeline

Everything in the application is built upon these entities.

---

# High-Level Architecture

```text
                User
                  │
                  ▼
          Jetpack Compose UI
                  │
                  ▼
             ViewModels
                  │
                  ▼
              Use Cases
                  │
                  ▼
            Repository Layer
                  │
                  ▼
        Life State Engine
                  │
     ┌────────────┼────────────┐
     ▼            ▼            ▼
Schema Engine  Rule Engine  Search Engine
     │            │            │
     └────────────┼────────────┘
                  ▼
          Persistence Layer
      (Room + File Storage)
                  │
                  ▼
           Local Device Storage
```

The architecture follows Clean Architecture with strict separation between presentation, domain and data layers.

---

# Technology Stack

## Language

* Kotlin

## UI

* Jetpack Compose
* Material Design 3

## Architecture

* Clean Architecture
* MVVM
* Repository Pattern
* Dependency Injection

## Dependency Injection

* Hilt

## Database

* Room

## Asynchronous Programming

* Kotlin Coroutines
* Kotlin Flow

## AI

* Provider-independent architecture
* Schema-driven prompts
* Structured JSON outputs

---

# Repository Structure

```text
LifePilot/

app/
core/
domain/
data/
features/
designsystem/
testing/

docs/

01-product/
02-data-model/
03-ai/
04-architecture/
05-design/
06-development/
decisions/
examples/
future/

test-fixtures/

README.md
MASTER-SPEC.md
CLAUDE.md
TASKS.md
ROADMAP.md
CHANGELOG.md
CONTRIBUTING.md
VERSION.md
```

---

# Documentation Guide

All contributors should begin with the following documents.

1. `MASTER-SPEC.md`
2. `CLAUDE.md`
3. `TASKS.md`

Then consult the relevant documentation inside the `docs/` directory before implementing a feature.

The documentation defines the intended architecture and should remain synchronized with the implementation.

---

# Development Workflow

Every feature should follow this lifecycle.

Specification

↓

Implementation

↓

Unit Tests

↓

Integration Tests

↓

Documentation Update

↓

Verification

↓

Commit

The project should remain buildable throughout development.

---

# Current Scope

Version 1 includes:

* Android
* Offline-first storage
* Profiles
* Objects
* Documents
* OCR
* AI-assisted metadata extraction
* Timeline
* Universal Search
* Rule Engine
* Dashboard
* Backup & Export

Out of scope for Version 1:

* Cloud Sync
* iOS
* Shared Accounts
* Backend
* Multi-device Sync
* Payments

---

# Project Status

Current Status:

**Pre-Alpha**

Primary Focus:

Building the foundational platform and core engine.

The current implementation prioritizes architectural correctness over feature completeness.

---

# Contributing

All contributors should follow:

* `CLAUDE.md`
* `Development-Guide.md`
* `Testing-Strategy.md`

Architectural consistency is prioritized over implementation speed.

---

# Guiding Principle

LifePilot is not intended to become another document manager or AI chatbot.

Its purpose is to become the structured operating system for a person's administrative life by maintaining an accurate, evolving Life State Engine while preserving user ownership, privacy and trust.

When faced with multiple valid implementation choices, prefer the solution that best supports long-term maintainability, extensibility and user trust.
