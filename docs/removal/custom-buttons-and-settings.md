# Removed: Custom Buttons, Cleanup Interval, and Delete APK Settings

**Date:** 2026-03-06
**Branch:** main

## Overview

Removed the App Detail Links (Custom Buttons) feature, the APK Cleanup Interval setting, and the Delete APK on Install setting.

## Custom Buttons (App Detail Links)

### What was removed

| Component | File | Description |
|-----------|------|-------------|
| Repository | `datastore/CustomButtonRepository.kt` | **Deleted** |
| Model | `datastore/model/CustomButton.kt` | **Deleted** |
| UI (Compose Settings) | `compose/settings/components/CustomButtonEditor.kt` | **Deleted** |
| UI (Compose Settings) | `compose/settings/components/CustomButtonsSettingItem.kt` | **Deleted** |
| UI (Compose App Detail) | `compose/appDetail/components/CustomButtonsRow.kt` | **Deleted** |
| UI (Legacy) | `ui/appDetail/CustomButtonsAdapter.kt` | **Deleted** |
| Settings Screen | `compose/settings/SettingsScreen.kt` | Removed custom buttons section |
| Settings ViewModel | `compose/settings/SettingsViewModel.kt` | Removed custom button methods |
| App Detail Screen | `compose/appDetail/AppDetailScreen.kt` | Removed custom buttons row |
| App Detail ViewModel | `compose/appDetail/AppDetailViewModel.kt` | Removed CustomButtonRepository injection |
| Legacy Fragment | `ui/appDetail/AppDetailFragment.kt` | Removed custom button collection |
| Legacy Adapter | `ui/appDetail/AppDetailAdapter.kt` | Removed CustomButtonsItem, ViewHolder, ViewType |
| Legacy ViewModel | `ui/appDetail/AppDetailViewModel.kt` | Removed CustomButtonRepository injection |

### Strings removed

- `custom_buttons`, `custom_buttons_description`, `custom_buttons_section`
- `custom_button_add`, `custom_button_edit`, `custom_button_label`, `custom_button_label_hint`
- `custom_button_url`, `custom_button_url_hint`, `custom_button_url_description`
- `custom_button_templates`, `custom_button_icon`, `custom_button_delete_confirmation`
- `custom_buttons_export`, `custom_buttons_import`, `custom_buttons_imported`

## APK Cleanup Interval Setting

### What was removed

| Component | File | Description |
|-----------|------|-------------|
| Settings field | `datastore/Settings.kt` | `cleanUpInterval` field removed |
| Repository Interface | `datastore/SettingsRepository.kt` | `setCleanUpInterval` method removed |
| Repository Impl | `datastore/PreferenceSettingsRepository.kt` | `CLEAN_UP_INTERVAL` key and setter removed |
| Settings ViewModel | `compose/settings/SettingsViewModel.kt` | `cleanUpIntervals`, `setCleanUpInterval`, `forceCleanup` removed |
| Settings Screen | `compose/settings/SettingsScreen.kt` | Cleanup interval selector and force cleanup button removed |
| Application | `AtdStore.kt` | Settings listener for cleanup interval changes removed |

### Behavior change

Cleanup is now hardcoded to run every **6 hours** (scheduled once at app startup).

```kotlin
// Before: Configurable via settings
settingsRepository.get { cleanUpInterval }.collect {
    if (it == INFINITE) CleanUpWorker.removeAllSchedules(context)
    else CleanUpWorker.scheduleCleanup(context, it)
}

// After: Fixed 6-hour interval
CleanUpWorker.scheduleCleanup(applicationContext, 6.hours)
```

### Strings removed

- `cleanup_title`
- `force_clean_up`, `force_clean_up_DESC`

## Delete APK on Install Setting

### What was removed

| Component | File | Description |
|-----------|------|-------------|
| Settings field | `datastore/Settings.kt` | `deleteApkOnInstall` field removed |
| Repository Interface | `datastore/SettingsRepository.kt` | `setDeleteApkOnInstall` method removed |
| Repository Impl | `datastore/PreferenceSettingsRepository.kt` | `DELETE_APK_ON_INSTALL` key and setter removed |
| Settings ViewModel | `compose/settings/SettingsViewModel.kt` | `setDeleteApkOnInstall` method removed |
| Settings Screen | `compose/settings/SettingsScreen.kt` | Toggle switch removed |
| Install Manager | `installer/InstallManager.kt` | Setting check removed |
| Test | `datastore/PreferenceSettingsRepositoryTest.kt` | Test case removed |

### Behavior change

APKs are now **always deleted** after successful installation (hardcoded to `true`).

```kotlin
// Before: Configurable via settings
if (result == InstallState.Installed) {
    if (deleteApkPreference.first()) {
        apkFile.delete()
    }
}

// After: Always delete
if (result == InstallState.Installed) {
    apkFile.delete()
}
```

### Strings removed

- `delete_apk_on_install`, `delete_apk_on_install_summary`

## Migration notes

- Custom buttons data file (`custom_buttons.json`) can be manually deleted from app data
- No user action needed - behavior changes are automatic
- PreferencesKeys for removed settings still exist in stored preferences but are ignored

## Changes

| Change | Change Doc |
|--------|------------|
| Class renames (AtdStore, AtdDatabase, AtdTheme) | [package-rename.md](../changes/package-rename.md) |
