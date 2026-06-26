# ADR-001: Configuration-Driven Schema Engine

## Status
Accepted

## Date
2026-06-27

## Context

LifePilot must support diverse object types (Passport, Job, Property, Vehicle, Insurance, Education, Health, etc.) without hardcoding forms, validation rules, metadata fields, or reminder logic in application code.

Requirements:
- New object types should be addable without code changes
- Each object type has unique metadata fields, lifecycle states, reminder rules, and AI extraction hints
- The system must validate user input per-field per-object-type
- AI extraction must know which fields to extract per document type

## Decision

Implement a **Schema Engine** that loads JSON schema files from the app's `assets/schemas/` directory at runtime.

Each schema file defines:
- `objectType` — unique identifier
- `domain` — logical grouping (Identity, Finance, Property, etc.)
- `fields` — typed metadata fields with validation rules, enums, AI extractability flags
- `lifecycle` — states, transitions, initial state
- `reminderRules` — field-triggered reminder definitions with priority and offset
- `searchConfig` — primary, full-text, and filterable fields
- `aiConfig` — extractable fields and classification hints

The `SchemaEngine` interface exposes:
- `registeredSchemas: StateFlow<Map<String, ObjectSchema>>` — reactive schema registry
- `getSchema(objectType)` — synchronous schema lookup
- `getAllObjectTypes()` — list of registered types
- `validateMetadataValue(objectType, fieldId, value)` — schema-based validation

Schemas are loaded once at startup by `AppInitializer`.

## Consequences

**Positive:**
- New object types (e.g. `investment`, `subscription`) can be added as JSON with zero code changes
- Domain packs can be distributed as JSON bundles in future releases
- Validation, reminders, and AI extraction are all driven from the same source of truth
- Simpler unit testing — validate schemas independently from Android

**Negative:**
- JSON schema loading has startup latency (mitigated by background loading in AppInitializer)
- Schema validation errors surface at runtime rather than compile-time
- Schema files must be carefully versioned to support migrations

## Alternatives Considered

1. **Kotlin DSL schemas** — type-safe but requires app rebuild for any new type
2. **Hardcoded forms per object type** — fast to prototype, impossible to scale
3. **Remote schema server** — violates offline-first principle
