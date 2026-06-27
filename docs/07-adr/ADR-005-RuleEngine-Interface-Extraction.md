# ADR-005: RuleEngine Domain Interface

## Status

Accepted

## Date

2026-06-27

## Context

`RuleEngineImpl` existed as a concrete singleton in the `data` module with no corresponding domain interface. Two callers — `LifeStateEngineImpl` and `ReminderEvaluationWorker` — were injecting the concrete class directly, violating the layer boundary constraint:

> Repositories hide implementation details. Repositories must never expose SQL, Room entities, file paths, OCR provider APIs, or AI provider APIs.

The same principle applies to engines: a concrete engine class leaked through the domain→data boundary.

## Decision

Extract a `RuleEngine` interface in the domain module containing a single method:

```kotlin
interface RuleEngine {
    suspend fun evaluateRemindersForObject(objectId: String)
}
```

`RuleEngineImpl` in the `data` module implements this interface. `RepositoryModule` binds it as a singleton. Both `LifeStateEngineImpl` and `ReminderEvaluationWorker` now depend on the interface.

## Consequences

- Layer boundaries are respected: domain-layer consumers depend on a domain interface.
- `RuleEngine` can be replaced with an alternative implementation (e.g., a no-op for testing, or a future rule-as-config engine) without modifying callers.
- Tests can mock `RuleEngine` without MockK reflection issues against a concrete class.
