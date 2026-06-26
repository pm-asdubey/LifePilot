# Navigation Architecture

**Location:** `docs/04-architecture/Navigation-Architecture.md`

---

# Purpose

This document defines how users navigate through LifePilot.

It specifies:

* Navigation Graph
* Routes
* Screen Hierarchy
* Deep Links
* Back Navigation
* Modal Navigation
* Profile Switching
* Navigation Rules

The navigation system should remain predictable, scalable and independent of business logic.

---

# Philosophy

Navigation should follow the user's mental model.

Users think about:

* What needs attention?
* What do I own?
* What changed?
* What am I looking for?

Navigation should support these questions directly.

---

# Navigation Principles

Every navigation decision should satisfy the following principles.

## Predictable

The Back button should always behave as expected.

---

## Shallow

Important information should be reachable within three taps.

---

## Object-Centric

Objects are destinations.

Screens exist to help users discover Objects.

---

## Stateless

Navigation should never contain business state.

Business state belongs to ViewModels.

---

## Restorable

Navigation state should survive:

* Configuration changes
* Process death
* Application restart (where appropriate)

---

# Primary Navigation

LifePilot uses four primary destinations.

```text
Home

Library

Search

Profile
```

---

## Home

Purpose

Operational dashboard.

Contains

* Dashboard
* Attention Required
* Tasks
* AI Insights
* Recent Activity

---

## Library

Purpose

Browse structured life data.

Contains

* Domains
* Objects
* Documents
* Timeline
* Relationships

---

## Search

Purpose

Universal discovery.

Supports:

* Text Search
* AI Query
* Filters
* Recent Searches

---

## Profile

Purpose

Manage the application.

Contains:

* Profiles
* Settings
* Notifications
* Backup
* Storage
* About

---

# Root Navigation Graph

```text
Root

├── Home

├── Library

│      ├── Domain

│      │      └── Object

│      │             ├── Documents

│      │             ├── Timeline

│      │             ├── Tasks

│      │             ├── Relationships

│      │             └── Metadata

├── Search

├── Profile

└── Global Modals
```

---

# Global Destinations

These may be opened from anywhere.

* Add Object
* Upload Document
* AI Assistant
* Filter Dialog
* Object Picker
* Profile Switcher

Global destinations should appear as bottom sheets or dialogs when appropriate.

---

# Object Navigation

Every Object uses the same route.

```text
/object/{objectId}
```

Examples

```
/object/OBJ_001

/object/OBJ_245

/object/OBJ_900
```

Navigation should never depend on Object Type.

---

# Domain Navigation

Domains use:

```text
/domain/{domainId}
```

Example

```
/domain/finance

/domain/property
```

---

# Document Navigation

Documents use:

```text
/document/{documentId}
```

Document Viewer should support:

* Zoom
* Share
* Replace
* Export

---

# Timeline Navigation

Timeline supports filters.

Examples

```text
/timeline

/timeline?profile=ash

/timeline?domain=career

/timeline?object=OBJ_001
```

Timeline is always filterable.

---

# Search Navigation

Search supports:

* Empty State
* Active Search
* Search Results
* AI Results

Recent searches should persist locally.

---

# Profile Switching

Switching Profiles should:

* Clear active Object state
* Refresh dashboard
* Refresh search scope
* Recalculate reminders

Navigation stack should remain valid where possible.

---

# Deep Links

Every major entity supports deep linking.

Examples

```
lifepilot://object/OBJ_001

lifepilot://document/DOC_014

lifepilot://task/TASK_118

lifepilot://timeline/TL_882
```

Future cloud sync may expose these externally.

---

# Bottom Sheets

Bottom sheets are preferred for lightweight actions.

Examples

* Object Actions
* Filters
* AI Suggestions
* Quick Edit
* Reminder Details

Avoid creating full screens unnecessarily.

---

# Dialogs

Dialogs should only be used for:

* Delete confirmation
* Permission requests
* Critical warnings

Complex workflows should never occur inside dialogs.

---

# Back Navigation Rules

The Back button should always return to the previous logical destination.

Example

```
Library

↓

Passport

↓

Document

↓

Back

↓

Passport
```

Users should never lose context.

---

# Navigation Events

Navigation should be event-driven.

Example

```
Upload Completed

↓

Navigate to Verification

↓

Verification Complete

↓

Navigate to Object

↓

Dashboard Refresh
```

Navigation should not be embedded inside business logic.

---

# Navigation Arguments

Arguments should remain lightweight.

Pass:

* IDs
* Filter values
* Simple flags

Never pass complete Objects through navigation.

All data should be reloaded from repositories.

---

# State Restoration

The application should restore:

* Selected tab
* Active Object
* Scroll position
* Search query
* Open filters

Where practical.

Sensitive flows (such as verification) should restore safely after interruption.

---

# Future Compatibility

Navigation should support future additions.

Examples

* Tablet layouts
* Foldables
* Desktop
* Multi-window
* External deep links
* Widgets

These should extend the graph rather than replace it.

---

# Design Rules

Navigation must be:

* Consistent
* Predictable
* Object-centric
* Easily testable
* Independent of UI implementation

The navigation graph should remain declarative.

---

# Summary

The Navigation Architecture defines how users move through LifePilot.

Every destination has a single responsibility.

Objects are the central navigation target.

The navigation system remains independent of business logic, allowing screens and workflows to evolve without changing the underlying application architecture.
