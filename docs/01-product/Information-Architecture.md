# Information Architecture

**Location:** `docs/01-product/Information-Architecture.md`

---

# Purpose

This document defines how information is organized, discovered, and navigated throughout LifePilot.

It is not a UI specification.

It defines the logical structure of the application and the relationship between screens.

Every future screen must fit into this architecture.

---

# Philosophy

LifePilot is not organized around files.

It is not organized around features.

It is organized around the user's life.

The user should never have to think:

* "Where did I save that?"

Instead they should think:

* "I need my passport."

The application should naturally lead them there.

---

# Navigation Principles

Navigation should satisfy five goals.

## 1. Minimal Depth

Any important information should be reachable within three taps.

---

## 2. Multiple Entry Points

Users should be able to reach the same information through different paths.

Example

Passport

Accessible from:

* Home
* Search
* Objects
* AI
* Timeline
* Profile

---

## 3. Context Preservation

Moving between screens should preserve context.

Example

Vehicle

↓

Insurance

↓

Claim

↓

Back

Should return the user to the Vehicle screen.

---

## 4. Object-Centric Navigation

Objects are the primary destination.

Screens exist to discover Objects.

Objects do not exist to support screens.

---

## 5. Progressive Disclosure

Show only what the user needs initially.

Reveal advanced information only when requested.

---

# Primary Navigation

Version 1 contains five primary destinations.

## Home

Purpose

Daily operational dashboard.

Answers:

"What needs my attention today?"

Displays

* Attention Required
* Upcoming Tasks
* Expiring Objects
* Recent Activity
* Suggested Actions
* Daily Summary

---

## Objects

Purpose

Browse everything the user manages.

Grouped by Domain.

Examples

Identity

Finance

Career

Health

Property

Travel

Legal

Education

Family

---

## Timeline

Purpose

View life chronologically.

Answers

"What happened?"

Supports

* Day
* Week
* Month
* Year

Filters

* Profile
* Domain
* Object
* Event Type

---

## AI

Purpose

Natural language interface.

Answers

"What do you want to know?"

AI is not the primary navigation mechanism.

It complements the rest of the application.

---

## Settings

Purpose

Application configuration.

Contains

* Profiles
* Notifications
* Storage
* AI
* Backup
* Privacy
* About

---

# Secondary Navigation

Within an Object.

Example

Passport

↓

Overview

Documents

Metadata

Timeline

Tasks

Relationships

AI Summary

History

Every Object uses the same navigation pattern.

---

# Universal Search

Search is globally accessible.

Search should return:

* Objects
* Documents
* Events
* Tasks
* Profiles
* Metadata
* Relationships

Results should be grouped by type.

---

# Navigation Hierarchy

```text
Application

├── Home
│
├── Objects
│     │
│     ├── Domain
│     │      │
│     │      └── Object
│     │             │
│     │             ├── Overview
│     │             ├── Documents
│     │             ├── Metadata
│     │             ├── Timeline
│     │             ├── Tasks
│     │             ├── Relationships
│     │             └── History
│
├── Timeline
│
├── AI
│
└── Settings
```

---

# Home Screen Structure

The Home screen should answer four questions.

## What needs attention?

Examples

* Passport expiring
* Insurance renewal
* EMI due

---

## What changed recently?

Examples

* New document added
* Job updated
* Property purchased

---

## What should I do?

Examples

* Update Resume
* Upload Registration
* Review Insurance

---

## What is coming up?

Examples

* FD Maturity
* Visa Expiry
* Appointment

---

# Object Screen Structure

Every Object screen follows the same layout.

Header

↓

Current State

↓

Quick Actions

↓

Overview

↓

Documents

↓

Metadata

↓

Tasks

↓

Timeline

↓

Relationships

↓

AI Insights

Users should never need to learn different layouts for different Objects.

---

# Timeline Structure

Timeline entries include:

* Timestamp
* Title
* Description
* Source Object
* Related Objects
* Quick Actions

Selecting an entry opens the originating Object.

Timeline is never a dead end.

---

# AI Screen

The AI screen contains:

Conversation

Suggested Questions

Recent Questions

Pinned Insights

AI should always reference existing Objects.

It should never become a generic chatbot.

---

# Search Experience

Search results ranked by:

1. Exact Match
2. Active Objects
3. Recently Updated
4. Relationship Distance
5. Semantic Similarity

Filters

* Profile
* Domain
* Object Type
* Date
* Status

---

# Cross Navigation

Every major entity links to related entities.

Example

Property

↓

Loan

↓

Bank Account

↓

Insurance

↓

Tax Receipt

Users should naturally explore connected information.

---

# Empty States

Every empty screen should educate the user.

Example

No Passports

Display

"Add your first passport to begin managing identity documents."

Include a clear call-to-action.

---

# Deep Links

Every Object should have a stable internal identifier.

Examples

* Open specific Object
* Open Task
* Open Timeline Event
* Open Reminder

Future versions may expose these as external deep links.

---

# Accessibility

Navigation must support:

* Screen readers
* Large text
* Keyboard navigation
* High contrast
* One-handed use

Accessibility is a first-class requirement.

---

# Design Rules

Navigation should always feel predictable.

Avoid:

* Hidden menus
* Duplicate navigation paths
* Dead-end screens
* Deep nesting
* Feature-based organization

Prefer:

* Object-centric flows
* Consistent layouts
* Discoverable actions
* Reversible navigation

---

# Summary

The Information Architecture defines how users move through LifePilot.

The application is organized around real-world Objects rather than files or features.

Every screen should help the user answer one of four questions:

* What do I have?
* What changed?
* What needs attention?
* What should I do next?

If a new feature cannot fit naturally into this structure, its design should be reconsidered before implementation.
