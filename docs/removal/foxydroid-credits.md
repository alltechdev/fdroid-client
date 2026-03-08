# FoxyDroid Credits Removal

## Overview

**Date:** 2026-03-07
**Branch:** main

Removed FoxyDroid from the Settings screen credits section.

## What Was Removed

### Settings Screen Credit

**File:** `compose/settings/SettingsScreen.kt`

```kotlin
// Removed constants
private const val FOXY_DROID_TITLE = "FoxyDroid"
private const val FOXY_DROID_URL = "https://github.com/kitsunyan/foxy-droid"

// Removed credit item
item {
    ActionSettingItem(
        title = stringResource(R.string.special_credits),
        description = FOXY_DROID_TITLE,
        onClick = { uriHandler.openUri(FOXY_DROID_URL) },
    )
}
```

### String Resources

Removed `special_credits` string from all 48 locale files:
- `values/strings.xml` and all locale variants

## Rationale

The app is being simplified and rebranded. While FoxyDroid was the original project that Droid-ify forked from, the Droid-ify credit is sufficient for attribution purposes.

## Impact

- Settings screen no longer shows FoxyDroid credit
- `special_credits` string resource is no longer available
- No functional impact on app behavior
