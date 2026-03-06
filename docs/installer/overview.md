# Installation System Overview

The installation system manages downloading APK files and installing them using one of several installer implementations.

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    InstallManager                        │
│  ┌───────────┐  ┌───────────┐  ┌───────────────────┐   │
│  │  Install  │  │ Uninstall │  │  State Tracking   │   │
│  │  Channel  │  │  Channel  │  │                   │   │
│  └─────┬─────┘  └─────┬─────┘  └───────────────────┘   │
│        │              │                                  │
│        └──────┬───────┘                                  │
│               ▼                                          │
│  ┌─────────────────────────────────────────────────┐    │
│  │                   Installer                      │    │
│  │  ┌─────────┐ ┌──────┐ ┌─────────┐ ┌────────┐   │    │
│  │  │ Session │ │ Root │ │ Shizuku │ │ Legacy │   │    │
│  │  └─────────┘ └──────┘ └─────────┘ └────────┘   │    │
│  └─────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────┘
```

## Key Files

| File | Purpose |
|------|---------|
| `installer/InstallManager.kt` | Orchestrates install/uninstall queue |
| `installer/installers/Installer.kt` | Installer interface |
| `installer/installers/session/SessionInstaller.kt` | Default installer |
| `installer/installers/root/RootInstaller.kt` | Root/Magisk installer |
| `installer/installers/shizuku/ShizukuInstaller.kt` | Shizuku installer |
| `installer/installers/LegacyInstaller.kt` | Intent-based installer |
| `installer/model/InstallItem.kt` | Install request data |
| `installer/model/InstallState.kt` | Installation state enum |

## Installer Interface

```kotlin
interface Installer : AutoCloseable {
    suspend fun install(installItem: InstallItem): InstallState
    suspend fun uninstall(packageName: PackageName)
}
```

All installers implement this interface, allowing the `InstallManager` to switch between them.

## Installer Types

| Type | Description | Requirements |
|------|-------------|--------------|
| `SESSION` | Android PackageInstaller API | None (default) |
| `ROOT` | Root shell commands | Root access (Magisk) |
| `SHIZUKU` | Shizuku privileged API | Shizuku installed & running |
| `LEGACY` | ACTION_INSTALL_PACKAGE intent | None |

### SessionInstaller (Default)

Uses Android's `PackageInstaller` API:

```kotlin
val installer = context.packageManager.packageInstaller
val id = installer.createSession(sessionParams)
val session = installer.openSession(id)

session.use { activeSession ->
    // Write APK to session
    cacheFile.inputStream().use { fileStream ->
        activeSession.openWrite(...).use { outputStream ->
            fileStream.copyTo(outputStream)
            activeSession.fsync(outputStream)
        }
    }
    // Commit installation
    activeSession.commit(pendingIntent.intentSender)
}
```

**Features:**
- No special permissions required
- Supports silent updates (Android 12+)
- Handles unarchive (Android 15+)

### RootInstaller

Uses root shell via `libsu`:

```kotlin
val installCommand = "cat %s | pm install --user %s -i %s -t -r -S %s"
    .format(apkPath, currentUser, packageName, fileSize)

Shell.cmd(installCommand).submit { result ->
    if (result.isSuccess) InstallState.Installed
    else InstallState.Failed
}
```

**Features:**
- Silent installation
- No user confirmation
- Requires Magisk or root

### ShizukuInstaller

Uses Shizuku's privileged shell:

```kotlin
// Create install session
val createResult = exec("pm install-create --user current -i $installerPackage -S $fileSize")
val sessionId = SESSION_ID_REGEX.find(createResult.out)?.value

// Write APK
file.inputStream().use {
    exec("pm install-write -S $fileSize $sessionId base -", it)
}

// Commit
exec("pm install-commit $sessionId")
```

**Features:**
- Silent installation (with Shizuku permission)
- No root required
- Needs Shizuku app installed and running

### LegacyInstaller

Uses system package installer intent:

```kotlin
val intent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
    data = FileProvider.getUriForFile(context, authority, file)
    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
}
context.startActivity(intent)
```

**Features:**
- Works on all devices
- Opens system installer UI
- User must manually confirm

## Install State

```kotlin
enum class InstallState {
    Pending,    // Queued for installation
    Installing, // Currently installing
    Installed,  // Successfully installed
    Failed      // Installation failed
}
```

## Install Flow

```
1. UI triggers install
         │
         ▼
2. InstallManager.install(item)
         │
         ▼
3. Item added to install channel
         │
         ▼
4. State updated to Pending
         │
         ▼
5. Item processed from queue
         │
         ▼
6. State updated to Installing
         │
         ▼
7. Installer.install(item) called
         │
         ▼
8. APK written and committed
         │
         ▼
9. State updated to Installed/Failed
         │
         ▼
10. Notification updated
```

## Notifications

Install progress is shown via notifications:

```kotlin
notificationManager?.installNotification(
    packageName = item.packageName.name,
    notification = context.createInstallNotification(
        appName = item.packageName.name,
        state = InstallState.Installing,
    )
)
```

## APK Cache

APKs are cached before installation:

```kotlin
val cacheFile = Cache.getReleaseFile(context, installItem.installFileName)
```

After successful installation, APKs can be deleted based on user preference:

```kotlin
if (deleteApkPreference.first()) {
    cacheFile.delete()
}
```

## Error Handling

Installation failures are captured:

```kotlin
val result = installer.use { it.install(item) }
// result is InstallState.Installed or InstallState.Failed

updateState { put(item.packageName, result) }
```

## Adding a New Installer

1. Create class implementing `Installer`:

```kotlin
class MyInstaller(private val context: Context) : Installer {
    override suspend fun install(installItem: InstallItem): InstallState {
        // Implementation
    }

    override suspend fun uninstall(packageName: PackageName) {
        // Implementation
    }

    override fun close() {
        // Cleanup
    }
}
```

2. Add to `InstallerType` enum in `datastore/model/`:

```kotlin
enum class InstallerType {
    LEGACY, SESSION, SHIZUKU, ROOT, MY_INSTALLER
}
```

3. Update `InstallManager.setInstaller()`:

```kotlin
private suspend fun setInstaller(installerType: InstallerType) {
    lock.withLock {
        _installer = when (installerType) {
            // ... existing
            InstallerType.MY_INSTALLER -> MyInstaller(context)
        }
    }
}
```

4. Add permission checks if needed (see `SettingsViewModel.setInstaller()`)
