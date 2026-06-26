# OCR Specification

**Location:** `docs/03-ai/OCR-Specification.md`

---

# Purpose

The OCR (Optical Character Recognition) subsystem converts scanned documents and images into structured textual information.

OCR is responsible only for extracting text and document layout.

It is **not** responsible for:

* Document classification
* Metadata extraction
* AI reasoning
* Relationship inference

These responsibilities belong to later stages of the Ingestion Pipeline.

---

# Design Principles

The OCR subsystem must be:

* Offline-first where possible
* Modular
* Replaceable
* Deterministic
* Observable
* Testable

OCR engines should be interchangeable without affecting higher layers.

---

# Responsibilities

The OCR subsystem performs:

* Text extraction
* Layout detection
* Page detection
* Language detection
* Confidence scoring
* Table extraction (when supported)
* Bounding box extraction

It does not modify the extracted content.

---

# Supported Inputs

Version 1 supports:

* Camera captures
* PDF files
* JPEG
* PNG
* WEBP

Future versions may support:

* HEIC
* TIFF
* Multi-page scans
* Handwritten notes

---

# Processing Pipeline

```text
Document

↓

Pre-processing

↓

OCR Engine

↓

Post-processing

↓

Structured OCR Output

↓

Metadata Extraction
```

Every uploaded document follows this pipeline.

---

# Image Pre-processing

Before OCR, optional enhancements may be applied:

* Rotation correction
* Perspective correction
* Deskewing
* Noise reduction
* Contrast enhancement
* Shadow removal
* Cropping

The original image must never be modified.

All processing occurs on temporary working copies.

---

# OCR Output

Every OCR operation produces a structured result.

The result contains:

* Raw text
* Pages
* Paragraphs
* Lines
* Words
* Confidence scores
* Bounding boxes
* Detected language

---

# Output Format

The OCR subsystem exposes a provider-independent model.

Example:

```text
DocumentOCR

├── Pages
│
├── Paragraphs
│
├── Lines
│
├── Words
│
├── Tables
│
├── Language
│
└── Confidence
```

Higher layers must never depend on a specific OCR SDK.

---

# Confidence

Confidence should be available at multiple levels:

* Document
* Page
* Paragraph
* Line
* Word

Low-confidence regions should be highlighted during metadata verification where practical.

---

# Multi-page Documents

Each page is processed independently.

The combined result preserves:

* Page order
* Coordinates
* Layout

Metadata extraction receives the complete document.

---

# Table Extraction

When supported, tables should be represented structurally rather than flattened into plain text.

Example:

```text
Table

↓

Rows

↓

Cells

↓

Coordinates
```

This is important for:

* Bank statements
* Payslips
* Insurance schedules
* Tax documents

---

# Language Detection

The OCR subsystem should detect the primary language automatically.

Version 1 should prioritize:

* English

Future versions may include:

* Hindi
* Other Indian languages
* Multilingual documents

The detected language becomes metadata for downstream processing.

---

# Provider Abstraction

OCR providers are accessed through a common interface.

Example responsibilities:

* Process document
* Return OCR model
* Report confidence
* Report capabilities

The rest of the application should never reference provider-specific APIs.

---

# OCR Providers

Version 1 should support a single provider initially.

The architecture should allow future providers such as:

* Google ML Kit
* Google Document AI
* Azure AI Vision
* AWS Textract
* Tesseract
* PaddleOCR

Switching providers should not affect the Ingestion Pipeline.

---

# Error Handling

Possible failures:

* Unsupported format
* Corrupt file
* Low-quality image
* OCR timeout
* Unsupported language

Failures should return structured errors.

The original document must always remain available for retry.

---

# Performance Targets

Single-page image:

< 2 seconds

Five-page PDF:

< 8 seconds

Large documents should process in the background.

The UI must remain responsive.

---

# Storage

OCR output is stored separately from:

* Original document
* Metadata
* AI summaries

Suggested structure:

```text
OCR/

document-id/

raw.json

cleaned.json

layout.json
```

This allows OCR to be regenerated without affecting user data.

---

# Privacy

OCR processing should occur on-device whenever possible.

If cloud OCR is introduced in the future:

* The user should opt in.
* Only the minimum required data should be transmitted.
* The provider should not retain document contents where configurable.

Privacy requirements apply equally to all providers.

---

# Testing

OCR must be tested using a representative corpus including:

* Clear scans
* Low-resolution images
* Rotated documents
* Multi-page PDFs
* Tables
* Documents with stamps and signatures
* Mixed-language samples (future)

Accuracy should be measured independently from metadata extraction.

---

# Metrics

Track:

* OCR success rate
* Average confidence
* Processing latency
* Retry rate
* User correction frequency
* Failed documents by type

Metrics should guide improvements over time.

---

# Future Compatibility

The OCR subsystem should support future enhancements such as:

* Handwriting recognition
* Checkbox detection
* Signature detection
* QR code extraction
* Barcode extraction
* Form field detection

These capabilities should extend the OCR model without changing downstream interfaces.

---

# Design Rules

The OCR subsystem must:

* Preserve original content.
* Never invent text.
* Be provider-independent.
* Expose confidence.
* Remain replaceable.
* Produce deterministic outputs for the same input.

---

# Summary

The OCR subsystem is the first stage of the Ingestion Pipeline.

Its responsibility is to faithfully extract textual and structural information from documents.

By separating OCR from classification, metadata extraction and AI reasoning, LifePilot maintains a modular architecture where each subsystem has a single, well-defined responsibility.
