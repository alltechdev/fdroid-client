# Removed: Unstable Updates, Incompatible Versions, and Ignore Signature Settings

**Date:** 2026-03-06
**Branch:** main

## Overview

Removed three version/signature related settings that are now hardcoded to `false` (disabled).

## Unstable Updates Setting

### What was removed

| Component | File | Description |
|-----------|------|-------------|
| Settings field | `datastore/Settings.kt` | `unstableUpdate` field removed |
| Repository Interface | `datastore/SettingsRepository.kt` | `enableUnstableUpdates` method removed |
| Repository Impl | `datastore/PreferenceSettingsRepository.kt` | `UNSTABLE_UPDATES` key and setter removed |
| Settings ViewModel | `compose/settings/SettingsViewModel.kt` | `setUnstableUpdates` method removed |
| Settings Screen | `compose/settings/SettingsScreen.kt` | Toggle switch removed |
| Application | `AtdStore.kt` | Settings listener that triggered force sync removed |
| Sync Service | `service/SyncService.kt` | Settings fetch replaced with hardcoded `false` |

### Behavior change

Unstable (beta/alpha) app versions are now **never suggested** for update (hardcoded to `false`).

```kotlin
// Before: Configurable via settings
val unstableUpdates = settingsRepository.getInitial().unstableUpdate

// After: Always false
unstableUpdates = false
```

### Strings removed

- `unstable_updates`, `unstable_updates_summary`

## Incompatible Versions Setting

### What was removed

| Component | File | Description |
|-----------|------|-------------|
| Settings field | `datastore/Settings.kt` | `incompatibleVersions` field removed |
| Repository Interface | `datastore/SettingsRepository.kt` | `enableIncompatibleVersion` method removed |
| Repository Impl | `datastore/PreferenceSettingsRepository.kt` | `INCOMPATIBLE_VERSIONS` key and setter removed |
| Settings ViewModel | `compose/settings/SettingsViewModel.kt` | `setIncompatibleUpdates` method removed |
| Settings Screen | `compose/settings/SettingsScreen.kt` | Toggle switch removed |
| App Detail ViewModel | `ui/appDetail/AppDetailViewModel.kt` | `allowIncompatibleVersions` from state removed |
| App Detail Fragment | `ui/appDetail/AppDetailFragment.kt` | Hardcoded to `false` |
| App List ViewModel | `ui/appList/AppListViewModel.kt` | Removed from state |

### Behavior change

Incompatible app versions are now **never shown** (hardcoded to `false`).

```kotlin
// Before: Configurable via settings
allowIncompatibleVersion = state.allowIncompatibleVersions

// After: Always false
allowIncompatibleVersion = false
```

### Strings removed

- `incompatible_versions`, `incompatible_versions_summary`

## Ignore Signature Setting

### What was removed

| Component | File | Description |
|-----------|------|-------------|
| Settings field | `datastore/Settings.kt` | `ignoreSignature` field removed |
| Repository Interface | `datastore/SettingsRepository.kt` | `setIgnoreSignature` method removed |
| Repository Impl | `datastore/PreferenceSettingsRepository.kt` | `IGNORE_SIGNATURE` key and setter removed |
| Settings ViewModel | `compose/settings/SettingsViewModel.kt` | `setIgnoreSignature` method removed |
| Settings Screen | `compose/settings/SettingsScreen.kt` | Toggle switch removed |
| App Detail ViewModel | `ui/appDetail/AppDetailViewModel.kt` | `shouldIgnoreSignature` method removed |
| App Detail Fragment | `ui/appDetail/AppDetailFragment.kt` | Always shows signature mismatch dialog |
| Sync Service | `service/SyncService.kt` | `skipSignature` hardcoded to `false` |
| Install Manager | `installer/InstallManager.kt` | `skipSignatureCheck` hardcoded to `false` |
| App List ViewModel | `ui/appList/AppListViewModel.kt` | `skipSignatureCheck` hardcoded to `false` |

### Behavior change

Signature verification is now **always enforced** (hardcoded to `false` = never ignore).

```kotlin
// Before: Configurable via settings
if (viewModel.shouldIgnoreSignature()) {
    queueReleaseInstall(release, installedItem)
} else {
    MessageDialog(Message.ReleaseSignatureMismatch).show(childFragmentManager)
}

// After: Always show mismatch dialog
MessageDialog(Message.ReleaseSignatureMismatch).show(childFragmentManager)
```

### Strings removed

- `ignore_signature`, `ignore_signature_summary`

## Migration notes

- No user action needed - behavior changes are automatic
- PreferencesKeys for removed settings still exist in stored preferences but are ignored
- These changes improve security by preventing signature bypass
- Users who previously used unstable updates will now only see stable versions

## Changes

| Change | Change Doc |
|--------|------------|
| Class renames (AtdStore, AtdDatabase, AtdTheme) | [package-rename.md](../changes/package-rename.md) |
