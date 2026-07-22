# ADR 0008: EncryptedSharedPreferences reserved for genuinely sensitive values only

**Status:** Accepted

## Context
Sprint 002.75 §16 promises specific privacy behavior. Using encrypted storage for every setting would be both unnecessary overhead and would blur the line between 'routine setting' and 'sensitive value' that the Trust Framework (Sprint 002.75 §18) depends on being clear.

## Decision
Routine settings (notification prefs, analytics opt-in, language) live in DataStore via core:data. Only genuinely sensitive values (e.g. a future subscription-entitlement cache, trusted-list tokens) use core:security's SecurePreferences.

## Consequences
Two storage mechanisms to maintain instead of one — accepted because conflating them would either weaken protection for sensitive values or add unnecessary crypto overhead to simple toggles. The specific list of what qualifies as 'sensitive' should be revisited each time a new persisted value is introduced in Sprint 004+.
