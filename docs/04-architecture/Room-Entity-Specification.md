# Room Entity Specification

**Location:** `docs/04-architecture/Room-Entity-Specification.md`

---

# Purpose

This document defines the complete Room persistence model for LifePilot.

It specifies:

* Database entities
* Foreign key relationships
* Indices
* Type converters
* Embedded models
* Migrations
* DAO boundaries

The Room schema is an implementation of the Canonical Data Model.

Business logic must never be implemented inside Room entities.

---

# Design Principles

The persistence layer must be:

* Offline-first
* Normalized where appropriate
* Transaction-safe
* Observable
* Migration-friendly
* Independent of UI

Room entities are persistence models.

Domain models remain separate.

---

# Database

Database Name

```text
lifepilot.db
```

Version

```text
1
```

Export schema

```kotlin
exportSchema = true
```

Schema history should be committed to version control.

---

# Entity Overview

Version 1 contains the following entities.

```text
ProfileEntity

ObjectEntity

MetadataEntity

DocumentEntity

DocumentVersionEntity

EventEntity

TaskEntity

RelationshipEntity

ReminderEntity

TimelineEntity

SearchIndexEntity

AuditLogEntity
```

Each entity owns one table.

---

# ProfileEntity

Primary Key

```text
profileId
```

Fields

* profileId
* displayName
* avatarPath
* isPrimary
* createdAt
* updatedAt

Indexes

* displayName

Relationships

One Profile

↓

Many Objects

---

# ObjectEntity

Primary Key

```text
objectId
```

Foreign Keys

ProfileEntity

Fields

* objectId
* profileId
* objectType
* domain
* title
* description
* status
* archived
* deleted
* createdAt
* updatedAt

Indexes

* profileId
* objectType
* domain
* status

---

# MetadataEntity

Stores schema-driven metadata.

Primary Key

```text
metadataId
```

Foreign Key

ObjectEntity

Fields

* metadataId
* objectId
* fieldId
* fieldType
* value
* version
* confidence
* source
* updatedAt

Indexes

* objectId
* fieldId

Metadata is stored vertically to support dynamic schemas.

---

# DocumentEntity

Represents a logical document.

Primary Key

```text
documentId
```

Foreign Key

ObjectEntity

Fields

* documentId
* objectId
* documentType
* currentVersionId
* createdAt

Relationships

One Document

↓

Many Versions

---

# DocumentVersionEntity

Stores immutable document versions.

Primary Key

```text
versionId
```

Foreign Key

DocumentEntity

Fields

* versionId
* documentId
* filePath
* originalName
* mimeType
* checksum
* size
* uploadedAt

Every upload creates a new version.

---

# EventEntity

Primary Key

```text
eventId
```

Foreign Key

ObjectEntity

Fields

* eventId
* objectId
* eventType
* payload
* timestamp
* source
* confidence

Events are append-only.

---

# TaskEntity

Primary Key

```text
taskId
```

Foreign Key

ObjectEntity

Fields

* taskId
* objectId
* title
* description
* priority
* dueDate
* status
* source
* completedAt

Indexes

* dueDate
* status

---

# RelationshipEntity

Primary Key

```text
relationshipId
```

Fields

* relationshipId
* sourceObjectId
* targetObjectId
* relationshipType
* status
* createdAt

Indexes

* sourceObjectId
* targetObjectId
* relationshipType

---

# ReminderEntity

Primary Key

```text
reminderId
```

Foreign Key

ObjectEntity

Fields

* reminderId
* objectId
* reminderType
* triggerDate
* priority
* status

Indexes

* triggerDate
* status

---

# TimelineEntity

Timeline is a materialized projection.

Fields

* timelineId
* sourceId
* sourceType
* timestamp
* title
* summary

Timeline entries can be rebuilt if necessary.

---

# SearchIndexEntity

Supports offline search.

Fields

* entityId
* entityType
* searchableText
* tokens
* updatedAt

Future versions may migrate to SQLite FTS5.

---

# AuditLogEntity

Records important operations.

Fields

* auditId
* operation
* entityType
* entityId
* timestamp
* actor
* details

Audit logs are append-only.

---

# Relationships

```text
Profile
    │
    └── Objects
            │
            ├── Metadata
            ├── Documents
            │      └── Versions
            ├── Events
            ├── Tasks
            ├── Reminders
            └── Relationships
```

Timeline references all entities but does not own them.

---

# Type Converters

Version 1 requires converters for:

* Instant
* LocalDate
* UUID
* Enum
* URI
* JSON payloads (where necessary)

Converters must remain deterministic.

---

# DAO Structure

Each aggregate owns one DAO.

```text
ProfileDao

ObjectDao

MetadataDao

DocumentDao

EventDao

TaskDao

RelationshipDao

ReminderDao

TimelineDao

SearchDao

AuditDao
```

DAOs expose persistence operations only.

---

# Transactions

Multi-entity updates must use Room transactions.

Examples

* Upload Document
* Create Object
* Complete Task
* Record Event

Repositories coordinate transactions.

---

# Migration Strategy

Every schema change requires:

1. Version increment.
2. Migration implementation.
3. Migration test.
4. Updated exported schema.

Destructive migrations are prohibited in production builds.

---

# Performance Targets

Object lookup

< 50 ms

Metadata query

< 50 ms

Timeline

< 100 ms

Search

< 200 ms

Database startup

< 150 ms

---

# Testing

Every DAO requires:

* Unit tests
* Integration tests
* Migration tests

Migration tests are mandatory for every schema version.

---

# Future Compatibility

The schema should accommodate:

* Cloud sync identifiers
* Soft deletes
* Encryption metadata
* Conflict resolution
* Shared profiles
* Plugin domains

These additions should avoid breaking existing entities.

---

# Design Rules

* Entities remain persistence-only.
* No business logic in entities or DAOs.
* Foreign keys enforce integrity.
* IDs are immutable.
* Events and document versions are append-only.
* Dynamic metadata avoids schema churn.

---

# Summary

The Room persistence model is the durable foundation of LifePilot.

It provides transactional, offline storage while remaining independent of business logic and presentation.

The schema is intentionally designed to support a configuration-driven platform capable of evolving through new Object Types and Domain Packs without frequent structural changes.
