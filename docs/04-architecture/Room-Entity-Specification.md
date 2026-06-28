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
3
```

Export schema

```kotlin
exportSchema = true
```

Schema history is committed to `data/schemas/` in version control.

---

# Entity Overview

The current schema (version 3) contains the following entities.

```text
ProfileEntity

ObjectEntity

MetadataEntity          ← includes verification_status column (added v2→3)

DocumentEntity

DocumentVersionEntity

EventEntity

TaskEntity              ← includes goal_id column (added v1→2)

GoalEntity              ← added v1→2

RelationshipEntity

ReminderEntity

TimelineEntity

ConversationEntity      ← added v1→2

ChatMessageEntity       ← added v1→2

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
* verificationStatus   ← TEXT NOT NULL DEFAULT 'UNVERIFIED' (added MIGRATION_2_3)
* updatedAt

Indexes

* objectId
* fieldId

Metadata is stored vertically to support dynamic schemas. The special `fieldId = "ai_context"` stores `AiObjectContext` JSON.

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

Foreign Keys

ObjectEntity, GoalEntity (nullable)

Fields

* taskId
* objectId
* goalId        ← nullable FK to GoalEntity (added MIGRATION_1_2)
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

# GoalEntity

Added in MIGRATION_1_2.

Table: `goals`

Primary Key

```text
goalId
```

Foreign Key

ProfileEntity

Fields

* goalId
* profileId
* title
* description
* deadline
* estimatedWeeks
* status          ← GoalStatus: DRAFT, PROPOSED, ACTIVE, COMPLETED, ARCHIVED, CANCELLED
* linkedObjectId  ← nullable FK to ObjectEntity
* createdAt
* updatedAt

Indexes

* profileId
* status

---

# ConversationEntity

Added in MIGRATION_1_2.

Table: `conversations`

Stores AI conversation sessions.

Primary Key

```text
conversationId
```

Foreign Key

ProfileEntity

Fields

* conversationId
* profileId
* title
* createdAt
* updatedAt

---

# ChatMessageEntity

Added in MIGRATION_1_2.

Table: `chat_messages`

Stores individual messages within a conversation.

Primary Key

```text
messageId
```

Foreign Key

ConversationEntity

Fields

* messageId
* conversationId
* role           ← USER or ASSISTANT
* content
* timestamp

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

# Migration History

## MIGRATION_1_2 (DB version 1 → 2)

Added:

* `goals` table (`GoalEntity`) with columns: `goalId`, `profileId`, `title`, `description`, `deadline`, `estimatedWeeks`, `status`, `linkedObjectId`, `createdAt`, `updatedAt`
* `conversations` table (`ConversationEntity`) with columns: `conversationId`, `profileId`, `title`, `createdAt`, `updatedAt`
* `chat_messages` table (`ChatMessageEntity`) with columns: `messageId`, `conversationId`, `role`, `content`, `timestamp`
* `goal_id TEXT` column (nullable) to the `tasks` table

## MIGRATION_2_3 (DB version 2 → 3)

Added:

* `verification_status TEXT NOT NULL DEFAULT 'UNVERIFIED'` column to the `metadata` table

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
