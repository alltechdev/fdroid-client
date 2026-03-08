# Background Services

Droid-ify uses Android services for long-running operations like repository sync and APK downloads. These run as foreground services with user-visible notifications.

## Overview

**Key Files:**
- `service/ConnectionService.kt` - Base service class
- `service/Connection.kt` - Service binding helper
- `service/SyncService.kt` - Repository synchronization
- `service/DownloadService.kt` - APK download management

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    Service Layer                         │
│                                                          │
│  ┌───────────────────────────────────────────────────┐  │
│  │              ConnectionService (base)              │  │
│  │                                                    │  │
│  │  - lifecycleScope (CoroutineScope)                │  │
│  │  - Binder implementation                          │  │
│  └───────────────────────────────────────────────────┘  │
│                    ▲                ▲                    │
│                    │                │                    │
│  ┌─────────────────┴───┐  ┌────────┴─────────────────┐  │
│  │    SyncService      │  │    DownloadService       │  │
│  │                     │  │                          │  │
│  │ - Repo sync queue   │  │ - APK download queue     │  │
│  │ - Index updates     │  │ - Progress tracking      │  │
│  │ - Auto-update       │  │ - Validation             │  │
│  └─────────────────────┘  └──────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
```

## ConnectionService

Base class for bound services with coroutine support:

```kotlin
abstract class ConnectionService<T : IBinder> : Service() {

    private val supervisorJob = SupervisorJob()
    val lifecycleScope = CoroutineScope(Dispatchers.Main + supervisorJob)

    abstract override fun onBind(intent: Intent): T

    override fun onDestroy() {
        super.onDestroy()
        lifecycleScope.cancel()
    }
}
```

### Features

- Built-in `CoroutineScope` tied to service lifecycle
- `SupervisorJob` for independent coroutine failure handling
- Generic binder type parameter

## Connection Helper

Type-safe service binding:

```kotlin
class Connection<B : IBinder, S : ConnectionService<B>>(
    private val serviceClass: Class<S>,
    private val onBind: ((Connection<B, S>, B) -> Unit)? = null,
    private val onUnbind: ((Connection<B, S>, B) -> Unit)? = null
) : ServiceConnection {
    var binder: B? = null
        private set

    fun bind(context: Context) {
        context.bindService(Intent(context, serviceClass), this, Context.BIND_AUTO_CREATE)
    }

    fun unbind(context: Context) {
        context.unbindService(this)
    }
}
```

### Usage

```kotlin
val syncConnection = Connection(
    serviceClass = SyncService::class.java,
    onBind = { connection, binder ->
        binder.sync(SyncRequest.AUTO)
    },
    onUnbind = { _, binder ->
        binder.cancelAuto()
    }
)

// Bind to service
syncConnection.bind(context)

// Access binder
syncConnection.binder?.sync(SyncRequest.MANUAL)

// Unbind when done
syncConnection.unbind(context)
```

## SyncService

Handles repository synchronization with queue management.

### State

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
}
```

### Sync Stages

| Stage | Description | Progress Range |
|-------|-------------|----------------|
| `DOWNLOAD` | Downloading index | 0-40% |
| `PROCESS` | Parsing index data | 50% |
| `MERGE` | Merging with database | 75% |
| `COMMIT` | Saving to database | 90% |

### Binder API

```kotlin
inner class Binder : android.os.Binder() {
    val state: SharedFlow<State>

    // Sync all enabled repos
    fun sync(request: SyncRequest)

    // Sync single repo
    fun sync(repository: Repository)

    // Update all apps with available updates
    suspend fun updateAllApps()

    // Enable/disable repository
    fun setEnabled(repository: Repository, enabled: Boolean): Boolean

    // Delete repository
    fun deleteRepository(repositoryId: Long): Boolean

    // Cancel auto-sync
    fun cancelAuto(): Boolean
}
```

### SyncRequest Types

```kotlin
enum class SyncRequest {
    AUTO,    // Background automatic sync
    MANUAL,  // User-initiated sync
    FORCE    // Force re-sync (ignores cache)
}
```

### Auto-Update Flow

```kotlin
suspend fun handleUpdates(
    notifyUpdates: Boolean,
    autoUpdate: Boolean,
    skipSignature: Boolean,
) {
    val updates = Database.ProductAdapter.getUpdates(skipSignature)

    if (updates.isNotEmpty()) {
        if (notifyUpdates) {
            notificationManager?.notify(
                Constants.NOTIFICATION_ID_UPDATES,
                updatesAvailableNotification(context, updates),
            )
        }
        if (autoUpdate) {
            autoUpdating = true
            updateAllAppsInternal(updates)
        }
    }
}
```

### JobScheduler Integration

For periodic background sync:

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

