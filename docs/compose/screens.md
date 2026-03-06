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

Full app details with versions and screenshots.

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
2. **CustomButtonsRow** - User-defined action buttons
3. **ScreenshotsRow** - Horizontal carousel with async loading
4. **CategoriesRow** - Filter chips for categories
5. **Summary** - Bold app summary text
6. **Description** - HTML-rendered description with link handling
7. **PackageItem** - Version list with suggested badge

### AppDetailViewModel

```kotlin
@HiltViewModel
class AppDetailViewModel @Inject constructor(
    private val appRepository: AppRepository,
    private val repoRepository: RepoRepository,
    private val customButtonRepository: CustomButtonRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel()
```

Uses `SavedStateHandle` to retrieve `packageName` from navigation arguments.

## RepoListScreen

**File:** `compose/repoList/RepoListScreen.kt`

Repository listing with enable/disable toggles.

```kotlin
@Composable
fun RepoListScreen(
    viewModel: RepoListViewModel,
    onRepoClick: (Int) -> Unit,
    onBackClick: () -> Unit,
)
```

### RepoItem

```kotlin
@Composable
private fun RepoItem(
    onClick: () -> Unit,
    onToggle: () -> Unit,
    repo: Repo,
)
```

- 80dp row height
- Grayscale filter for disabled repos
- FilledIconToggleButton for enable/disable

### Grayscale Effect

```kotlin
val GrayScaleColorFilter = ColorFilter.colorMatrix(
    ColorMatrix().apply { setToSaturation(0f) }
)
```

## RepoEditScreen

**File:** `compose/repoEdit/RepoEditScreen.kt`

Add or edit repository form.

```kotlin
@Composable
fun RepoEditScreen(
    repoId: Int?,
    onBackClick: () -> Unit,
    viewModel: RepoEditViewModel = hiltViewModel(),
)
```

### Form Fields

| Field | Type | Validation |
|-------|------|------------|
| Address | OutlinedTextField | URL format |
| Fingerprint | OutlinedTextField | Hex format |
| Username | OutlinedTextField | Required if auth |
| Password | OutlinedTextField | PasswordVisualTransformation |

### Error State

```kotlin
data class ErrorState(
    val addressError: String? = null,
    val fingerprintError: String? = null,
    val usernameError: String? = null,
    val passwordError: String? = null,
) {
    val hasError: Boolean get() = ...
}
```

### Features
- Animated authentication fields visibility
- Loading overlay with progress indicator
- Skip validation button

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
| Updates | Auto-update, Notify, Unstable, Incompatible, Signature |
| Sync | Auto-sync mode, Cleanup interval, Force cleanup |
| Install | Installer type, Legacy component, Delete APK |
| Proxy | Type, Host, Port |
| Import/Export | Settings, Repos, Custom buttons |
| Custom Buttons | Add/Edit/Remove/Export/Import |
| Credits | FoxyDroid, Droid-ify version |

### Setting Components

**File:** `compose/settings/components/`

| Component | Usage |
|-----------|-------|
| `SettingHeader` | Section headers |
| `SwitchSettingItem` | Toggle settings |
| `SelectionSettingItem` | Dropdown/dialog selection |
| `ActionSettingItem` | Clickable actions |
| `TextInputSettingItem` | Text input fields |
| `CustomButtonsSettingItem` | Custom button management |
| `WarningBanner` | Battery optimization warning |
| `CustomButtonEditor` | Button add/edit dialog |

### Import/Export

Uses `ActivityResultContracts` for file picking:

```kotlin
val exportSettingsLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CreateDocument(BACKUP_MIME_TYPE),
) { uri -> uri?.let { viewModel.exportSettings(it) } }
```

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
