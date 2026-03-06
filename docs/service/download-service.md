# Download Service

Background service for downloading and installing APK files.

## Overview

**File:** `service/DownloadService.kt`

A bound service that manages APK downloads with progress notifications, validation, and automatic installation.

## Architecture

```
DownloadService
├── Binder (public API)
│   ├── enqueue(packageName, name, repository, release)
│   ├── cancel(packageName)
│   └── downloadState: StateFlow<DownloadState>
├── Task queue management
├── ReleaseFileValidator integration
└── InstallManager integration
```

## State Model

### DownloadState

```kotlin
data class DownloadState(
    val currentItem: State = State.Idle,
    val queue: List<String> = emptyList(),
) {
    infix fun isDownloading(packageName: String): Boolean
    infix fun isComplete(packageName: String): Boolean
}
```

### State Sealed Class

```kotlin
sealed class State(val packageName: String) {
    data object Idle : State("")
    data class Connecting(val name: String) : State(name)
    data class Downloading(val name: String, val read: DataSize, val total: DataSize?) : State(name)
    data class Error(val name: String) : State(name)
    data class Cancel(val name: String) : State(name)
    data class Success(val name: String, val release: Release) : State(name)
}
```

## Binder API

```kotlin
inner class Binder : android.os.Binder() {
    val downloadState = _downloadState.asStateFlow()

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

### Enqueue Logic

1. Create `Task` with download details
2. Check if release already cached - skip download if exists
3. Cancel any existing tasks for same package
4. Add to queue or start immediately if idle
5. Update queue state

## Download Flow

```
┌─────────────────────────────────────────────────────────────┐
│                    enqueue(...)                              │
└─────────────────────────┬───────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                    Cache Check                               │
│  Cache.getReleaseFile(release.cacheFileName).exists()?      │
└─────────────────────────┬───────────────────────────────────┘
          │ no                              │ yes
          ▼                                 ▼
┌─────────────────────┐        ┌─────────────────────────────┐
│   Add to queue      │        │   publishSuccess(task)      │
└─────────────────────┘        └─────────────────────────────┘
          │
          ▼
┌─────────────────────────────────────────────────────────────┐
│                    handleDownload()                          │
│  1. Pop task from queue                                     │
│  2. Create notification                                     │
│  3. Launch download coroutine                               │
└─────────────────────────┬───────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                    downloadFile()                            │
│  1. Create ReleaseFileValidator                             │
│  2. Download to partial file                                │
│  3. Validate (hash, signature, permissions)                 │
│  4. Rename to final release file                            │
└─────────────────────────┬───────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                    publishSuccess()                          │
│  1. Show install notification                               │
│  2. Auto-install if Root/Shizuku/Session update             │
└─────────────────────────────────────────────────────────────┘
```

## Task Management

```kotlin
private class Task(
    val packageName: String,
    val name: String,
    val release: Release,
    val url: String,
    val authentication: String,
    val isUpdate: Boolean = false,
) {
    val notificationTag: String get() = "download-$packageName"
}
```

## Notifications

### Progress Notification

- Shows download progress with bytes read/total
- Cancel action button
- Updates sampled at 400ms intervals

### Error Notification

```kotlin
private sealed interface ErrorType {
    data object IO : ErrorType
    data object Http : ErrorType
    data object SocketTimeout : ErrorType
    data object ConnectionTimeout : ErrorType
    class Validation(val exception: ValidationException) : ErrorType
}
```

Error types mapped to user-friendly messages.

### Install Notification

Shown on successful download with tap-to-install action.

## Auto-Installation

```kotlin
val autoInstallWithSessionInstaller =
    SdkCheck.canAutoInstall(release.targetSdkVersion) &&
    currentInstaller == InstallerType.SESSION &&
    task.isUpdate

if (currentInstaller == InstallerType.ROOT ||
    currentInstaller == InstallerType.SHIZUKU ||
    autoInstallWithSessionInstaller
) {
    installer install installItem
}
```

Auto-installs for:
- ROOT installer (always)
- SHIZUKU installer (always)
- SESSION installer (updates only, when targeting compatible SDK)

## Validation

Uses `ReleaseFileValidator` to verify:
1. File checksum matches expected hash
2. APK can be parsed
3. Package name matches
4. Version code matches
5. Signature matches (if not ignoring)
6. No excessive permissions

## Dependencies

```kotlin
@Inject lateinit var settingsRepository: SettingsRepository
@Inject lateinit var downloader: Downloader
@Inject lateinit var installer: InstallManager
```

## Service Lifecycle

- Starts as foreground service during downloads
- Stops when queue empty and no active download
- Handles `ACTION_CANCEL` intent to stop current download
- Proper cleanup in `onDestroy()`

## Thread Safety

Uses `Mutex` for thread-safe task management:

```kotlin
private val lock = Mutex()

// In download completion
lock.withLock { currentTask = null }
handleDownload()
```
