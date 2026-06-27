# ADR-006: kotlinx.serialization.json in Domain Use Cases

## Status

Accepted

## Date

2026-06-27

## Context

`ImportDataUseCase` in the domain module parses JSON export payloads. An initial implementation used `org.json.JSONObject` from the Android SDK. This caused unit tests to fail with `ClassNotFoundException` because `org.json` classes are not available in JVM unit test environments (only in instrumented tests).

## Decision

Use `kotlinx.serialization.json` (`Json`, `jsonObject`, `jsonPrimitive`, etc.) in domain use cases that need JSON parsing. This library is a Kotlin multiplatform library and runs correctly in JVM unit tests.

The dependency was already present in the domain module's build.gradle for schema deserialization, so no new dependency was required.

## Consequences

- Domain-layer use cases remain fully testable without an Android device or emulator.
- `org.json.JSONObject` must not be introduced in the `domain` or any module that is expected to have JVM unit tests.
- `data`-layer classes (e.g., AI providers) may continue to use `org.json` since they are Android-only runtime code and are tested at the integration level.
