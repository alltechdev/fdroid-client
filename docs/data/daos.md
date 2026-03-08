# Data Access Objects (DAOs)

Room DAOs for database operations.

## Overview

**Directory:** `data/local/dao/`

## AppDao

Queries for app data with localization support.

### Dynamic Queries

```kotlin
@Dao
interface AppDao {
    @RawQuery
    suspend fun _rawQueryAppMinimal(query: SimpleSQLiteQuery): List<AppMinimalRow>

    suspend fun query(
        sortOrder: SortOrder,
        searchQuery: String? = null,
        repoId: Int? = null,
        categoriesToInclude: List<DefaultName>? = null,
        categoriesToExclude: List<DefaultName>? = null,
        locale: String,
    ): List<AppMinimal>
}
```

### Query Builder

The `searchQueryMinimal` function builds SQL dynamically:

```kotlin
SELECT
    app.id AS appId,
    app.packageName AS packageName,
    COALESCE(n_loc.name, n_en.name) AS name,
    COALESCE(s_loc.summary, s_en.summary) AS summary,
    repo.address AS baseAddress,
    COALESCE(i_loc.icon_name, i_en.icon_name) AS iconName,
    (SELECT v.versionName FROM version v
     WHERE v.appId = app.id
     ORDER BY v.versionCode DESC LIMIT 1) AS suggestedVersion
FROM app
JOIN repository AS repo ON app.repoId = repo.id
LEFT JOIN localized_app_name AS n_loc ON n_loc.appId = app.id AND n_loc.locale = ?
LEFT JOIN localized_app_name AS n_en ON n_en.appId = app.id AND n_en.locale = 'en-US'
-- ... more joins for localized data
```

### Localization Fallback

Uses `COALESCE` to fall back to `en-US` when requested locale unavailable:

```sql
COALESCE(n_loc.name, n_en.name) AS name
```

### Installed Apps Query

```kotlin
@Query("""
    SELECT app.*
    FROM app
    LEFT JOIN installed ON app.packageName = installed.packageName
    LEFT JOIN version ON version.appId = app.id
    WHERE installed.packageName IS NOT NULL
    ORDER BY
        CASE WHEN version.versionCode > installed.versionCode THEN 1 ELSE 2 END,
        app.lastUpdated DESC
""")
fun installedStream(): Flow<List<AppEntity>>
```

Orders updates before non-updates.

## RepoDao

Repository and category queries.

```kotlin
@Dao
interface RepoDao {
    @Query("SELECT * FROM repository")
    fun stream(): Flow<List<RepoEntity>>

    @Query("SELECT * FROM repository WHERE id = :repoId")
    fun repo(repoId: Int): Flow<RepoEntity?>

    @Query("SELECT * FROM category GROUP BY category.defaultName")
    fun categories(): Flow<List<CategoryEntity>>

    @Query("""
        SELECT * FROM category
        JOIN category_repo_relation ON category.defaultName = category_repo_relation.defaultName
        WHERE category_repo_relation.id = :repoId
    """)
    @RewriteQueriesToDropUnusedColumns
    fun categoriesByRepoId(repoId: Int): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM mirror WHERE repoId = :repoId")
    suspend fun mirrors(repoId: Int): List<MirrorEntity>

    @Query("UPDATE repository SET timestamp = NULL WHERE id = :id")
    suspend fun resetTimestamp(id: Int)
}
```

## IndexDao

Bulk insert operations for repository sync.

### Main Insert Method

```kotlin
@Transaction
suspend fun insertIndex(
    fingerprint: Fingerprint,
    index: IndexV2,
    expectedRepoId: Int = 0,
) {
    // 1. Upsert repository
    val repoId = upsertRepo(repo.repoEntity(...))

    // 2. Insert repo-scoped data (mirrors, categories, antifeatures)
    insertRepoScopeData(repoId, index)

    // 3. Batch collect authors and get IDs
    val authorIdsByAuthor = mutableMapOf<AuthorEntity, Int>()
    packageEntries.forEach { author ->
        authorIdsByAuthor[author] = upsertAuthor(author)
    }

    // 4. Separate apps into update vs insert batches
    val existing = appIdsByPackageNames(repoId, packageNames)
    val toUpdate = appEntities.filter { ... }
    val toInsert = appEntities.filter { ... }

    // 5. Batch insert all related data
    insertVersions(allVersions)
    insertCategoryAppRelation(allCategoryAppRelations)
    insertLocalizedAppData(names, summaries, descriptions, icons)
    // ...
}
```

### Upsert Pattern

```kotlin
@Transaction
suspend fun upsertRepo(repoEntity: RepoEntity): Int {
    val id = insertRepo(repoEntity)  // Returns -1 if exists
    return if (id == -1L) {
        repoEntity.also { updateRepo(it) }.id
    } else {
        id.toInt()
    }
}
```

### Batch Operations

```kotlin
@Insert(onConflict = OnConflictStrategy.IGNORE)
suspend fun insertApps(apps: List<AppEntity>): List<Long>

@Upsert
suspend fun upsertApps(apps: List<AppEntity>)

@Insert(onConflict = OnConflictStrategy.REPLACE)
suspend fun insertVersions(versions: List<VersionEntity>)
```

## InstalledDao

Track installed apps.

```kotlin
@Dao
interface InstalledDao {
    @Query("SELECT * FROM installed")
    fun stream(): Flow<List<InstalledEntity>>

    @Query("SELECT * FROM installed WHERE packageName = :packageName")
    fun installed(packageName: String): Flow<InstalledEntity?>

    @Upsert
    suspend fun upsert(entity: InstalledEntity)

    @Query("DELETE FROM installed WHERE packageName = :packageName")
    suspend fun delete(packageName: String)
}
```

## AuthDao

Repository authentication credentials.

```kotlin
@Dao
interface AuthDao {
    @Query("SELECT * FROM authentication WHERE repoId = :repoId")
    suspend fun getAuth(repoId: Int): AuthenticationEntity?

    @Upsert
    suspend fun upsert(entity: AuthenticationEntity)

    @Query("DELETE FROM authentication WHERE repoId = :repoId")
    suspend fun delete(repoId: Int)
}
```

## RBLogDao

Reproducible build logs.

```kotlin
@Dao
interface RBLogDao {
    @Query("SELECT * FROM rb_log WHERE packageName = :packageName")
    fun getFlow(packageName: String): Flow<List<RBLogEntity>>

    @Upsert
    suspend fun upsert(logs: List<RBLogEntity>)
}
```

## DownloadStatsDao

Download statistics.

```kotlin
@Dao
interface DownloadStatsDao {
    @Query("SELECT SUM(downloads) FROM download_stats WHERE packageName = :packageName")
    fun total(packageName: String): Flow<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(stats: List<DownloadStats>)
}
```

## Query Patterns

### Flow for Observation
```kotlin
fun stream(): Flow<List<Entity>>  // Auto-updates on changes
```

### Suspend for One-shot
```kotlin
suspend fun get(): Entity?  // Single query
```

### Transaction for Atomicity
```kotlin
@Transaction
suspend fun insertIndex(...)  // Multiple operations atomic
```

## Removed

| Feature | Removal Doc |
|---------|-------------|
| Package: `com.looker.droidify` | [package-rename.md](../changes/package-rename.md) |
| Favourite app DAO methods | [favourites.md](../removal/favourites.md) |
| `antiFeaturesToInclude`/`antiFeaturesToExclude` parameters | [versions-antifeatures.md](../removal/versions-antifeatures.md) |
| Anti-feature insertion methods in `IndexDao` | [versions-antifeatures.md](../removal/versions-antifeatures.md) |
