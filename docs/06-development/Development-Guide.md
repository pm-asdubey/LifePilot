# Development Guide

**Location:** `docs/06-development/Development-Guide.md`

---

# Purpose

This document defines the engineering conventions used throughout the LifePilot project.

Its purpose is to ensure that every contributor—human or AI—implements features consistently while preserving the project's architecture.

This guide supplements, but does not replace, the architectural documentation.

---

# Engineering Philosophy

Every implementation should prioritize:

* Simplicity
* Readability
* Maintainability
* Testability
* Consistency

Avoid clever solutions when simpler solutions achieve the same result.

---

# Architecture Rules

All code must respect the documented architecture.

Specifically:

* UI contains no business logic.
* Domain contains no Android dependencies.
* Data contains no UI code.
* Storage contains no business rules.
* Repositories hide persistence details.
* Services own business behavior.

Architectural boundaries should not be bypassed for convenience.

---

# Development Workflow

Every feature should follow the same lifecycle.

```text id="6bxgzg"
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

Code Review

↓

Merge
```

Skipping steps should be exceptional.

---

# Feature Development

Each new feature should include:

* Use Cases
* Repository changes (if needed)
* Tests
* Documentation updates
* UI implementation
* Accessibility review

Features should be developed vertically from Domain to UI.

---

# Branch Strategy

Recommended branches:

```text id="6kcmzc"
main

develop

feature/*

bugfix/*

release/*
```

The `main` branch should always be releasable.

---

# Commit Messages

Use a consistent convention.

Examples

```text id="hl4klt"
feat: add passport schema

fix: correct reminder scheduling

refactor: simplify object renderer

docs: update schema engine

test: add migration tests
```

Keep commits focused on a single logical change.

---

# Coding Standards

General principles:

* Prefer composition over inheritance.
* Favor immutable data.
* Use descriptive names.
* Avoid deep nesting.
* Keep functions small.
* Prefer explicitness over cleverness.

Code should optimize for readability.

---

# Kotlin Guidelines

Preferred practices:

* Data classes for immutable models.
* Sealed classes for state.
* Coroutines for asynchronous work.
* Flow for observable streams.
* Extension functions only when they improve clarity.

Avoid unnecessary abstractions.

---

# Dependency Injection

Use Hilt consistently.

Rules:

* Constructor injection by default.
* Avoid service locators.
* Minimize singleton scope.
* Prefer interfaces at architectural boundaries.

Dependencies should be easy to replace in tests.

---

# Error Handling

Errors should:

* Be recoverable where possible.
* Be translated into domain-friendly results.
* Never expose implementation details to users.

Prefer typed results over unchecked exceptions for expected failures.

---

# Logging

Log:

* Important lifecycle events
* Performance metrics
* Non-sensitive errors

Do not log:

* Personal documents
* OCR text
* Prompt contents
* Sensitive metadata
* Encryption keys

Logs should aid debugging without compromising privacy.

---

# Documentation

Every public module should include a `README.md` describing:

* Purpose
* Responsibilities
* Public APIs
* Dependencies
* Example usage

Major architectural decisions should be captured in Architecture Decision Records (ADRs).

---

# Code Reviews

Reviewers should verify:

* Architectural compliance
* Test coverage
* Naming consistency
* Performance impact
* Security implications
* Documentation updates

Reviews should focus on correctness and maintainability.

---

# Performance

Developers should consider:

* Startup time
* Memory usage
* Database efficiency
* Search latency
* Background work
* Battery consumption

Performance is a feature.

Avoid premature optimization, but measure before introducing complexity.

---

# Accessibility

Every UI change should consider:

* Screen readers
* Large text
* Color contrast
* Touch target sizes
* Keyboard navigation (where applicable)

Accessibility regressions should be treated as bugs.

---

# Dependency Management

Before adding a new dependency, evaluate:

* Maintenance activity
* License
* Binary size
* Security history
* Community adoption

Prefer platform libraries where practical.

Unused dependencies should be removed.

---

# Continuous Integration

Every pull request should pass:

* Formatting
* Static analysis
* Unit tests
* Integration tests
* Schema validation
* Migration tests

No failing checks should be merged into `main`.

---

# AI Coding Guidelines

When using AI coding assistants:

* Follow the documented architecture.
* Reuse existing abstractions.
* Avoid introducing new architectural patterns without discussion.
* Update documentation when architectural changes occur.
* Generate tests alongside implementation.

AI should implement the specification rather than redefine it.

---

# Refactoring

Refactoring should:

* Preserve behavior.
* Improve readability or maintainability.
* Retain test coverage.
* Avoid unnecessary architectural changes.

Large refactors should be incremental.

---

# Release Process

Before release:

* Run full test suite.
* Verify migrations.
* Test backup and restore.
* Validate offline operation.
* Confirm AI features.
* Update release notes.

Each release should be reproducible.

---

# Project Values

The project values:

* User ownership of data.
* Privacy.
* Reliability.
* Predictability.
* Long-term maintainability.
* Thoughtful engineering over rapid complexity.

These values should guide implementation decisions when trade-offs arise.

---

# Definition of Done

A feature is complete only when:

* Implementation is finished.
* Tests pass.
* Documentation is updated.
* Accessibility has been reviewed.
* Performance impact is acceptable.
* Code review is complete.

---

# Summary

The Development Guide establishes the engineering standards for LifePilot.

By following consistent architectural boundaries, coding conventions, testing practices and documentation requirements, contributors can extend the application without increasing complexity or compromising quality.

The guide is intended to keep the project coherent over years of development, regardless of whether code is written by humans or AI-assisted tools.
