# Contributing to LifePilot

Thank you for contributing to LifePilot.

The goal of this project is to build a long-lived, maintainable platform for managing a person's administrative life.

Every contribution should improve the project without increasing unnecessary complexity.

---

# Before You Start

Before making any changes, read the following documents in order:

1. `README.md`
2. `MASTER-SPEC.md`
3. `CLAUDE.md`
4. `TASKS.md`

Then review the relevant documentation under `docs/`.

Do not begin implementation until the architecture is understood.

---

# Project Principles

Every contribution should preserve these principles.

* Offline First
* User Owns Their Data
* Objects Over Folders
* AI Retrieves Rather Than Owns Truth
* User Verification Before Canonical Updates
* Configuration-Driven Architecture
* Long-Term Maintainability

If a change conflicts with these principles, discuss the architecture before implementing it.

---

# Development Workflow

Every feature should follow this workflow.

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

Review

↓

Merge

The repository should remain buildable throughout development.

---

# Branch Naming

Use descriptive branch names.

Examples

```text
feature/schema-engine

feature/object-renderer

bugfix/reminder-crash

refactor/search-engine

docs/navigation
```

---

# Commit Messages

Follow Conventional Commits.

Examples

```text
feat: implement schema registry

fix: correct reminder scheduling

refactor: simplify object renderer

docs: update AI prompt specification

test: add Room migration tests
```

Each commit should represent one logical change.

---

# Coding Standards

All code should be:

* Readable
* Modular
* Testable
* Well documented
* Consistent

Prefer:

* Composition over inheritance
* Immutable data
* Small focused functions
* Clear naming

Avoid:

* Large classes
* Duplicate logic
* Premature optimization
* Unnecessary abstractions

---

# Architecture Rules

Always follow the documented architecture.

Presentation

↓

ViewModel

↓

Use Cases

↓

Repositories

↓

Persistence Gateway

↓

Room / File Storage

Never bypass architectural layers.

---

# Testing Requirements

Every meaningful change should include appropriate tests.

Where applicable:

* Unit Tests
* Integration Tests
* UI Tests
* Migration Tests

A feature is not complete until the relevant tests pass.

---

# Documentation

Documentation is part of the implementation.

Update documentation whenever:

* Architecture changes
* Public APIs change
* New modules are introduced
* Workflows change

Documentation should remain synchronized with the codebase.

---

# Dependencies

Before introducing a new dependency, evaluate:

* Maintenance
* License
* Security
* Binary size
* Community adoption

Prefer existing platform libraries where practical.

---

# Pull Request Checklist

Before merging, verify:

* Project builds successfully
* Tests pass
* Documentation is updated
* Architecture remains consistent
* No sensitive information has been introduced
* No unnecessary dependencies have been added

---

# Reporting Issues

When reporting bugs, include:

* Steps to reproduce
* Expected behavior
* Actual behavior
* Device information
* Application version
* Relevant logs (excluding sensitive data)

---

# Architecture Changes

Major architectural changes require:

1. Updating the relevant specification.
2. Creating or updating an Architecture Decision Record (ADR).
3. Updating tests.
4. Updating implementation.

Avoid making undocumented architectural changes.

---

# Guiding Principle

Every contribution should make LifePilot easier to understand, easier to maintain and more reliable.

When multiple valid approaches exist, choose the one that best supports long-term maintainability, architectural consistency and user trust.
