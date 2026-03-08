# Package Rename: com.looker.droidify → com.atd.store

## Overview

**Date:** 2026-03-07
**Branch:** chore/change-package-name

The application package was renamed from `com.looker.droidify` to `com.atd.store` to establish a distinct identity for this simplified fork.

## Changes Made

### Build Configuration

**File:** `app/build.gradle.kts`

```kotlin
// Before
namespace = "com.looker.droidify"
applicationId = "com.looker.droidify"
testInstrumentationRunner = "com.looker.droidify.TestRunner"

// After
namespace = "com.atd.store"
applicationId = "com.atd.store"
testInstrumentationRunner = "com.atd.store.TestRunner"
```

### AndroidManifest.xml

Updated intent action from `com.looker.droidify.intent.action.COPY_ERROR` to `com.atd.store.intent.action.COPY_ERROR`.

### Source Directory Structure

| Before | After |
|--------|-------|
| `app/src/main/kotlin/com/looker/droidify/` | `app/src/main/kotlin/com/atd/store/` |
| `app/src/test/kotlin/com/looker/droidify/` | `app/src/test/kotlin/com/atd/store/` |
| `app/src/androidTest/kotlin/com/looker/droidify/` | `app/src/androidTest/kotlin/com/atd/store/` |

### Internal Class Names

All internal class names rebranded with ATD:

| Before | After | File |
|--------|-------|------|
| `Droidify` | `AtdStore` | `AtdStore.kt` |
| `DroidifyDatabase` | `AtdDatabase` | `AtdDatabase.kt` |
| `droidifyDatabase()` | `atdDatabase()` | `AtdDatabase.kt` |
| `DroidifyTheme` | `AtdTheme` | `Theme.kt` |
| `droidify_room` | `atd_room` | Database file name |
| `DB_LEGACY_NAME` | `atd_legacy` | `DatabaseHelper.kt` |

### Room Database Schema

Renamed schema directory:
- Before: `app/schemas/com.looker.droidify.data.local.DroidifyDatabase/`
- After: `app/schemas/com.atd.store.data.local.AtdDatabase/`

### All Kotlin Files

Updated in all 241 source files:
- Package declarations: `package com.looker.droidify.*` → `package com.atd.store.*`
- Import statements: `import com.looker.droidify.*` → `import com.atd.store.*`

### IDE Run Configurations

Updated activity class references in `.run/*.xml` files.

### README.md (Deleted)

README.md, CONTRIBUTING.md, and metadata/ directory were subsequently deleted as part of removing Droid-ify branding.

## Files NOT Changed

Test fixture JSON files in `app/src/test/resources/` and `app/src/androidTest/assets/` contain F-Droid index data that references `com.looker.droidify` as an **external app listing**. These are intentional test fixtures and were not modified.

## Impact

- All existing installations will be treated as separate apps
- Users must reinstall to migrate to the new package
- Database schema path changed (Room handles migration automatically)

## Migration Notes

For developers:
1. Clean build required after pulling these changes: `./gradlew clean`
2. IDE may require project reimport to recognize new package structure
3. Any local branches need to be rebased to incorporate the rename
