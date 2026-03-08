# Dead Code Cleanup

Removed dead code left over from previous feature removals.

## Date
2026-03-07

## Branch
main

## Reason

Clean up unused parameters and functions that were left behind after removing unstable updates, incompatible versions, and other features.

## What Was Removed

### SyncPreference.kt
- `toWorkConstraints()` extension function - never called

### AppDetailAdapter.kt
- `allowIncompatibleVersion` parameter from `setProducts()` - always passed as `false`
- Simplified release filter to always exclude incompatible versions

### SyncService.kt
- `unstableUpdates` parameter from `downloadFile()` - always `false`
- Removed parameter passing to `RepositoryUpdater.update()`

### RepositoryUpdater.kt
- `unstable` parameter from `update()` - always `false`
- `unstable` parameter from private `update()` overload
- `unstable` parameter from `processFile()`
- `unstable` parameter from `transformProduct()`
- Simplified predicate to only consider stable versions

### Intent.kt
- `Uri?.get(key: String)` operator - only used by removed deep link parsing

### Locale String Files (all locales)
- `days` plurals - never used in code
- `hours` plurals - never used in code

## Behavior Change

No behavior change - these parameters were already hardcoded to `false`.

| Before | After |
|--------|-------|
| `allowIncompatibleVersion = false` passed explicitly | Parameter removed, always filters incompatible |
| `unstableUpdates = false` passed explicitly | Parameter removed, always stable only |
| `toWorkConstraints()` defined but unused | Function removed |

## Files Changed

| File | Change |
|------|--------|
| `sync/SyncPreference.kt` | Removed `toWorkConstraints()` |
| `ui/appDetail/AppDetailAdapter.kt` | Removed `allowIncompatibleVersion` parameter |
| `ui/appDetail/AppDetailFragment.kt` | Removed parameter from `setProducts()` call |
| `service/SyncService.kt` | Removed `unstableUpdates` parameter |
| `index/RepositoryUpdater.kt` | Removed `unstable` parameter from all functions |
| `utility/common/extension/Intent.kt` | Removed `Uri?.get()` operator |
| `res/values*/strings.xml` | Removed unused `days` and `hours` plurals from all locales |
