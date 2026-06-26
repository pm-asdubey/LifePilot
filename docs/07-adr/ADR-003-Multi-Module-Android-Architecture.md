# ADR-003: Multi-Module Android Architecture

## Status
Accepted

## Date
2026-06-27

## Context

LifePilot is a complex application covering many life domains. A single-module Android project would result in:
- Long compile times as the codebase grows
- Poor separation of concerns
- Difficult parallel development
- Risk of UI layers importing business logic directly

## Decision

Adopt a **multi-module architecture** with strict layer boundaries:

```
:app                          ← Android entry point, DI wiring, navigation
:domain                       ← Pure Kotlin; models, repository interfaces, use cases, engine interfaces
:data                         ← Room, repositories, schema engine, workers
:designsystem                 ← Compose theme, components, tokens
:core:common                  ← AppResult, shared utilities
:features:home                ← Home dashboard feature
:features:library             ← Object library feature
:features:search              ← Search feature
:features:object              ← Object detail + creation + metadata editing
:features:document            ← Document viewer + upload
:features:settings            ← Profile management + AI config
:features:timeline            ← Timeline feature
:features:ai                  ← AI chat feature
```

**Dependency rules:**
- `:domain` has no Android dependencies
- `:data` depends on `:domain` only
- `:designsystem` depends on nothing
- Feature modules depend on `:domain` + `:designsystem` (and `:features:document`/`:features:object` for cross-feature sheets)
- `:app` depends on all modules to wire DI and navigation

## Consequences

**Positive:**
- Business logic in `:domain` is testable with pure JVM tests (no Android SDK)
- Compile times scale better: Gradle can parallelize independent module builds
- Feature teams can own their module without touching others
- Clean Architecture enforced structurally (cannot import Room from `:domain`)

**Negative:**
- Initial setup overhead (11 modules with individual build files)
- Cross-feature navigation requires extra coordination
- Some modules share cross-cutting code (feature:object exposes CreateObjectSheet to home/library)

## Alternatives Considered

1. **Single module with package structure** — simpler initially, messy at scale
2. **Feature-first layering** — feature modules contain their own data/domain; rejected because it duplicates business logic and prevents the Life State Engine from having a unified view
