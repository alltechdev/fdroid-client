# App Branding: Droid-ify → ATD Store

## Overview

**Date:** 2026-03-07
**Branch:** main

The app's visible branding was changed from "Droid-ify" to "ATD Store" to establish a distinct identity for this simplified fork.

## Changes Made

### App Name (strings.xml)

**Files:**
- `app/src/main/res/values/strings.xml`
- `app/src/alpha/res/values/strings.xml`

```xml
<!-- Before -->
<string name="application_name" translatable="false">Droid-ify</string>

<!-- After -->
<string name="application_name" translatable="false">ATD Store</string>
```

Alpha variant changed from "Droid-ify α" to "ATD Store α".

### TopBar Titles

**Files:**
- `compose/home/HomeScreen.kt`
- `compose/appList/AppListScreen.kt`

```kotlin
// Before
TopAppBar(title = { Text("Droid-ify") })

// After
TopAppBar(title = { Text("ATD Store") })
```

### User Agent

**File:** `di/NetworkModule.kt`

```kotlin
// Before
agent = "Droid-ify/${VERSION_NAME}-${BUILD_TYPE}"

// After
agent = "ATDStore/${VERSION_NAME}-${BUILD_TYPE}"
```

### Settings Credits

**File:** `compose/settings/SettingsScreen.kt`

```kotlin
// Before
private const val DROID_IFY_TITLE = "Droid-ify"

// After
private const val DROID_IFY_TITLE = "Based on Droid-ify"
```

### Code Comments

**File:** `service/SyncService.kt`

```kotlin
// Before
// Update Droid-ify the last

// After
// Update ATD Store the last
```

## Files Changed

| File | Change |
|------|--------|
| `app/src/main/res/values/strings.xml` | App name |
| `app/src/alpha/res/values/strings.xml` | Alpha app name |
| `compose/home/HomeScreen.kt` | TopBar title |
| `compose/appList/AppListScreen.kt` | TopBar title |
| `di/NetworkModule.kt` | User agent |
| `compose/settings/SettingsScreen.kt` | Credits title |
| `service/SyncService.kt` | Comment |

## Files NOT Changed

- Settings screen still links to Droid-ify GitHub as credit (intentional)
- Test fixture JSON files contain "droidify" as external app data

## Impact

- App displays as "ATD Store" in launcher and UI
- HTTP requests identify as "ATDStore/..." user agent
- Settings credits acknowledge Droid-ify as the original project

## Rationale

Establishes clear identity separate from upstream Droid-ify while maintaining proper attribution in the credits section.
