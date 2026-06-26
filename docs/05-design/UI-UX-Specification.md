# UI / UX Specification

**Location:** `docs/05-design/UI-UX-Specification.md`

---

# Purpose

This document defines the visual language, interaction principles and user experience standards for LifePilot.

The objective is not simply to create an attractive application.

The objective is to build an application that feels calm, trustworthy, modern and effortless to use despite managing large amounts of information.

Every screen should reduce cognitive load rather than increase it.

---

# Design Philosophy

LifePilot should feel like:

> Apple Health meets Notion meets Linear.

Not because it copies them visually, but because it shares their principles.

The interface should be:

* Calm
* Premium
* Minimal
* Fast
* Information Dense
* Never Overwhelming

Every screen should communicate confidence and clarity.

---

# Theme

Version 1 supports:

**Dark Mode Only**

There is no Light Theme.

Every component should be designed specifically for dark surfaces rather than simply inverted from a light palette.

The design system should be built around dark mode from the beginning.

---

# Visual Style

The visual style should be:

* Elegant
* Modern
* Spacious
* Professional

Avoid:

* Bright saturated colors
* Heavy gradients
* Glassmorphism
* Skeuomorphism
* Cartoon styling

Prefer subtle depth through spacing, elevation and typography.

---

# Color Philosophy

Most screens should remain neutral.

Accent colours communicate meaning.

Examples

Blue

Information

Green

Success

Orange

Warning

Red

Critical

Purple

AI Features

Color should never be decorative.

It should communicate state.

---

# Typography

Typography should establish clear hierarchy.

Prefer:

* Large page titles
* Medium section headings
* Comfortable body text
* Small metadata labels

Avoid long paragraphs.

Information should be easily scannable.

---

# Layout Philosophy

Whitespace is a feature.

Every screen should have:

* Clear hierarchy
* Consistent spacing
* Predictable alignment
* Generous padding

Avoid crowded layouts.

---

# Navigation

Navigation should require minimal thinking.

Bottom Navigation contains only:

* Home
* Library
* Search
* Profile

Every important destination should be reachable within three taps.

---

# Home Screen

The Home Screen answers one question.

> **What needs my attention today?**

The dashboard should prioritise:

1. Critical reminders
2. Today's tasks
3. Upcoming expiries
4. AI insights
5. Recent activity

Avoid showing unnecessary information.

---

# Object Screens

Every Object Screen follows the same structure.

Header

↓

Current Status

↓

Important Metadata

↓

Documents

↓

Timeline

↓

Tasks

↓

Relationships

↓

AI Summary

Consistency is more important than creativity.

---

# Forms

Forms are generated from schemas.

Every form should:

* Minimise typing
* Use appropriate input controls
* Validate immediately
* Explain errors clearly

Never overwhelm users with long forms.

Prefer progressive disclosure.

---

# Cards

Cards are the primary UI surface.

Every card should answer a single question.

Examples:

Passport Card

"Is my passport valid?"

Loan Card

"When is my next EMI?"

Resume Card

"When was it last updated?"

Cards should be concise.

---

# AI Experience

AI is an assistant.

Not the application.

The AI interface should feel secondary.

The Home Screen should never be replaced by chat.

AI responses should:

* Reference real Objects
* Explain reasoning
* Suggest actions
* Never invent facts

---

# Search

Search is one of the primary features.

Search should feel instant.

Support:

* Objects
* Documents
* Events
* Tasks
* Metadata

Results should highlight why they matched.

---

# Motion

Animations should be subtle.

Examples:

* Fade
* Scale
* Slide

Avoid:

* Long animations
* Bouncy effects
* Decorative transitions

Motion should support understanding, not entertainment.

---

# Empty States

Every empty state should help the user move forward.

Examples:

No Passport

↓

"Upload your passport to begin."

No Tasks

↓

"You're all caught up."

Avoid empty screens.

---

# Icons

Use a single icon family throughout the application.

Icons should:

* Be simple
* Consistent
* Easily recognisable

Do not mix icon styles.

---

# Illustrations

Avoid large illustrations.

When required:

* Use clean line illustrations.
* Keep them monochromatic.
* Avoid playful or childish artwork.

---

# Accessibility

The application should support:

* Large font sizes
* Screen readers
* High contrast
* Large touch targets

Accessibility should be considered during initial implementation rather than added later.

---

# Responsiveness

The layout should adapt gracefully to:

* Small phones
* Large phones
* Tablets (future)

Avoid fixed dimensions whenever possible.

---

# Performance

The interface should feel immediate.

Target:

* Fast startup
* Smooth scrolling
* Responsive search
* Instant navigation

Visual polish should never compromise responsiveness.

---

# Design Consistency

Every screen should appear to belong to the same product.

If two screens require different interaction patterns, there should be a strong usability reason.

Consistency should outweigh novelty.

---

# Success Criteria

A successful interface should make users feel:

* Organised
* In control
* Confident
* Calm

Users should spend their time managing life, not learning the interface.

---

# Final Principle

Whenever multiple valid UI solutions exist:

Choose the one that reduces cognitive load.

LifePilot should disappear into the background while quietly helping users manage their administrative life.

Beautiful software is software that users do not have to think about.
