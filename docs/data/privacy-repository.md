# Privacy Repository

Repository for privacy-related data: Reproducible Build logs and download statistics.

## Overview

**File:** `app/src/main/kotlin/com/looker/droidify/data/PrivacyRepository.kt`

Manages data related to app transparency and popularity metrics.

## Class Definition

```kotlin
class PrivacyRepository(
    private val rbDao: RBLogDao,
    private val downloadStatsDao: DownloadStatsDao,
    private val settingsRepo: SettingsRepository,
)
```

## Reproducible Build Logs

### What Are RB Logs?

Reproducible Build logs verify that an APK was built from the published source code. F-Droid maintains a log of apps that have been verified.

### Data Model

```kotlin
@Entity(tableName = "rb_log")
data class RBLogEntity(
    @PrimaryKey val packageName: String,
    val lastChecked: Long,
    val isReproducible: Boolean
)
```

### API

```kotlin
fun getRBLogs(packageName: String): Flow<List<RBLogEntity>>

suspend fun upsertRBLogs(lastModified: Date, logs: List<RBLogEntity>)
```

### Usage

```kotlin
// Observe reproducible build status for an app
privacyRepository.getRBLogs("org.mozilla.firefox")
    .collect { logs ->
        val isReproducible = logs.any { it.isReproducible }
        showReproducibilityBadge(isReproducible)
    }
```

## Download Statistics

### What Are Download Stats?

Monthly download counts per app from F-Droid's download statistics API.

### Data Model

```kotlin
@Entity(tableName = "download_stats")
data class DownloadStats(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val downloads: Long,
    val month: YearMonth
)
```

### API

```kotlin
fun getLatestDownloadStats(packageName: String): Flow<Long>

suspend fun save(downloadStats: List<DownloadStats>)
```

### Usage

```kotlin
// Show download count on app detail screen
privacyRepository.getLatestDownloadStats(packageName)
    .collect { totalDownloads ->
        binding.downloadCount.text = formatDownloadCount(totalDownloads)
    }
```

## Data Sources

### RB Logs Worker

`RBLogWorker` periodically fetches reproducible build data:

```kotlin
class RBLogWorker : CoroutineWorker {
    override suspend fun doWork(): Result {
        val logs = fetchRBLogs()
        privacyRepository.upsertRBLogs(Date(), logs)
        return Result.success()
    }
}
```

### Download Stats Worker

`DownloadStatsWorker` fetches monthly download statistics:

```kotlin
class DownloadStatsWorker : CoroutineWorker {
    override suspend fun doWork(): Result {
        val stats = fetchMonthlyStats()
        privacyRepository.save(stats)
        return Result.success()
    }
}
```

## DAOs

### RBLogDao

```kotlin
@Dao
interface RBLogDao {
    @Query("SELECT * FROM rb_log WHERE packageName = :packageName")
    fun getFlow(packageName: String): Flow<List<RBLogEntity>>

    @Upsert
    suspend fun upsert(logs: List<RBLogEntity>)
}
```

### DownloadStatsDao

```kotlin
@Dao
interface DownloadStatsDao {
    @Query("SELECT SUM(downloads) FROM download_stats WHERE packageName = :packageName")
    fun total(packageName: String): Flow<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(stats: List<DownloadStats>)
}
```

## UI Integration

### App Detail Screen

```kotlin
@Composable
fun AppPrivacyInfo(packageName: String) {
    val rbLogs by privacyRepo.getRBLogs(packageName).collectAsState(emptyList())
    val downloads by privacyRepo.getLatestDownloadStats(packageName).collectAsState(0L)

    Column {
        if (rbLogs.any { it.isReproducible }) {
            ReproducibleBadge()
        }

        if (downloads > 0) {
            Text("${formatNumber(downloads)} downloads")
        }
    }
}
```

## Dependency Injection

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object RepoModule {
    @Provides
    fun providePrivacyRepository(
        rbDao: RBLogDao,
        downloadStatsDao: DownloadStatsDao,
        settingsRepo: SettingsRepository
    ): PrivacyRepository = PrivacyRepository(rbDao, downloadStatsDao, settingsRepo)
}
```

## Threading

All operations run on `Dispatchers.IO`:

```kotlin
private val cc = Dispatchers.IO

fun getRBLogs(packageName: String): Flow<List<RBLogEntity>> =
    rbDao.getFlow(packageName).flowOn(cc)
```
