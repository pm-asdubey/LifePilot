# Life State Engine

**Location:** `docs/02-data-model/Life-State-Engine.md`

---

# Purpose

The Life State Engine is the central system that powers LifePilot.

It maintains a continuously evolving representation of a person's administrative life.

Unlike traditional applications that simply store documents, the Life State Engine continuously updates the user's current state as new information becomes available.

Every upload, event, task completion, reminder, and AI extraction modifies this state.

All application features ultimately interact with the Life State Engine.

---

# Philosophy

LifePilot does not organize files.

LifePilot models reality.

Reality changes over time.

The application continuously reflects those changes.

AI does not remember anything.

AI reasons over the current Life State.

---

# Core Principle

The Life State Engine is the single source of truth.

Every subsystem writes to it.

Every subsystem reads from it.

No screen should maintain its own interpretation of user state.

---

# High-Level Flow

```
User Action

↓

Ingestion

↓

Validation

↓

Life State Update

↓

Derived Changes

↓

UI Refresh

↓

AI Retrieval
```

Every operation follows this pipeline.

---

# Inputs

The Life State Engine accepts six input types.

## 1. Document Upload

Examples

* Passport PDF
* Insurance Policy
* Offer Letter
* Medical Report

Pipeline

```
Upload

↓

OCR

↓

Metadata Extraction

↓

User Verification

↓

Object Updated

↓

Timeline Entry Created

↓

Tasks Evaluated

↓

Reminders Evaluated
```

---

## 2. User Event

Examples

* Joined Company
* Left Job
* Bought House
* Opened FD
* Visa Approved

Pipeline

```
Create Event

↓

Update Object State

↓

Generate Tasks

↓

Evaluate Reminders

↓

Update Timeline

↓

Refresh Dashboard
```

---

## 3. Manual Object Edit

Examples

* Edit passport number
* Change salary
* Rename property

Pipeline

```
Edit

↓

Validate

↓

Update Metadata

↓

Refresh Search Index

↓

Update AI Context
```

---

## 4. Task Completion

Pipeline

```
Task Completed

↓

Update Task

↓

Evaluate Parent Object

↓

Update Dashboard

↓

Generate Follow-up Tasks
```

---

## 5. Reminder Trigger

Pipeline

```
Reminder Fires

↓

Dashboard Updated

↓

Notification Generated

↓

Reminder Status Updated
```

---

## 6. AI Extraction

AI never directly updates the Life State.

Instead

```
AI Suggestion

↓

User Review

↓

Approval

↓

Life State Update
```

User approval is mandatory for canonical updates.

---

# Current State

Every Object maintains a Current State.

Examples

Passport

```
Status

Active

Expires

2034

Last Updated

2025

Reminder

2033 Renewal
```

Job

```
Status

Active

Employer

Google

Role

Product Manager

Joining Date

2025
```

Loan

```
Status

Closed

Closed Date

2028
```

Current State always represents "now."

---

# Historical State

History is never overwritten.

Instead

Events accumulate.

Example

```
Joined Company

↓

Promotion

↓

Department Change

↓

Salary Revision

↓

Left Company
```

Current State is computed from history.

---

# State Transition Rules

Objects transition through well-defined states.

Example

Passport

```
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

Loan

```
Draft

↓

Applied

↓

Approved

↓

Active

↓

Closed
```

Job

```
Interviewing

↓

Offer Received

↓

Accepted

↓

Active

↓

Notice Period

↓

Ended
```

Every Object Type defines its own lifecycle.

---

# Derived State

Some information should never be stored directly.

Instead, it is derived.

Example

```
Days Until Passport Expiry

↓

Calculated Daily
```

Example

```
Number of Active Loans

↓

Calculated
```

Example

```
Current Employer

↓

Most Recent Active Job
```

Derived values reduce duplication.

---

# Automatic Task Generation

The engine generates tasks whenever appropriate.

Examples

User Event

```
Left Job
```

Generated Tasks

* Update Resume
* Update LinkedIn
* Review Health Insurance
* Begin Job Search

---

User Event

```
Bought Vehicle
```

Generated Tasks

* Upload Registration
* Add Insurance
* Schedule Service Reminder

---

User Event

```
Passport Renewed
```

Generated Tasks

* Upload New Scan
* Update Visa Applications

Task generation is rule-based.

---

# Reminder Generation

Reminders are generated from metadata.

Examples

```
Passport Expiry

↓

Renewal Reminder
```

```
Insurance Expiry

↓

Renewal Reminder
```

```
FD Maturity

↓

Investment Reminder
```

Users may also create manual reminders.

---

# Dashboard Refresh

Whenever the Life State changes, the Home Dashboard recalculates:

* Attention Required
* Upcoming Tasks
* Upcoming Expiries
* Recently Updated Objects
* AI Suggestions
* Daily Digest

The dashboard never stores independent state.

It is a projection of the Life State Engine.

---

# Search Index

Whenever state changes:

* Search index updated
* Metadata indexed
* OCR text indexed
* Object titles indexed
* Relationship graph updated

Search should remain nearly instantaneous.

---

# AI Retrieval

AI never queries raw documents first.

Instead

```
Question

↓

Intent Detection

↓

Retrieve Objects

↓

Retrieve Metadata

↓

Retrieve Relationships

↓

Retrieve Timeline

↓

Retrieve Documents

↓

Assemble Context

↓

LLM

↓

Response
```

The Life State Engine prepares context.

The LLM generates language.

---

# Design Constraints

The following rules are mandatory.

* Original documents are immutable.
* Events are immutable.
* AI cannot modify canonical data without approval.
* Derived values are never manually edited.
* Dashboard is always computed.
* Timeline is generated from source entities.
* Every state change is traceable.
* Every update has an originating source.

---

# Future Extensions

The Life State Engine should support future capabilities without redesign.

Examples

* Cross-device synchronization
* Cloud backup
* Shared family profiles
* Collaborative editing
* Tax calculation
* Immigration workflows
* Healthcare workflows
* Estate planning

These features should consume the Life State Engine rather than replacing it.

---

# Summary

The Life State Engine is the heart of LifePilot.

Documents provide evidence.

Events record change.

Metadata describes reality.

Relationships connect entities.

Tasks represent work.

Reminders represent future attention.

The engine continuously combines these inputs into a structured, current representation of a person's administrative life.

Every feature in LifePilot should either:

* update the Life State Engine,
* retrieve information from it,
* or present its current state to the user.

No feature should bypass it.