## DownloadService

Manages APK download queue with progress tracking.

### State

```kotlin
sealed class State(val packageName: String) {
    data object Idle : State("")
    data class Connecting(val name: String) : State(name)
    data class Downloading(val name: String, val read: DataSize, val total: DataSize?) : State(name)
    data class Error(val name: String) : State(name)
    data class Cancel(val name: String) : State(name)
    data class Success(val name: String, val release: Release) : State(name)
}

data class DownloadState(
    val currentItem: State = State.Idle,
    val queue: List<String> = emptyList(),
)
```

### Binder API

```kotlin
inner class Binder : android.os.Binder() {
    val downloadState: StateFlow<DownloadState>

    fun enqueue(
        packageName: String,
        name: String,
        repository: Repository,
        release: Release,
        isUpdate: Boolean = false,
    )

    fun cancel(packageName: String)
}
```

### Download Flow

```
1. Check cache (skip download if APK exists)
2. Add to queue
3. Start foreground notification
4. Download with progress updates
5. Validate downloaded file
6. On success: trigger installation
7. On error: show error notification
```

### Error Types

```kotlin
private sealed interface ErrorType {
    data object IO : ErrorType
    data object Http : ErrorType
    data object SocketTimeout : ErrorType
    data object ConnectionTimeout : ErrorType
    class Validation(val exception: ValidationException) : ErrorType
}
```

### Auto-Install

After successful download:

```kotlin
suspend fun publishSuccess(task: Task) {
    val currentInstaller = installerType.first()

    // Show install notification
    showNotificationInstall(task)

    // Auto-install with privileged installers or session installer for updates
    if (currentInstaller == InstallerType.ROOT ||
        currentInstaller == InstallerType.SHIZUKU ||
        autoInstallWithSessionInstaller
    ) {
        installer install installItem
    }
}
```

## Notifications

### Notification Channels

| Channel ID | Purpose |
|------------|---------|
| `NOTIFICATION_CHANNEL_SYNCING` | Repository sync progress |
| `NOTIFICATION_CHANNEL_DOWNLOADING` | Download progress |
| `NOTIFICATION_CHANNEL_UPDATES` | Available updates |
| `NOTIFICATION_CHANNEL_INSTALL` | Installation status |

### Progress Notification

```kotlin
stateNotificationBuilder
    .setContentTitle(getString(R.string.downloading_FORMAT, taskName))
    .setContentText("${bytesRead} / ${totalBytes}")
    .setProgress(100, percentComplete, false)
```

### Cancel Action

```kotlin
private const val ACTION_CANCEL = "${BuildConfig.APPLICATION_ID}.intent.action.CANCEL"

// In notification
.addAction(
    0,
    getString(R.string.cancel),
    PendingIntent.getService(
        this,
        0,
        Intent(this, this::class.java).setAction(ACTION_CANCEL),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    ),
)
```

## Foreground Service

Both services run as foreground services:

```kotlin
override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    if (intent?.action == ACTION_CANCEL) {
        // Handle cancellation
    } else {
        startForeground(
            NOTIFICATION_ID,
            notificationBuilder.build(),
        )
    }
    return super.onStartCommand(intent, flags, startId)
}
```

### Timeout Handling (Android 15+)

```kotlin
override fun onTimeout(startId: Int) {
    super.onTimeout(startId)
    onDestroy()
    stopSelf()
}
```

## Thread Safety

| Mechanism | Purpose |
|-----------|---------|
| `Mutex` | Protects currentTask during access |
| `Channel` | Sequential task processing |
| `StateFlow` | Thread-safe state updates |

```kotlin
private val lock = Mutex()

// Protected access
lock.withLock {
    currentTask = null
}
```

## Usage from Activity/Fragment

```kotlin
class MainActivity : AppCompatActivity() {
    private val syncConnection = Connection(SyncService::class.java)
    private val downloadConnection = Connection(DownloadService::class.java)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        syncConnection.bind(this)
        downloadConnection.bind(this)
    }

    override fun onDestroy() {
        syncConnection.unbind(this)
        downloadConnection.unbind(this)
        super.onDestroy()
    }

    fun syncRepositories() {
        syncConnection.binder?.sync(SyncRequest.MANUAL)
    }

    fun downloadApp(packageName: String, repo: Repository, release: Release) {
        downloadConnection.binder?.enqueue(
            packageName = packageName,
            name = "App Name",
            repository = repo,
            release = release,
        )
    }
}
```

## Changes

| Change | Change Doc |
|--------|------------|
| Package: `com.looker.droidify` → `com.atd.store` | [package-rename.md](../changes/package-rename.md) |
| App branding Droid-ify → ATD Store | [app-branding.md](../changes/app-branding.md) |
