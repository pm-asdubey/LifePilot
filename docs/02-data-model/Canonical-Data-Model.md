# Canonical Data Model

**Location:** `docs/02-data-model/Canonical-Data-Model.md`

---

# Purpose

This document defines the canonical entities that make up LifePilot.

Every piece of information stored by the application must ultimately belong to one of these entities.

No feature should introduce a new storage concept without updating this document.

This document is the foundation for:

* Database Schema
* AI Retrieval
* Search
* Timeline
* Object Views
* OCR
* Relationships
* Tasks

---

# Entity Hierarchy

```
Profile
│
├── Domains
│
├── Objects
│     │
│     ├── Documents
│     ├── Events
│     ├── Tasks
│     ├── Metadata
│     └── Relationships
│
└── Timeline
```

Everything in the application ultimately belongs to a Profile.

---

# 1. Profile

Represents a single person.

Examples

* Me
* Dad
* Mom
* Brother

## Fields

```
id

displayName

photo

dateCreated

lastModified

isPrimaryProfile
```

A profile owns every other entity.

Deleting a profile removes every associated object.

---

# 2. Domain

A logical grouping of related objects.

Domains are fixed.

Version 1 includes:

* Identity
* Finance
* Career
* Property
* Health
* Education
* Travel
* Legal
* Family

A Domain contains many Objects.

---

# 3. Object

Objects are the core entity of LifePilot.

Objects represent things that exist in the real world.

Examples

Passport

Vehicle

Job

Property

Insurance

Bank Account

Resume

Visa

Investment

Loan

Medical Record

Degree

---

## Object Fields

```
id

profileId

domain

objectType

title

status

description

createdAt

updatedAt

archived

deleted
```

---

# Object State

Every object always has exactly one state.

Common states include:

Draft

Active

Pending

Expired

Completed

Closed

Archived

Deleted

Individual object types may define additional states.

Example

Loan

Open

Closed

Defaulted

Example

Job

Interviewing

Active

Notice Period

Ended

---

# 4. Metadata

Metadata stores structured information about an Object.

Metadata is extensible.

Passport Metadata

```
passportNumber

country

issueDate

expiryDate

issuingAuthority
```

Vehicle Metadata

```
registrationNumber

manufacturer

model

purchaseDate

insuranceExpiry
```

Job Metadata

```
company

role

joiningDate

salary

employmentType
```

Metadata should always be type-specific.

---

# 5. Document

Represents an uploaded original file.

Documents never change.

If the user edits a document, a new version should be created.

---

## Document Fields

```
id

objectId

originalFilePath

thumbnailPath

mimeType

fileSize

createdAt

ocrStatus
```

LifePilot never modifies original files.

---

# 6. Event

Events represent something that happened.

Events modify Object state.

Examples

Joined Company

Bought Property

Passport Renewed

Hospital Visit

Graduation

Loan Closed

Visa Approved

---

## Event Fields

```
id

objectId

eventType

timestamp

description

createdBy

confidence

```

Events are immutable.

Corrections create new events.

---

# 7. Task

Represents work.

Tasks may be:

Manual

AI Generated

System Generated

---

## Task Fields

```
id

objectId

title

description

priority

dueDate

status

source

completedAt
```

Status

Pending

In Progress

Completed

Cancelled

Deferred

---

# 8. Relationship

Relationships connect Objects together.

Examples

Passport

↓

Required For

↓

Visa

Property

↓

Financed By

↓

Loan

Job

↓

Deposits Into

↓

Bank Account

Insurance

↓

Covers

↓

Vehicle

---

## Relationship Fields

```
id

sourceObject

relationshipType

targetObject

createdAt
```

Relationships allow AI to reason across multiple Objects.

---

# 9. Reminder

Represents future attention.

Generated from:

Metadata

Events

Tasks

Manual reminders

---

## Reminder Fields

```
id

objectId

type

triggerDate

priority

status
```

---

# 10. Timeline Entry

Timeline is not a separate source of truth.

It is a projection generated from:

Events

Tasks

Documents

Reminders

Each Timeline Entry references its originating entity.

---

# Entity Rules

The following rules are mandatory.

* Every Object belongs to exactly one Profile.
* Every Document belongs to exactly one Object.
* Every Event belongs to exactly one Object.
* Every Task belongs to exactly one Object.
* Every Reminder belongs to exactly one Object.
* Every Relationship connects exactly two Objects.
* Timeline entries are generated, never manually edited.
* Metadata is attached to Objects, never Documents.

---

# Design Philosophy

LifePilot should never introduce additional storage concepts unless absolutely necessary.

Every feature should reduce to combinations of:

* Profiles
* Domains
* Objects
* Metadata
* Documents
* Events
* Tasks
* Relationships
* Reminders

If a proposed feature cannot be expressed using these entities, reconsider the design before implementation.
