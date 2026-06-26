# LifePilot Master Specification

**Version:** 1.0

This document is the canonical entry point for the LifePilot project.

Every contributor—human or AI—should begin here before reading any other documentation.

---

# Vision

LifePilot is an **Offline-First AI Life State Engine**.

Its purpose is to become the operating system for managing a person's administrative life.

LifePilot is **not**:

* A chatbot
* A note-taking application
* A document storage application
* A reminder application

LifePilot continuously maintains a structured representation of a person's administrative life.

AI is only an interface to retrieve and reason over that structured information.

---

# Product Principles

The following principles are non-negotiable.

1. Offline First
2. User Owns Their Data
3. AI Retrieves, Never Owns Truth
4. Objects Over Folders
5. Events Change Object State
6. User Verification Before Canonical Updates
7. Configuration-Driven Architecture
8. Long-Term Maintainability Over Short-Term Speed

---

# Core Architecture

The platform consists of:

* Life State Engine
* Schema Engine
* Rule Engine
* Search Engine
* AI Retrieval Engine
* Security Service
* File Storage Service

All major functionality should build upon these services.

---

# Canonical Domain Model

The application revolves around the following entities:

* Profile
* Object
* Document
* Metadata
* Event
* Task
* Reminder
* Relationship
* Timeline

Avoid introducing new root entities unless absolutely necessary.

---

# High-Level Data Flow

```text
Document

↓

OCR

↓

Classification

↓

Metadata Extraction

↓

User Verification

↓

Life State Engine

↓

Rule Engine

↓

Timeline

↓

Dashboard

↓

AI Retrieval
```

Every ingestion workflow should ultimately update the Life State Engine.

---

# Architectural Rules

Always follow:

* Clean Architecture
* MVVM
* Repository Pattern
* Dependency Injection
* Immutable UI State
* Jetpack Compose
* Material Design 3
* Room
* Kotlin Coroutines
* Kotlin Flow

Never bypass architectural layers.

---

# Source of Truth

The following order of authority applies.

1. MASTER-SPEC.md
2. Architecture documentation
3. Product documentation
4. Design documentation
5. Development documentation
6. Source code

If implementation conflicts with the specification, the implementation should be updated unless an Architecture Decision Record explicitly changes the specification.

---

# Documentation Index

## Product

* Vision
* PRD
* Information Architecture
* Home Dashboard
* Object Screen Specification

## Architecture

* System Architecture
* Project Structure
* Navigation Architecture
* Schema Engine
* Object Definition DSL
* Repository Interfaces
* Search Architecture
* Reminder & Rule Engine
* File Storage Architecture
* Security Architecture
* Room Entity Specification

## AI

* AI Prompt Specification
* OCR Specification

## Design

* Design System

## Development

* Testing Strategy
* Development Guide

---

# Current Scope

## Version 1

* Android
* Offline First
* Local Storage
* OCR
* AI-Assisted Metadata Extraction
* Objects
* Timeline
* Search
* Dashboard
* Rule Engine
* Backup & Export

---

## Explicitly Out of Scope

* Cloud Sync
* Backend
* Multi-device Sync
* Shared Accounts
* Payments
* iOS
* Web Application

The architecture should support these in the future, but they must not be implemented in Version 1.

---

# Definition of Success

LifePilot should enable a user to:

* Store important documents.
* Understand the current state of their administrative life.
* Find any document in seconds.
* Receive proactive reminders.
* Query their life using natural language.
* Trust that their information remains private, structured and under their control.

---

# Guiding Principle

When multiple valid implementation choices exist:

Choose the solution that best preserves:

* Simplicity
* Maintainability
* Extensibility
* User Trust
* Offline Capability

The architecture should still make sense five years from now.
