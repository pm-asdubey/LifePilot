# Object Definitions

**Location:** `docs/02-data-model/Object-Definitions.md`

---

# Purpose

This document defines every Object Type supported by LifePilot Version 1.

Objects represent real-world entities that users own, manage, or track.

Objects are the primary organizational unit of LifePilot.

Every Object Type specifies:

* Purpose
* Metadata
* Supported Documents
* Supported Events
* Generated Tasks
* Reminder Rules
* Relationships
* Lifecycle

All Object Types inherit the common Object schema defined in `Canonical-Data-Model.md`.

---

# Object Template

Every Object follows this structure.

## Common Properties

* Object ID
* Profile
* Domain
* Object Type
* Title
* Description
* Status
* Created At
* Updated At

---

Every Object additionally defines

* Metadata
* Documents
* Events
* Tasks
* Relationships
* Reminder Rules
* Lifecycle

---

# Identity Domain

---

## Passport

### Purpose

Represents an individual's passport.

---

### Metadata

* Passport Number
* Country
* Nationality
* Holder Name
* Date of Birth
* Gender
* Place of Birth
* Date of Issue
* Date of Expiry
* Issuing Authority

---

### Documents

* Passport Scan
* Renewal Receipt
* Police Verification (optional)

---

### Events

* Issued
* Renewed
* Lost
* Replaced
* Expired

---

### Generated Tasks

* Renew Passport
* Upload New Scan
* Update Linked Visa Applications

---

### Reminder Rules

* 12 months before expiry
* 6 months before expiry
* 3 months before expiry
* Expired

---

### Relationships

Passport

↓

Supports

↓

Visa

Passport

↓

Owned By

↓

Profile

---

### Lifecycle

Draft

↓

Issued

↓

Active

↓

Renewal Due

↓

Expired

↓

Archived

---

## Aadhaar

### Metadata

* Aadhaar Number
* Name
* DOB
* Address
* Mobile
* UIDAI Version

### Events

* Issued
* Updated
* Reprinted

### Reminder

No automatic reminder.

---

## PAN

### Metadata

* PAN Number
* Name
* Category

---

### Events

* Issued
* Updated

---

## Driving Licence

### Metadata

* Licence Number
* Vehicle Classes
* Issue Date
* Expiry Date

---

### Reminder

Licence Renewal

---

# Finance Domain

---

## Bank Account

### Metadata

* Bank
* Branch
* Account Number (masked)
* IFSC
* Account Type
* Open Date

---

### Events

* Opened
* Closed
* Dormant
* Frozen

---

### Relationships

Receives Salary

Pays Loan

Linked Investments

---

## Fixed Deposit

### Metadata

* Bank
* Principal
* Interest Rate
* Start Date
* Maturity Date
* Nominee

---

### Reminder

* 30 Days Before Maturity
* Maturity Date

---

## Credit Card

### Metadata

* Issuer
* Last Four Digits
* Credit Limit
* Billing Date
* Due Date

---

### Reminder

Monthly Due Date

---

## Insurance

Supports

* Health
* Vehicle
* Property
* Life
* Travel

Metadata

* Provider
* Policy Number
* Premium
* Coverage
* Renewal Date

Reminder

Policy Renewal

---

## Loan

Metadata

* Lender
* Loan Type
* Principal
* Interest
* EMI
* Outstanding Balance

Events

* Approved
* Disbursed
* Prepayment
* Closed

---

# Career Domain

---

## Job

Metadata

* Employer
* Designation
* Employment Type
* Salary
* Joining Date
* Leaving Date
* Manager
* Location

Events

* Interview
* Offer Received
* Offer Accepted
* Joined
* Promotion
* Salary Revision
* Team Change
* Resigned
* Left Company

Generated Tasks

* Update Resume
* Review Insurance
* Update LinkedIn

Relationships

Employer

↓

Pays

↓

Salary

---

## Resume

Metadata

* Version
* Last Updated

Documents

* Resume PDF
* Cover Letter

Events

* Updated

---

## Interview

Metadata

* Company
* Position
* Recruiter
* Interview Date
* Result

Events

* Scheduled
* Completed
* Offer
* Rejected

---

# Property Domain

---

## Property

Metadata

* Property Type
* Address
* Purchase Date
* Purchase Value
* Ownership %

Documents

* Registry
* Sale Deed
* Possession Letter
* Tax Receipt

Events

* Purchased
* Registered
* Sold

Relationships

Property

↓

Financed By

↓

Loan

---

## Vehicle

Metadata

* Manufacturer
* Model
* Registration Number
* Purchase Date

Reminder

Insurance

PUC

Service

---

# Education Domain

Objects

* Degree
* Certificate
* Marksheet
* Admission

---

# Health Domain

Objects

* Medical Report
* Vaccination
* Health Insurance
* Prescription

---

# Travel Domain

Objects

* Visa
* OCI
* Travel Booking

---

# Legal Domain

Objects

* Contract
* Court Order
* Power of Attorney
* Will

---

# Object Rules

Every Object must satisfy the following:

* Belongs to one Domain.
* Belongs to one Profile.
* May contain multiple Documents.
* May contain multiple Events.
* May contain multiple Tasks.
* May contain multiple Relationships.
* May generate multiple Reminders.
* Has one Current State.

---

# Extensibility

New Object Types should be added by extending this document.

No Object Type should require changes to the core application architecture.

Each new Object should simply define:

* Metadata
* Documents
* Events
* Lifecycle
* Reminder Rules
* Relationships
* Generated Tasks

The application should automatically support new Object Types through configuration rather than custom code wherever possible.
