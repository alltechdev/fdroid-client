# Data Repositories

Repository pattern implementations for data access.

## Overview

**Directory:** `data/`

Clean architecture repository classes that provide domain-level data access, combining Room DAOs, settings, and transformations.

## AppRepository

**File:** `data/AppRepository.kt`

App data access with locale-aware queries and favorites management.

```kotlin
class AppRepository @Inject constructor(
    private val appDao: AppDao,
    private val repoDao: RepoDao,
    private val settingsRepository: SettingsRepository,
)
```

### Query Apps

```kotlin
suspend fun apps(
    sortOrder: SortOrder,
    searchQuery: String? = null,
    repoId: Int? = null,
    categoriesToInclude: List<DefaultName>? = null,
    categoriesToExclude: List<DefaultName>? = null,
    antiFeaturesToInclude: List<Tag>? = null,
    antiFeaturesToExclude: List<Tag>? = null,
): List<AppMinimal>
```

**Parameters:**
| Parameter | Description |
|-----------|-------------|
| `sortOrder` | ALPHABETIC, UPDATED, ADDED |
| `searchQuery` | Full-text search filter |
| `repoId` | Filter to specific repository |
| `categoriesToInclude` | Only apps with these categories |
| `categoriesToExclude` | Exclude apps with these categories |
| `antiFeaturesToInclude` | Only apps with these anti-features |
| `antiFeaturesToExclude` | Exclude apps with these anti-features |

### Get Single App

```kotlin
fun getApp(packageName: PackageName): Flow<List<App>>
```

Returns a Flow of all versions of an app across repositories, transformed to domain model with current locale.

### Categories

```kotlin
val categories: Flow<List<DefaultName>>
```

Stream of all available categories from enabled repositories.

### Favorites

```kotlin
suspend fun addToFavourite(packageName: PackageName): Boolean
```

Toggles favorite status, returns `true` if added to favorites.

## RepoRepository

**File:** `data/RepoRepository.kt`

Repository management with sync, authentication, and index handling.

```kotlin
class RepoRepository @Inject constructor(
    encryptionStorage: EncryptionStorage,
    downloader: Downloader,
    @param:ApplicationContext private val context: Context,
    @IoDispatcher syncDispatcher: CoroutineDispatcher,
    private val repoDao: RepoDao,
    private val authDao: AuthDao,
    private val indexDao: IndexDao,
    private val settingsRepository: SettingsRepository,
    private val appDao: AppDao,
)
```

### Syncables

Maintains sync implementations for different index formats:

```kotlin
private val v2Syncable = EntrySyncable(...)  // V2 format
private val v1Syncable = V1Syncable(...)      // V1 format (legacy)
private val localSyncable = LocalSyncable(...)  // Local files
```

### Get Repository

```kotlin
suspend fun getRepo(id: Int): Repo?
fun repo(id: Int): Flow<Repo?>
```

Retrieves repository with:
- Localized name/description
- Authentication (decrypted)
- Mirrors list
- Enabled status

### List Repositories

```kotlin
val repos: Flow<List<Repo>>  // All repositories
val addresses: Flow<Set<String>>  // All addresses including mirrors
fun getEnabledRepos(): Flow<List<Repo>>  // Only enabled
```

### Insert Repository

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

Creates repository with:
1. RepoEntity in database
2. Localized name/description if provided
3. Encrypted authentication if provided

### Enable/Disable

```kotlin
suspend fun enableRepository(repo: Repo, enable: Boolean)
```

When enabling:
- Updates settings
- Triggers sync via SyncWorker

When disabling:
- Resets timestamp
- Deletes cached index files
- Removes apps from this repo

### Sync

```kotlin
suspend fun sync(repo: Repo, onState: ((SyncState) -> Unit)? = null): Boolean
```

Syncs a single repository:
1. Calls V2 syncable
2. On success, inserts index into database
3. Returns success status

```kotlin
suspend fun syncAll(): Boolean
```

Syncs all enabled repositories in parallel using `supervisorScope`.

## InstalledRepository

**File:** `data/InstalledRepository.kt`

Tracks installed apps on device.

```kotlin
class InstalledRepository @Inject constructor(
    private val installedDao: InstalledDao,
)
```

### API

```kotlin
fun getStream(packageName: String): Flow<InstalledItem?>
fun getAllStream(): Flow<List<InstalledItem>>
suspend fun get(packageName: String): InstalledItem?
suspend fun put(installedItem: InstalledItem)
suspend fun putAll(installedItems: List<InstalledItem>)
suspend fun delete(packageName: String): Int
```

### InstalledItem

```kotlin
data class InstalledItem(
    val packageName: String,
    val versionCode: Long,
    val versionName: String,
    val signature: String,
)
```

## PrivacyRepository

**File:** `data/PrivacyRepository.kt`

Stores privacy-related data like download statistics and reproducible build logs.

## StringHandler

**File:** `data/StringHandler.kt`

Provides localized strings to ViewModels without direct context access.

```kotlin
class StringHandler @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    fun getString(@StringRes id: Int): String
    fun getString(@StringRes id: Int, vararg args: Any): String
}
```

## Data Flow

```
┌─────────────────────────────────────────────────────────────┐
│                    ViewModel                                 │
│  - Collects flows as StateFlow                              │
│  - Calls repository methods                                 │
└─────────────────────────┬───────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                    Repository                                │
│  - Combines DAO queries                                     │
│  - Applies business logic                                   │
│  - Transforms to domain models                              │
└─────────────────────────┬───────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                    Room DAOs                                 │
│  - AppDao, RepoDao, AuthDao, etc.                           │
│  - SQLite queries                                           │
└─────────────────────────────────────────────────────────────┘
```

## Dependency Injection

Repositories are provided as singletons via Hilt:

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object RepoModule {
    // Repositories auto-provided via @Inject constructor
}
```
