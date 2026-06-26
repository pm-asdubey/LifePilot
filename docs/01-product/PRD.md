# Product Requirements Document (PRD)

**Project:** LifePilot
**Version:** 1.0
**Status:** Draft
**Related Documents:** MASTER-SPEC.md, CLAUDE.md

---

# 1. Executive Summary

LifePilot is an offline-first Android application that organizes a person's administrative life into structured entities.

Rather than functioning as a document manager or AI chatbot, LifePilot continuously maintains a structured representation of a user's life through Profiles, Domains, Objects, Documents, Events and Tasks.

Artificial Intelligence acts as an interface to retrieve, explain and reason over that structured state.

---

# 2. Problem Statement

Managing modern life requires keeping track of hundreds of documents, deadlines and administrative events.

Examples include:

* Passports
* Insurance policies
* Property papers
* Offer letters
* Medical reports
* Bank accounts
* Investments
* Degrees
* Visas

Today these are scattered across:

* Downloads
* WhatsApp
* Google Drive
* Gmail
* Photos
* Physical folders
* Notes
* Calendar
* Todo applications

As a result users frequently forget:

* what they own
* where documents are stored
* when things expire
* what requires action

Current software manages files.

LifePilot manages life state.

---

# 3. Product Vision

LifePilot should become the operating system for a person's administrative life.

Users should think in terms of real-world entities rather than applications or folders.

Example:

Instead of remembering:

"My passport scan is inside Downloads/Travel/Documents"

The user simply asks:

"Show my passport."

---

# 4. Goals

Version 1 should allow users to:

* Store important documents.
* Organize information into structured objects.
* Track life events.
* Automatically generate tasks.
* Search across their entire life.
* Ask AI questions about stored information.
* Receive proactive reminders.

---

# 5. Non-Goals

Version 1 intentionally excludes:

* Cloud synchronization
* iOS support
* Shared accounts
* Collaboration
* Email parsing
* Calendar synchronization
* WhatsApp integration
* Payments
* Subscription system
* Backend infrastructure

---

# 6. Target Users

Primary users include:

* Professionals
* Students
* Families
* Property owners
* Frequent travelers
* Individuals managing documents for parents or relatives

---

# 7. Core Product Concepts

The application revolves around seven entities.

## Profile

Represents an individual.

Examples:

* Me
* Dad
* Mom
* Brother

---

## Domain

High-level organizational areas.

Examples:

* Identity
* Career
* Finance
* Health
* Property
* Education
* Travel
* Legal

---

## Object

A real-world entity requiring management.

Examples:

* Passport
* Resume
* Job
* Insurance
* Vehicle
* Property
* Bank Account
* Investment

Objects are the primary organizational unit.

---

## Document

Original uploaded files.

Supported formats include:

* PDF
* Image
* Camera scan

Documents remain immutable.

---

## Event

Something that changes an object's state.

Examples:

* Passport Renewed
* Joined Company
* Bought Property
* Hospitalized

---

## Task

An action that should be completed.

Tasks may be:

* Manual
* Automatically generated

---

## Relationship

Defines how entities connect.

Example:

Passport

↓

Required For

↓

Visa

---

# 8. User Journey

Typical flow:

Upload Document

↓

OCR

↓

Metadata Extraction

↓

User Verification

↓

Object Updated

↓

Timeline Updated

↓

Tasks Generated

↓

Home Dashboard Updated

↓

AI Can Answer Questions

---

# 9. Navigation Structure

Version 1 contains five primary sections.

## Home

Purpose:

Provide a daily overview.

Displays:

* Attention Required
* Upcoming Tasks
* Expiring Documents
* Recent Activity
* AI Suggestions

---

## Objects

Displays all managed objects grouped by domain.

Each object includes:

* Overview
* Documents
* Timeline
* Tasks
* Metadata
* Relationships
* AI Summary

---

## Timeline

Chronological history of life events.

Supports:

* Day
* Week
* Month
* Year

---

## AI

Conversational interface for querying the Life State Engine.

AI retrieves structured information.

Examples:

"What expires next?"

"When did I buy my house?"

"Show Dad's insurance."

---

## Settings

Application preferences.

Includes:

* Profiles
* Backup
* Storage
* AI
* Notifications
* Privacy

---

# 10. Search

Universal search across:

* Profiles
* Domains
* Objects
* Documents
* Events
* Tasks

Search should prioritize meaning rather than filenames.

---

# 11. Notifications

Only three notification categories exist.

Daily Digest

Weekly Digest

Critical Reminder

Every notification derives from these categories.

---

# 12. Success Metrics

Version 1 is considered successful if users can reliably:

* Retrieve documents in seconds.
* Understand what needs attention today.
* Track important life events.
* Receive timely reminders.
* Trust AI answers because they are grounded in stored data.

---

# 13. Future Vision

Future releases may introduce:

Version 2

* Google Drive Backup
* Encrypted Export
* Widgets
* Improved OCR

Version 3

* Cloud Sync
* LifePilot Account
* Cross-device support
* iOS

Version 4

* Domain Packs
* Immigration
* Tax
* Healthcare
* Estate Planning

---

# 14. Product Principles

Every feature added to LifePilot should satisfy the following questions.

* Does it strengthen the Life State Engine?
* Does it reduce cognitive load?
* Does it minimize user effort?
* Does it preserve user ownership?
* Does it work offline?
* Can AI infer this automatically instead of asking the user?

If the answer to these questions is consistently "no", the feature should be reconsidered or rejected.
