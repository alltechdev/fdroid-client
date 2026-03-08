# Compose Screens

Modern Jetpack Compose UI screens.

## Overview

**Directory:** `compose/`

The app is transitioning to Compose-based screens. Each screen follows the MVVM pattern with ViewModels, StateFlows, and `collectAsStateWithLifecycle`.

## Screen Architecture

```
compose/
├── home/              # Main home screen
├── appList/           # App browsing screen
├── appDetail/         # App details screen
├── repoList/          # Repository listing
├── repoEdit/          # Add/edit repository
├── repoDetail/        # Repository details
├── settings/          # Settings screen
├── components/        # Shared components
└── theme/             # Material 3 theming
```

## HomeScreen

**File:** `compose/home/HomeScreen.kt`

Main entry point with bottom navigation.

```kotlin
@Composable
fun HomeScreen(
    onNavigateToApps: () -> Unit,
    onNavigateToRepos: () -> Unit,
    onNavigateToSettings: () -> Unit,
)
```

### Features
- Material 3 TopAppBar
- Bottom navigation bar with Apps, Repos, Settings buttons
- Welcome content with navigation buttons

## AppListScreen

**File:** `compose/appList/AppListScreen.kt`

Browsable app list with search and filtering.

```kotlin
@Composable
fun AppListScreen(
    viewModel: AppListViewModel,
    onAppClick: (String) -> Unit,
    onNavigateToRepos: () -> Unit,
    onNavigateToSettings: () -> Unit,
)
```

### Features

- **LazyColumn** with sticky header for filters
- **Category chips** - horizontal scrollable filter chips
- **Favourites toggle** - filter to show only bookmarked apps
- **SearchBar** - animated search field with press feedback
- **AppItem** - row with icon, name, version badge, summary

### AppItem Component

```kotlin
@Composable
private fun AppItem(
    app: AppMinimal,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
)
```

- 80dp row height
- Async icon loading with Coil
- Fallback icon support
- Version badge with colored background
- 2-line summary truncation

### AppListViewModel

```kotlin
@HiltViewModel
class AppListViewModel @Inject constructor(
    private val appRepository: AppRepository,
    private val repoRepository: RepoRepository,
) : ViewModel()
```

**State:**
- `appsState: StateFlow<List<AppMinimal>>`
- `selectedCategories: StateFlow<Set<String>>`
- `categories: StateFlow<List<DefaultName>>`
- `favouritesOnly: StateFlow<Boolean>`

## AppDetailScreen

**File:** `compose/appDetail/AppDetailScreen.kt`

Full app details with screenshots.

```kotlin
@Composable
fun AppDetailScreen(
    onBackClick: () -> Unit,
    viewModel: AppDetailViewModel,
)
```

### State Model

```kotlin
sealed interface AppDetailState {
    object Loading : AppDetailState
    data class Error(val message: String) : AppDetailState
    data class Success(
        val app: App,
        val packages: List<Pair<Package, Repo>>,
    ) : AppDetailState
}
```

### Sections

1. **HeaderSection** - Icon, name, version, author, favorite toggle
2. ~~**CustomButtonsRow**~~ - *Removed*
3. **ScreenshotsRow** - Horizontal carousel with async loading
4. **CategoriesRow** - Filter chips for categories
5. **Summary** - Bold app summary text
6. **Description** - HTML-rendered description with link handling

### AppDetailViewModel

```kotlin
@HiltViewModel
class AppDetailViewModel @Inject constructor(
    private val appRepository: AppRepository,
    private val repoRepository: RepoRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel()
```

> **Note:** `customButtonRepository` dependency has been removed.

Uses `SavedStateHandle` to retrieve `packageName` from navigation arguments.

## SettingsScreen

**File:** `compose/settings/SettingsScreen.kt`

Full settings screen with all preferences.

```kotlin
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBackClick: () -> Unit,
)
```

### Setting Sections

