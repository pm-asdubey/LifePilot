# LifePilot Privacy Declaration

LifePilot is built on the principle that **your administrative life belongs to
you**. This document explains what data stays on your device, what leaves it,
and why.

---

## What stays on your phone

The following are processed and stored locally only:

- **Original documents and photos** — kept in the app’s private storage.
- **OCR text** extracted from documents — stored locally, never sent unless you
  explicitly choose AI metadata extraction.
- **Structured records, metadata, tasks, reminders, goals, and timeline** —
  stored in the local LifePilot database.
- **Biometric authentication** — handled by Android; no biometric data is
  stored or transmitted by LifePilot.
- **Rule engine evaluations** — reminder and task generation run entirely on
  your device.
- **Encryption keys** — protected by Android Keystore.

---

## What leaves your phone

### AI chat and metadata extraction

When you use the AI assistant or ask it to extract details from a document,
the following are sent to the configured AI provider (NVIDIA NIM or Anthropic
Claude) over HTTPS:

- The **system prompt**, which contains summaries of your records. Structured
  sensitive values (such as passport numbers, Aadhaar, PAN, and account
  numbers) are automatically redacted before the prompt is sent.
- Your **current message** and **recent conversation history** — these are your
  explicit inputs to the model.
- If you use AI metadata extraction, the **OCR text** of the document you are
  extracting from is sent so the AI can suggest fields.

AI suggestions are shown to you for approval before they become part of your
records. The AI never writes data directly.

### App update check

Once per day LifePilot checks GitHub for a newer version. Only the configured
repository slug is sent; no personal data is included.

---

## What we do not do

- We do not operate a backend that receives your data.
- We do not run ads or analytics that send personal information off-device.
- We do not back up your data to the cloud automatically. Backups are created
  only when you explicitly export, and you choose where the file goes.

---

## Known limitations in Version 1.0

- The local database is not yet encrypted at rest (planned).
- Export files are plaintext JSON; keep them in a secure location (planned
  encryption in a future release).

---

## Your controls

- Choose whether to enable the AI assistant and which provider to use.
- Enable biometric lock in Settings.
- Delete your profile and all data at any time from Settings.
