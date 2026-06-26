# Ingestion Pipeline

**Location:** `docs/03-ai/Ingestion-Pipeline.md`

---

# Purpose

The Ingestion Pipeline transforms raw user input into structured knowledge within the Life State Engine.

The pipeline is responsible for:

* Accepting user input
* Understanding the content
* Creating or updating Objects
* Extracting metadata
* Generating Events
* Generating Tasks
* Creating Relationships
* Updating Search
* Updating Timeline
* Updating the Home Dashboard

No information should enter the Life State Engine except through this pipeline.

---

# Design Principles

The ingestion process must always be:

* Predictable
* Explainable
* Reversible
* User-verifiable
* Incremental
* Offline-first where possible

The user should always understand what changed.

---

# Supported Inputs

Version 1 supports the following inputs.

## Camera Scan

Examples

* Passport
* Aadhaar
* PAN
* Insurance

---

## PDF Upload

Examples

* Offer Letter
* Bank Statement
* Degree
* Medical Report

---

## Image Upload

Examples

* Photograph
* Screenshot
* Receipt

---

## Manual Entry

Examples

* Add Job
* Add Property
* Add Bank Account
* Add Vehicle

---

Future versions may support:

* Email
* WhatsApp
* Calendar
* Cloud Imports
* Government APIs

---

# Universal Pipeline

Every input follows the same architecture.

```text
Input

↓

Pre-processing

↓

Classification

↓

OCR (if required)

↓

Metadata Extraction

↓

Confidence Evaluation

↓

Object Matching

↓

User Verification

↓

Life State Update

↓

Derived Actions

↓

Dashboard Refresh
```

Each stage has a clearly defined responsibility.

---

# Stage 1 — Input

The application receives raw content.

Examples

* PDF
* Image
* Camera Capture
* Manual Form

The original file is stored immediately.

No modifications are made.

---

# Stage 2 — Pre-processing

Optional processing includes:

* Image rotation
* Cropping
* Perspective correction
* Noise reduction
* Compression
* Thumbnail generation

The original file remains unchanged.

Only working copies are processed.

---

# Stage 3 — Classification

Determine what the user uploaded.

Possible classifications include:

Identity

* Passport
* Aadhaar
* PAN
* Driving Licence

Finance

* FD Receipt
* Loan
* Insurance
* Credit Card

Career

* Resume
* Offer Letter
* Payslip

Health

* Medical Report
* Prescription

Travel

* Visa
* Ticket

Legal

* Contract
* Court Order

If classification confidence is low, ask the user.

---

# Stage 4 — OCR

If the document contains text:

Extract:

* Raw text
* Layout
* Tables
* Key-value pairs
* Images

OCR output is stored separately.

OCR may be rerun later if engines improve.

---

# Stage 5 — Metadata Extraction

AI extracts structured fields.

Example

Passport

↓

Passport Number

↓

Issue Date

↓

Expiry Date

↓

Nationality

↓

Name

↓

Authority

Example

Offer Letter

↓

Company

↓

Role

↓

Joining Date

↓

Salary

↓

Location

Each Object Type defines its own extraction schema.

---

# Stage 6 — Confidence Evaluation

Every extracted field receives:

* Value
* Confidence Score
* Extraction Source

Example

Passport Number

T1234567

Confidence

99%

Source

OCR

---

Confidence thresholds:

95–100%

Auto-select for review.

80–94%

Highlight for confirmation.

Below 80%

Require manual entry.

Thresholds should be configurable.

---

# Stage 7 — Object Matching

Determine whether this document:

* Creates a new Object
* Updates an existing Object
* Creates a new version
* Belongs to multiple Objects

Example

Passport renewed

↓

Existing Passport found

↓

Update existing Object

↓

Create Renewal Event

↓

Archive previous document version

---

# Stage 8 — User Verification

The user reviews proposed changes.

The review screen displays:

* Original document
* Extracted metadata
* Existing metadata
* Proposed updates
* Confidence indicators

The user may:

* Accept
* Edit
* Reject
* Save as Draft

No canonical state changes occur before confirmation.

---

# Stage 9 — Life State Update

Once approved:

* Object updated
* Metadata updated
* Events created
* Relationships evaluated
* Timeline updated
* Search re-indexed

This is an atomic transaction.

Either all updates succeed or none do.

---

# Stage 10 — Derived Actions

The engine evaluates rules.

Possible outputs:

Task Generation

Examples

* Renew passport
* Upload supporting document
* Review insurance

Reminder Generation

Examples

* Expiry reminder
* EMI reminder
* Maturity reminder

Relationship Suggestions

Examples

* Link Visa to Passport
* Link Loan to Property

Dashboard Updates

Examples

* New document added
* Object updated
* Attention required

---

# Duplicate Detection

The pipeline checks for duplicates before creating new Objects.

Matching signals include:

* Document fingerprint
* OCR similarity
* Metadata similarity
* Existing identifiers
* User confirmation

Duplicate detection should minimize unnecessary Objects while avoiding accidental merges.

---

# Error Handling

Failures should occur gracefully.

Possible failures:

* OCR failed
* Unsupported document
* Corrupt file
* Missing metadata
* Low confidence extraction

The original document should always remain safely stored.

The user should always be able to retry processing.

---

# Audit Trail

Every ingestion operation creates an audit record.

The audit log contains:

* Timestamp
* Source
* Extracted fields
* User decisions
* Created Objects
* Updated Objects
* Generated Tasks
* Generated Events

This provides complete traceability.

---

# Future Compatibility

The pipeline is intentionally modular.

Future AI models should replace individual stages without requiring redesign.

Examples:

* Better OCR engine
* Better metadata extraction
* Better duplicate detection
* Better relationship inference

Each stage should expose a stable interface so improvements remain isolated.

---

# Summary

The Ingestion Pipeline is the gateway into the Life State Engine.

Its responsibility is not simply to store files, but to transform user input into structured, verified knowledge.

Every stage must prioritize:

* Accuracy
* Transparency
* User trust
* Reversibility
* Maintainability

The integrity of the entire LifePilot application depends on the quality and predictability of this pipeline.
