# Object Screen Specification

**Location:** `docs/01-product/Object-Screen-Specification.md`

---

# Purpose

This document defines how every Object is displayed within LifePilot.

Every Object Type shares the same screen architecture.

Passport.

Vehicle.

Loan.

Resume.

Property.

Insurance.

Job.

All use the same rendering engine.

The screen changes only according to the Object Definition.

This guarantees consistency while allowing unlimited future Object Types.

---

# Philosophy

The application should not contain a

Passport Screen.

Vehicle Screen.

Loan Screen.

Insurance Screen.

Instead, it should contain one:

Object Screen.

The rendering engine should read the Object Definition and dynamically construct the interface.

---

# High Level Layout

Every Object screen follows the same structure.

```
Header

↓

Current State

↓

Quick Actions

↓

Overview

↓

Metadata

↓

Documents

↓

Timeline

↓

Tasks

↓

Relationships

↓

AI Insights
```

Every Object uses this exact layout.

Only the content changes.

---

# Header

Displays:

* Object Icon
* Object Name
* Domain
* Current Status
* Last Updated

Actions

* Edit
* Share
* Archive
* Delete

---

# Current State Card

Always visible.

Purpose:

Summarize the object.

Example

Passport

```
Status

Active

Expires

March 2033

Country

India
```

Vehicle

```
Status

Active

Insurance

Valid

Next Service

October
```

Loan

```
Status

Open

Outstanding

₹14,20,000

Next EMI

5 July
```

The Current State Card is generated from metadata.

---

# Quick Actions

Generated dynamically.

Passport

* View Scan
* Renew
* Add Visa

Vehicle

* Add Insurance
* Record Service
* Upload RC

Resume

* Replace PDF
* Export
* Share

The Object Definition controls available actions.

---

# Overview

Displays a concise summary.

Contains:

* Description
* Important Dates
* Statistics
* Current Status

No editing occurs here.

---

# Metadata Section

Metadata is generated automatically.

The Metadata Schema defines:

* Label
* Field Type
* Validation
* Formatting

Examples

Passport Number

Expiry Date

Nationality

Issue Date

The screen engine chooses the correct component.

---

# Documents Section

Displays all attached documents.

Each document shows:

* Preview
* Name
* Upload Date
* Type
* Version

Actions

* Open
* Replace
* Download
* Delete

Documents are chronological.

Latest appears first.

---

# Timeline Section

Displays all Events affecting this Object.

Examples

Passport

Issued

↓

Renewed

↓

Visa Added

↓

Renewed Again

Vehicle

Purchased

↓

Insurance Added

↓

Service

↓

Insurance Renewed

Timeline is immutable.

---

# Tasks Section

Shows all tasks related to the Object.

Grouped into:

Pending

Completed

Cancelled

Quick completion should be available.

---

# Relationships Section

Displays connected Objects.

Example

Passport

↓

Visa

↓

Trip

↓

Insurance

↓

Tickets

Relationships should be interactive.

Selecting a related Object opens it immediately.

---

# AI Insights

Generated on demand.

Examples

Passport expires in 11 months.

Vehicle has no insurance.

Property has no tax receipt uploaded.

Loan interest rate is unusually high.

AI Insights never modify the Object.

---

# Edit Mode

Edit Mode should be metadata-driven.

The Metadata Schema determines:

* Form Fields
* Validation
* Required Fields
* Input Components

Developers should never manually build forms for individual Object Types.

---

# Attach Document

Users may attach:

* Camera Scan
* PDF
* Image

After upload

↓

Ingestion Pipeline

↓

Metadata Extraction

↓

Verification

↓

Object Updated

The Object Screen simply initiates the pipeline.

---

# Empty Sections

Empty sections should disappear automatically.

Example

No Relationships

↓

Hide Relationships section.

No Tasks

↓

Hide Tasks section.

This keeps the UI clean.

---

# Loading Strategy

Load progressively.

Priority

1. Header

2. Current State

3. Metadata

4. Documents

5. Timeline

6. Tasks

7. Relationships

8. AI

The user should never wait for AI to load.

---

# Performance

Opening an Object

Target

<150ms

Metadata

Immediate

Documents

Lazy

Timeline

Lazy

AI

On Demand

---

# Configuration Driven UI

The Object Screen should never know:

Passport

Vehicle

Loan

Insurance

Instead it receives:

```
Object Definition

↓

Metadata Schema

↓

Lifecycle Definition

↓

Relationship Rules

↓

Quick Actions

↓

UI Renderer
```

This allows new Object Types without writing UI code.

---

# Future Compatibility

Future Domain Packs should contribute:

* Metadata
* Icons
* Actions
* Lifecycle
* AI Prompts

The Object Screen automatically supports them.

---

# Design Rules

Every Object Screen must:

* Look identical.
* Behave identically.
* Navigate identically.
* Load identically.

Only the content changes.

Consistency reduces user learning and engineering complexity.

---

# Summary

The Object Screen is a generic renderer for every Life Object.

Its layout is fixed.

Its behavior is fixed.

Its appearance is generated from Object Definitions.

This makes LifePilot infinitely extensible while keeping the codebase small, maintainable, and consistent.
