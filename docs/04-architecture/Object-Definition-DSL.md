# Object Definition DSL

**Location:** `docs/04-architecture/Object-Definition-DSL.md`

---

# Purpose

The Object Definition DSL (Domain Specific Language) defines how Object Types are described within LifePilot.

Rather than writing Kotlin whenever a new Object Type is introduced, developers create declarative schema files.

The Schema Engine interprets these files at runtime.

This document defines the canonical DSL used throughout the platform.

---

# Design Goals

The DSL must be:

* Human-readable
* Versioned
* Declarative
* Extensible
* Type-safe
* Machine-parseable
* Backwards compatible

YAML is the preferred authoring format.

JSON may be generated from YAML during build time if required.

---

# Directory Structure

```text
schemas/

identity/

passport/

object.yaml

metadata.yaml

lifecycle.yaml

relationships.yaml

rules.yaml

ai.yaml

finance/

loan/

...

career/

job/

...
```

Every Object Type lives inside its own directory.

---

# Object Definition

The root file is `object.yaml`.

Example

```yaml
id: passport

version: 1

displayName: Passport

domain: identity

icon: badge

color: blue

description: Government-issued passport.

supportsDocuments: true

supportsTimeline: true

supportsRelationships: true

supportsTasks: true

supportsReminders: true

metadata: metadata.yaml

lifecycle: lifecycle.yaml

relationships: relationships.yaml

rules: rules.yaml

ai: ai.yaml
```

---

# Metadata Definition

Each metadata field follows a standard structure.

Example

```yaml
fields:

- id: passportNumber

  label: Passport Number

  type: string

  required: true

  searchable: true

  editable: true

  aiExtractable: true

- id: expiryDate

  label: Expiry Date

  type: date

  required: true

  searchable: true

  editable: true

  aiExtractable: true
```

---

# Supported Field Types

Version 1 supports:

```text
string

integer

decimal

currency

boolean

date

datetime

time

duration

email

phone

url

country

address

enum

tags

objectReference

fileReference

list

notes
```

The engine should reject unsupported types.

---

# Validation Rules

Example

```yaml
validation:

required: true

minLength: 8

maxLength: 9

regex: "[A-Z][0-9]{7}"

errorMessage: Invalid passport number.
```

Multiple validation rules may exist for a field.

---

# Display Configuration

The UI renderer uses display metadata.

Example

```yaml
display:

section: Overview

order: 2

icon: badge

highlight: true

copyable: true

mask: false
```

No UI should hardcode field placement.

---

# Lifecycle Definition

Example

```yaml
states:

- Draft

- Issued

- Active

- Renewal Due

- Expired

- Archived

transitions:

- Draft -> Issued

- Issued -> Active

- Active -> Renewal Due

- Renewal Due -> Expired

- Expired -> Archived
```

Transitions are validated during runtime.

---

# Relationship Definition

Example

```yaml
relationships:

- type: Supports

target: Visa

cardinality: many

- type: OwnedBy

target: Profile

cardinality: one
```

Relationship types must be registered with the Relationship Engine.

---

# Reminder Rules

Example

```yaml
rules:

- trigger:

field: expiryDate

offset: -365d

action:

Renew Passport

priority: High

- trigger:

field: expiryDate

offset: -180d

action:

Passport Renewal Reminder
```

The Rule Engine consumes these definitions.

---

# Task Templates

Example

```yaml
tasks:

- title: Upload Passport Scan

priority: Medium

- title: Verify Passport Details

priority: High
```

Tasks may be generated during ingestion or lifecycle transitions.

---

# AI Extraction

Example

```yaml
extract:

- passportNumber

- expiryDate

- issueDate

- nationality

- holderName
```

Each field may optionally define:

* confidenceThreshold
* extractionHints
* fallbackStrategy

---

# Search Configuration

Example

```yaml
search:

passportNumber:

strategy: exact

holderName:

strategy: prefix

nationality:

strategy: keyword
```

The Search Engine builds indexes automatically.

---

# Quick Actions

Object-specific actions are declarative.

Example

```yaml
actions:

- id: renew

label: Renew Passport

icon: refresh

- id: uploadScan

label: Upload Scan

icon: upload
```

The Object Screen renders these automatically.

---

# AI Prompt Extensions

Objects may contribute additional prompt context.

Example

```yaml
promptContext:

When summarizing a Passport:

Highlight expiry date first.

Mention linked visas if any exist.
```

This supplements the global AI prompts without replacing them.

---

# Schema Versioning

Every schema includes:

```yaml
schemaVersion: 1

minimumEngineVersion: 1

objectVersion: 4
```

The Schema Engine validates compatibility during startup.

---

# Required Files

Every Object Type must contain:

```text
object.yaml

metadata.yaml

lifecycle.yaml

relationships.yaml

rules.yaml

ai.yaml
```

Optional files include:

```text
translations.yaml

examples.yaml

migrations.yaml

tests.yaml

permissions.yaml
```

---

# Validation Checklist

During startup the Schema Engine validates:

* Unique Object ID
* Valid metadata fields
* Valid lifecycle transitions
* Valid relationship references
* Valid rule syntax
* Valid AI configuration
* Valid action definitions
* Schema version compatibility

An invalid schema must never crash the application.

---

# Future Compatibility

The DSL is intentionally extensible.

Future versions may add:

* Custom widgets
* Dynamic dashboards
* Enterprise permissions
* Domain Pack dependencies
* Localized schemas
* Third-party extensions

The engine should ignore unknown optional fields while warning developers.

---

# Design Principles

The DSL should describe **what** an Object is, not **how** it is implemented.

Implementation details belong to the Schema Runtime.

Schemas should remain readable by both developers and AI coding agents.

---

# Summary

The Object Definition DSL is the contract between Domain Packs and the LifePilot platform.

Every Object Type is expressed through declarative configuration rather than application code.

The Schema Engine interprets these definitions to generate forms, screens, validation, search behavior, AI extraction, reminders, lifecycle management, and relationships.

This approach allows LifePilot to scale from a handful of Object Types to hundreds while keeping the core engine stable, reusable and maintainable.
