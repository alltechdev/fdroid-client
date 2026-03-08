# WorkManager Workers

Droid-ify uses WorkManager for periodic and one-time background tasks. Workers are Hilt-injected and support foreground notifications.

## Overview

**Key Files:**
- `work/SyncWorker.kt` - Repository synchronization
- `work/CleanUpWorker.kt` - Cache cleanup
- `work/DownloadStatsWorker.kt` - Privacy stats fetching
- `work/RBLogWorker.kt` - Reproducible build logs
- `work/UnarchiveWorker.kt` - App unarchiving (Android 15+)

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    WorkManager                           │
│                                                          │
│  ┌─────────────────────────────────────────────────────┐│
│  │                  Periodic Work                       ││
│  │                                                      ││
│  │  ┌───────────────┐  ┌───────────────┐               ││
│  │  │  SyncWorker   │  │ CleanUpWorker │               ││
│  │  │  (schedule)   │  │  (schedule)   │               ││
│  │  └───────────────┘  └───────────────┘               ││
│  └─────────────────────────────────────────────────────┘│
│                                                          │
│  ┌─────────────────────────────────────────────────────┐│
│  │                  One-Time Work                       ││
│  │                                                      ││
│  │  ┌─────────────┐ ┌───────────────┐ ┌──────────────┐ ││
│  │  │ RBLogWorker │ │DownloadStats │ │UnarchiveWork │ ││
│  │  └─────────────┘ └───────────────┘ └──────────────┘ ││
│  └─────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────┘
```

## Worker Base Pattern

All workers use Hilt injection:

```kotlin
@HiltWorker
class MyWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: MyRepository,  // Injected
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // Work implementation
            Result.success()
        } catch (e: Exception) {
            e.exceptCancellation()
            Result.failure()
        }
    }
}
```

## SyncWorker

Synchronizes repository indexes in the background.

### Features

- Periodic or user-triggered sync
- Single repo or all repos
- Foreground notification with progress
- Network constraint

### Scheduling

```kotlin
companion object {
    // User-triggered sync
    fun enqueueUserSync(context: Context, repoId: Int? = null) {
        val data = Data.Builder()
            .putString(KEY_TRIGGER, TRIGGER_USER)
            .apply { if (repoId != null) putInt(KEY_REPO_ID, repoId) }
            .build()

        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setInputData(data)
            .setConstraints(defaultConstraints)
            .setBackoffCriteria(BackoffPolicy.LINEAR, 30, TimeUnit.SECONDS)
            .addTag(TAG)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork("$TAG.user", ExistingWorkPolicy.KEEP, request)
    }

    // Periodic sync
    fun schedulePeriodicSync(context: Context, repeatInterval: Duration) {
        val request = PeriodicWorkRequestBuilder<SyncWorker>(repeatInterval.toJavaDuration())
            .setInputData(Data.Builder().putString(KEY_TRIGGER, TRIGGER_PERIODIC).build())
            .setConstraints(defaultConstraints)
            .addTag(TAG)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(TAG, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    // Cancel all
    fun cancelAll(context: Context) {
        WorkManager.getInstance(context).cancelAllWorkByTag(TAG)
    }
}
```

### Constraints

```kotlin
private val defaultConstraints: Constraints = Constraints.Builder()
    .setRequiredNetworkType(NetworkType.CONNECTED)
    .build()
```

### Foreground Info

```kotlin
private fun createForegroundInfo(name: String, percent: Int): ForegroundInfo {
    val notification = NotificationCompat.Builder(applicationContext, "sync_channel")
        .setContentTitle("Syncing: $name")
        .setProgress(100, percent, percent == -1)
        .setSmallIcon(R.drawable.ic_sync)
        .setOngoing(true)
        .addAction(R.drawable.ic_cancel, "Cancel", cancelIntent)
        .build()

    return notification.toForegroundInfo(124)
}
```

## CleanUpWorker

Cleans up cached files periodically.

### Features

- Periodic cleanup schedule
- Force cleanup option
- Records last cleanup time

### Scheduling

```kotlin
companion object {
    fun scheduleCleanup(context: Context, duration: Duration) {
        val workManager = WorkManager.getInstance(context)
        val cleanup = PeriodicWorkRequestBuilder<CleanUpWorker>(duration.toJavaDuration())
            .build()

        workManager.enqueueUniquePeriodicWork(
            TAG,
            ExistingPeriodicWorkPolicy.UPDATE,
            cleanup
        )
    }

    fun force(context: Context) {
        val cleanup = OneTimeWorkRequestBuilder<CleanUpWorker>().build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork("$TAG.force", ExistingWorkPolicy.KEEP, cleanup)
    }

    fun removeAllSchedules(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(TAG)
    }
}
```

### Implementation

```kotlin
override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
    try {
        settingsRepository.setCleanupInstant()
        Cache.cleanup(applicationContext)
        Result.success()
    } catch (e: Exception) {
        Result.failure()
    }
}
```

## DownloadStatsWorker

Fetches download statistics from IzzyOnDroid for privacy metrics.

### Features

- Downloads monthly stats files
- Concurrent downloads with semaphore
- Conditional requests (If-Modified-Since)
- Foreground notification

### Implementation

```kotlin
@HiltWorker
class DownloadStatsWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val privacyRepository: PrivacyRepository,
    private val settingsRepo: SettingsRepository,
    private val downloader: Downloader,
) : CoroutineWorker(context, params) {

    val downloadSemaphores = Semaphore(2)  // Max 2 concurrent downloads

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        setForegroundAsync(createNotification().toForegroundInfo())
        fetchData()
        Result.success()
    }

    private suspend fun fetchData() = supervisorScope {
        val fileNames = generateMonthlyFileNames(lastModified)

        while (fileNames.isNotEmpty()) {
            launch {
                downloadSemaphores.withPermit {
                    val fileName = fileNames.poll() ?: return@withPermit
                    // Download and process file
                }
            }
        }
    }
}
```

### Triggering

```kotlin
companion object {
    private const val IZZY_STATS_MONTHLY =
        "https://dlstats.izzyondroid.org/iod-stats-collector/stats/basic/monthly/"

    fun fetchDownloadStats(context: Context) {
        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                "download_stats",
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<DownloadStatsWorker>().build(),
            )
    }
}
```

## RBLogWorker

Fetches Reproducible Build logs for privacy information.

### Features

- Downloads RB log index from Codeberg
- Uses conditional requests
- Parses JSON and saves to database

### Implementation

```kotlin
override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
    val target = Cache.getTemporaryFile(context)
    try {
        val lastModified = settingsRepository.getInitial().lastRbLogFetch
        val response = downloader.downloadToFile(
            url = BASE_URL,
            target = target,
            headers = {
                if (lastModified != null) ifModifiedSince(Date(lastModified))
            }
        )

        if (response is NetworkResponse.Success && response.statusCode != 304) {
            val logs = JsonParser.decodeFromString<Map<String, List<RBData>>>(target.readText())
            privacyRepository.upsertRBLogs(
                lastModified = response.lastModified ?: Date(),
                logs = logs.toLogs()
            )
        }
        Result.success()
    } finally {
        withContext(NonCancellable) { target.delete() }
    }
}
```

### Triggering

```kotlin
companion object {
    private const val BASE_URL =
        "https://codeberg.org/IzzyOnDroid/rbtlog/raw/branch/izzy/log/index.json"

    fun fetchRBLogs(context: Context) {
        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                "rb_index",
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<RBLogWorker>().build(),
            )
    }
}
```

## UnarchiveWorker

Handles app unarchiving on Android 15+ (Vanilla Ice Cream).

### Features

- Expedited work for quick response
- Downloads archived app
- Triggers installation with unarchive ID

### Implementation

```kotlin
@HiltWorker
@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
class UnarchiveWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val installManager: InstallManager
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val packageName = inputData.getString(EXTRA_UNARCHIVE_PACKAGE_NAME)
            ?: return@withContext Result.failure()
        val unarchiveId = inputData.getInt(EXTRA_UNARCHIVE_ID, -1)

        // Find matching app
        val product = Database.ProductAdapter.getArchivedApp(packageName)
            .filter { it.compatible && it.signatures.contains(sig) }
            .maxByOrNull { it.versionCode }
            ?: return@withContext Result.failure()

        // Download and install
        val result = downloadProductAndWait(applicationContext, packageName, product, repository)

        if (result.currentItem is DownloadService.State.Success) {
            installManager.install(InstallItem(
                PackageName(packageName),
                result.currentItem.release.cacheFileName,
                unarchiveId,
            ))
            Result.success()
        } else {
            Result.failure()
        }
    }

    companion object {
        fun updateNow(context: Context, packageName: String, unarchiveId: Int, allUsers: Boolean) {
            val data = Data.Builder()
                .putString(EXTRA_UNARCHIVE_PACKAGE_NAME, packageName)
                .putInt(EXTRA_UNARCHIVE_ID, unarchiveId)
                .putBoolean(EXTRA_UNARCHIVE_ALL_USERS, allUsers)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<UnarchiveWorker>()
                .setInputData(data)
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()

            WorkManager.getInstance(context).enqueue(workRequest)
        }
    }
}
```

## Worker Initialization

Workers are initialized during sync in `SyncService`:

```kotlin
fun sync(request: SyncRequest) {
    // Trigger privacy workers during sync
    RBLogWorker.fetchRBLogs(applicationContext)
    DownloadStatsWorker.fetchDownloadStats(applicationContext)

    // Continue with repo sync
    val ids = Database.RepositoryAdapter.getAll()
        .filter { it.enabled }
        .map { it.id }
    sync(ids, request)
}
```

## Work Policies

| Policy | Behavior |
|--------|----------|
| `ExistingWorkPolicy.KEEP` | Ignore new request if work exists |
| `ExistingWorkPolicy.REPLACE` | Cancel existing and start new |
| `ExistingPeriodicWorkPolicy.UPDATE` | Update schedule |
| `ExistingPeriodicWorkPolicy.KEEP` | Keep existing schedule |

## Foreground Info Helper

```kotlin
fun Notification.toForegroundInfo(id: Int): ForegroundInfo =
    ForegroundInfo(id, this)

// Usage
setForegroundAsync(notification.toForegroundInfo(NOTIFICATION_ID))
```

## Error Handling

```kotlin
override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
    try {
        // Work logic
        Result.success()
    } catch (e: CancellationException) {
        throw e  // Don't catch cancellation
    } catch (e: Exception) {
        Log.e(TAG, "Worker failed", e)
        Result.retry()  // or Result.failure()
    }
}
```

### exceptCancellation Extension

```kotlin
inline fun Throwable.exceptCancellation() {
    if (this is CancellationException) throw this
}
```

## Testing Workers

```kotlin
@Test
fun testSyncWorker() {
    val worker = TestListenableWorkerBuilder<SyncWorker>(context)
        .setInputData(workDataOf(KEY_REPO_ID to 1))
        .build()

    runBlocking {
        val result = worker.doWork()
        assertThat(result).isEqualTo(Result.success())
    }
}
```

## Changes

| Change | Change Doc |
|--------|------------|
| Package: `com.looker.droidify` → `com.atd.store` | [package-rename.md](../changes/package-rename.md) |
| App branding Droid-ify → ATD Store | [app-branding.md](../changes/app-branding.md) |
