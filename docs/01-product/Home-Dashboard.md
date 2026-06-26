# Home Dashboard

**Location:** `docs/01-product/Home-Dashboard.md`

---

# Purpose

The Home Dashboard is the primary screen of LifePilot.

It should answer one question:

> **"What requires my attention right now?"**

The dashboard is not a navigation screen.

It is an operational control center.

Every card displayed on the Home Dashboard is derived from the current Life State.

The dashboard stores no independent state.

---

# Design Philosophy

The dashboard should feel calm.

It should never overwhelm the user.

Only actionable information belongs here.

Everything displayed should satisfy at least one of these conditions:

* Requires attention
* Recently changed
* Needs action
* Provides useful insight

If something is not actionable or informative, it should not appear.

---

# Information Hierarchy

The Home Dashboard contains six primary sections.

1. Daily Summary
2. Attention Required
3. Today's Tasks
4. Upcoming Events
5. Recent Activity
6. AI Insights

Sections may be hidden automatically when empty.

---

# Section 1 — Daily Summary

Purpose:

Provide a quick overview of the user's administrative life.

Examples:

* Active Objects
* Open Tasks
* Upcoming Expiries
* Recent Changes

Example

```text
12 Active Identity Documents

4 Pending Tasks

2 Upcoming Renewals

1 Recent Job Update
```

The summary should never exceed one screen.

---

# Section 2 — Attention Required

This is the highest priority section.

Only items requiring immediate user action appear here.

Examples

* Passport expires in 90 days
* Insurance premium overdue
* Loan EMI due tomorrow
* OCR verification pending

Each card contains:

* Title
* Description
* Priority
* Recommended Action

Cards should be ordered by urgency.

---

# Section 3 — Today's Tasks

Displays all tasks due today.

Each task includes:

* Title
* Related Object
* Due Time (if applicable)
* Status
* Quick Complete Action

Tasks should be grouped by priority.

Completing a task should immediately update the dashboard.

---

# Section 4 — Upcoming Events

Displays upcoming milestones.

Examples

* FD Maturity
* Visa Expiry
* Insurance Renewal
* Property Tax Due
* Appointment

Time windows

* Today
* This Week
* This Month

Users can expand for additional detail.

---

# Section 5 — Recent Activity

Displays recently updated Objects.

Examples

* Passport uploaded
* Resume updated
* Interview added
* Loan closed

Each activity links directly to the originating Object.

---

# Section 6 — AI Insights

AI generates observations based on the Life State.

Examples

* Your passport expires before your planned trip.
* Your vehicle insurance has not been uploaded.
* You changed jobs but have not updated your resume.
* Two investments mature within the next month.

AI Insights are suggestions.

They never modify data automatically.

---

# Dashboard Cards

Every dashboard card follows the same structure.

Fields

* Title
* Subtitle
* Icon
* Priority
* Related Object
* Timestamp
* Primary Action
* Secondary Action

Consistency across cards is mandatory.

---

# Priority Levels

Dashboard items use four priority levels.

Critical

Requires immediate attention.

Examples

* Expired passport
* Missed EMI

High

Should be handled soon.

Examples

* Renewal due
* Verification pending

Medium

Routine administrative work.

Examples

* Resume update
* Insurance upload

Low

Informational.

Examples

* Recently added document

Priority determines ordering throughout the dashboard.

---

# Personalization

The dashboard adapts to the user's current Life State.

Examples

A student may see:

* Exams
* Certificates
* Applications

A working professional may see:

* Salary
* Resume
* Interviews

A retiree may see:

* Investments
* Medical Reports
* Insurance

No separate dashboard templates are required.

The Life State Engine determines relevance.

---

# Empty Dashboard

When no action is required, the dashboard should reassure the user.

Example

"Everything looks up to date."

Optional suggestions:

* Review documents
* Add a new Object
* Explore AI

Avoid creating unnecessary work.

---

# Refresh Strategy

The dashboard refreshes when:

* Object updated
* Task completed
* Reminder triggered
* Document uploaded
* Event added
* Profile switched

The refresh should be incremental.

Only affected sections should update.

---

# Performance Targets

Dashboard load:

< 300 ms

Incremental refresh:

< 100 ms

Scrolling:

60 FPS

The dashboard should feel instantaneous.

---

# Future Widgets

The architecture should allow additional cards without redesign.

Potential future widgets:

* Weather for upcoming trips
* Tax season reminders
* Immigration deadlines
* Health summaries
* Investment performance
* Family activity

Widgets should register themselves with the dashboard.

The dashboard should not hardcode widget implementations.

---

# Dashboard Rules

The Home Dashboard must never:

* Display duplicate information
* Require excessive scrolling
* Become a notification feed
* Show irrelevant AI suggestions
* Hide critical actions

The dashboard should always answer:

* What needs attention?
* What should I do next?
* What changed?
* What is coming up?

---

# Summary

The Home Dashboard is the operational center of LifePilot.

It transforms the current Life State into a focused, actionable overview.

Rather than acting as a launch screen, it should continuously guide the user toward maintaining an organized and up-to-date administrative life with the least possible effort.
