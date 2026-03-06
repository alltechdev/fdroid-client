# Database

Droid-ify uses [Room](https://developer.android.com/training/data-storage/room) for its primary database. The database stores F-Droid repository data, app metadata, and installation state.

## Overview

| Property | Value |
|----------|-------|
| Database name | `droidify_room` |
| Current version | 2 |
| Migration strategy | Destructive (fallback) |
| Journal mode | WAL |

## Database Class

Located at `data/local/DroidifyDatabase.kt`:

```kotlin
@Database(
    version = 2,
    exportSchema = true,
    entities = [/* all entities */],
)
@TypeConverters(PermissionConverter::class, Converters::class)
abstract class DroidifyDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao
    abstract fun repoDao(): RepoDao
    abstract fun authDao(): AuthDao
    abstract fun indexDao(): IndexDao
    abstract fun rbLogDao(): RBLogDao
    abstract fun downloadStatsDao(): DownloadStatsDao
    abstract fun installedDao(): InstalledDao
}
```

## Entities

### Core Entities

| Entity | Table | Description |
|--------|-------|-------------|
| `RepoEntity` | `repository` | F-Droid repositories |
| `AppEntity` | `app` | Application metadata |
| `VersionEntity` | `version` | App versions/releases |
| `InstalledEntity` | `installed` | Locally installed apps |

### Metadata Entities

| Entity | Table | Description |
|--------|-------|-------------|
| `CategoryEntity` | `category` | App categories |
| `AntiFeatureEntity` | `anti_feature` | Anti-features (ads, tracking, etc.) |
| `AuthorEntity` | `author` | App authors |
| `DonateEntity` | `donate` | Donation links |
| `LinksEntity` | `links` | App links (website, source, etc.) |
| `MirrorEntity` | `mirror` | Repository mirrors |
| `ScreenshotEntity` | `screenshot` | App screenshots |
| `GraphicEntity` | `graphics` | Feature graphics |

### Relation Entities

| Entity | Table | Description |
|--------|-------|-------------|
| `CategoryAppRelation` | `category_app_relation` | App-to-category mapping |
| `CategoryRepoRelation` | `category_repo_relation` | Repo-to-category mapping |
| `AntiFeatureAppRelation` | `anti_features_app_relation` | App-to-antifeature mapping |
| `AntiFeatureRepoRelation` | `anti_features_repo_relation` | Repo-to-antifeature mapping |

### Localized Entities

| Entity | Table | Description |
|--------|-------|-------------|
| `LocalizedAppNameEntity` | `localized_app_name` | App names by locale |
| `LocalizedAppSummaryEntity` | `localized_app_summary` | App summaries by locale |
| `LocalizedAppDescriptionEntity` | `localized_app_description` | App descriptions by locale |
| `LocalizedAppIconEntity` | `localized_app_icon` | App icons by locale |
| `LocalizedRepoNameEntity` | `localized_repo_name` | Repo names by locale |
| `LocalizedRepoDescriptionEntity` | `localized_repo_description` | Repo descriptions by locale |
| `LocalizedRepoIconEntity` | `localized_repo_icon` | Repo icons by locale |

### Privacy Entities

| Entity | Table | Description |
|--------|-------|-------------|
| `RBLogEntity` | `rb_log` | Reproducible build logs |
| `DownloadStats` | `download_stats` | Download statistics |
| `AuthenticationEntity` | `authentication` | Encrypted repo credentials |

## DAOs

### AppDao

Primary DAO for app queries.

**Key methods:**

```kotlin
// Query apps with filtering and sorting
suspend fun query(
    sortOrder: SortOrder,
    searchQuery: String? = null,
    repoId: Int? = null,
    categoriesToInclude: List<DefaultName>? = null,
    categoriesToExclude: List<DefaultName>? = null,
    antiFeaturesToInclude: List<Tag>? = null,
    antiFeaturesToExclude: List<Tag>? = null,
    locale: String,
): List<AppMinimal>

// Stream installed apps with update status
fun installedStream(): Flow<List<AppEntity>>

// Get full app details with relations
fun queryAppEntity(packageName: String): Flow<List<AppEntityRelations>>
```

**Query implementation:**

The `query()` method uses raw SQL for complex filtering. It:
1. Joins localized tables for current locale with `en-US` fallback
2. Applies category/anti-feature filters via relation tables
3. Implements search across name, summary, description, package name
4. Sorts results with search relevance scoring

### RepoDao

Repository queries.

```kotlin
// Stream all repos
fun stream(): Flow<List<RepoEntity>>

// Get single repo
suspend fun getRepo(repoId: Int): RepoEntity?

// Get categories for a repo
fun categoriesByRepoId(repoId: Int): Flow<List<CategoryEntity>>

// Get mirrors
suspend fun mirrors(repoId: Int): List<MirrorEntity>
```

### IndexDao

Bulk insertion of synced index data.

```kotlin
// Insert complete index (called after sync)
suspend fun insertIndex(
    fingerprint: Fingerprint?,
    index: IndexV2,
    expectedRepoId: Int,
)

// Insert repo
suspend fun insertRepo(repo: RepoEntity): Long

// Insert apps, versions, categories, etc.
suspend fun insertApps(apps: List<AppEntity>)
suspend fun insertVersions(versions: List<VersionEntity>)
// ... more insert methods
```

### InstalledDao

Tracks locally installed apps.

```kotlin
// Stream single app
fun stream(packageName: String): Flow<InstalledEntity?>

// Stream all installed
fun streamAll(): Flow<List<InstalledEntity>>

// Insert/update
suspend fun insert(entity: InstalledEntity)

// Replace all (used on app startup)
suspend fun replaceAll(entities: List<InstalledEntity>)
```

### AuthDao

Encrypted repository credentials.

```kotlin
suspend fun authFor(repoId: Int): AuthenticationEntity?
suspend fun insert(entity: AuthenticationEntity)
```

### RBLogDao & DownloadStatsDao

Privacy/analytics data from external sources.

## Type Converters

### PermissionConverter

Converts `List<Permission>` to/from JSON string.

### Converters

General converters for:
- `Fingerprint` ↔ `String`
- `Instant` ↔ `Long`
- `Duration` ↔ `Long`

## Adding a New Entity

### Step 1: Create Entity Class

In `data/local/model/`:

```kotlin
@Entity(tableName = "my_table")
data class MyEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val repoId: Int,  // Foreign key
)
```

### Step 2: Add to Database

In `DroidifyDatabase.kt`, add to entities list:

```kotlin
@Database(
    entities = [
        // ... existing entities
        MyEntity::class,
    ],
)
```

### Step 3: Create DAO

In `data/local/dao/`:

```kotlin
@Dao
interface MyDao {
    @Query("SELECT * FROM my_table WHERE repoId = :repoId")
    fun streamByRepo(repoId: Int): Flow<List<MyEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: MyEntity)
}
```

### Step 4: Add to Database Class

```kotlin
abstract class DroidifyDatabase : RoomDatabase() {
    // ... existing DAOs
    abstract fun myDao(): MyDao
}
```

### Step 5: Add to DI

In `di/DatabaseModule.kt`:

```kotlin
@Singleton
@Provides
fun provideMyDao(db: DroidifyDatabase): MyDao = db.myDao()
```

### Step 6: Bump Version

Increment database version. Since destructive migration is enabled, this will clear data on upgrade:

```kotlin
@Database(
    version = 3,  // was 2
    // ...
)
```

## Schema Export

Schemas are exported to `app/schemas/` for documentation and migration testing:

```kotlin
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}
```

## Performance Optimizations

The database is configured for performance:

```kotlin
.addCallback(object : RoomDatabase.Callback() {
    override fun onOpen(db: SupportSQLiteDatabase) {
        db.query("PRAGMA synchronous = OFF")  // Faster writes
        db.query("PRAGMA journal_mode = WAL")  // Write-ahead logging
    }
})
```

**Note:** `synchronous = OFF` trades durability for speed. Data may be lost on crash, but for cache data this is acceptable.

## Legacy Database

There is also a legacy SQLite database (`database/Database.kt`) that is being migrated to Room. New features should use the Room database exclusively.
