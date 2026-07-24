# Security Messaging Style Guide

**Provenance, stated honestly:** no Sprint 002.75 source document (Vocabulary Dictionary, Security Messaging Guide) has ever been committed to this repository. Every reference to "Sprint 002.75 §N" scattered across this codebase's comments (30+ files, sections §4 through §21) is a citation to a document that exists only outside version control, if at all. This file does not reproduce that document — it consolidates the specific, consistently-applied rules that can be reconstructed with confidence from how those citations have actually been used in real, shipped code, so that Sprint 016 (`ThreatDescriptionProvider`) has a real, checkable artifact to write against instead of an unverifiable external reference. Found and flagged before writing any copy, per this sprint's own "stop and report" instruction — treated as a fix to make, not just a gap to note.

**If the real Sprint 002.75 document exists outside this repository, it should supersede this file and this file should be updated to match it exactly.** Until then, this is the closest thing this project has to a checkable source of truth for security messaging.

## Rules extracted from consistent citation across the codebase

**Never exaggerate risk (§17).** A heuristic that can't distinguish a legitimate use case from a malicious one must not use language claiming that certainty. This is why `RiskLevel.ATTENTION` ("worth a look"), not `ACTION_NEEDED`, is used throughout Sprints 014/015's analyzers — and why messaging for `ATTENTION`-tier findings must never use words like "dangerous," "malicious," "virus," or "infected."

**Always show evidence, never a vague label (§17, enforced by `Detection.evidenceDescription` and `Threat`'s own `init` block).** Every user-facing message must be traceable to specific evidence, not a generic "suspicious behavior detected." Messaging must incorporate the actual `Detection.evidenceDescription` text, not replace it with vaguer paraphrasing.

**Severity is a 3-tier scale, not a numeric score (§4).** `RiskLevel` (`INFO` / `ATTENTION` / `ACTION_NEEDED`) is deliberately not a percentage or numeric risk score. Messaging must never imply false precision ("87% malicious") that the underlying heuristics can't support.

**Explain why, and what a user might consider doing — proportionate to confidence.** For `ATTENTION`-tier heuristic findings specifically, suggested actions must be phrased as something to *consider* or *review*, never an imperative demand ("Uninstall this app now"). Certainty the analyzer doesn't have shouldn't be borrowed by the copy layer.

**Positive states get equal care, not just negative ones (§10).** A clean scan result deserves a genuinely reassuring, specific statement — not just the absence of a warning.

**Plain, specific, factual language over marketing or alarm language throughout.** No exclamation points, no all-caps, no "WARNING" banners implied in copy — consistent with every UI-adjacent citation of §4/§5 across this codebase's design-system components.

## Applying this to `ThreatDescriptionProvider`

- **Title:** short (2-5 words), names the category of concern factually, never the verdict ("Unusual permission combination," not "Malware Detected").
- **Description:** three parts, one cohesive string — (1) a plain-language lead-in naming the category of concern, (2) every piece of evidence from every `Detection` passed in, not just ones matching the "driving" `threatType` — a `Threat` combining findings from multiple analyzers must show all of them, per the always-show-evidence rule above, (3) a proportionate closing statement of what a user might consider, phrased as a suggestion for `ATTENTION`-tier findings.
- Every `ThreatType` value must have coverage, including ones no analyzer currently produces (`MALWARE`, `UNKNOWN`) — the provider is a general contract, not scoped to only today's two analyzers.
