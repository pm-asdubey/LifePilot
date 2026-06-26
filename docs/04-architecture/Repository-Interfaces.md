# Repository Interfaces

**Location:** `docs/04-architecture/Repository-Interfaces.md`

---

# Purpose

This document defines every Repository Interface used by LifePilot.

Repositories expose business-oriented operations to the Domain Layer.

Repositories hide implementation details including:

* Room
* SQLite
* File Storage
* OCR
* AI Providers
* Android APIs

The Domain Layer communicates only with repository interfaces.

---

# Philosophy

Repositories should express business intent.

Good examples

* Add Passport
* Find Active Loans
* Upload Document

Poor examples

* Insert Row
* Update Table
* Execute SQL

The Domain Layer should never know how information is stored.

---

# Repository Principles

Every repository should be:

* Small
* Focused
* Testable
* Platform-independent
* Asynchronous
* Transaction-safe

Repositories must never contain business rules.

---

# Repository Overview

LifePilot defines the following repositories.

```text
ProfileRepository

ObjectRepository

DocumentRepository

MetadataRepository

EventRepository

TaskRepository

RelationshipRepository

ReminderRepository

TimelineRepository

SearchRepository

BackupRepository

AIRepository
```

Each repository owns a single aggregate.

---

# Profile Repository

Responsibilities

* Create Profile
* Update Profile
* Delete Profile
* Switch Active Profile
* Observe Active Profile

Example operations

```kotlin
createProfile()

updateProfile()

getProfile()

observeProfiles()

switchProfile()
```

---

# Object Repository

Responsibilities

* Create Object
* Archive Object
* Delete Object
* Observe Object
* Query Objects

Example operations

```kotlin
createObject()

updateObject()

getObject()

observeObject()

observeObjects()

archiveObject()
```

Objects are the primary aggregate root.

---

# Metadata Repository

Responsibilities

* Read Metadata
* Update Metadata
* Validate Metadata
* Retrieve Metadata History

Example operations

```kotlin
saveMetadata()

getMetadata()

observeMetadata()

metadataHistory()
```

---

# Document Repository

Responsibilities

* Upload Document
* Replace Version
* Delete Document
* Retrieve Documents

The repository coordinates with the File Storage Service.

Example operations

```kotlin
uploadDocument()

replaceDocument()

getDocuments()

deleteDocument()
```

---

# Event Repository

Responsibilities

* Record Events
* Query Events
* Observe Event Stream

Events are append-only.

Example operations

```kotlin
recordEvent()

observeEvents()

findEvents()
```

---

# Task Repository

Responsibilities

* Create Tasks
* Complete Tasks
* Cancel Tasks
* Observe Tasks

Example operations

```kotlin
createTask()

completeTask()

observeTasks()

getPendingTasks()
```

---

# Relationship Repository

Responsibilities

* Create Relationships
* Remove Relationships
* Query Graph
* Retrieve Connected Objects

Example operations

```kotlin
createRelationship()

findRelationships()

connectedObjects()

removeRelationship()
```

---

# Reminder Repository

Responsibilities

* Schedule Reminder
* Complete Reminder
* Snooze Reminder
* Observe Reminders

Example operations

```kotlin
scheduleReminder()

completeReminder()

observeReminders()

dismissReminder()
```

---

# Timeline Repository

Responsibilities

* Retrieve Timeline
* Filter Timeline
* Observe Timeline

Timeline is read-only.

Example operations

```kotlin
timeline()

observeTimeline()

filterTimeline()
```

---

# Search Repository

Responsibilities

* Execute Search
* Update Index
* Suggest Results

Example operations

```kotlin
search()

reindex()

recentSearches()

suggest()
```

---

# Backup Repository

Responsibilities

* Export Data
* Import Data
* Verify Backup
* Restore Backup

Example operations

```kotlin
createBackup()

restoreBackup()

verifyBackup()
```

---

# AI Repository

Responsibilities

* Metadata Extraction
* Summarization
* Context Retrieval
* AI Insights

The Domain Layer should never know which LLM provider is being used.

Example operations

```kotlin
extractMetadata()

summarize()

generateInsight()

answerQuestion()
```

---

# Repository Communication

Repositories must never call one another directly.

Instead:

```text
Use Case

↓

Repositories

↓

Results

↓

Use Case
```

If multiple repositories are required, coordination belongs inside a Use Case or Domain Service.

---

# Transactions

Business operations spanning multiple repositories must execute atomically.

Example

Upload Passport

↓

Object Repository

↓

Document Repository

↓

Metadata Repository

↓

Event Repository

↓

Commit

Repositories should participate in a shared transaction where required.

---

# Reactive APIs

Read operations should expose reactive streams where appropriate.

Preferred pattern:

* Flow for observation
* suspend functions for commands

Example

```kotlin
observeTasks(): Flow<List<Task>>

completeTask(taskId)
```

---

# Error Handling

Repositories return domain-friendly results.

Never expose:

* SQLite exceptions
* File exceptions
* HTTP exceptions

Translate implementation failures into meaningful domain errors.

---

# Testing

Every repository interface must have:

* Unit tests
* Fake implementation
* Integration tests for the concrete implementation

Business logic tests should depend on fake repositories rather than Room.

---

# Future Compatibility

Repositories should remain stable even if implementations change.

Possible future implementations:

* Room
* Cloud database
* Hybrid sync
* REST API
* Graph database

The Domain Layer should not require modification.

---

# Design Rules

Repositories must:

* Represent business concepts.
* Hide storage details.
* Be asynchronous.
* Avoid side effects outside their aggregate.
* Return domain models rather than persistence models.

---

# Summary

Repository Interfaces define the contracts between the Domain Layer and the Data Layer.

By designing repositories around business capabilities instead of database operations, LifePilot maintains a clean separation of concerns, supports multiple storage implementations, and remains testable as the project evolves.
