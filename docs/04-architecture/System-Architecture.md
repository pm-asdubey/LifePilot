# System Architecture

**Location:** `docs/04-architecture/System-Architecture.md`

---

# Purpose

This document defines the overall software architecture of LifePilot.

It describes the major application layers, system boundaries, responsibilities and dependencies.

Every implementation should conform to this architecture.

No feature should bypass these layers.

---

# Architectural Principles

LifePilot follows the following principles.

* Clean Architecture
* Domain Driven Design
* Offline First
* Modular Design
* Dependency Inversion
* Single Responsibility
* Composition over Inheritance
* Configuration Driven
* Event Driven

---

# High Level Architecture

```text
                    UI Layer
                        │
        ┌───────────────┼───────────────┐
        │               │               │
 Home  Objects      Search        Settings
        │               │
        ▼               ▼

──────────── Presentation Layer ────────────

ViewModels

Navigation

UI State

Presentation Models

──────────── Domain Layer ────────────

Life State Engine

Ingestion Service

Search Service

Rule Engine

Relationship Service

Timeline Service

AI Retrieval Service

Object Definition Engine

Schema Engine

──────────── Data Layer ────────────

Repositories

Data Sources

Room

File Storage

OCR

LLM

Search Index

Android APIs

──────────── Storage Layer ────────────

SQLite

Files

Preferences

Encrypted Storage
```

---

# Layer Responsibilities

## UI Layer

Responsible only for presentation.

Contains

* Compose Screens
* Components
* Navigation
* Animations

Must never contain business logic.

---

## Presentation Layer

Responsible for:

* UI State
* Screen Events
* ViewModels
* Navigation Decisions

Communicates only with Use Cases.

---

## Domain Layer

The heart of the application.

Contains

* Business Rules
* Services
* Object Definitions
* Rule Engine
* Life State Engine
* AI Retrieval

Must remain independent of Android.

The Domain Layer should be testable on the JVM.

---

## Data Layer

Responsible for communication.

Contains

Repositories

Local Database

File Storage

OCR

Search

AI Providers

Future Cloud Providers

Business rules do not belong here.

---

## Storage Layer

Responsible only for persistence.

Contains

SQLite

Files

Preferences

Encrypted Files

No business logic.

---

# Service Architecture

The Domain Layer exposes the following services.

## Life State Engine

Maintains the current state.

---

## Ingestion Service

Processes incoming information.

---

## Search Service

Indexes and retrieves data.

---

## Relationship Service

Maintains graph connections.

---

## Rule Engine

Evaluates reminders, recommendations and tasks.

---

## Timeline Service

Maintains chronological history.

---

## AI Retrieval Service

Builds LLM context.

---

## Schema Engine

Loads Object Definitions.

Generates

* Forms
* Validation
* Metadata
* Actions

---

# Communication Rules

Allowed

UI

↓

ViewModel

↓

Use Case

↓

Repository

↓

Data Source

↓

Storage

Forbidden

UI

↓

Room

UI

↓

File System

ViewModel

↓

SQLite

Repository

↓

Compose

Every dependency must point downward.

---

# Event Driven Architecture

Most updates should be event driven.

Example

Passport Uploaded

↓

Metadata Updated

↓

Life State Updated

↓

Rule Engine Evaluated

↓

Reminder Generated

↓

Dashboard Updated

↓

Notification Scheduled

Every service reacts to events.

---

# Dependency Rules

Each layer depends only on lower abstractions.

Example

Presentation

↓

Domain Interface

↓

Repository Interface

↓

Implementation

Concrete implementations never leak upward.

---

# Module Structure

Suggested modules.

```text
app/

core/

domain/

data/

features/

designsystem/

ai/

ocr/

search/

notifications/

testing/
```

Each module should have a clear purpose.

---

# Feature Modules

Every feature follows the same pattern.

```text
feature-job/

data/

domain/

presentation/

ui/
```

Consistency is mandatory.

---

# Dependency Injection

All dependencies should be injected.

Recommended

Hilt

Scopes

Singleton

Activity

ViewModel

Never manually instantiate repositories or services.

---

# Error Handling

Errors flow upward.

Storage

↓

Repository

↓

Domain

↓

Presentation

↓

UI

The UI should never receive raw exceptions.

Errors should be translated into user-friendly states.

---

# Background Work

Background operations use:

WorkManager

Examples

OCR

Reminder Evaluation

Backups

Thumbnail Generation

Index Updates

Never block the UI thread.

---

# Configuration

Configuration should live outside code where practical.

Examples

Object Definitions

Metadata Schemas

Reminder Rules

Lifecycle Definitions

Validation Rules

Future Domain Packs

The engine loads configuration at runtime.

---

# Future Architecture

The architecture should allow:

Cloud Sync

Cross Device Sync

Plugin Domain Packs

Multiple AI Providers

On Device AI

Desktop Version

Web Version

Without changing the Domain Layer.

---

# Architecture Constraints

The following are prohibited.

Business Logic inside Compose.

Database Queries inside UI.

Hardcoded Object Types.

Hardcoded Metadata Forms.

Hardcoded Reminder Rules.

Hardcoded AI Prompts.

Hardcoded Validation Rules.

The application should remain data-driven.

---

# Summary

LifePilot follows a layered, modular architecture centered around the Life State Engine.

The Domain Layer owns business behavior.

The Data Layer owns persistence.

The Presentation Layer owns user interaction.

The UI Layer owns rendering.

Every feature should be implemented by extending the existing architecture rather than introducing new patterns.

The goal is a codebase that remains understandable, testable and maintainable even after years of development.
