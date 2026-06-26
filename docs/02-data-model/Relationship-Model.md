# Relationship Model

**Location:** `docs/02-data-model/Relationship-Model.md`

---

# Purpose

LifePilot is not a collection of isolated objects.

Everything in life is connected.

Relationships enable LifePilot to understand these connections and allow AI to reason across a user's entire administrative life.

Every Object may participate in one or more Relationships.

Relationships are directional, typed, and versioned.

---

# Philosophy

Traditional document managers answer:

"Where is this file?"

LifePilot answers:

"What is connected to this?"

Example

Passport

↓

Required For

↓

Visa

↓

Used In

↓

Trip

↓

Destination

↓

Japan

The application understands these relationships rather than simply storing files.

---

# Relationship Types

Relationships are divided into categories.

## Ownership

Examples

Profile

↓

Owns

↓

Passport

Profile

↓

Owns

↓

Vehicle

Profile

↓

Owns

↓

Property

---

## Dependency

Examples

Visa

↓

Depends On

↓

Passport

Loan

↓

Depends On

↓

Property

Insurance

↓

Depends On

↓

Vehicle

---

## Financial

Examples

Employer

↓

Pays

↓

Salary

Salary

↓

Deposited Into

↓

Bank Account

Loan

↓

Paid From

↓

Bank Account

Investment

↓

Purchased Through

↓

Broker

---

## Administrative

Examples

Passport

↓

Supports

↓

Visa

Degree

↓

Supports

↓

Job Application

Resume

↓

References

↓

Degree

---

## Family

Examples

Parent

↓

Guardian Of

↓

Child

Health Insurance

↓

Covers

↓

Parent

Nominee

↓

Linked To

↓

Investment

---

## Geographic

Examples

Property

↓

Located At

↓

Address

Visa

↓

Issued At

↓

Embassy

Job

↓

Located In

↓

City

---

## Temporal

Examples

Interview

↓

Leads To

↓

Offer

Offer

↓

Leads To

↓

Employment

Employment

↓

Ends With

↓

Resignation

---

# Relationship Structure

Each relationship contains:

```text
Relationship ID

Source Object

Target Object

Relationship Type

Created At

Updated At

Created By

Confidence

Status
```

---

# Cardinality

The engine supports:

One → One

Example

Passport

↓

Belongs To

↓

Profile

---

One → Many

Example

Property

↓

Contains

↓

Documents

---

Many → One

Example

Insurance Policies

↓

Cover

↓

Vehicle

---

Many → Many

Example

Person

↓

Owns

↓

Multiple Properties

↓

Joint Ownership

---

# Inverse Relationships

Every relationship automatically generates an inverse.

Example

Passport

↓

Supports

↓

Visa

Automatically creates

Visa

↓

Requires

↓

Passport

This avoids duplicate storage while improving navigation.

---

# Relationship Graph

The application internally maintains a graph.

Example

Ash

↓

Owns

↓

Passport

↓

Supports

↓

Visa

↓

Used For

↓

Japan Trip

↓

Contains

↓

Tickets

AI can traverse this graph to answer questions.

---

# Relationship Traversal

Traversal depth is configurable.

Default AI traversal:

Depth 2

Examples

"What documents relate to my Japan trip?"

Trip

↓

Visa

↓

Passport

↓

Travel Insurance

↓

Flight Ticket

↓

Hotel Booking

AI retrieves all connected Objects.

---

# Relationship Lifecycle

Relationships have their own lifecycle.

States:

* Active
* Pending
* Historical
* Archived
* Deleted

Example

Job

↓

Pays

↓

Salary

↓

Active

When employment ends

Relationship becomes Historical.

---

# Derived Relationships

Some relationships are not manually created.

Examples

Passport uploaded

↓

Visa uploaded

↓

System detects shared passport number

↓

Relationship proposed

↓

User confirms

↓

Relationship stored

---

Property registry uploaded

↓

Home loan uploaded

↓

Matching address

↓

Relationship suggested

---

# AI Generated Relationships

AI may recommend relationships.

Examples

Resume mentions

Google

Offer Letter mentions

Google

↓

Suggest

Resume

↓

References

↓

Offer

These remain suggestions until user approval.

---

# Relationship Rules

Relationships must satisfy:

* Source must exist.
* Target must exist.
* Relationship Type must be valid.
* Circular references are allowed only when explicitly supported.
* Deleted Objects invalidate active relationships.
* Historical relationships are never removed automatically.

---

# Search Integration

Search should index:

* Object names
* Relationship types
* Connected Objects
* Connected Documents
* Connected Events

Searching "Google" should return:

* Job
* Resume
* Interview
* Offer Letter
* Salary
* Experience Letter

through relationship traversal.

---

# Dashboard Integration

The dashboard uses relationships to generate insights.

Examples

Passport expires soon

↓

Connected Visa also affected

Vehicle insurance expires

↓

Vehicle attention card generated

Job ended

↓

Resume update suggested

↓

LinkedIn update suggested

↓

Insurance review suggested

---

# Future Compatibility

The relationship engine is intentionally generic.

Future modules such as:

* Tax
* Immigration
* Estate Planning
* Healthcare
* Family Sharing

should integrate by defining new relationship types rather than modifying the engine.

---

# Summary

Relationships transform LifePilot from a document manager into a Life Knowledge Graph.

Every Object exists within a connected network.

The Relationship Model allows AI to reason across this network, enabling richer search, proactive recommendations, and context-aware answers without requiring the user to manually organize or link every piece of information.
