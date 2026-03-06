# Session Installer

The `SessionInstaller` uses Android's `PackageInstaller` API to install apps through the standard system installation flow.

## Overview

**Files:**
- `installer/installers/session/SessionInstaller.kt` - Main installer
- `installer/installers/session/SessionInstallerReceiver.kt` - Broadcast receiver for results

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                   SessionInstaller                       │
│                                                          │
│  ┌───────────────────────────────────────────────────┐  │
│  │              PackageInstaller                      │  │
│  │                                                    │  │
│  │  1. createSession() ──► Session ID                │  │
│  │  2. openSession()   ──► Session handle            │  │
│  │  3. openWrite()     ──► Write APK data            │  │
│  │  4. commit()        ──► Trigger installation      │  │
│  └───────────────────────────────────────────────────┘  │
│                         │                                │
│                         ▼                                │
│  ┌───────────────────────────────────────────────────┐  │
│  │           SessionInstallerReceiver                 │  │
│  │                                                    │  │
│  │  Receives: STATUS_SUCCESS, STATUS_FAILURE, etc.   │  │
│  │  Updates: InstallManager state, notifications     │  │
│  └───────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
```

## SessionInstaller

```kotlin
class SessionInstaller(private val context: Context) : Installer {

    private val installer = context.packageManager.packageInstaller
    private val intent = Intent(context, SessionInstallerReceiver::class.java)
}
```

### Session Parameters

```kotlin
private val sessionParams = PackageInstaller.SessionParams(
    PackageInstaller.SessionParams.MODE_FULL_INSTALL
).apply {
    // Android 12+: Don't require user action for updates
    sdkAbove(sdk = Build.VERSION_CODES.S) {
        setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
    }
    // Android 14+: Request update ownership
    sdkAbove(sdk = Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        setRequestUpdateOwnership(true)
    }
}
```

### Install Implementation

```kotlin
override suspend fun install(
    installItem: InstallItem
): InstallState = suspendCancellableCoroutine { cont ->
    val cacheFile = Cache.getReleaseFile(context, installItem.installFileName)

    // Android 15+: Support for unarchiving
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM
        && installItem.unarchiveId != null) {
        sessionParams.setUnarchiveId(installItem.unarchiveId)
    }

    // Create session
    val id = installer.createSession(sessionParams)

    // Register callback for result
    val installerCallback = object : PackageInstaller.SessionCallback() {
        override fun onCreated(sessionId: Int) {}
        override fun onBadgingChanged(sessionId: Int) {}
        override fun onActiveChanged(sessionId: Int, active: Boolean) {}
        override fun onProgressChanged(sessionId: Int, progress: Float) {}
        override fun onFinished(sessionId: Int, success: Boolean) {
            if (sessionId == id) {
                cont.resume(if (success) InstallState.Installed else InstallState.Failed)
            }
        }
    }
    installer.registerSessionCallback(installerCallback, Handler(Looper.getMainLooper()))

    // Open session and write APK
    val session = installer.openSession(id)
    session.use { activeSession ->
        val sizeBytes = cacheFile.length()
        cacheFile.inputStream().use { fileStream ->
            activeSession.openWrite(cacheFile.name, 0, sizeBytes).use { outputStream ->
                if (cont.isActive) {
                    fileStream.copyTo(outputStream)
                    activeSession.fsync(outputStream)
                }
            }
        }

        // Commit session
        val pendingIntent = PendingIntent.getBroadcast(context, id, intent, flags)
        if (cont.isActive) activeSession.commit(pendingIntent.intentSender)
    }

    // Handle cancellation
    cont.invokeOnCancellation {
        try {
            installer.abandonSession(id)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
}
```

### Uninstall Implementation

```kotlin
override suspend fun uninstall(packageName: PackageName) =
    suspendCancellableCoroutine { cont ->
        intent.putExtra(SessionInstallerReceiver.ACTION_UNINSTALL, true)
        val pendingIntent = PendingIntent.getBroadcast(context, -1, intent, flags)
        installer.uninstall(packageName.name, pendingIntent.intentSender)
        cont.resume(Unit)
    }
```

### Cleanup

```kotlin
override fun close() {
    installerCallbacks?.let {
        installer.unregisterSessionCallback(it)
        installerCallbacks = null
    }
    try {
        installer.mySessions.forEach { installer.abandonSession(it.sessionId) }
    } catch (e: SecurityException) {
        log(e.message, type = Log.ERROR)
    }
}
```

## SessionInstallerReceiver

Handles installation results via broadcast:

```kotlin
@AndroidEntryPoint
class SessionInstallerReceiver : BroadcastReceiver() {

    @Inject
    lateinit var installManager: InstallManager

    override fun onReceive(context: Context, intent: Intent) {
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -1)

        if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
            // User needs to grant permission
            val promptIntent: Intent? = intent.getParcelableExtra(Intent.EXTRA_INTENT)
            promptIntent?.let {
                it.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
                it.putExtra(Intent.EXTRA_INSTALLER_PACKAGE_NAME, "com.android.vending")
                it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(it)
            }
        } else {
            notifyStatus(intent, context)
        }
    }
}
```

### Status Handling

```kotlin
private fun notifyStatus(intent: Intent, context: Context) {
    val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -1)
    val packageName = intent.getStringExtra(PackageInstaller.EXTRA_PACKAGE_NAME)
    val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
    val isUninstall = intent.getBooleanExtra(ACTION_UNINSTALL, false)

    when (status) {
        PackageInstaller.STATUS_SUCCESS -> {
            notificationManager?.removeInstallNotification(packageName)
            val notification = context.createInstallNotification(
                appName = appName,
                state = InstallState.Installed,
                isUninstall = isUninstall,
            )
            notificationManager?.installNotification(packageName, notification)
        }

        PackageInstaller.STATUS_FAILURE_ABORTED -> {
            notificationManager?.removeInstallNotification(packageName)
            installManager.setFailed(packageName.toPackageName())
        }

        else -> {
            installManager.remove(packageName.toPackageName())
            val notification = context.createInstallNotification(
                appName = appName,
                state = InstallState.Failed,
            ) {
                setContentText(message)
            }
            notificationManager?.installNotification(packageName, notification)
        }
    }
}
```

## Status Codes

| Status | Meaning |
|--------|---------|
| `STATUS_SUCCESS` | Installation completed successfully |
| `STATUS_FAILURE` | Installation failed |
| `STATUS_FAILURE_ABORTED` | User cancelled |
| `STATUS_FAILURE_BLOCKED` | Blocked by device policy |
| `STATUS_FAILURE_CONFLICT` | Package conflict |
| `STATUS_FAILURE_INCOMPATIBLE` | Incompatible with device |
| `STATUS_FAILURE_INVALID` | Invalid APK |
| `STATUS_FAILURE_STORAGE` | Insufficient storage |
| `STATUS_PENDING_USER_ACTION` | User confirmation needed |

## PendingIntent Flags

```kotlin
private val flags = if (SdkCheck.isSnowCake) {
    PendingIntent.FLAG_MUTABLE
} else {
    0
}
```

Android 12+ requires `FLAG_MUTABLE` for PendingIntents that the system modifies.

## Advantages

- Standard Android installation flow
- User sees familiar install prompts
- Works on all Android versions
- Supports silent updates when authorized (Android 12+)

## Limitations

- Requires user confirmation for most installs
- Can't install to system partition
- Slower than privileged installers
