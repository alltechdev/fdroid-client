# Compose Screens

This document describes the Jetpack Compose UI layer in Droid-ify.

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                  MainComposeActivity                     │
│                                                          │
│  ┌────────────────────────────────────────────────────┐ │
│  │                    NavHost                          │ │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────────────┐   │ │
│  │  │ AppList  │ │AppDetail │ │    Settings      │   │ │
│  │  │  Screen  │ │  Screen  │ │     Screen       │   │ │
│  │  └────┬─────┘ └────┬─────┘ └────────┬─────────┘   │ │
│  │       │            │                │              │ │
│  │       ▼            ▼                ▼              │ │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────────────┐   │ │
│  │  │ AppList  │ │AppDetail │ │    Settings      │   │ │
│  │  │ViewModel │ │ViewModel │ │    ViewModel     │   │ │
│  │  └──────────┘ └──────────┘ └──────────────────┘   │ │
│  └────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────┘
```

## Screen Overview

| Screen | Route | Description |
|--------|-------|-------------|
| `HomeScreen` | `Home` | Landing page with navigation buttons |
| `AppListScreen` | `AppList` | Browse and search apps |
| `AppDetailScreen` | `AppDetail(packageName)` | App details, versions, install |
| `RepoListScreen` | `RepoList` | List of repositories |
| `RepoDetailScreen` | `RepoDetail(repoId)` | Repository details |
| `RepoEditScreen` | `RepoEdit(repoId)` | Edit repository settings |
| `SettingsScreen` | `Settings` | App settings |

## Screen Structure

Each screen follows this pattern:

```
compose/{feature}/
├── {Feature}Screen.kt          # Composable screen
├── {Feature}ViewModel.kt       # ViewModel with state
├── navigation/
│   └── {Feature}Navigation.kt  # Navigation setup
└── components/                  # Screen-specific components (optional)
```

## AppListScreen

**Location:** `compose/appList/AppListScreen.kt`

Displays a searchable, filterable list of apps.

### UI Components

```kotlin
@Composable
fun AppListScreen(
    viewModel: AppListViewModel,
    onAppClick: (String) -> Unit,
    onNavigateToRepos: () -> Unit,
    onNavigateToSettings: () -> Unit,
)
```

**Features:**
- Sticky header with category chips
- Favorites filter
- App list with icons, names, versions
- Search bar
- Sort options

### ViewModel

```kotlin
@HiltViewModel
class AppListViewModel @Inject constructor(
    private val appRepository: AppRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    val searchQuery = TextFieldState("")
    val categories: StateFlow<List<DefaultName>>
    val selectedCategories: StateFlow<Set<DefaultName>>
    val favouritesOnly: StateFlow<Boolean>
    val appsState: StateFlow<List<AppMinimal>>

    fun toggleCategory(category: DefaultName)
    fun toggleFavouritesOnly()
}
```

**State flow:**
```
searchQuery + selectedCategories + sortOrder + favouritesOnly + favouriteApps
                                    │
                                    ▼
                            combine & filter
                                    │
                                    ▼
                              appsState
```

## AppDetailScreen

**Location:** `compose/appDetail/AppDetailScreen.kt`

Shows app details, screenshots, versions, and install actions.

### Components

- App header (icon, name, summary)
- Screenshots carousel
- Description
- Version list with download buttons
- Custom action buttons
- Links (website, source, etc.)

## RepoListScreen

**Location:** `compose/repoList/RepoListScreen.kt`

Lists all configured repositories with enable/disable toggles.

### ViewModel

```kotlin
@HiltViewModel
class RepoListViewModel @Inject constructor(
    private val repoRepository: RepoRepository,
) : ViewModel() {

    val repos: StateFlow<List<Repo>>

    fun toggleRepo(repo: Repo)
    fun deleteRepo(repo: Repo)
}
```

## SettingsScreen

**Location:** `compose/settings/SettingsScreen.kt`

App configuration with various setting types.

See [settings.md](../settings.md) for detailed documentation.

## State Management

### StateFlow Pattern

ViewModels expose `StateFlow` for reactive UI updates:

```kotlin
// In ViewModel
val appsState: StateFlow<List<AppMinimal>> = combine(
    searchQueryStream,
    selectedCategories,
    sortOrderFlow,
) { query, categories, sortOrder ->
    appRepository.apps(sortOrder, query, categories)
}.asStateFlow(emptyList())

// In Composable
@Composable
fun AppListScreen(viewModel: AppListViewModel) {
    val apps by viewModel.appsState.collectAsStateWithLifecycle()
    // Use apps
}
```

### Extension Functions

The `asStateFlow` extension converts `Flow` to `StateFlow` with initial value:

```kotlin
fun <T> Flow<T>.asStateFlow(initialValue: T): StateFlow<T>
```

## Common Components

### BackButton

```kotlin
@Composable
fun BackButton(onClick: () -> Unit)
```

Standard back navigation button for top app bars.

### AsyncImage (Coil)

Used for loading app icons:

```kotlin
AsyncImage(
    model = iconUrl,
    onError = { /* fallback */ },
    contentDescription = null,
    modifier = Modifier.clip(MaterialTheme.shapes.small),
)
```

## Adding a New Screen

### Step 1: Create Screen Composable

```kotlin
// compose/myFeature/MyFeatureScreen.kt
@Composable
fun MyFeatureScreen(
    viewModel: MyFeatureViewModel,
    onBackClick: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Feature") },
                navigationIcon = { BackButton(onBackClick) },
            )
        },
    ) { padding ->
        // Content
    }
}
```

### Step 2: Create ViewModel

```kotlin
// compose/myFeature/MyFeatureViewModel.kt
@HiltViewModel
class MyFeatureViewModel @Inject constructor(
    private val repository: MyRepository,
) : ViewModel() {

    val state: StateFlow<MyState> = repository.data
        .stateIn(viewModelScope, SharingStarted.Lazily, MyState.Loading)
}
```

### Step 3: Create Navigation

```kotlin
// compose/myFeature/navigation/MyFeatureNavigation.kt
@Serializable
object MyFeature  // or data class for parameters

fun NavController.navigateToMyFeature() {
    navigate(MyFeature, navOptions {
        launchSingleTop = true
    })
}

fun NavGraphBuilder.myFeature(
    onBackClick: () -> Unit,
) {
    composable<MyFeature> {
        MyFeatureScreen(
            viewModel = hiltViewModel(),
            onBackClick = onBackClick,
        )
    }
}
```

### Step 4: Add to NavHost

In `MainComposeActivity.kt`:

```kotlin
NavHost(navController, startDestination = AppList) {
    // ... existing
    myFeature(onBackClick = { navController.popBackStack() })
}
```

## Best Practices

### 1. Hoist State to ViewModel

```kotlin
// Good - state in ViewModel
val searchQuery = viewModel.searchQuery

// Avoid - local state for shared data
var searchQuery by remember { mutableStateOf("") }
```

### 2. Use collectAsStateWithLifecycle

```kotlin
// Good - lifecycle-aware
val state by viewModel.state.collectAsStateWithLifecycle()

// Avoid - not lifecycle-aware
val state by viewModel.state.collectAsState()
```

### 3. Keep Screens Stateless

Screens should receive all state from ViewModels and emit events up:

```kotlin
@Composable
fun MyScreen(
    state: MyState,           // State down
    onAction: (Action) -> Unit,  // Events up
)
```

### 4. Use Preview Functions

```kotlin
@Preview
@Composable
private fun MyScreenPreview() {
    DroidifyTheme {
        MyScreen(
            state = MyState.Preview,
            onAction = {},
        )
    }
}
```
