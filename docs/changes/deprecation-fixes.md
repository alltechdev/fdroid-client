# Deprecation Fixes

Fixed deprecated API usages across the codebase using modern alternatives.

## Date
2026-03-06

## Branch
removals

## Reason

Replace deprecated Android and Kotlin APIs with their modern equivalents to eliminate build warnings and ensure future compatibility.

## Changes

### BundleCompat / IntentCompat

Replaced deprecated `Bundle.getParcelable()` and `Intent.getParcelableExtra()` with compat library methods for backward-compatible API usage.

| File | Change |
|------|--------|
| `MainActivity.kt` | `BundleCompat.getParcelableArrayList()` for fragment stack restoration |
| `MessageDialog.kt` | `BundleCompat.getParcelable()` for Message retrieval |
| `AppDetailFragment.kt` | `BundleCompat.getParcelable()` for adapter state and layout manager |
| `AppListFragment.kt` | `BundleCompat.getParcelable()` for layout manager state |
| `SessionInstallerReceiver.kt` | `IntentCompat.getParcelableExtra()` for prompt intent |

### Locale.Builder

Replaced deprecated `Locale(String)` and `Locale(String, String)` constructors with `Locale.Builder()`.

| File | Change |
|------|--------|
| `SettingsViewModel.kt` | `String.toLocale()` extension now uses `Locale.Builder()` |

```kotlin
// Before (deprecated)
Locale(languageCode)
Locale(languageCode, regionCode)

// After
Locale.Builder()
    .setLanguage(languageCode)
    .setRegion(regionCode)  // if applicable
    .build()
```

### enableEdgeToEdge

Replaced deprecated `window.statusBarColor` and `window.navigationBarColor` with `enableEdgeToEdge()` from AndroidX Activity.

| File | Change |
|------|--------|
| `MainActivity.kt` | Use `enableEdgeToEdge()` instead of manual color setting |

```kotlin
// Before (deprecated)
window.statusBarColor = resources.getColor(android.R.color.transparent, theme)
window.navigationBarColor = resources.getColor(android.R.color.transparent, theme)

// After
enableEdgeToEdge()
```

### @MapColumn

Replaced deprecated `@MapInfo` annotation with `@MapColumn` in Room DAO methods.

| File | Change |
|------|--------|
| `AppDao.kt` | `suggestedVersionNamesAll()` return type annotation |

```kotlin
// Before (deprecated)
@MapInfo(keyColumn = "appId", valueColumn = "versionName")
suspend fun suggestedVersionNamesAll(): Map<Int, String>

// After
suspend fun suggestedVersionNamesAll(): Map<@MapColumn(columnName = "appId") Int, @MapColumn(columnName = "versionName") String>
```

### Calendar API

Replaced deprecated `Date(year, month, day)` constructor with `Calendar` API.

| File | Change |
|------|--------|
| `DownloadStats.kt` | `String.toEpochMillis()` extension |

```kotlin
// Before (deprecated)
Date(year - 1900, month, day).time

// After
Calendar.getInstance().apply {
    set(year, month, day, 0, 0, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis
```

### getDeclaredConstructor().newInstance()

Replaced deprecated `Class.newInstance()` with `getDeclaredConstructor().newInstance()`.

| File | Change |
|------|--------|
| `MainActivity.kt` | Fragment instantiation in `popFragment()` |

```kotlin
// Before (deprecated)
Class.forName(className).newInstance() as Fragment

// After
Class.forName(className).getDeclaredConstructor().newInstance() as Fragment
```

### TypedValue.COMPLEX_UNIT_SP

Replaced deprecated `scaledDensity` usage with `TypedValue.COMPLEX_UNIT_SP` for text sizing.

| File | Change |
|------|--------|
| `View.kt` | `TextView.setTextSizeScaled()` extension |

```kotlin
// Before (deprecated)
val scaledSize = size * resources.displayMetrics.scaledDensity
textSize = scaledSize

// After
setTextSize(TypedValue.COMPLEX_UNIT_SP, size.toFloat())
```

### @OptIn for FlowPreview

Added `@OptIn` annotation for experimental FlowPreview API usage.

| File | Change |
|------|--------|
| `DownloadService.kt` | Added `@OptIn(kotlinx.coroutines.FlowPreview::class)` |

### Room Query Column Mismatch

Fixed Room query that returned unused columns from JOIN, causing KSP warning.

| File | Change |
|------|--------|
| `RepoDao.kt` | `categoriesByRepoId()` - use `SELECT category.*` instead of `SELECT *` |

```kotlin
// Before (warning: unused column 'id' from category_repo_relation)
@Query("""
    SELECT * FROM category
    JOIN category_repo_relation ON category.defaultName = category_repo_relation.defaultName
    WHERE category_repo_relation.id = :repoId
""")
@RewriteQueriesToDropUnusedColumns
fun categoriesByRepoId(repoId: Int): Flow<List<CategoryEntity>>

// After (no warning)
@Query("""
    SELECT category.* FROM category
    JOIN category_repo_relation ON category.defaultName = category_repo_relation.defaultName
    WHERE category_repo_relation.id = :repoId
""")
fun categoriesByRepoId(repoId: Int): Flow<List<CategoryEntity>>
```

## Compatibility

All changes maintain backward compatibility with minSdk 23 (Android 6.0 Marshmallow). The compat libraries handle API differences internally.

## Files Changed

| File | Change |
|------|--------|
| `MainActivity.kt` | BundleCompat, enableEdgeToEdge, getDeclaredConstructor |
| `MessageDialog.kt` | BundleCompat |
| `AppDetailFragment.kt` | BundleCompat |
| `AppListFragment.kt` | BundleCompat |
| `SessionInstallerReceiver.kt` | IntentCompat |
| `SettingsViewModel.kt` | Locale.Builder |
| `AppDao.kt` | @MapColumn |
| `RepoDao.kt` | SELECT category.* in JOIN query |
| `DownloadStats.kt` | Calendar API |
| `View.kt` | TypedValue.COMPLEX_UNIT_SP |
| `DownloadService.kt` | @OptIn annotation |
