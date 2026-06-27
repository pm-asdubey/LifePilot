# ADR-004: Encrypted Credential Storage Using Android Keystore

## Status

Accepted

## Date

2026-06-27

## Context

LifePilot requires users to supply an AI provider API key (e.g., NVIDIA NIM, Anthropic) at runtime via Settings. This key is a sensitive credential and must not be stored in plaintext on device.

`DataStore<Preferences>` (the primary preference store) writes values to a plaintext protobuf file on the device's internal storage. While internal storage is not directly accessible to other apps on non-rooted devices, the file is still technically readable by root or adb on debug builds. An API key in plaintext fails compliance with OWASP MASVS-STORAGE-1.

## Decision

AI API keys are encrypted using AES-256-GCM via the Android Keystore System before storage.

Architecture:

1. `EncryptedKeyStorage` (singleton in `:data`) wraps a Keystore-backed `SecretKey` with a custom `SharedPreferences` store.
2. Each value is encrypted with a fresh random IV; IV + ciphertext are concatenated, Base64-encoded, and stored in `SharedPreferences`.
3. `PreferenceManager` delegates `aiApiKey` reads and writes to `EncryptedKeyStorage`; the decrypted value is propagated via a `MutableStateFlow` for reactive UI observation.
4. All other non-sensitive preferences remain in `DataStore<Preferences>`.

## Consequences

**Positive:**
- API key is never stored in plaintext on-device.
- Android Keystore hardware-backed keys (where available) are protected by the secure enclave.
- Existing callers continue to use `PreferenceManager` without awareness of encryption details.

**Negative:**
- `EncryptedKeyStorage` depends on `SharedPreferences` for persistence. While functional, a future migration could replace this with `EncryptedSharedPreferences` from Jetpack Security for more complete coverage.
- Key loss (factory reset, keystore wipe) will require the user to re-enter the API key. This is acceptable behavior.

## Alternatives Considered

- **Jetpack Security `EncryptedSharedPreferences`**: Wraps `SharedPreferences` with a master key in the Keystore. Would achieve the same result but adds a dependency. Manual Keystore usage was chosen for more explicit control.
- **Leave key in DataStore**: Unacceptable; violates security requirements.
- **Network-side key management**: Out of scope for Version 1 (offline-first, no backend).
