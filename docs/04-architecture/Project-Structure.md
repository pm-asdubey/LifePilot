# Project Structure

**Location:** `docs/04-architecture/Project-Structure.md`

---

# Purpose

This document defines the physical project structure of the LifePilot codebase.

Every file added to the project should fit naturally into this structure.

Consistency is mandatory.

Developers should never need to wonder where new code belongs.

---

# Guiding Principles

The project structure should satisfy the following goals.

* Feature-oriented
* Modular
* Scalable
* Testable
* Platform-independent
* Easy to navigate
* Low coupling
* High cohesion

---

# Top-Level Structure

```text
LifePilot/

app/

core/

features/

domain/

data/

designsystem/

docs/

assets/

scripts/

testing/
```

Each top-level module has one responsibility.

---

# app/

Contains the Android application.

Responsibilities

* Application class
* Dependency Injection
* Navigation Host
* Theme Initialization
* Startup

Should contain almost no business logic.

---

# core/

Shared infrastructure.

Examples

Logging

Result Wrappers

Utilities

Constants

Extensions

Date Utilities

File Utilities

Permissions

Validation Helpers

Anything reusable belongs here.

---

# domain/

Contains platform-independent business logic.

Subfolders

```text
domain/

entities/

usecases/

services/

rules/

repositories/

models/

events/
```

This module must not depend on Android.

Everything here should run on the JVM.

---

# data/

Responsible for persistence and external integrations.

```text
data/

database/

dao/

datasources/

repositories/

filesystem/

ocr/

ai/

search/

preferences/

mappers/
```

Contains implementations.

Never business rules.

---

# features/

Each user-facing feature lives in its own module.

Example

```text
features/

home/

library/

search/

timeline/

settings/

profile/

object/

document/

task/

reminder/
```

Each feature follows the same structure.

---

# Feature Structure

Example

```text
features/home/

presentation/

components/

navigation/

viewmodel/

state/

events/

ui/

```

Business logic belongs in the Domain layer, not inside features.

---

# designsystem/

Reusable UI components.

```text
designsystem/

components/

theme/

colors/

typography/

icons/

spacing/

animations/
```

Every screen should reuse these components.

---

# AI Module

```text
data/ai/

retrieval/

ingestion/

providers/

prompts/

context/

summarization/

```

The AI module should expose interfaces.

The application should not depend on a specific LLM provider.

---

# OCR Module

```text
data/ocr/

engines/

parsers/

models/

pipeline/

```

Future OCR engines should be interchangeable.

---

# Search Module

```text
data/search/

index/

ranking/

retrieval/

matching/

fts/
```

Search is shared infrastructure.

---

# Database Module

```text
data/database/

entities/

dao/

migrations/

converters/

relations/

```

Room-specific code remains isolated here.

---

# File Storage

```text
data/filesystem/

documents/

thumbnails/

exports/

backups/

```

This module is responsible only for file operations.

---

# Testing Structure

```text
testing/

unit/

integration/

ui/

fixtures/

sampledata/
```

Every module should have corresponding tests.

---

# Naming Conventions

Classes

PascalCase

Functions

camelCase

Constants

UPPER_SNAKE_CASE

Packages

lowercase

Files should describe their primary class.

---

# Dependency Rules

Allowed

Feature

↓

Domain

↓

Repository Interface

↓

Data

↓

Storage

Forbidden

Feature

↓

Room

Feature

↓

SQLite

Feature

↓

OCR

Feature

↓

File System

---

# Resource Organization

```text
res/

drawable/

font/

values/

xml/

```

Keep resources organized by purpose.

Avoid unused assets.

---

# Build Variants

Prepare for:

Debug

Release

Benchmark (future)

Feature flags should be injectable.

---

# Documentation

Every major module contains:

README.md

Purpose

Responsibilities

Public APIs

Dependencies

This allows developers and AI coding agents to quickly understand each module.

---

# Future Expansion

The structure should support future additions without reorganization.

Examples

Desktop

Web

Cloud Sync

Plugin System

Additional AI Providers

Domain Packs

Future modules should plug into existing layers rather than requiring restructuring.

---

# Design Rules

The project structure should optimize for:

* Discoverability
* Consistency
* Isolation
* Reuse
* Long-term maintenance

Whenever a new file is added, its location should be obvious.

If two possible locations exist, the structure should be reconsidered.

---

# Summary

The Project Structure provides a predictable organization for the entire LifePilot codebase.

Every layer has a clearly defined responsibility.

Every dependency flows in one direction.

This organization minimizes architectural drift, improves onboarding, and enables both human developers and AI coding agents to work effectively within a shared, consistent structure.
