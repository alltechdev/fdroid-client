# Repositories

This document describes the repository pattern implementation in Droid-ify. Repositories serve as the single source of truth for domain data, abstracting data sources from ViewModels.

## Overview

```
┌─────────────────────────────────────────────────────────┐
│                     ViewModels                           │
└────────────────────────┬────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────┐
│                    Repositories                          │
│  ┌──────────────┐ ┌──────────────┐ ┌────────────────┐   │
│  │ AppRepository│ │RepoRepository│ │InstalledRepo   │   │
│  └──────────────┘ └──────────────┘ └────────────────┘   │
└────────────────────────┬────────────────────────────────┘
                         │
         ┌───────────────┼───────────────┐
         ▼               ▼               ▼
    ┌─────────┐    ┌──────────┐    ┌──────────┐
    │  DAOs   │    │ Network  │    │DataStore │
    └─────────┘    └──────────┘    └──────────┘
```

## AppRepository

Provides access to application data.

**Location:** `data/AppRepository.kt`

**Dependencies:**
- `AppDao` - App database queries
- `RepoDao` - Repository data for icon URLs
- `SettingsRepository` - User preferences (locale, favorites)

### Key Methods

#### apps()

Query apps with filtering and sorting:

```kotlin
suspend fun apps(
    sortOrder: SortOrder,
    searchQuery: String? = null,
    repoId: Int? = null,
    categoriesToInclude: List<DefaultName>? = null,
    categoriesToExclude: List<DefaultName>? = null,
): List<AppMinimal>
```

**Usage:**
```kotlin
// Get all apps sorted by update date
val apps = appRepository.apps(sortOrder = SortOrder.UPDATED)

// Search with category filter
val games = appRepository.apps(
    sortOrder = SortOrder.NAME,
    searchQuery = "puzzle",
    categoriesToInclude = listOf("Games"),
)
```

#### getApp()

Get full app details as a Flow:

```kotlin
fun getApp(packageName: PackageName): Flow<List<App>>
```

Returns a list because the same package may exist in multiple repositories.

**Usage:**
```kotlin
appRepository.getApp(PackageName("org.example.app"))
    .collect { apps ->
        // apps from different repos
    }
```

#### categories

Get all available categories:

```kotlin
val categories: Flow<List<DefaultName>>
```

#### addToFavourite()

Toggle favorite status:

```kotlin
suspend fun addToFavourite(packageName: PackageName): Boolean
```

Returns `true` if added, `false` if removed.

## RepoRepository

Manages F-Droid repositories including sync operations.

**Location:** `data/RepoRepository.kt`

**Dependencies:**
- `RepoDao`, `AppDao`, `AuthDao`, `IndexDao` - Database access
- `SettingsRepository` - Enabled repos, locale
- `EncryptionStorage` - Decrypt repo credentials
- `Downloader` - Network operations
- `EntrySyncable`, `V1Syncable` - Index parsing

### Key Methods

#### repos

Stream all repositories:

```kotlin
val repos: Flow<List<Repo>>
```

#### repo()

Stream a single repository:

```kotlin
fun repo(id: Int): Flow<Repo?>
```

#### getRepo()

Get repository synchronously:

```kotlin
suspend fun getRepo(id: Int): Repo?
```

#### getEnabledRepos()

Stream only enabled repositories:

```kotlin
fun getEnabledRepos(): Flow<List<Repo>>
```

#### insertRepo()

Add a new repository:

```kotlin
suspend fun insertRepo(
    address: String,
    fingerprint: String?,
    username: String?,
    password: String?,
    name: String? = null,
    description: String? = null,
)
```

Credentials are encrypted before storage.

#### deleteRepo()

Remove a repository:

```kotlin
suspend fun deleteRepo(id: Int)
```

#### enableRepository()

Enable or disable a repository:

```kotlin
suspend fun enableRepository(repo: Repo, enable: Boolean)
```

When enabling, triggers sync. When disabling, clears cached data.

#### sync()

Sync a single repository:

```kotlin
suspend fun sync(repo: Repo, onState: ((SyncState) -> Unit)? = null): Boolean
```

**Sync flow:**
1. Download entry.json (V2) or index-v1.jar (V1)
2. Parse index data
3. Insert into database via `IndexDao`
4. Return success/failure

