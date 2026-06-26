# Design System

**Location:** `docs/05-design/Design-System.md`

---

# Purpose

The Design System defines the visual language and interaction patterns used throughout LifePilot.

Its purpose is to ensure consistency, reduce design decisions during implementation, and create a calm, professional user experience.

The Design System is the single source of truth for:

* Colors
* Typography
* Spacing
* Icons
* Components
* Animations
* Layout
* Elevation
* Interaction Patterns

No screen should introduce its own design language.

---

# Design Philosophy

LifePilot is an administrative life management application.

It should feel:

* Calm
* Professional
* Trustworthy
* Minimal
* Structured

It should never feel:

* Playful
* Gamified
* Busy
* Social
* Corporate Dashboard-like

The design should disappear into the background while helping the user complete important administrative work.

---

# Material Design

Version 1 follows:

* Material Design 3
* Jetpack Compose
* Dynamic Color (optional)
* Responsive layouts

Material components may be customized but should not be replaced unnecessarily.

---

# Visual Principles

## Consistency

The same action should always look identical.

Example

Primary Button

↓

Always same shape

Always same elevation

Always same interaction

---

## Simplicity

Every screen should have one primary purpose.

Avoid multiple competing calls to action.

---

## Progressive Disclosure

Show only the information required initially.

Additional details appear only when requested.

---

## Hierarchy

Information importance should be visually obvious.

Current State

↓

Tasks

↓

Supporting Details

↓

History

---

# Color System

Version 1 uses a restrained palette.

Primary

Used for:

* Buttons
* Selected states
* Links

Secondary

Used sparingly.

Surface

Primary background.

Surface Variant

Cards

Lists

Containers

Error

Critical reminders.

Warning

Upcoming attention.

Success

Completed tasks.

Info

Neutral information.

Avoid decorative colors.

Color communicates meaning.

---

# Typography

Recommended hierarchy.

Display

Application titles.

Headline

Screen titles.

Title

Card titles.

Body

Primary content.

Label

Buttons

Badges

Metadata

Caption

Secondary information.

Use consistent sizing across the application.

---

# Spacing

Adopt an 8dp grid.

Allowed spacing:

4dp

8dp

16dp

24dp

32dp

48dp

Avoid arbitrary spacing.

---

# Corner Radius

Small

8dp

Cards

Medium

12dp

Dialogs

Large

16dp

Bottom Sheets

Consistency is mandatory.

---

# Elevation

Minimal elevation.

Flat UI preferred.

Elevation should communicate hierarchy, not decoration.

Cards

Low elevation.

Dialogs

Medium elevation.

Floating Action Button

High elevation.

---

# Icons

Use Material Symbols.

Icons should communicate object types.

Examples

Passport

Badge

Vehicle

Directions Car

Property

Home

Loan

Account Balance

Insurance

Shield

Resume

Description

Job

Work

Avoid custom icons in Version 1.

---

# Buttons

Only three button types.

Primary

Main action.

Secondary

Supporting action.

Text

Low-priority action.

Never place more than one primary button in the same visual group.

---

# Cards

Cards are the primary UI building block.

Every card contains:

* Title
* Supporting text
* Optional icon
* Optional status
* Actions

Cards should remain visually lightweight.

---

# Lists

Lists display:

* Objects
* Tasks
* Documents
* Timeline

Every list item should contain:

* Leading icon
* Primary text
* Secondary text
* Status
* Optional trailing action

---

# Forms

Forms are generated from Metadata Schemas.

Input components include:

* Text Field
* Number
* Date Picker
* Dropdown
* Toggle
* Checkbox
* File Picker

Validation appears inline.

Forms should never overwhelm users.

---

# Dialogs

Dialogs are reserved for:

* Confirmation
* Deletion
* Irreversible actions

Avoid using dialogs for complex workflows.

Prefer full-screen flows.

---

# Bottom Sheets

Use bottom sheets for:

* Quick Actions
* Filters
* Object Actions
* AI Suggestions

Avoid nesting bottom sheets.

---

# Navigation

Primary Navigation

Bottom Navigation

Secondary Navigation

Top App Bar

Object Navigation

Tabs

Search

Always globally accessible.

---

# Empty States

Every empty state should contain:

* Illustration (optional)
* Explanation
* Primary action

Example

"No Passports Added"

↓

"Add your first passport."

Avoid negative language.

---

# Loading States

Use skeleton placeholders.

Avoid blocking the screen.

AI sections load independently.

Documents load independently.

Timeline loads independently.

The application should always feel responsive.

---

# Error States

Errors should:

Explain the problem.

Suggest recovery.

Offer retry.

Never expose technical exceptions.

---

# Motion

Animation should be subtle.

Recommended duration:

150–300 ms

Examples

Card expansion

Navigation

Dialogs

Bottom sheets

Avoid decorative animations.

Motion should reinforce understanding.

---

# Accessibility

Support:

* Large fonts
* Screen readers
* High contrast
* Keyboard navigation
* Reduced motion
* Color-independent status indicators

Accessibility is a core requirement.

---

# Responsive Layout

Support:

Phone portrait

Phone landscape

Tablet (future)

The layout system should adapt rather than duplicate screens.

---

# Component Library

Every reusable component belongs in the Design System.

Examples

* Object Card
* Task Card
* Timeline Card
* Reminder Card
* Metadata Row
* Document Preview
* Search Result Card
* Dashboard Widget
* Status Chip
* Empty State

No feature should invent its own components.

---

# Design Rules

Every screen should satisfy:

* One primary purpose.
* One primary action.
* Consistent spacing.
* Predictable navigation.
* Minimal cognitive load.
* Fast visual scanning.

If a screen feels crowded, simplify it rather than shrinking elements.

---

# Summary

The Design System ensures that LifePilot remains visually consistent as it grows.

Rather than designing individual screens independently, every interface is composed from a shared library of reusable components and interaction patterns.

The user should never have to learn a new interface when navigating to a different part of the application.

Consistency builds trust, and trust is essential for an application responsible for managing important personal information.
