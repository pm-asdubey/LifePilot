# Metadata System

**Location:** `docs/02-data-model/Metadata-System.md`

---

# Purpose

Metadata describes Objects.

Every Object contains a common set of fields defined in the Canonical Data Model.

Beyond those common fields, every Object Type defines its own metadata schema.

Examples

Passport

* Passport Number
* Expiry Date
* Nationality

Vehicle

* Registration Number
* Manufacturer
* Insurance Expiry

Job

* Company
* Salary
* Joining Date

The Metadata System allows LifePilot to support thousands of Object Types without changing the application architecture.

---

# Philosophy

Objects define structure.

Metadata defines details.

The application should understand:

"This is a Passport."

The metadata explains:

"Passport Number = XXXXX"

"Expiry = 2033"

The engine should never hardcode metadata fields into business logic.

---

# Metadata Architecture

Every Object consists of two parts.

```text
Core Object

↓

Metadata
```

Core Object

Contains universal fields.

Metadata

Contains object-specific fields.

---

# Core Object Fields

Every Object includes:

* Object ID
* Profile
* Domain
* Object Type
* Title
* Description
* Status
* Created At
* Updated At

These fields never change.

---

# Metadata Definition

Each Object Type declares a Metadata Schema.

Example

Passport

```yaml
passportNumber:
    type: string
    required: true

expiryDate:
    type: date
    required: true

issueDate:
    type: date

nationality:
    type: string

issuingAuthority:
    type: string
```

Vehicle

```yaml
registrationNumber:
    type: string

manufacturer:
    type: string

model:
    type: string

purchaseDate:
    type: date

insuranceExpiry:
    type: date
```

Job

```yaml
company:
    type: string

designation:
    type: string

joiningDate:
    type: date

salary:
    type: currency

employmentType:
    type: enum
```

---

# Supported Metadata Types

Version 1 supports:

* String
* Integer
* Decimal
* Currency
* Boolean
* Date
* DateTime
* Time
* Enumeration
* Email
* Phone
* URL
* Address
* Country
* Percentage
* File Reference
* Object Reference
* List
* Tags

Future types can be added without changing the engine.

---

# Field Properties

Each metadata field defines:

* Name
* Display Name
* Data Type
* Required
* Default Value
* Validation Rules
* Searchable
* AI Extractable
* User Editable
* Display Order

Example

```yaml
expiryDate

Type: Date

Required: Yes

Searchable: Yes

AI Extractable: Yes

Editable: Yes
```

---

# Validation Rules

Fields may specify validation.

Examples

Passport Number

* Length
* Format
* Country-specific rules

Phone Number

* Country Code
* Digits
* Formatting

Email

* RFC Validation

Date

* Valid calendar date
* Cannot precede issue date (where applicable)

Validation rules should be reusable.

---

# Computed Metadata

Some metadata should never be stored directly.

Instead it is computed.

Examples

Passport

Days Until Expiry

↓

Calculated

Vehicle

Vehicle Age

↓

Calculated

Loan

Remaining Duration

↓

Calculated

Job

Years of Experience

↓

Calculated

Computed metadata should update automatically.

---

# AI Extraction

Each field defines whether AI may populate it.

Example

Passport

Passport Number

AI Extractable

Yes

Confidence Required

95%

Example

Personal Notes

AI Extractable

No

User Only

This prevents AI from modifying sensitive or subjective information.

---

# Metadata Provenance

Every metadata entry carries provenance information that describes where the value came from and whether a human has verified it.

## Source

The `source` field records what wrote the value.

```
USER          — entered directly by the user
OCR           — extracted from document text by ML Kit
AI            — proposed by the AI and accepted by the user
SYSTEM        — set automatically by the Life State Engine
RULE_ENGINE   — set by the Rule Engine
```

## Confidence

A `0.0–1.0` float indicating how confident the writing source was in the extracted value. Always `1.0` for user-entered values.

## VerificationStatus

Every entry has a `verificationStatus` column (`TEXT NOT NULL DEFAULT 'UNVERIFIED'`, added in MIGRATION_2_3):

```
UNVERIFIED   — default; not yet reviewed by a human
VERIFIED     — user explicitly accepted this value
REJECTED     — user explicitly rejected this value
```

AI-extracted values begin as `UNVERIFIED`. When a user approves a `MetadataUpdate` proposal card, `MetadataRepository.verifyMetadata()` is called and the status transitions to `VERIFIED`. When rejected, `MetadataRepository.rejectMetadata()` is called.

## LastUpdated

The `updatedAt` timestamp on each `MetadataEntity` records when the value was last written.

---

# ProvenanceBadge

In the Object Detail screen, every metadata entry in the Details list displays a `ProvenanceBadge` showing:

* The source label (e.g., "AI", "OCR", "User")
* The verification status label (e.g., "Verified", "Unverified", "Rejected")

The badge uses `MaterialTheme.colorScheme` tokens — green tones for `VERIFIED`, amber for `UNVERIFIED`, red for `REJECTED`.

---

# Special Field: ai_context

The field `fieldId = "ai_context"` is a reserved metadata key on every Object.

It stores an `AiObjectContext` serialised as JSON in the `value` column of the `metadata` table. The schema engine ignores this field during normal metadata display.

In the Object Detail screen, if an `ai_context` entry is present, it is rendered as a distinct `secondaryContainer` card labelled "AI Context", positioned above the regular Details list. It is never included in the editable Details form.

`AiObjectContext` structure:

```
summary           String   — one-paragraph description of the object's current state
importantFacts    List     — key facts the AI extracted
currentSituation  String   — what is actively happening (e.g., "Expires in 45 days")
suggestions       List     — recommended next actions
lastUpdated       Instant  — when this context was last generated
confidence        Float    — 0.0–1.0
```

---

# Metadata Versioning

Metadata changes over time.

Every modification creates a new version.

Example

Salary

2025

₹20 LPA

↓

2026

₹28 LPA

↓

2027

₹35 LPA

The current value is always available.

Historical values remain accessible.

---

# Search Index

Metadata fields marked as searchable become part of the search index.

Examples

Searching

"HDFC"

returns

* Bank Account
* Loan
* Credit Card

Searching

"Google"

returns

* Job
* Interview
* Resume
* Offer Letter

Metadata powers most search operations.

---

# Display Rules

The UI should not hardcode metadata fields.

Instead:

1. Read Metadata Schema
2. Generate appropriate UI component
3. Apply validation
4. Display values

This allows new Object Types to work without writing new forms.

---

# Import and Export

Metadata should support:

Import

* JSON
* CSV
* Future API

Export

* JSON
* Encrypted Backup

This allows portability without affecting the storage engine.

---

# Security

Metadata may contain sensitive information.

Examples

* Aadhaar Number
* PAN
* Passport Number
* Bank Account
* Loan Details

Sensitive fields should support:

* Masking
* Encryption at rest
* Permission-based visibility
* Secure export

---

# Future Compatibility

Future Domain Packs should only provide:

* Metadata Schema
* Lifecycle
* Event Definitions
* Reminder Rules

The Metadata Engine should automatically support them.

---

# Design Principles

The Metadata System should satisfy the following goals:

* Extensible
* Type-safe
* Searchable
* AI-friendly
* Versioned
* Validated
* Configuration-driven
* Independent of UI

No Object Type should require changes to the Metadata Engine itself.

---

# Summary

The Metadata System is the descriptive layer of the Life State Engine.

Objects define what exists.

Metadata describes those Objects.

By making metadata schema-driven rather than hardcoded, LifePilot can grow from a handful of Object Types to hundreds without redesigning the database, UI, or AI pipelines.
