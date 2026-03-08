# Auto-Sync Setting Removal

Removed the configurable auto-sync setting. Sync now always runs automatically.

## Date
2026-03-06

## Branch
main

## Reason

Simplify app by removing user configuration for sync behavior. App now always syncs automatically when network is available.

## What Was Removed

### Model
- `AutoSync` enum (`datastore/model/AutoSync.kt`) - deleted entirely

### Settings
- `autoSync` field from `Settings` data class
- `setAutoSync()` method from `SettingsRepository` interface
- `setAutoSync()` implementation from `PreferenceSettingsRepository`
- `AUTO_SYNC` preference key

### UI
- `AutoSyncSetting` composable from `SettingsScreen.kt`
- "Sync repositories" header from settings
- `WarningBanner` component (`compose/settings/components/WarningBanner.kt`) - deleted entirely
- `isBackgroundAllowed` state from `SettingsViewModel`
- `setAutoSync()` method from `SettingsViewModel`

### Application
- `updateSyncJob()` simplified to `scheduleSyncJob()` in `Droidify.kt`
- Removed preference collection for sync mode changes

### Strings Removed
- `only_on_wifi`
- `only_on_wifi_with_charging`
- `require_background_access`
- `require_background_access_DESC`
- `sync_repositories`
- `sync_repositories_automatically`
- `force_sync_repositories`

### Tests
- Removed `setAutoSync` test from `PreferenceSettingsRepositoryTest`
- Removed `AutoSync` assertions from settings tests

## Behavior Change

| Before | After |
|--------|-------|
| User could choose: Never, WiFi Only, WiFi+Charging, Always | Always syncs when network available |
| Sync disabled if set to "Never" | Sync always enabled |
| Battery optimization warning shown | No warning needed |

## Files Changed

| File | Change |
|------|--------|
| `datastore/model/AutoSync.kt` | Deleted |
| `datastore/Settings.kt` | Removed `autoSync` field |
| `datastore/SettingsRepository.kt` | Removed `setAutoSync()` |
| `datastore/PreferenceSettingsRepository.kt` | Removed implementation |
| `compose/settings/SettingsScreen.kt` | Removed UI |
| `compose/settings/SettingsViewModel.kt` | Removed method and state |
| `compose/settings/components/WarningBanner.kt` | Deleted |
| `Droidify.kt` | Simplified sync scheduling |
| `app/src/main/res/values*/strings.xml` | Removed strings |
