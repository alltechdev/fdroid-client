# Removed: Category/Section Filtering

**Date:** 2026-03-06
**Branch:** main

## Overview

Removed the category and repository section filtering from the Explore tab. The app now shows all apps without any filtering UI.

## UI Components Removed

| Component | Location |
|-----------|----------|
| Section dropdown | `tabs_toolbar.xml` |
| Section list overlay | `TabsFragment.kt` |
| Category chips | Explore tab header |

## TabsFragment Changes

### Removed BackAction Values

```kotlin
// Before
enum class BackAction {
    ProductAll,
    CollapseSearchView,
    HideSections,
    None,
}

// After
enum class BackAction {
    CollapseSearchView,
    None,
}
```

### Removed Variables

- `showSections: Boolean`
- `sectionsList: RecyclerView?`
- `sectionsAdapter: SectionsAdapter?`
- `sectionsAnimator: ValueAnimator?`
- `Layout` class (section view references)

### Removed Methods

- `updateSections()`
- `updateSection()`
- `animateSectionsList()`

### Removed Inner Class

- `SectionsAdapter` - RecyclerView adapter for section list

## TabsViewModel Changes

### Removed State

- `currentSection: StateFlow<ProductItem.Section>`
- `sections: StateFlow<List<ProductItem.Section>>`
- `showSections: MutableStateFlow<Boolean>`

### Removed Methods

- `setSection(section: ProductItem.Section)`

## AppListFragment Changes

### Source Enum Simplified

```kotlin
// Before
enum class Source(
    val titleResId: Int,
    val sections: Boolean,
    val updateAll: Boolean,
)

// After
enum class Source(
    val titleResId: Int,
    val updateAll: Boolean,
)
```

### Removed Methods

- `setSection(section: ProductItem.Section)`

## AppListViewModel Changes

### Removed State

- `sections: MutableStateFlow<ProductItem.Section>`

### Removed Methods

- `setSection(newSection: ProductItem.Section)`

## ProductItem.Section Removed

**File:** `model/ProductItem.kt`

```kotlin
// Removed entirely
sealed interface Section : Parcelable {
    @Parcelize
    data object All : Section

    @Parcelize
    data class Category(val name: String) : Section

    @Parcelize
    data class Repository(val id: Long, val name: String) : Section
}
```

## Database Changes

### ProductAdapter.query()

**File:** `database/Database.kt`

Removed `section` parameter and category join/filter logic:

```kotlin
// Before
fun query(
    installed: Boolean,
    updates: Boolean,
    skipSignatureCheck: Boolean = false,
    searchQuery: String,
    section: ProductItem.Section,
    order: SortOrder,
    signal: CancellationSignal?,
): Cursor

// After
fun query(
    installed: Boolean,
    updates: Boolean,
    skipSignatureCheck: Boolean = false,
    searchQuery: String,
    order: SortOrder,
    signal: CancellationSignal?,
): Cursor
```

## CursorOwner Changes

### Request.Available Simplified

```kotlin
// Before
data class Available(
    val searchQuery: String,
    val section: ProductItem.Section,
    val order: SortOrder,
    val skipSignatureCheck: Boolean = false,
) : Request(1)

// After
data class Available(
    val searchQuery: String,
    val order: SortOrder,
    val skipSignatureCheck: Boolean = false,
) : Request(1)
```

## Layout Changes

**File:** `res/layout/tabs_toolbar.xml`

Removed section dropdown FrameLayout containing:
- `section_layout` (FrameLayout container)
- `section_change` (LinearLayout)
- `section_name` (TextView)
- `section_icon` (ShapeableImageView)

## Files Modified

- `ui/tabsFragment/TabsFragment.kt`
- `ui/tabsFragment/TabsViewModel.kt`
- `ui/appList/AppListFragment.kt`
- `ui/appList/AppListViewModel.kt`
- `model/ProductItem.kt`
- `database/Database.kt`
- `database/CursorOwner.kt`
- `res/layout/tabs_toolbar.xml`

## Files Deleted

- `res/layout/settings_page.xml` (unused legacy layout)

## Orphaned Resources Cleaned

Removed orphaned translation strings from all locale files:
- `proxy_port`, `proxy_type`, `socks_proxy`, `http_proxy`, `no_proxy`, `proxy_host`
- `unstable_updates`, `unstable_updates_summary`
- `custom_button_*` (15 strings)
- `import_export`, `import_*`, `export_*` (10 strings)
- `ignore_signature`, `ignore_signature_summary`
- `incompatible_versions`, `incompatible_versions_summary`
- `delete_apk_on_install`, `delete_apk_on_install_summary`
- `cleanup_title`, `force_clean_up`, `force_clean_up_DESC`
