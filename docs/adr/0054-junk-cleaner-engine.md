# ADR 0054: The Junk Cleaner Engine — Real Deletion, and Where It Stops (Sprint 039)

**Status:** Accepted

## Context

Sprint 038 built a Cleaner UI on a domain layer that could only scan.
ADR 0053 documented the omissions that forced: no clean action, no
cancel, no cleaning progress, no bytes freed, no storage overview, no
cleanup history — because none of it existed. Sprint 039 builds the
engine those omissions were waiting on.

Before writing any of it, the same verification pass ADR 0053 used was
run against the actual platform constraints, because "implement real
file deletion" on Android in 2026 is not the simple statement it sounds
like.

## What this app can and cannot delete

`targetSdk` is 36, `minSdk` 26, and the manifest declares **no storage
permissions at all**. Under scoped storage that yields a precise,
closed answer:

**Deletable, permission-free — app-private storage:**
`filesDir`, `cacheDir`, `getExternalFilesDir(null)`, `externalCacheDir`.
These are directories Android grants this app exclusive ownership of.

**Not deletable, at any price — other apps' cache directories.**
Clearing another app's cache requires a system/privileged permission
(`CLEAR_APP_CACHE`, or `PackageManager.freeStorageAndNotify`) that a
Play-distributed third-party app cannot hold. This is worth stating
plainly because it is the single biggest gap between what the reference
designs implied and what any app of this kind can actually do: the
"App Cache" a cleaner on the Play Store claims to clear is, on any
modern Android, its own cache and nothing else.

**Not deletable without a product decision — shared storage.**
Downloads, Pictures, the rest of `/sdcard` require either
`MANAGE_EXTERNAL_STORAGE` (Play-restricted; anti-virus is on Google's
permitted-use list, but it is a declaration-form, review-gated
permission this project has declined since Sprint 001 and
`ScanScopePathResolver` documents declining) or per-batch `MediaStore`
delete requests that prompt the user each time and need an Activity
result flow.

This has a consequence the sprint brief did not anticipate and the
project owner should weigh: **the `LEFTOVER_INSTALLER` category is
currently unreachable in practice.** `JunkFileClassifier` implements the
rule, but the only scopes the Cleaner scans are app-private, and stale
`.apk` files live in shared Downloads. The category is left implemented
and honest rather than deleted, because it becomes reachable the moment
a shared-storage decision is made — but nothing in the UI claims to find
files it cannot reach.

## Decision

Build the full engine, bounded to app-private storage.

`ScanScope.ApplicationCache` was added so the Cleaner scans the app's own
cache as well as its files directory. Without it the feature would have
been close to vacuous — `filesDir` on a scanner app holds almost nothing,
while `cacheDir` is where real reclaimable bytes actually accumulate. It
is a distinct scope rather than folded into `InternalStorage` because
`filesDir` holds data the app expects to keep and `cacheDir` holds data
Android documents as discardable at any time; a scope conflating them
would remove the ability to treat them differently.

### The containment guard

`AppPrivateStorageRoots.contains()` is checked inside
`FileDeletionRepositoryImpl.deleteFile`, below every use case. Not in the
use case, and not in the ViewModel.

This placement is the most important decision in the sprint. A use case
is one caller among possible future many; a future background cleaner, a
settings action, or a bad candidate list must not be able to route
around the boundary. Paths are compared after `canonicalFile`
resolution, so `cacheDir/../../../sdcard/DCIM` and a symlink pointing
outside both resolve to their real location before the check and both
fail it — raw string prefix comparison would let either through, and
would also treat `/data/.../files_backup` as inside `/data/.../files`.
A root is deliberately not contained in itself: deleting `filesDir`
wholesale is not a cleanup.

The instrumented suite deletes real files on a real filesystem and
asserts that a file outside the sandbox is refused and still exists
afterwards.

### Progress: real where it can be, absent where it can't

**Cleaning shows a real percentage.** The candidate list is known before
deletion starts, so `CleaningProgress.fraction` is a real fraction of
real work.

