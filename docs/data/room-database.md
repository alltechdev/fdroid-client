# Room Database

Modern Room database for app, repository, and sync data.

## Overview

**File:** `data/local/DroidifyDatabase.kt`

Primary data storage using Room with optimized settings.

## Database Definition

```kotlin
@Database(
    version = 2,
    exportSchema = true,
    entities = [
        // Core entities
        AppEntity::class,
        RepoEntity::class,
        VersionEntity::class,
        InstalledEntity::class,
        AuthenticationEntity::class,

        // Relations
        CategoryEntity::class,
        CategoryAppRelation::class,
        CategoryRepoRelation::class,
        AntiFeatureEntity::class,
        AntiFeatureAppRelation::class,
        AntiFeatureRepoRelation::class,

        // App metadata
        AuthorEntity::class,
        DonateEntity::class,
        GraphicEntity::class,
        LinksEntity::class,
        MirrorEntity::class,
        ScreenshotEntity::class,

        // Privacy data
        RBLogEntity::class,
        DownloadStats::class,

        // Localized data
        LocalizedAppNameEntity::class,
        LocalizedAppSummaryEntity::class,
        LocalizedAppDescriptionEntity::class,
        LocalizedAppIconEntity::class,
        LocalizedRepoNameEntity::class,
        LocalizedRepoDescriptionEntity::class,
        LocalizedRepoIconEntity::class,
    ],
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

## Database Builder

```kotlin
fun droidifyDatabase(context: Context): DroidifyDatabase = Room
    .databaseBuilder(
        context = context,
        klass = DroidifyDatabase::class.java,
        name = "droidify_room",
    )
    .fallbackToDestructiveMigration(true)
    .addCallback(
        object : RoomDatabase.Callback() {
            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                db.query("PRAGMA synchronous = OFF")
                db.query("PRAGMA journal_mode = WAL")
            }
        },
    )
    .build()
```

### SQLite Optimizations

| PRAGMA | Effect |
|--------|--------|
| `synchronous = OFF` | Faster writes, slightly less durable |
| `journal_mode = WAL` | Write-Ahead Logging for concurrency |

## Entity Summary

### Core Entities

| Entity | Primary Key | Description |
|--------|-------------|-------------|
| `AppEntity` | `id: Int` | App metadata |
| `RepoEntity` | `id: Int` | Repository info |
| `VersionEntity` | composite | App release versions |
| `InstalledEntity` | `packageName` | Installed app tracking |
| `AuthenticationEntity` | `repoId` | Repo credentials |

### Relation Entities

| Entity | Purpose |
|--------|---------|
| `CategoryAppRelation` | App ↔ Category |
| `CategoryRepoRelation` | Repo ↔ Category |
| `AntiFeatureAppRelation` | App ↔ AntiFeature |
| `AntiFeatureRepoRelation` | Repo ↔ AntiFeature |

### Localized Entities

| Entity | Columns |
|--------|---------|
| `LocalizedAppNameEntity` | `appId`, `locale`, `name` |
| `LocalizedAppSummaryEntity` | `appId`, `locale`, `summary` |
| `LocalizedAppDescriptionEntity` | `appId`, `locale`, `description` |
| `LocalizedAppIconEntity` | `appId`, `locale`, `icon_name` |
| `LocalizedRepoNameEntity` | `repoId`, `locale`, `name` |
| `LocalizedRepoDescriptionEntity` | `repoId`, `locale`, `description` |
| `LocalizedRepoIconEntity` | `repoId`, `locale`, `icon` |

## Type Converters

### Converters.kt

```kotlin
object Converters {
    @TypeConverter
    fun fromLocalizedString(value: LocalizedString): String =
        JsonParser.encodeToString(localizedStringSerializer, value)

    @TypeConverter
    fun toLocalizedString(value: String): LocalizedString =
        JsonParser.decodeFromString(localizedStringSerializer, value)

    @TypeConverter
    fun fromStringList(value: List<String>): String =
        JsonParser.encodeToString(stringListSerializer, value)

    @TypeConverter
    fun toStringList(value: String): List<String> =
        JsonParser.decodeFromString(stringListSerializer, value)
}
```

### PermissionConverter.kt

```kotlin
object PermissionConverter {
    @TypeConverter
    fun fromPermissionV2List(value: List<PermissionV2>): String =
        JsonParser.encodeToString(permissionListSerializer, value)

    @TypeConverter
    fun toPermissionV2List(value: String): List<PermissionV2> =
        JsonParser.decodeFromString(permissionListSerializer, value)
}
```

## Schema Diagram

```
┌─────────────┐     ┌─────────────────┐     ┌──────────────┐
│ repository  │────<│ app             │────<│ version      │
│ id          │     │ id              │     │ appId        │
│ address     │     │ packageName     │     │ versionCode  │
│ fingerprint │     │ repoId          │     │ versionName  │
│ timestamp   │     │ added           │     │ apk_size     │
└──────┬──────┘     │ lastUpdated     │     │ hash         │
       │            │ authorId        │     └──────────────┘
       │            └────────┬────────┘
       │                     │
       ▼                     ▼
┌─────────────────┐   ┌─────────────────┐
│ mirror          │   │ category_app    │
│ repoId          │   │ id (appId)      │
│ url             │   │ defaultName     │
│ isPrimary       │   └─────────────────┘
└─────────────────┘
                      ┌─────────────────┐
                      │ localized_*     │
                      │ appId/repoId    │
                      │ locale          │
                      │ value           │
                      └─────────────────┘
```

## Migration Strategy

```kotlin
.fallbackToDestructiveMigration(true)
```

Database wipes on schema changes since data can be re-synced from repositories.
