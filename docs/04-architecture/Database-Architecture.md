# Database Architecture

**Location:** `docs/04-architecture/Database-Architecture.md`

---

# Purpose

This document defines the persistent storage architecture for LifePilot Version 1.

The database is responsible for storing the structured representation of a user's administrative life.

It does **not** store business logic.

It does **not** perform AI reasoning.

It acts as the persistent storage layer beneath the Life State Engine.

---

# Design Goals

The database must be:

* Offline-first
* Fast
* Reliable
* Transactional
* Versionable
* Searchable
* AI-friendly
* Easily migratable

---

# Database Technology

Version 1 will use:

* SQLite
* Room ORM
* Kotlin Coroutines
* Flow

Reasons:

* Native Android support
* Mature ecosystem
* Offline capability
* Excellent performance
* Simple backup strategy

---

# Architecture

```text
UI

↓

ViewModels

↓

Repositories

↓

Life State Engine

↓

Room Database

↓

SQLite
```

Repositories are the only layer allowed to communicate with Room.

---

# Database Modules

The database consists of the following logical modules.

* Profiles
* Objects
* Metadata
* Documents
* Events
* Tasks
* Relationships
* Reminders
* Timeline
* Search

Each module owns its own DAO.

---

# Core Tables

Version 1 contains the following primary tables.

## Profiles

Stores user profiles.

Columns

* profileId
* displayName
* photo
* createdAt
* updatedAt

---

## Objects

Stores all Life Objects.

Columns

* objectId
* profileId
* domain
* objectType
* title
* description
* status
* archived
* createdAt
* updatedAt

---

## Metadata

Stores flexible object metadata.

Columns

* metadataId
* objectId
* fieldName
* fieldType
* fieldValue
* confidence
* source
* version
* updatedAt

Metadata remains schema-driven rather than column-driven.

---

## Documents

Stores document references.

Columns

* documentId
* objectId
* originalPath
* thumbnailPath
* mimeType
* checksum
* fileSize
* createdAt

The database stores references only.

Files remain on disk.

---

## Events

Stores immutable events.

Columns

* eventId
* objectId
* eventType
* timestamp
* payload
* createdBy
* confidence

Events are append-only.

---

## Tasks

Columns

* taskId
* objectId
* title
* description
* priority
* dueDate
* status
* completedAt
* source

---

## Relationships

Columns

* relationshipId
* sourceObjectId
* targetObjectId
* relationshipType
* status
* createdAt

---

## Reminders

Columns

* reminderId
* objectId
* reminderType
* triggerDate
* status
* priority

---

## Timeline

Timeline is a materialized projection.

Columns

* timelineId
* sourceType
* sourceId
* timestamp
* title
* summary

Timeline entries are regenerated when required.

---

# Search Strategy

Search combines three approaches.

## Exact Search

Used for:

* Passport numbers
* PAN
* Aadhaar
* Registration numbers

---

## Prefix Search

Used for:

* Names
* Companies
* Titles

---

## Full Text Search

Used for:

* OCR text
* Notes
* Document summaries
* Descriptions

SQLite FTS should be used where appropriate.

---

# Transactions

Every Life State update occurs inside a database transaction.

Example

```text
Upload Passport

↓

Update Object

↓

Insert Metadata

↓

Insert Event

↓

Insert Timeline

↓

Generate Tasks

↓

Commit
```

If any operation fails:

Rollback entire transaction.

Partial updates are never allowed.

---

# Versioning

The database schema follows semantic versioning.

Examples

Version 1.0

↓

Migration

↓

Version 1.1

↓

Migration

↓

Version 2.0

Every schema migration must be reversible where practical.

---

# Indexing Strategy

Indexes should exist for:

* Object ID
* Profile ID
* Domain
* Object Type
* Status
* Created Date
* Updated Date
* Due Date
* Trigger Date
* Relationship Source
* Relationship Target

Frequently queried metadata fields should also be indexed.

---

# Backup Strategy

The database contains only structured information.

Original files remain outside the database.

A complete backup consists of:

LifePilot/

├── database.db

├── documents/

├── thumbnails/

└── metadata/

Restoring these components recreates the user's life state.

---

# Encryption

Sensitive fields should support encryption.

Examples

* Passport Number
* Aadhaar Number
* PAN
* Account Number
* Loan Information

Encryption should occur below the repository layer so business logic remains unchanged.

---

# Performance Targets

Application startup:

< 2 seconds

Object retrieval:

< 100 ms

Search:

< 200 ms

Dashboard refresh:

< 300 ms

Large document import:

Background processing

These are targets rather than guarantees.

---

# Future Compatibility

The database architecture should support future additions including:

* Cloud synchronization
* End-to-end encryption
* Shared profiles
* Audit history
* Multi-device conflict resolution
* Domain Packs

These features should extend the existing schema rather than replacing it.

---

# Design Principles

The database is responsible only for persistence.

Business rules belong to the Life State Engine.

AI reasoning belongs to the Retrieval Pipeline.

UI state belongs to ViewModels.

Maintaining this separation is mandatory.

---

# Summary

The Room database serves as the persistent foundation of LifePilot.

It stores structured entities, maintains transactional integrity, and provides efficient retrieval while remaining independent of AI logic, business rules, and presentation.

The database should be simple, deterministic, and optimized for long-term maintainability rather than cleverness.