**Scanning still shows none**, and this is not a leftover from Sprint
038's constraints. A filesystem walk does not know how many files it
will visit until it has visited them. A percentage would require a full
counting pre-pass — doubling the I/O purely to animate a bar — or an
invented denominator. `JunkScanProgress` therefore carries real counters
(`filesInspected`, `junkFound`, `bytesFound`) and no total. Sprint 038's
indeterminate ring stays, now with real numbers beside it.

**No time-remaining anywhere.** A countdown predicts how long the
remaining work will take. That is an estimate presented as a
measurement, and the real percentage and counts carry the same
information honestly.

### Bytes freed are measured, not assumed

`FileDeletionRepository.deleteFile` returns the file's size measured
immediately before deletion, not the size recorded when it was scanned —
files change in between. A file whose deletion fails contributes to
`itemsFailed` and contributes zero bytes. "Freed 240 MB" therefore means
240 MB that is genuinely no longer on disk.

### Cancellation is an outcome, not an error

`ensureActive()` is checked between files in both the enumeration walk
(`enumerateFilesAsFlow`) and the deletion loop. The walk needed it
explicitly: `walkTopDown` is blocking, synchronous I/O that never
suspends, so a cancelled coroutine would otherwise keep walking a large
tree to completion.

A cancelled cleanup leaves real deletions behind. Those bytes are
genuinely freed, so the history record is written inside
`NonCancellable` before the cancellation propagates — dropping it would
lose a true fact about the user's device because they pressed a button.
The completion screen reports the partial run as partial, with
`wasCancelled` and `itemsSkipped`, rather than dressing it up as a
finished one.

### An unreadable scope must not read as "clean"

`enumerateFilesAsFlow` cannot report a root-resolution failure per
element. Left alone, an unreadable volume would complete the flow empty
and render as "Your storage is clean" — telling the user their device
was checked when it never was. `isScopeAvailable` was added for exactly
this: a cheap stat, checked before streaming, producing
`JunkScanEvent.Failed`. The distinction between "looked, found nothing"
and "could not look" is preserved end to end and asserted in tests on
both sides.

### Deletions

`FindCleanableItemsUseCase` (Sprint 022) was deleted.
`ScanForJunkFilesUseCase` supersedes it, and once the ViewModel moved
across it had no production caller — two use cases performing the same
enumerate-then-classify orchestration in two shapes, one kept alive only
by its own test. `JunkFileClassifier` is untouched and remains the single
place junk policy lives.

## Schema

`AppDatabase` 4 → 5, adding `cleanup_records`. Destructive migration
again, per ADR 0023's standing reasoning for a pre-1.0 app with no real
persisted rows to preserve. The table stores outcome totals only and
deliberately no file paths: a durable inventory of what was on a user's
device is a data liability with no feature behind it.

## Module placement

New module `core:cleaningdata`, holding deletion, cleanup history and
storage statistics — the same shape as `core:securitydata` (Sprint 011)
and `core:trusteddata` (Sprint 012). Deletion could have gone in
`core:enumeration` next to the path resolver, but that module's contract
is explicitly "answering what can be scanned, never acting on it," and
putting the project's only destructive operation inside it would have
quietly broken that.

## What was not touched

`JunkFileClassifier`, `CleanableItem`, `CleanableCategory`, the
navigation graph, and every other feature module. The Sprint 038
layouts, spacing, typography and colour are unchanged; Cleaning and
Completed are assembled from the same local building blocks
(`FeatureHeader`, `InfoCard`, `CounterCell`) rather than introducing any
new visual language.

Two pieces of Sprint 038 copy were changed, because Sprint 039 made them
false: the results hero no longer says nothing has been deleted, and the
reassurance card no longer says the scan is read-only. Both now describe
the real, enforced boundary instead.

## Consequences

- The Cleaner genuinely cleans, and every figure on screen is measured.
- It cleans **the app's own storage only**. That is an honest and
  defensible MVP, but it is materially narrower than the reference
  designs implied, and the product framing should match it.
- Two decisions are now teed up for the project owner, both with real
  Play policy weight rather than engineering weight: whether to pursue
  `MANAGE_EXTERNAL_STORAGE` under the anti-virus permitted-use category,
  and whether to add a `MediaStore` delete-request flow for Downloads.
  Either would make `LEFTOVER_INSTALLER` reachable. Neither should be
  decided inside an engineering sprint.
