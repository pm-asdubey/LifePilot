# Reminder & Rule Engine

**Location:** `docs/04-architecture/Reminder-Rule-Engine.md`

---

# Purpose

The Reminder & Rule Engine continuously evaluates the user's Life State and determines whether any action, reminder, warning or recommendation should be generated.

Unlike a traditional reminder application, reminders are primarily derived from structured data rather than manually created by the user.

The engine is responsible for answering:

* What requires attention?
* What is due soon?
* What changed?
* What should happen next?

---

# Philosophy

Users should not have to remember administrative deadlines.

The application should monitor their life on their behalf.

Whenever possible, reminders should be generated automatically from metadata and events.

Manual reminders remain supported but are secondary.

---

# Rule Engine

Every Object Type defines a collection of Rules.

Example

Passport

Rules

* Renewal Reminder
* Expiry Reminder

Vehicle

Rules

* Insurance Renewal
* Service Reminder
* PUC Reminder

Loan

Rules

* EMI Reminder
* Closure Reminder

Resume

Rules

* Review every 6 months

The Rule Engine evaluates these continuously.

---

# Rule Structure

Each Rule contains:

* Rule ID
* Name
* Object Type
* Trigger Condition
* Priority
* Generated Action
* Repeat Policy
* Enabled

Rules are configuration-driven.

No rule should require hardcoded application logic.

---

# Rule Types

## Date-Based

Examples

* Passport expires in 12 months
* Insurance renews next week
* Loan EMI due tomorrow

---

## State-Based

Examples

Job Status

↓

Ended

↓

Generate Resume Tasks

Vehicle

↓

Insurance Missing

↓

Warning

---

## Relationship-Based

Examples

Passport

↓

Renewed

↓

Visa linked

↓

Recommend Visa update

Property

↓

Sold

↓

Linked Loan still active

↓

Generate Review Task

---

## Metadata-Based

Example

Insurance Coverage

↓

Less than Vehicle Value

↓

Recommendation

---

## Manual Rules

Users may create reminders manually.

Examples

* Renew gym membership
* Review investments
* Call insurance agent

Manual reminders behave like system-generated reminders.

---

# Evaluation Pipeline

```text
Life State Updated

↓

Identify Changed Objects

↓

Load Applicable Rules

↓

Evaluate Conditions

↓

Generate Results

↓

Update Dashboard

↓

Schedule Notifications
```

Only affected Objects are evaluated.

The engine should never scan the entire database after every change.

---

# Trigger Sources

Rules are evaluated when:

* Object created
* Object updated
* Metadata changed
* Event added
* Task completed
* Reminder dismissed
* Daily background refresh

---

# Reminder Lifecycle

Every Reminder follows this lifecycle.

```text
Generated

↓

Scheduled

↓

Active

↓

Dismissed

↓

Completed

↓

Archived
```

Completed reminders remain visible in history.

---

# Reminder Priority

Four priority levels exist.

Critical

Examples

* Passport expired
* Loan payment overdue

High

Examples

* Insurance renewal
* Visa expiry

Medium

Examples

* Resume review
* Vehicle service

Low

Examples

* Annual document cleanup

Priority determines dashboard ordering and notification behavior.

---

# Repeat Policies

Rules define repetition.

Supported policies:

* Never
* Daily
* Weekly
* Monthly
* Yearly
* Custom Interval

Example

Vehicle Service

Repeat every

12 months

---

# Smart Dismissal

Dismissing a reminder should not always delete it.

Examples

Passport expires

Dismiss

↓

Show again in

30 days

Insurance renewal

Dismiss

↓

Show again

7 days later

Each rule defines dismissal behavior.

---

# Dashboard Integration

Generated reminders automatically appear in:

* Attention Required
* Upcoming Events
* Today's Tasks

The Home Dashboard simply renders Reminder Engine output.

---

# Notification Integration

The Notification System subscribes to Reminder Engine output.

Reminder Engine

↓

Notification Scheduler

↓

Android Notification

The Rule Engine never displays notifications directly.

---

# AI Integration

AI may recommend additional reminders.

Examples

"You changed jobs but haven't updated your resume."

"Your passport expires before your planned trip."

These remain suggestions until accepted.

Accepted suggestions become normal reminders.

---

# Background Processing

The Rule Engine runs:

* Immediately after relevant changes
* Daily scheduled refresh
* On application startup (lightweight validation)

Long-running evaluations should execute in the background.

---

# Performance

Rule evaluation should be incremental.

Target times:

Single Object Update

<50 ms

Daily Evaluation

<500 ms

Dashboard Refresh

<100 ms

Performance must scale with increasing numbers of Objects.

---

# Extensibility

Each Domain Pack contributes:

* Rules
* Reminder Templates
* Priorities
* Suggested Tasks

The engine itself remains unchanged.

Examples

Tax Pack

* Income Tax Due
* GST Filing
* TDS Review

Healthcare Pack

* Annual Checkup
* Vaccination
* Prescription Renewal

Immigration Pack

* Visa Renewal
* Residency Expiry

---

# Design Principles

The Reminder & Rule Engine must be:

* Predictable
* Explainable
* Incremental
* Configuration-driven
* Extensible
* Offline-first

Every generated reminder should have a traceable reason.

Users should always be able to answer:

"Why am I seeing this reminder?"

---

# Summary

The Reminder & Rule Engine transforms passive stored information into proactive guidance.

Rather than asking users to remember deadlines, the engine continuously evaluates their Life State and surfaces the right action at the right time.

It is the system that makes LifePilot feel like an intelligent assistant instead of a static document manager.
