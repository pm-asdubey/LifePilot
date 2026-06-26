# Domain Pack Specification

**Location:** `docs/04-architecture/Domain-Pack-Specification.md`

---

# Purpose

Domain Packs are the primary extension mechanism of LifePilot.

Rather than hardcoding every supported Object Type into the application, related Objects are grouped into Domain Packs.

The Schema Engine discovers, validates and registers Domain Packs during application startup.

This architecture allows the platform to grow without modifying the core engine.

---

# Philosophy

The core application understands infrastructure.

Domain Packs provide knowledge.

The core platform should not know what a Passport, Loan or Property is.

It only understands:

* Objects
* Metadata
* Rules
* Relationships
* Lifecycle
* Search
* AI Configuration

Everything else belongs inside a Domain Pack.

---

# Version 1 Domain Packs

LifePilot ships with the following built-in packs.

## Identity

Contains

* Passport
* Aadhaar
* PAN
* Driving Licence
* Voter ID
* Birth Certificate

---

## Finance

Contains

* Bank Account
* Fixed Deposit
* Loan
* Credit Card
* Mutual Fund
* Insurance

---

## Career

Contains

* Resume
* Job
* Interview
* Offer Letter
* Experience Letter

---

## Education

Contains

* Degree
* Marksheet
* Certificate
* Admission

---

## Health

Contains

* Medical Report
* Prescription
* Vaccination
* Health Insurance

---

## Property

Contains

* Property
* Registry
* Sale Deed
* Possession
* Property Tax

---

## Travel

Contains

* Visa
* Passport
* OCI
* Tickets

---

## Legal

Contains

* Contract
* Power of Attorney
* Court Order
* Agreement

---

# Directory Structure

Every Domain Pack follows the same structure.

```text
domains/

identity/

manifest.yaml

objects/

passport.schema.yaml

aadhaar.schema.yaml

pan.schema.yaml

icons/

translations/

examples/

tests/

finance/

career/

...
```

Every Domain Pack is completely self-contained.

---

# Manifest

Every Domain Pack contains a manifest.

Example

```yaml
id: identity

displayName: Identity

version: 1

description: Identity documents.

icon: badge

author: LifePilot

dependencies: []
```

The manifest identifies and registers the pack.

---

# Object Registration

Each schema inside the `objects/` directory is automatically discovered during startup.

Application Startup

↓

Load Domain Packs

↓

Read Manifest

↓

Validate Schemas

↓

Register Objects

↓

Expose Registry

The application must never manually register Object Types.

---

# Assets

A Domain Pack may contain:

* Icons
* Illustrations
* Localization
* Sample Documents
* Test Fixtures

These assets remain isolated from the core application.

---

# Dependencies

A Domain Pack may depend on another pack.

Example

Travel

↓

Depends on

↓

Identity

The dependency graph must be acyclic.

Circular dependencies are prohibited.

---

# Localization

Every pack may provide translations.

Example

```text
translations/

en.json

hi.json
```

The platform loads translations automatically.

---

# Icons

Each Object Type may define:

* Filled icon
* Outline icon
* Accent color

The UI renderer consumes these assets.

---

# Examples

Every Domain Pack should include example data.

Example

```text
examples/

passport.pdf

passport.expected.json

passport.timeline.json
```

These files support testing and AI evaluation.

---

# Tests

Each Domain Pack should include validation tests.

Examples

* Schema validation
* Metadata validation
* AI extraction
* Reminder generation

The application should reject invalid packs before startup completes.

---

# Versioning

Every Domain Pack contains:

* Pack Version
* Minimum Engine Version
* Schema Version

Incompatible packs should fail validation with clear diagnostics.

---

# Runtime Registry

After loading, every pack is registered in the Domain Registry.

Example

```text
Identity

↓

Passport

↓

Schema

↓

Renderer

↓

Rules

↓

AI

↓

Search
```

All downstream services query the registry rather than loading files directly.

---

# Future Compatibility

The architecture should support:

* Community Domain Packs
* Enterprise Domain Packs
* Premium Packs
* Remote Distribution
* Hot-swappable Packs

These capabilities are future extensions and must not complicate Version 1.

---

# Design Principles

Domain Packs should be:

* Self-contained
* Versioned
* Testable
* Declarative
* Extensible

Business knowledge belongs inside Domain Packs.

Platform infrastructure belongs inside the core application.

---

# Summary

Domain Packs separate LifePilot's domain knowledge from its execution engine.

The platform provides generic infrastructure while Domain Packs define the real-world objects, metadata, rules and relationships that make the application useful.

This architecture enables LifePilot to scale to hundreds of supported Object Types without increasing complexity in the core engine.