**State callback:**
```kotlin
repoRepository.sync(repo) { state ->
    when (state) {
        is SyncState.Downloading -> updateProgress(state.progress)
        is SyncState.JsonParsing.Success -> handleSuccess()
        is SyncState.Failed -> handleError(state.error)
    }
}
```

#### syncAll()

Sync all enabled repositories in parallel:

```kotlin
suspend fun syncAll(): Boolean
```

### Sync Implementations

The repository uses two sync implementations:

| Implementation | Format | Usage |
|----------------|--------|-------|
| `EntrySyncable` | V2 (entry.json + index-v2.json) | Modern repos |
| `V1Syncable` | V1 (index-v1.jar) | Legacy repos |

V2 is tried first, falling back to V1 if needed.

## InstalledRepository

Tracks locally installed applications.

**Location:** `data/InstalledRepository.kt`

**Dependencies:**
- `InstalledDao` - Database access

### Key Methods

#### getStream()

Stream a single installed app:

```kotlin
fun getStream(packageName: String): Flow<InstalledItem?>
```

#### getAllStream()

Stream all installed apps:

```kotlin
fun getAllStream(): Flow<List<InstalledItem>>
```

#### get()

Get installed app synchronously:

```kotlin
suspend fun get(packageName: String): InstalledItem?
```

#### put()

Insert or update installed app:

```kotlin
suspend fun put(installedItem: InstalledItem)
```

#### putAll()

Replace all installed apps (used at startup):

```kotlin
suspend fun putAll(installedItems: List<InstalledItem>)
```

#### delete()

Remove installed app record:

```kotlin
suspend fun delete(packageName: String): Int
```

## PrivacyRepository

Handles privacy-related data (reproducible build logs, download stats).

**Location:** `data/PrivacyRepository.kt`

**Dependencies:**
- `RBLogDao` - Reproducible build logs
- `DownloadStatsDao` - Download statistics
- `SettingsRepository` - Last fetch timestamps

### Key Methods

```kotlin
// Fetch and store RB logs
suspend fun fetchRBLogs()

// Fetch and store download stats
suspend fun fetchDownloadStats()

// Get RB status for a package
fun getRBStatus(packageName: String): Flow<RBStatus?>
```

## Adding a New Repository

### Step 1: Create Repository Class

```kotlin
class MyRepository @Inject constructor(
    private val myDao: MyDao,
    private val settingsRepository: SettingsRepository,
) {
    fun getAll(): Flow<List<MyEntity>> = myDao.streamAll()

    suspend fun insert(item: MyItem) {
        myDao.insert(item.toEntity())
    }
}
```

### Step 2: Add to DI

In `di/RepoModule.kt`:

```kotlin
@Provides
fun provideMyRepository(
    myDao: MyDao,
    settingsRepository: SettingsRepository,
): MyRepository = MyRepository(myDao, settingsRepository)
```

### Step 3: Use in ViewModel

```kotlin
@HiltViewModel
class MyViewModel @Inject constructor(
    private val myRepository: MyRepository,
) : ViewModel() {
    val items = myRepository.getAll()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
}
```

## Best Practices

### 1. Expose Flows for Reactive Data

```kotlin
// Good - reactive
fun getApps(): Flow<List<App>>

// Avoid for data that changes
suspend fun getApps(): List<App>
```

### 2. Use Suspend for One-Shot Operations

```kotlin
// Good - one-shot
suspend fun deleteApp(id: Int)

// Avoid for mutations
fun deleteApp(id: Int): Flow<Unit>
```

### 3. Handle Locale in Repository

```kotlin
class AppRepository(
    private val settingsRepository: SettingsRepository,
) {
    private val localeStream = settingsRepository.get { language }

    fun getApp(id: Int): Flow<App> = combine(
        appDao.get(id),
        localeStream,
    ) { app, locale ->
        app.localize(locale)
    }
}
```

### 4. Domain Model Conversion

Convert database entities to domain models in the repository:

```kotlin
fun getApps(): Flow<List<App>> = appDao.streamAll()
    .map { entities -> entities.map { it.toDomain() } }
```

## Removed

| Feature | Removal Doc |
|---------|-------------|
| Sort order UI selector (sort order still used internally) | [sort-order-ui.md](../removal/sort-order-ui.md) |
| `antiFeaturesToInclude`/`antiFeaturesToExclude` parameters | [versions-antifeatures.md](../removal/versions-antifeatures.md) |
