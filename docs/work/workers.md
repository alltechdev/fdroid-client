# WorkManager Workers

Background workers for scheduled and on-demand tasks.

## Overview

**Directory:** `work/`

Uses AndroidX WorkManager with Hilt integration for dependency injection.

## CleanUpWorker

Periodic cache cleanup.

```kotlin
@HiltWorker
class CleanUpWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val settingsRepository: SettingsRepository,
) : CoroutineWorker(context, workerParams)
```

### Scheduling

```kotlin
companion object {
    fun schedule(context: Context, interval: Duration) {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()
        val request = PeriodicWorkRequestBuilder<CleanUpWorker>(
            interval.inWholeMinutes, TimeUnit.MINUTES
        ).setConstraints(constraints).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            TAG, ExistingPeriodicWorkPolicy.UPDATE, request
        )
    }

    fun force(context: Context) {
        WorkManager.getInstance(context).enqueue(
            OneTimeWorkRequestBuilder<CleanUpWorker>().build()
        )
    }
}
```

## SyncWorker

Repository synchronization.

```kotlin
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repoRepository: RepoRepository,
) : CoroutineWorker(context, workerParams)
```

### Auto-Sync Modes

| Mode | Constraints |
|------|-------------|
| `ALWAYS` | Any network |
| `WIFI_ONLY` | Unmetered network |
| `WIFI_PLUGGED_IN` | Unmetered + charging |
| `NEVER` | Manual only |

### Scheduling

```kotlin
fun schedule(context: Context, autoSync: AutoSync) {
    val constraints = when (autoSync) {
        AutoSync.WIFI_ONLY -> Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED)
            .build()
        AutoSync.WIFI_PLUGGED_IN -> Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED)
            .setRequiresCharging(true)
            .build()
        else -> Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
    }
}
```

## DownloadStatsWorker

Fetches download statistics from F-Droid API.

```kotlin
@HiltWorker
class DownloadStatsWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val privacyRepository: PrivacyRepository,
    private val downloader: Downloader,
) : CoroutineWorker(context, workerParams)
```

### Work

```kotlin
override suspend fun doWork(): Result {
    val stats = fetchDownloadStats()
    privacyRepository.save(stats)
    return Result.success()
}
```

## RBLogWorker

Fetches Reproducible Build verification logs.

```kotlin
@HiltWorker
class RBLogWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val privacyRepository: PrivacyRepository,
    private val downloader: Downloader,
) : CoroutineWorker(context, workerParams)
```

Fetches data about which apps have been verified as reproducibly built.

## UnarchiveWorker

Android 15+ app unarchiving support.

**File:** `work/UnarchiveWorker.kt`

```kotlin
@HiltWorker
@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
class UnarchiveWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val installManager: InstallManager
) : CoroutineWorker(context, workerParams)
```

### Triggering

```kotlin
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
```

### Unarchive Flow

```kotlin
override suspend fun doWork(): Result {
    // 1. Get archived package info
    val packageInfo = packageManager.getPackageInfo(
        packageName,
        PackageManager.PackageInfoFlags.of(MATCH_ARCHIVED_PACKAGES)
    )

    // 2. Find matching product in database
    val product = Database.ProductAdapter.getArchivedApp(packageName)
        .filter { it.compatible && it.signatures.contains(sig) }
        .maxByOrNull { it.versionCode }

    // 3. Download the APK
    val result = downloadProductAndWait(context, packageName, product, repository)

    // 4. Install with unarchive ID
    installManager.install(
        InstallItem(
            PackageName(packageName),
            result.currentItem.release.cacheFileName,
            unarchiveId,  // Android 15+ unarchive support
        )
    )
}
```

## Hilt Integration

All workers use `@HiltWorker` annotation:

```kotlin
@HiltWorker
class MyWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: MyRepository,  // Regular injection
) : CoroutineWorker(context, workerParams)
```

## Worker Lifecycle

```
┌─────────────────────────────────────────────────────────────┐
│                    WorkManager                               │
│  - Schedules based on constraints                           │
│  - Handles retries and backoff                              │
└─────────────────────────┬───────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                    CoroutineWorker                           │
│  - doWork() runs on Dispatchers.Default                     │
│  - Returns Result.success/failure/retry                     │
└─────────────────────────┬───────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                    Injected Dependencies                     │
│  - Repositories                                             │
│  - Downloader                                               │
│  - InstallManager                                           │
└─────────────────────────────────────────────────────────────┘
```

## Removed

| Feature | Removal Doc |
|---------|-------------|
| `AutoSync` work constraints | [auto-sync-setting.md](../removal/auto-sync-setting.md) |
| `toWorkConstraints()` extension | [dead-code-cleanup.md](../removal/dead-code-cleanup.md) |
