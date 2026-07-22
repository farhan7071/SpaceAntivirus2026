# ADR 0010: ProtectionService foreground-service-type decision deferred to feature:realtime implementation

**Status:** Proposed

## Context
Sprint 001 Risk #2 and Sprint 002 §9 both flagged that real-time protection's foreground service needs a specific, justified foregroundServiceType (most likely specialUse, which requires a written Play Console rationale) before it can ship on target SDK 34+.

## Decision
Sprint 003 deliberately does NOT declare any <service> for real-time protection in the manifest (see AndroidManifest.xml comment) — declaring one now, without the real implementation or the Play Console justification text ready, would be exactly the kind of placeholder-that-needs-rework Sprint 003's strict rules prohibit.

## Consequences
feature:realtime ships as an empty placeholder screen in Sprint 003 with no backing service. The actual service declaration, foregroundServiceType, and Play Console specialUse justification are Sprint 004's first task for that feature, not deferred further.
