# ADR 0006: Detekt + ktlint applied at the root, not per-module

**Status:** Accepted

## Context
Task 14 required static analysis, formatting, and lint across ~20 modules. Applying and configuring these per-module would mean 20 near-identical config blocks to keep in sync.

## Decision
Apply both plugins in the root build.gradle.kts's subprojects{} block, pointing at one shared config/detekt/detekt.yml and one root .editorconfig for ktlint.

## Consequences
A rule change is a one-file edit, not a 20-module edit. Tradeoff: a module can't easily opt out of a rule without a root-config exception list — acceptable, since inconsistent enforcement was exactly the risk this centralization avoids.
