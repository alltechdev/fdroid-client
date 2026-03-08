# Favorites

The favorites feature allows users to bookmark apps for quick access across the app list and dedicated views.

## Overview

**Key Files:**
- `datastore/Settings.kt` - Contains `favouriteApps: Set<String>`
- `datastore/SettingsRepository.kt` - Favorite management methods
- `ui/favourites/FavouritesViewModel.kt` - Legacy favorites view
- `compose/appList/AppListViewModel.kt` - Favorites filter in Compose
- `compose/appDetail/AppDetailScreen.kt` - Toggle favorite action

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    Favorites System                      │
│                                                          │
│  ┌─────────────────────────────────────────────────────┐│
│  │              SettingsRepository                      ││
│  │                                                      ││
│  │  favouriteApps: Set<String>                         ││
│  │  (persisted via DataStore)                          ││
│  └─────────────────────────────────────────────────────┘│
│                         │                                │
│           ┌─────────────┴─────────────┐                 │
│           │                           │                 │
│           ▼                           ▼                 │
│  ┌────────────────┐       ┌────────────────────────┐   │
│  │  AppListScreen │       │   AppDetailScreen      │   │
│  │                │       │                        │   │
│  │ Filter toggle  │       │  Add/Remove favorite   │   │
│  │ "Show favs"    │       │  via heart button      │   │
│  └────────────────┘       └────────────────────────┘   │
└─────────────────────────────────────────────────────────┘
```

## Data Model

Favorites are stored as a set of package names in Settings:

```kotlin
@Serializable
data class Settings(
    // ... other settings
    val favouriteApps: Set<String> = emptySet(),
)
```

## SettingsRepository API

```kotlin
interface SettingsRepository {
    val data: Flow<Settings>

    suspend fun toggleFavourite(packageName: String)

    // Extension to check if app is favorited
    fun isFavourite(packageName: String): Flow<Boolean> =
        data.map { packageName in it.favouriteApps }
}
```

### Implementation

```kotlin
class PreferenceSettingsRepository @Inject constructor(
    private val dataStore: DataStore<Settings>,
) : SettingsRepository {

    override suspend fun toggleFavourite(packageName: String) {
        dataStore.updateData { settings ->
            val current = settings.favouriteApps
            val updated = if (packageName in current) {
                current - packageName
            } else {
                current + packageName
            }
            settings.copy(favouriteApps = updated)
        }
    }
}
```

## Using Favorites

### In App Detail Screen

Toggle favorite status:

```kotlin
@Composable
fun AppDetailScreen(viewModel: AppDetailViewModel) {
    val isFavorite by viewModel.isFavorite.collectAsStateWithLifecycle()

    TopAppBar(
        actions = {
            IconButton(onClick = viewModel::toggleFavorite) {
                Icon(
                    imageVector = if (isFavorite) {
                        Icons.Filled.Favorite
                    } else {
                        Icons.Outlined.FavoriteBorder
                    },
                    contentDescription = "Toggle favorite",
                )
            }
        }
    )
}
```

### In ViewModel

```kotlin
@HiltViewModel
class AppDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val packageName: String = savedStateHandle["packageName"]!!

    val isFavorite: StateFlow<Boolean> = settingsRepository
        .get { packageName in favouriteApps }
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    fun toggleFavorite() {
        viewModelScope.launch {
            settingsRepository.toggleFavourite(packageName)
        }
    }
}
```

## Filtering by Favorites

### AppListViewModel

```kotlin
@HiltViewModel
class AppListViewModel @Inject constructor(
    private val appRepository: AppRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _favouritesOnly = MutableStateFlow(false)
    val favouritesOnly: StateFlow<Boolean> = _favouritesOnly.asStateFlow()

    private val favouriteApps = settingsRepository.get { favouriteApps }

    val appsState: StateFlow<List<AppMinimal>> = combine(
        searchQueryStream,
        selectedCategories,
        sortOrderFlow,
        favouritesOnly,
        favouriteApps,
    ) { query, categories, sortOrder, showOnlyFavs, favs ->
        val apps = appRepository.apps(sortOrder, query, categories)
        if (showOnlyFavs) {
            apps.filter { it.packageName in favs }
        } else {
            apps
        }
    }.asStateFlow(emptyList())

    fun toggleFavouritesOnly() {
        _favouritesOnly.update { !it }
    }
}
```

### UI Toggle

```kotlin
@Composable
fun AppListScreen(viewModel: AppListViewModel) {
    val showFavoritesOnly by viewModel.favouritesOnly.collectAsStateWithLifecycle()

    FilterChip(
        selected = showFavoritesOnly,
        onClick = viewModel::toggleFavouritesOnly,
        label = { Text("Favorites") },
        leadingIcon = {
            Icon(
                imageVector = if (showFavoritesOnly) {
                    Icons.Filled.Favorite
                } else {
                    Icons.Outlined.FavoriteBorder
                },
                contentDescription = null,
            )
        },
    )
}
```

## Legacy Favorites Fragment

The legacy UI has a dedicated favorites tab:

```kotlin
@HiltViewModel
class FavouritesViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
) : ViewModel() {

    val favouriteApps: StateFlow<List<ProductItem>> =
        settingsRepository
            .get { favouriteApps }
            .map { favourites ->
                favourites.mapNotNull { app ->
                    val products = Database.ProductAdapter.get(app, null)
                    val product = products.firstOrNull() ?: return@mapNotNull null
                    val installed = Database.InstalledAdapter.get(app, null)

                    product.item().apply {
                        this.installedVersion = installed?.version.orEmpty()
                        this.canUpdate = product.canUpdate(installed)
                    }
                }
            }.asStateFlow(emptyList())
}
```

## Persistence

Favorites persist across app restarts via DataStore:

```kotlin
// Settings stored in: data/data/com.atd.store/files/datastore/settings.json
{
    "favouriteApps": [
        "com.example.app1",
        "org.fdroid.fdroid",
        "com.termux"
    ],
    // ... other settings
}
```

## Sync Considerations

Favorites are:
- Local only (not synced with servers)
- Preserved during app updates
- Exportable via Settings export
- Package name based (survive app reinstalls from different repos)

## Adding to Favorites Programmatically

```kotlin
@Inject lateinit var settingsRepository: SettingsRepository

// Toggle (add if not present, remove if present)
settingsRepository.toggleFavourite("com.example.app")

// Check status
settingsRepository.data
    .map { "com.example.app" in it.favouriteApps }
    .collect { isFavorite ->
        // Use status
    }
```

## Best Practices

1. **Use package names** - Favorites are identified by package name, ensuring they work across repositories
2. **Handle missing apps** - A favorited app might be removed from repositories; filter nulls
3. **Performance** - The favorites set is small, so filtering is cheap
4. **UI feedback** - Provide clear visual indication of favorite status

## Removed

| Feature | Removal Doc |
|---------|-------------|
| Package: `com.looker.droidify` | [package-rename.md](../changes/package-rename.md) |
| `Favourites` | [favourites.md](../removal/favourites.md) |
