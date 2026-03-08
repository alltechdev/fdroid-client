# Sort Order UI Removal

## Summary

Removed the sort order button from the top app bar on the home screen. The UI element for changing sort order has been removed from both legacy and Compose UI implementations.

## Date

2026-03-06

## Branch

main

## What Was Removed

### UI Components

| Component | Location | Description |
|-----------|----------|-------------|
| Sort order menu | `TabsFragment.kt` | Dropdown menu with sort options (Updated, Added, Name) |
| Sort button | `AppListScreen.kt` | Sort icon button in Compose top bar |

### Code Changes

#### TabsFragment.kt

Removed:
- `sortOrderMenu` variable and initialization
- `updateOrder()` function
- Sort order menu in `pageChangeCallback`
- Related imports (`SortOrder`, `supportedSortOrders`, `sortOrderName`)

#### TabsViewModel.kt

Removed:
- `sortOrder` StateFlow property
- `setSortOrder()` function

#### AppListScreen.kt (Compose)

Removed:
- Sort IconButton from `AppListTopBar`
- `Icons.AutoMirrored.Filled.Sort` import

#### Preferences.kt

Removed:
- `sortOrderName()` extension function

#### SortOrder.kt

Removed:
- `supportedSortOrders()` function
- TODO comment about size sorting

### Resources Removed

| Resource | Type | Files |
|----------|------|-------|
| `ic_sort.xml` | Drawable | 1 file |
| `sorting_order` | String | All locale files |
| `recently_updated` | String | All locale files |
| `whats_new` | String | All locale files |
| `name` | String | All locale files |

## What Was Kept

- `SortOrder` enum - Still used internally by database queries
- `sortOrder` in `Settings` data class - Still stored in preferences
- Database sorting functionality - Apps are still sorted by `SortOrder.UPDATED` (default)

## Rationale

Simplifying the UI by removing the sort order selector. The app now uses a fixed sort order (recently updated first), which is the most commonly used option.

## Impact

### User-Facing

- Users can no longer change how apps are sorted in the list
- Apps are always sorted by most recently updated

### Code

- `TabsViewModel` no longer exposes sort order state
- Compose `AppListViewModel` still has `sortOrder` (hardcoded to default)

## Affected Files

| File | Change |
|------|--------|
| `ui/tabsFragment/TabsFragment.kt` | Removed sort menu |
| `ui/tabsFragment/TabsViewModel.kt` | Removed sortOrder and setSortOrder |
| `compose/appList/AppListScreen.kt` | Removed sort button |
| `datastore/extension/Preferences.kt` | Removed sortOrderName function |
| `datastore/model/SortOrder.kt` | Removed supportedSortOrders function |
| `res/drawable/ic_sort.xml` | Deleted |
| `res/values*/strings.xml` | Removed sort-related strings |

## Migration

No migration needed. The app will continue using the default sort order (`SortOrder.UPDATED`).
