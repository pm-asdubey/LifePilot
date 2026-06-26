# Schema Engine

**Location:** `docs/04-architecture/Schema-Engine.md`

---

# Purpose

The Schema Engine is responsible for transforming Object Definitions into working application behavior.

Rather than hardcoding support for every Object Type, the application interprets schemas at runtime to generate forms, screens, validation rules, AI extraction targets, reminder rules and search indexes.

The Schema Engine is one of the foundational components of LifePilot.

---

# Philosophy

The application should understand generic concepts.

It should not understand specific Object Types.

Example

The application understands:

* Metadata Field
* Lifecycle
* Relationship
* Reminder Rule
* Validation Rule

It does **not** understand:

* Passport
* Loan
* Property
* Vehicle

Those become configuration.

---

# Responsibilities

The Schema Engine is responsible for:

* Loading Object Definitions
* Validating Schemas
* Generating Forms
* Generating Object Screens
* Providing Metadata Definitions
* Providing Validation Rules
* Providing Lifecycle Definitions
* Providing Reminder Rules
* Providing AI Extraction Rules
* Providing Search Configuration

The Schema Engine does not contain UI.

It produces configuration consumed by other layers.

---

# Inputs

The engine consumes:

* Object Definition
* Metadata Schema
* Lifecycle Definition
* Relationship Definition
* Reminder Rules
* AI Rules
* Validation Rules

---

# Outputs

The engine exposes:

* Form Definition
* Screen Definition
* Validation Specification
* Search Specification
* AI Extraction Specification
* Lifecycle Graph
* Reminder Configuration

Every subsystem consumes these outputs.

---

# Object Definition

Every Object Type is defined declaratively.

Example

```yaml
objectType: Passport

domain: Identity

icon: badge

metadata: passport.schema.yaml

lifecycle: passport.lifecycle.yaml

relationships: passport.relationships.yaml

rules: passport.rules.yaml

ai: passport.ai.yaml
```

The application should load this definition during startup.

---

# Metadata Schema

Metadata fields define:

* Identifier
* Display Name
* Data Type
* Required
* Searchable
* Editable
* AI Extractable
* Validation Rules

Example

```yaml
passportNumber

type: string

required: true

searchable: true

editable: true
```

---

# Lifecycle Definition

Every Object defines its own lifecycle.

Example

```text
Draft

↓

Issued

↓

Active

↓

Renewal Due

↓

Expired

↓

Archived
```

Transitions are configuration.

The application should never hardcode state transitions.

---

# Validation Rules

Validation is defined declaratively.

Example

Passport Number

Rules

* Required
* Maximum Length
* Country Pattern

Expiry Date

Rules

* Must be after Issue Date

Validation Engine executes these rules.

---

# Form Generation

The Form Generator reads metadata.

Example

```yaml
joiningDate

type: date
```

↓

Automatically generates

Date Picker

Example

```yaml
salary

type: currency
```

↓

Currency Input

No custom form code should be required.

---

# Object Screen Generation

The Object Screen is assembled from schema sections.

Input

```text
Metadata

Documents

Timeline

Tasks

Relationships
```

↓

Generated Screen

Different Object Types share the same renderer.

---

# AI Extraction Specification

Schemas specify AI extraction targets.

Example

Passport

Extract

* Passport Number
* Issue Date
* Expiry Date
* Nationality

Resume

Extract

* Company
* Skills
* Experience
* Education

AI uses schema definitions rather than custom prompts.

---

# Reminder Rules

Schemas define reminder triggers.

Example

Passport

Expiry

↓

12 Months

↓

Generate Reminder

Loan

EMI

↓

Monthly

↓

Generate Reminder

The Reminder Engine reads these rules.

---

# Search Configuration

Every metadata field declares search behavior.

Example

Passport Number

Exact Match

Employer

Prefix Match

Description

Full Text

The Search Engine builds indexes accordingly.

---

# Schema Versioning

Every schema includes:

* Schema Version
* Created Version
* Migration Version

The application validates compatibility during startup.

Future migrations should upgrade older schemas automatically where possible.

---

# Runtime Loading

Application Startup

↓

Load Domain Packs

↓

Validate Schemas

↓

Register Object Types

↓

Build Registry

↓

Expose Registry

All downstream services query the registry rather than reading raw schema files.

---

# Object Registry

The Schema Engine maintains a runtime registry.

Example

```text
Passport

↓

Metadata

↓

Lifecycle

↓

Validation

↓

Rules

↓

Renderer
```

The Registry becomes the single source of truth for Object Types.

---

# Error Handling

If a schema fails validation:

* Report the error
* Disable that Object Type
* Continue loading remaining schemas where safe
* Never crash the application because of one invalid schema

Schema validation errors should be easy to diagnose.

---

# Future Compatibility

The Schema Engine should support:

* Third-party Domain Packs
* User-defined Object Types
* Schema upgrades
* Remote schema distribution (future)
* Enterprise extensions

The engine should not assume schemas originate from the application itself.

---

# Design Principles

The Schema Engine must be:

* Configuration-driven
* Type-safe
* Extensible
* Deterministic
* Testable
* Platform-independent

Business logic belongs in services.

Object-specific behavior belongs in schemas.

---

# Summary

The Schema Engine is the abstraction layer that separates LifePilot's core platform from the Object Types it manages.

Instead of writing Kotlin whenever a new Object Type is introduced, developers define declarative schemas.

The engine interprets these schemas and supplies every other subsystem—forms, screens, validation, search, AI, reminders and lifecycle management—with the information they require.

This architecture enables LifePilot to grow from dozens of Object Types to hundreds while keeping the core application stable and maintainable.
