# File Storage Architecture

**Location:** `docs/04-architecture/File-Storage-Architecture.md`

---

# Purpose

This document defines how all user files are stored, organized, versioned and managed within LifePilot.

The file storage system is responsible for:

* Original Documents
* Images
* Camera Scans
* OCR Output
* Generated Thumbnails
* Export Packages
* Backup Data

The database stores references.

The filesystem stores files.

---

# Philosophy

The filesystem is the user's property.

If LifePilot is uninstalled tomorrow, the user should still be able to browse every uploaded file.

LifePilot should never lock documents inside an opaque proprietary format.

Files should remain portable.

---

# Design Goals

The storage system must be:

* Offline-first
* Human-readable
* Portable
* Versioned
* Backup friendly
* Fast
* Predictable

---

# Root Directory

```text
LifePilot/

├── Profiles/
├── Documents/
├── OCR/
├── Thumbnails/
├── Exports/
├── Cache/
├── Logs/
└── Temp/
```

Each folder has a single responsibility.

---

# Profiles Folder

Each profile owns a directory.

```text
Profiles/

Ash/

Dad/

Mom/
```

Profile folders should never directly contain documents.

Instead they reference Objects.

---

# Documents Folder

Documents are grouped by Object ID rather than Object Type.

Example

```text
Documents/

OBJ_0001/

document_001.pdf

document_002.jpg

OBJ_0002/

offer_letter.pdf
```

This avoids moving files if an Object changes Domain or Type.

---

# File Naming

Original filenames should be preserved whenever possible.

Internally every file receives:

* UUID
* Original Filename
* MIME Type
* SHA-256 Checksum

Example

```text
UUID:
4b82...

Original:
Passport.pdf

Stored:
4b82....pdf
```

Display names come from metadata.

Storage names guarantee uniqueness.

---

# Versioning

Documents are immutable.

Replacing a document creates a new version.

Example

```text
Passport

Version 1

↓

Version 2

↓

Version 3
```

Previous versions remain accessible until deleted by the user.

---

# OCR Storage

OCR results are stored separately.

```text
OCR/

document_uuid/

raw.txt

structured.json

layout.json
```

OCR can therefore be regenerated without touching the original file.

---

# Thumbnail Storage

Generated previews are stored independently.

```text
Thumbnails/

document_uuid.webp
```

Thumbnails are disposable.

They may be regenerated at any time.

---

# Cache

Temporary processing files.

Examples

* Camera preprocessing
* OCR intermediate files
* AI temporary summaries

The Cache directory may be safely cleared.

---

# Temporary Files

The Temp directory is used during processing.

Example

Upload

↓

Temp

↓

Processing

↓

Success

↓

Move to Documents

If processing fails, Temp can be cleaned automatically.

---

# Export Structure

Future exports should produce:

```text
LifePilot-Export/

database.db

Documents/

OCR/

Metadata/

manifest.json
```

This allows complete restoration.

---

# File Metadata

The database stores:

* File UUID
* Object ID
* Profile ID
* Original Name
* MIME Type
* Size
* Checksum
* Created Date
* Version
* Storage Path

The filesystem never stores business metadata.

---

# Security

Sensitive files should support:

* Encryption at rest (future)
* Secure deletion
* Integrity verification
* Checksum validation

No file should be modified after import.

---

# Duplicate Detection

Duplicate uploads are detected using:

1. SHA-256 checksum
2. File size
3. OCR similarity
4. Metadata similarity

Duplicates should be suggested to the user rather than automatically removed.

---

# Backup Strategy

Backup consists of:

* SQLite Database
* Documents Folder
* OCR Folder

Everything else can be regenerated.

Examples

Can regenerate:

* Thumbnails
* Search Index
* Cache

Cannot regenerate:

* Original Documents
* Database

---

# Restore Strategy

Restore Process

```text
Restore Backup

↓

Restore Database

↓

Restore Documents

↓

Validate Checksums

↓

Rebuild Search Index

↓

Regenerate Thumbnails

↓

Regenerate Cache

↓

Ready
```

The restore process should be deterministic.

---

# Storage Limits

The application should not impose arbitrary limits.

Only device storage limits apply.

Users should always be able to view storage usage.

---

# File Integrity

A periodic integrity check should verify:

* Missing files
* Corrupted files
* Invalid references
* Duplicate files

Problems should be reported to the user.

---

# Android Storage

Version 1 should use Android's app-specific storage for application-managed files while providing an explicit export mechanism for backups and migration.

The storage implementation should remain abstracted behind a File Storage Service so it can evolve without affecting higher layers.

---

# File Storage Service

The rest of the application must never access the filesystem directly.

Instead:

```text
Object Screen

↓

Repository

↓

File Storage Service

↓

Filesystem
```

The File Storage Service is responsible for:

* Save
* Read
* Delete
* Move
* Copy
* Version
* Export
* Import
* Integrity Check

---

# Design Principles

The storage system must:

* Preserve original files
* Never overwrite documents
* Support version history
* Be portable
* Remain implementation-independent
* Keep the filesystem predictable

---

# Summary

The File Storage Architecture ensures that LifePilot treats user documents as durable, user-owned assets.

The database describes those assets.

The filesystem stores them.

By separating storage from business logic and preserving immutable originals, the application provides reliability, portability and long-term trust.
