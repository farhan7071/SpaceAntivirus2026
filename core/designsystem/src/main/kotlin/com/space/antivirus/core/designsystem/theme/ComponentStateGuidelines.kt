package com.space.antivirus.core.designsystem.theme

/**
 * Space Design System v1.0 — Component State Guidelines.
 *
 * This is guidance for Phase 2 (actual component implementation) to
 * follow, not a redesign of any existing component — Phase 1's own
 * scope is the design system itself, not screens or the components
 * that will consume it. Nothing in this file is wired into AppButton,
 * AppCard, StatusChip, or any other existing component yet.
 *
 * FOUR STATES EVERY INTERACTIVE COMPONENT SHOULD REASON ABOUT:
 *
 * 1. Default — idle, ready for interaction. No special handling; this
 *    is simply the component's own normal appearance.
 *
 * 2. Disabled — not interactive right now. Delegate to Material3's own
 *    built-in disabled handling (`enabled = false` on Button/
 *    OutlinedButton/TextButton/Card, etc.) rather than inventing a
 *    custom opacity or color — M3 already applies its own, accessible,
 *    theme-aware disabled-content alpha (`ButtonDefaults`,
 *    `CardDefaults`, and friends compute this internally from
 *    `MaterialTheme.colorScheme`). AppButton's three variants already
 *    do this correctly (each forwards its own `enabled` parameter
 *    straight to the underlying M3 component) — this is the existing,
 *    correct pattern to keep following, not a change.
 *
 * 3. Loading — an async operation this component triggered or
 *    represents is in progress. Not the same as Disabled: a loading
 *    button should visually communicate "working," not just "unusable"
 *    — M3 has no built-in concept of this (it's product-specific
 *    behavior, not an interaction-source state), which is why it's the
 *    first of the two states this design system actually names below
 *    (`ComponentState.Loading`). Guidance for a future component
 *    implementation: replace the component's primary content with a
 *    same-sized progress indicator (AppCircularProgress, already
 *    exists) rather than shrinking the component or leaving its label
 *    in place alongside a spinner — the goal is "this is doing
 *    something," communicated at a glance, not a cluttered composite.
 *    A loading component should also be treated as non-interactive
 *    while loading (same visual/interaction discipline as Disabled),
 *    to prevent duplicate triggering.
 *
 * 4. Error — something about this component's own content or the last
 *    action taken on it is invalid or failed. Also not natively modeled
 *    by M3's interaction-state system. Guidance: use the semantic
 *    High Risk/Attention color tokens (SeverityColors.ActionNeeded*
 *    or SeverityColors.Attention*, matching how severe the error is),
 *    never an arbitrary red — this keeps error presentation consistent
 *    with how the rest of the app already communicates severity, rather
 *    than introducing a second, parallel "error red" that means
 *    something different from the app's own High Risk red.
 *
 * WHAT THIS FILE DELIBERATELY DOES NOT DEFINE:
 *
 * Pressed, Hovered, and Focused are not modeled here as named states —
 * Compose's own `InteractionSource`/`Indication` system (ripple,
 * press-scale, focus rings) already handles these correctly and
 * automatically for every M3 component this project's own components
 * are built on top of (Button, Card, OutlinedButton...). Naming a
 * parallel `ComponentState.Pressed` would either duplicate what Compose
 * already does for free, or — worse — invite a future component to
 * hand-roll its own press/focus visuals instead of using the platform's
 * own, already-accessible mechanism. Selected is intentionally also not
 * defined generically here: "selected" means something different for a
 * badge (Sprint 034's ScanResultBadge/StatusChip are display-only, never
 * selectable) than for a future filter chip (Part 7's own "filter by"
 * groundwork) — that's a per-component decision Phase 2 should make
 * with real context, not a token this file can usefully generalize.
 */
enum class ComponentState {
    Loading,
    Error,
}