| Section | Settings |
|---------|----------|
| Personalization | Language, Theme, Dynamic colors, Home swiping |
| Updates | Auto-update, Notify |
| Install | Installer type, Legacy component |
| Credits | Droid-ify version |

### Removed Sections

| Section | Reason |
|---------|--------|
| Cleanup interval | Hardcoded to 6 hours |
| Delete APK | Hardcoded to always delete |
| Proxy | Feature removed |
| Import/Export | Feature removed |
| Custom Buttons | Feature removed |
| Sync Repositories | Sync always enabled automatically |

### Setting Components

**File:** `compose/settings/components/`

| Component | Usage |
|-----------|-------|
| `SettingHeader` | Section headers |
| `SwitchSettingItem` | Toggle settings |
| `SelectionSettingItem` | Dropdown/dialog selection |
| `ActionSettingItem` | Clickable actions |

### Removed Components

| Component | Reason |
|-----------|--------|
| `TextInputSettingItem` | Only used for proxy |
| `CustomButtonsSettingItem` | Custom buttons removed |
| `CustomButtonEditor` | Custom buttons removed |
| `WarningBanner` | Auto-sync removed, no battery warning needed |

### Import/Export (REMOVED)

> **Removed:** Import/export functionality has been removed.

## Shared Components

**File:** `compose/components/`

### BackButton

```kotlin
@Composable
fun BackButton(onBackClick: () -> Unit)
```

Standard navigation back button.

### ButtonColors

Color utilities for consistent button styling.

## Theme

**Directory:** `compose/theme/`

Material 3 theming with dynamic color support.

## Navigation

Each screen has a navigation file in `**/navigation/`:

```kotlin
// Example: AppDetailNavigation.kt
fun NavController.navigateToAppDetail(packageName: String) {
    navigate("app/$packageName")
}

fun NavGraphBuilder.appDetailScreen(
    onBackClick: () -> Unit,
) {
    composable(
        route = "app/{packageName}",
        arguments = listOf(navArgument("packageName") { type = NavType.StringType })
    ) {
        val viewModel: AppDetailViewModel = hiltViewModel()
        AppDetailScreen(onBackClick = onBackClick, viewModel = viewModel)
    }
}
```

## State Collection

All screens use lifecycle-aware state collection:

```kotlin
val state by viewModel.state.collectAsStateWithLifecycle()
```

This ensures proper lifecycle handling and avoids memory leaks.

## Image Loading

Uses Coil 3 for async image loading:

```kotlin
AsyncImage(
    model = iconUrl,
    contentDescription = null,
    modifier = Modifier.clip(RoundedCornerShape(12.dp)),
)
```

With error/fallback handling:

```kotlin
var icon by remember { mutableStateOf(app.icon?.path) }
AsyncImage(
    model = icon,
    onError = { icon = app.fallbackIcon?.path },
    ...
)
```

## Removed

| Feature | Removal Doc |
|---------|-------------|
| Package: `com.looker.droidify` | [package-rename.md](../changes/package-rename.md) |
| `CustomButtonsRow`, `CustomButtonEditor` | [custom-buttons-and-settings.md](../removal/custom-buttons-and-settings.md) |
| `TextInputSettingItem` (proxy) | [proxy-and-backup.md](../removal/proxy-and-backup.md) |
| `AutoSyncSetting`, `WarningBanner` | [auto-sync-setting.md](../removal/auto-sync-setting.md) |
| Favourites filter chip, favourite toggle button | [favourites.md](../removal/favourites.md) |
| `ReleaseItem`, `PackageItem`, versions list, anti-features section | [versions-antifeatures.md](../removal/versions-antifeatures.md) |
| FoxyDroid credits, `special_credits` string | [foxydroid-credits.md](../removal/foxydroid-credits.md) |

## Changes

| Change | Change Doc |
|--------|------------|
| App branding Droid-ify → ATD Store | [app-branding.md](../changes/app-branding.md) |
