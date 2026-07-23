package com.space.antivirus.core.model

/**
 * Identifies which analyzer produced a Detection — the provenance this
 * project needs so a future signature engine, heuristic engine, AI
 * engine, or cloud-reputation engine can all contribute findings without
 * their evidence becoming indistinguishable. A value class, not a raw
 * String, so "which analyzer" is a real type at every call site rather
 * than an easily-mistyped string parameter.
 */
@JvmInline
value class AnalyzerId(val value: String) {
    init {
        require(value.isNotBlank()) { "AnalyzerId cannot be blank" }
    }
}
