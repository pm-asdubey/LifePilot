# Security Architecture

**Location:** `docs/04-architecture/Security-Architecture.md`

---

# Purpose

This document defines the security model for LifePilot.

The application manages highly sensitive personal information and must protect it throughout its lifecycle.

Security is considered a foundational architectural concern rather than an optional feature.

This document defines:

* Data protection
* Authentication
* Encryption
* Backup security
* AI privacy
* Secure storage
* Threat mitigation

---

# Security Principles

LifePilot follows these principles.

* Privacy by Design
* Least Privilege
* Offline First
* User Ownership
* Secure by Default
* Defense in Depth
* Explicit Consent

Users should always know:

* What data is stored.
* Where it is stored.
* When it leaves the device.
* Why it leaves the device.

---

# Threat Model

Version 1 considers the following threats.

* Device loss
* Unauthorized device access
* Accidental data exposure
* Malicious applications
* Backup interception
* AI provider data exposure
* File corruption

Network-based attacks are limited because Version 1 has no backend.

---

# Data Classification

All data is classified into one of four levels.

## Public

Examples

* App settings
* Theme preference

---

## Private

Examples

* Notes
* Tasks
* Timeline

---

## Sensitive

Examples

* Passport Number
* PAN
* Aadhaar
* Bank Account
* Loan Details
* Insurance Policy

---

## Highly Sensitive

Examples

* Original identity documents
* Medical records
* Property documents
* Export archives

Protection requirements increase with classification.

---

# Device Authentication

LifePilot should support optional application lock.

Methods:

* Device PIN
* Fingerprint
* Face Authentication

Authentication should rely on Android's biometric framework.

The app must never implement its own password storage.

---

# Encryption

## Database

Sensitive fields should support encryption at rest.

Examples:

* Passport Number
* Aadhaar Number
* PAN
* Account Number

Non-sensitive fields may remain unencrypted for indexing efficiency.

---

## Files

Original documents should support encrypted storage in a future release.

The File Storage Service should abstract encryption so callers remain unaware of implementation details.

---

## Keys

Encryption keys must never be stored in application code.

Use:

* Android Keystore

Keys should be non-exportable where supported.

---

# AI Privacy

AI requests should follow strict minimization principles.

Only transmit:

* Required document text
* Relevant metadata
* Required user question

Never transmit:

* Entire database
* Unrelated Objects
* Local storage paths
* Internal identifiers unless required

Future providers should be selectable.

---

# Backup Security

Backups should support:

* Optional encryption
* Integrity verification
* Manifest validation

Encrypted backups should require a user-provided passphrase.

The passphrase must never be recoverable by the application.

---

# Export Security

Exports should include:

* Manifest
* Checksums
* Version information

Optional encrypted exports should use modern authenticated encryption.

---

# Permission Model

Request permissions only when needed.

Examples

Camera

Only when scanning.

Storage

Only when importing or exporting.

Notifications

Only when reminders are enabled.

The application should function with the minimum practical permissions.

---

# Secure Deletion

Deleting an Object should:

* Remove database references.
* Remove associated files when requested.
* Remove thumbnails.
* Remove OCR output.
* Remove search index entries.

Users should be able to choose between:

* Archive
* Delete metadata only
* Delete permanently

---

# Integrity Verification

The application should periodically verify:

* Missing files
* Modified files
* Checksum mismatches
* Invalid references

Problems should be reported clearly.

---

# Logging

Logs must never contain:

* Passport numbers
* Aadhaar numbers
* PAN numbers
* Bank account numbers
* OCR text
* AI prompts containing user data

Logs should contain identifiers rather than sensitive content.

---

# Clipboard Handling

Sensitive values copied to the clipboard should:

* Display a warning.
* Optionally clear automatically after a configurable period.

---

# Screenshot Policy

Version 1 should allow screenshots by default.

Future versions may allow users to disable screenshots for sensitive screens.

This preference should apply consistently across the application.

---

# Search Privacy

Sensitive fields should support masking in search results.

Example

Passport

```
P******123
```

Full values appear only when opening the Object.

---

# Audit Trail

Security-relevant events should be recorded.

Examples

* Backup created
* Backup restored
* Export generated
* Biometric authentication failed
* Application unlocked

Audit entries should avoid storing sensitive payloads.

---

# Third-Party Components

Every external dependency should be reviewed for:

* License
* Maintenance
* Security history
* Update frequency

Unused dependencies should be removed promptly.

---

# Security Testing

Every release should include:

* Static analysis
* Dependency vulnerability scan
* Encryption verification
* Backup validation
* Permission review

Security testing is part of the release process.

---

# Future Compatibility

The architecture should support:

* End-to-end encrypted cloud sync
* Hardware-backed encryption
* Secure sharing
* Multiple trusted devices
* Enterprise security policies

These features should build upon the existing model rather than replace it.

---

# Design Rules

The application must:

* Default to privacy.
* Minimize collected data.
* Encrypt sensitive information where appropriate.
* Never expose secrets in logs.
* Never send unnecessary information to AI providers.
* Respect user ownership of data.

Security decisions should favor protecting user information over convenience.

---

# Summary

LifePilot manages some of the most important documents in a person's life.

Its security architecture is therefore centered on protecting confidentiality, preserving integrity, and maintaining user control.

Security is not a separate feature; it is a property of every subsystem within the application.
