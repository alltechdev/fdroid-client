# Sync Service

Background service for repository synchronization.

## Overview

**File:** `service/SyncService.kt`

A bound service that manages repository index synchronization with progress notifications, error handling, and automatic updates.

## Architecture

```
SyncService
├── Binder (public API)
│   ├── sync(request: SyncRequest)
│   ├── sync(repository: Repository)
│   ├── updateAllApps()
│   ├── setEnabled(repository, enabled)
│   ├── deleteRepository(repositoryId)
│   └── state: SharedFlow<State>
├── Task queue management
├── RepositoryUpdater integration
├── DownloadService connection (for auto-updates)
└── JobService inner class (for scheduled sync)
```

## State Model

```kotlin
sealed class State(val name: String) {
    class Connecting(appName: String) : State(appName)

    class Syncing(
        val appName: String,
        val stage: RepositoryUpdater.Stage,
        val read: DataSize,
        val total: DataSize?,
    ) : State(appName)

    data object Finish : State("")

    val progress: Int  // -∞ to +∞, represents sync progress
}
```

### Progress Calculation

| Stage | Progress |
|-------|----------|
| DOWNLOAD | 0-40% |
| PROCESS | 50% |
| MERGE | 75% |
| COMMIT | 90% |
| Finish | 100% |

## Sync Requests

```kotlin
enum class SyncRequest { AUTO, MANUAL, FORCE }
```

| Type | Behavior |
|------|----------|
| AUTO | Background sync, no foreground notification |
| MANUAL | Foreground notification, interruptible |
| FORCE | Cancels existing sync for same repos |

## Binder API

```kotlin
inner class Binder : android.os.Binder() {
    val state: SharedFlow<State>

    fun sync(request: SyncRequest)  // Sync all enabled repos
    fun sync(repository: Repository)  // Sync single repo
    suspend fun updateAllApps()  // Update all apps with available updates
    fun setEnabled(repository, enabled): Boolean
    fun deleteRepository(repositoryId): Boolean
    fun setUpdateNotificationBlocker(fragment: Fragment?)
    fun isCurrentlySyncing(repositoryId: Long): Boolean
    fun cancelAuto(): Boolean  // Cancel auto-sync tasks
}
```

## Sync Flow

```
┌─────────────────────────────────────────────────────────────┐
│                    sync(SyncRequest)                         │
│  1. Fetch RB logs (RBLogWorker)                             │
│  2. Fetch download stats (DownloadStatsWorker)              │
│  3. Get enabled repository IDs                              │
└─────────────────────────┬───────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                    handleNextTask()                          │
│  1. Pop task from queue                                     │
│  2. Get repository from database                            │
│  3. Create CurrentTask with job                             │
│  4. Emit Connecting state                                   │
└─────────────────────────┬───────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                    RepositoryUpdater.update()                │
│  1. Download index (v1 or v2)                               │
│  2. Parse JSON                                              │
│  3. Merge with database                                     │
│  4. Commit changes                                          │
└─────────────────────────┬───────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                    handleUpdates()                           │
│  1. Query for available updates                             │
│  2. Show updates notification (if enabled)                  │
│  3. Auto-update apps (if enabled)                           │
│  4. Emit Finish state                                       │
└─────────────────────────────────────────────────────────────┘
```

## Task Management

```kotlin
private class Task(val repositoryId: Long, val manual: Boolean)

private data class CurrentTask(
    val task: Task?,
    val job: CoroutinesJob,
    val hasUpdates: Boolean,
    val lastState: State,
)
```

## Notifications

### Progress Notification

Shows per-stage progress:
- Download: bytes read / total
- Processing: percentage
- Merging: items merged / total
- Commit: "Saving details..."

### Error Notification

```kotlin
private fun showNotificationError(repository: Repository, exception: Exception) {
    // Shows error type + message
    // "Copy Error Details" action for debugging
}
```

Error types:
- NETWORK - Network connectivity issues
- HTTP - HTTP status errors
- VALIDATION - Index signature/fingerprint mismatch
- PARSING - JSON parsing errors

### Update Notification

When sync completes with available updates:
```kotlin
notificationManager?.notify(
    Constants.NOTIFICATION_ID_UPDATES,
    updatesAvailableNotification(this, updates),
)
```

## Auto-Update

```kotlin
private suspend fun updateAllAppsInternal(updates: List<ProductItem>) {
    updates
        // Update Droid-ify the last (self-update last)
        .sortedBy { if (it.packageName == packageName) 1 else -1 }
        .forEach { ... }
}
```

## JobService Integration

Nested `Job` class for scheduled syncs:

```kotlin
class Job : JobService() {
    companion object {
        fun create(
            context: Context,
            periodMillis: Long,
            networkType: Int,
            isCharging: Boolean,
            isBatteryLow: Boolean,
        ): JobInfo
    }
}
```

Job constraints based on AutoSync settings:
- Network type (any/unmetered)
- Charging requirement
- Battery level

## Service States

```kotlin
private enum class Started { NO, AUTO, MANUAL }
```

| State | Behavior |
|-------|----------|
| NO | Not syncing |
| AUTO | Background sync, no foreground service |
| MANUAL | Foreground service with progress notification |

## Dependencies

```kotlin
@Inject lateinit var settingsRepository: SettingsRepository

private val downloadConnection = Connection(DownloadService::class.java)
```

## Companion Object

```kotlin
companion object {
    val syncState = MutableSharedFlow<State>()  // Global sync state

    var autoUpdating = false  // Currently auto-updating
    var autoUpdateStartedFor: List<String> = emptyList()  // Packages being updated
}
```

## Thread Safety

Uses `Mutex` for concurrent task management:

```kotlin
private val lock = Mutex()

withContext(NonCancellable) {
    lock.withLock { currentTask = null }
    handleNextTask(isNewlyModified)
}
```

## Lifecycle

- Creates notification channels on `onCreate()`
- Binds to DownloadService for auto-updates
- Handles `ACTION_CANCEL` to stop sync
- Cleans up in `onDestroy()`

## Removed

| Feature | Removal Doc |
|---------|-------------|
| `AutoSync` enum | [auto-sync-setting.md](../removal/auto-sync-setting.md) |
| Auto-sync scheduling options | [auto-sync-setting.md](../removal/auto-sync-setting.md) |
