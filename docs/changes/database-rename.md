# Database Name Rename

## Overview

**Date:** 2026-03-07
**Branch:** main

Database file names were updated to better distinguish between the modern Room database and the legacy SQLite database.

## Changes Made

### Room Database Name

**File:** `data/local/AtdDatabase.kt`

```kotlin
// Before
name = "atd_store"

// After
name = "atd_room"
```

### Legacy Database Name

**File:** `database/table/DatabaseHelper.kt`

```kotlin
// Before
private const val DB_LEGACY_NAME = "atd_store"

// After
private const val DB_LEGACY_NAME = "atd_legacy"
```

## Rationale

The previous naming (`atd_store` for both) was confusing since:
- Both databases had similar names
- It was unclear which was the modern Room database vs legacy SQLite

New naming makes the distinction clear:
- `atd_room` - Modern Room database (primary)
- `atd_legacy` - Legacy SQLite database (being phased out)

## Impact

- Existing app installations will create new database files
- Data from previous `atd_store` databases will not migrate automatically
- Apps will re-sync data from repositories on first launch after update

## Migration Notes

No migration needed since:
1. Room uses `fallbackToDestructiveMigration(true)`
2. Legacy database is read-only for migration purposes
3. All data can be re-synced from F-Droid repositories
