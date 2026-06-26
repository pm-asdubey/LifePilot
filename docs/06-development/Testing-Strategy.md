# Testing Strategy

**Location:** `docs/06-development/Testing-Strategy.md`

---

# Purpose

This document defines the testing philosophy and quality assurance strategy for LifePilot.

Testing is considered part of development rather than a separate phase.

Every feature should be accompanied by automated tests before it is considered complete.

---

# Quality Goals

LifePilot should be:

* Reliable
* Predictable
* Deterministic
* Maintainable
* Regression-resistant

Users should trust that important documents and life events are handled correctly.

---

# Testing Pyramid

The project follows the standard testing pyramid.

```text
                UI Tests
             Integration Tests
               Unit Tests
```

Approximate distribution:

* Unit Tests: 70%
* Integration Tests: 20%
* UI Tests: 10%

Unit tests provide the majority of confidence.

---

# Unit Tests

Every domain service requires unit tests.

Examples

* Life State Engine
* Rule Engine
* Schema Engine
* Search Service
* AI Context Builder
* Reminder Engine

Unit tests should:

* Run quickly
* Have no Android dependencies
* Use fake repositories
* Be deterministic

---

# Repository Tests

Every repository implementation should include:

* CRUD operations
* Error handling
* Transaction behavior
* Reactive streams
* Edge cases

Repository tests should use an in-memory Room database where appropriate.

---

# Migration Tests

Every Room migration must include:

* Forward migration
* Data preservation
* Index verification
* Foreign key verification

No schema migration should be released without automated tests.

---

# Integration Tests

Integration tests verify collaboration between components.

Examples

Upload Document

↓

OCR

↓

Metadata Extraction

↓

Verification

↓

Life State Update

↓

Reminder Generation

↓

Dashboard Refresh

These tests validate end-to-end workflows without requiring UI interaction.

---

# UI Tests

Compose UI tests should verify:

* Navigation
* Forms
* Search
* Dashboard
* Object Screens
* Timeline
* Profile Switching

UI tests should focus on user behavior rather than implementation details.

---

# Schema Validation Tests

Every Object Definition schema must pass validation.

Checks include:

* Required fields
* Lifecycle transitions
* Relationship references
* Rule syntax
* AI configuration
* Search configuration

Invalid schemas should fail CI immediately.

---

# AI Prompt Tests

Every prompt should have:

* Golden test inputs
* Expected JSON outputs
* Malformed document tests
* Empty input tests
* Hallucination prevention tests

Prompt templates should be version-controlled and evaluated after changes.

---

# OCR Tests

OCR should be tested using a representative document corpus.

Include:

* Identity documents
* Bank statements
* Offer letters
* Insurance policies
* Rotated images
* Low-quality scans
* Multi-page PDFs

Accuracy and confidence should be tracked separately.

---

# Rule Engine Tests

Verify:

* Reminder generation
* Reminder dismissal
* Task generation
* State transitions
* Date calculations

Date-sensitive tests should use an injectable clock.

---

# Search Tests

Search should verify:

* Exact matching
* Prefix matching
* Full-text search
* Ranking
* Duplicate detection
* Relationship expansion

Search performance should also be measured.

---

# Performance Tests

Track:

* Cold start time
* Dashboard load
* Search latency
* Object loading
* OCR throughput
* Database size growth
* Memory usage

Performance regressions should fail CI when thresholds are exceeded.

---

# Security Tests

Verify:

* Encryption and decryption
* Backup integrity
* Secure deletion
* Permission enforcement
* Data masking
* Keystore integration

Sensitive data must never appear in logs during tests.

---

# Test Data

Maintain a reusable corpus of sample data.

Examples

Identity

* Passport
* Aadhaar
* PAN

Finance

* Loan
* FD
* Credit Card

Career

* Resume
* Offer Letter

Health

* Medical Report

Travel

* Visa

Legal

* Contract

All test data should be synthetic and free of real personal information.

---

# Continuous Integration

Every pull request should execute:

* Static analysis
* Formatting checks
* Unit tests
* Integration tests
* Schema validation
* Migration tests

The main branch should remain releasable.

---

# Code Coverage

Suggested minimums:

Domain Layer: 90%

Data Layer: 80%

Repositories: 85%

UI Layer: 60%

Coverage is a guide, not the primary quality metric.

Meaningful assertions matter more than percentages.

---

# Regression Testing

Every bug fix should include:

* A failing test
* The fix
* A passing test

This prevents recurrence of known issues.

---

# Manual Testing

Before each release:

* Upload representative documents
* Verify reminders
* Test profile switching
* Export and restore backup
* Validate AI responses
* Confirm offline behavior

Manual testing complements automation.

---

# Release Checklist

A release is ready only when:

* All automated tests pass.
* No critical security issues remain.
* Database migrations succeed.
* Backup and restore succeed.
* Performance budgets are met.
* Documentation is updated.

---

# Design Principles

Testing should be:

* Fast
* Repeatable
* Deterministic
* Independent
* Easy to maintain

Tests are part of the product, not an afterthought.

---

# Summary

The Testing Strategy ensures that LifePilot remains reliable as it evolves.

By emphasizing automated testing, schema validation, integration workflows, and AI evaluation, the project minimizes regressions while maintaining confidence in both traditional software behavior and AI-assisted features.
