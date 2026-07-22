plugins {
    id("spaceav.jvm.library")
}

// BUGFIX (Sprint 003.5 recovery): same issue and same fix as core:common —
// see docs/adr/0011-core-common-and-core-model-are-pure-kotlin.md.
// Deliberately empty of business entities per Sprint 003 Task 11 / the
// "no business entities yet" rule. Entities land here in Sprint 004.
