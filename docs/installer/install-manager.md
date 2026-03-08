# Install Manager

The `InstallManager` orchestrates the download and installation queue, manages installation state, and coordinates between the UI and installer implementations.

## Overview

**File:** `installer/InstallManager.kt`

```kotlin
class InstallManager(
    private val context: Context,
    private val settingsRepository: SettingsRepository
)
```

## Key Responsibilities

| Responsibility | Description |
|----------------|-------------|
| Queue management | Processes install/uninstall requests sequentially |
| State tracking | Maintains installation state per package |
| Installer switching | Changes installer based on user preference |
| Notification | Shows install progress notifications |
| Auto-update handling | Manages auto-update notification state |

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    InstallManager                        │
│                                                          │
│  ┌──────────────────┐    ┌──────────────────┐           │
│  │  installItems    │    │  uninstallItems  │           │
│  │    Channel       │    │     Channel      │           │
│  └────────┬─────────┘    └────────┬─────────┘           │
│           │                       │                      │
│           ▼                       ▼                      │
│  ┌──────────────────────────────────────────┐           │
│  │              installer()                  │           │
│  │           coroutine loop                  │           │
│  └────────────────────┬─────────────────────┘           │
│                       │                                  │
│                       ▼                                  │
│  ┌──────────────────────────────────────────┐           │
│  │          state: StateFlow<Map>            │           │
│  │      PackageName → InstallState           │           │
│  └──────────────────────────────────────────┘           │
└─────────────────────────────────────────────────────────┘
```

## State Management

### State Flow

```kotlin
val state = MutableStateFlow<Map<PackageName, InstallState>>(emptyMap())
```

The state map tracks installation status for each package:

| State | Meaning |
|-------|---------|
| `Pending` | Queued, waiting to be processed |
| `Installing` | Currently being installed |
| `Installed` | Successfully installed |
| `Failed` | Installation failed |

### Observing State

```kotlin
// In ViewModel
val installState = installManager.state
    .map { it[packageName] }
    .stateIn(viewModelScope, SharingStarted.Lazily, null)

// In UI
val state by viewModel.installState.collectAsStateWithLifecycle()
when (state) {
    InstallState.Pending -> ShowPending()
    InstallState.Installing -> ShowProgress()
    InstallState.Installed -> ShowInstalled()
    InstallState.Failed -> ShowError()
    null -> ShowInstallButton()
}
```

## Channels

### Install Channel

```kotlin
private val installItems = Channel<InstallItem>()

suspend infix fun install(installItem: InstallItem) {
    installItems.send(installItem)
}
```

### Uninstall Channel

```kotlin
private val uninstallItems = Channel<PackageName>()

suspend infix fun uninstall(packageName: PackageName) {
    uninstallItems.send(packageName)
}
```

## Initialization

The `InstallManager` is started in `AtdStore.kt`:

```kotlin
// In Application.onCreate()
appScope.launch { installer() }
```

The `invoke()` operator starts all processing loops:

```kotlin
suspend operator fun invoke() = coroutineScope {
    setupInstaller()  // Listen for installer type changes
    installer()       // Process install queue
    uninstaller()     // Process uninstall queue
}
```

## Install Processing

```kotlin
private fun CoroutineScope.installer() = launch {
    val currentQueue = mutableSetOf<String>()

    installItems.filter { item ->
        // Deduplicate: only add if not already queued
        currentQueue.addAndCompute(item.packageName.name) { isAdded ->
            if (isAdded) {
                updateState { put(item.packageName, InstallState.Pending) }
            }
        }
    }.consumeEach { item ->
        if (state.value.containsKey(item.packageName)) {
            // Update state to Installing
            updateState { put(item.packageName, InstallState.Installing) }

            // Show notification
            notificationManager?.installNotification(...)

            // Perform installation
            val result = installer.use { it.install(item) }

            // Handle result
            if (result == InstallState.Installed) {
                if (deleteApkPreference.first()) {
                    Cache.getReleaseFile(context, item.installFileName).delete()
                }
            }

            // Update notification
            notificationManager?.removeInstallNotification(item.packageName.name)

            // Update state
            updateState { put(item.packageName, result) }
            currentQueue.remove(item.packageName.name)
        }
    }
}
```

## Installer Switching

The installer type can be changed at runtime:

```kotlin
private fun CoroutineScope.setupInstaller() = launch {
    installerPreference.collectLatest(::setInstaller)
}

private suspend fun setInstaller(installerType: InstallerType) {
    lock.withLock {
        _installer = when (installerType) {
            InstallerType.LEGACY -> LegacyInstaller(context, settingsRepository)
            InstallerType.SESSION -> SessionInstaller(context)
            InstallerType.SHIZUKU -> ShizukuInstaller(context)
            InstallerType.ROOT -> RootInstaller(context)
        }
    }
}
```

The `lock` ensures thread-safe installer switching during installation.

## Public API

### Install

```kotlin
// Queue an installation
installManager install InstallItem(
    packageName = PackageName("org.example.app"),
    installFileName = "org.example.app_1.apk"
)

// Or using the infix extension
"org.example.app" installFrom "org.example.app_1.apk"
```

### Uninstall

```kotlin
installManager uninstall PackageName("org.example.app")
```

### Remove from State

```kotlin
// Clear state after user dismisses error
installManager remove packageName
```

### Mark as Failed

```kotlin
// Externally mark as failed (e.g., download failed)
installManager setFailed packageName
```

## Auto-Update Integration

When auto-updating, the manager updates the notification:

```kotlin
if (result == InstallState.Installed && SyncService.autoUpdating) {
    val updates = Database.ProductAdapter.getUpdates(skipSignature.first())
    when {
        updates.isEmpty() -> {
            SyncService.autoUpdating = false
            notificationManager?.cancel(Constants.NOTIFICATION_ID_UPDATES)
        }
        updates.map { it.packageName } != SyncService.autoUpdateStartedFor -> {
            notificationManager?.notify(
                Constants.NOTIFICATION_ID_UPDATES,
                updatesAvailableNotification(context, updates),
            )
        }
    }
}
```

## Cleanup

```kotlin
fun close() {
    _installer = null  // Calls close() on current installer
    uninstallItems.close()
    installItems.close()
}
```

Called when the application terminates.

## Thread Safety

| Mechanism | Purpose |
|-----------|---------|
| `Channel` | Sequential processing of requests |
| `Mutex` | Safe installer switching |
| `StateFlow` | Thread-safe state updates |

## Usage Example

```kotlin
@HiltViewModel
class AppDetailViewModel @Inject constructor(
    private val installManager: InstallManager,
    private val downloader: Downloader,
) : ViewModel() {

    val installState = installManager.state
        .map { it[currentPackage] }
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    fun install(release: Release) {
        viewModelScope.launch {
            // Download APK
            val file = downloader.download(release.url)

            // Queue installation
            installManager install InstallItem(
                packageName = release.packageName,
                installFileName = file.name,
            )
        }
    }

    fun cancelInstall() {
        installManager remove currentPackage
    }
}
```

## Changes

| Change | Change Doc |
|--------|------------|
| Class renames (AtdStore, AtdDatabase, AtdTheme) | [package-rename.md](../changes/package-rename.md) |
