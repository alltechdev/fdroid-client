# Installer Backends

Four different installation methods for different use cases.

## SessionInstaller (Default)

**File:** `installer/installers/session/SessionInstaller.kt`

Uses Android's PackageInstaller session API.

### Features
- Silent installation on Android 12+ (if app is update owner)
- Progress tracking via callbacks
- Cancel support

### Implementation

```kotlin
class SessionInstaller(private val context: Context) : Installer {

    companion object {
        private val sessionParams = PackageInstaller.SessionParams(MODE_FULL_INSTALL).apply {
            sdkAbove(Build.VERSION_CODES.S) {
                setRequireUserAction(USER_ACTION_NOT_REQUIRED)
            }
            sdkAbove(Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                setRequestUpdateOwnership(true)
            }
        }
    }

    override suspend fun install(installItem: InstallItem): InstallState =
        suspendCancellableCoroutine { cont ->
            val id = installer.createSession(sessionParams)

            val callback = object : PackageInstaller.SessionCallback() {
                override fun onFinished(sessionId: Int, success: Boolean) {
                    if (sessionId == id)
                        cont.resume(if (success) Installed else Failed)
                }
                // ... other callbacks
            }

            installer.registerSessionCallback(callback, Handler(Looper.getMainLooper()))
            val session = installer.openSession(id)

            session.use { activeSession ->
                // Copy APK to session
                cacheFile.inputStream().use { fileStream ->
                    activeSession.openWrite(cacheFile.name, 0, sizeBytes).use { out ->
                        fileStream.copyTo(out)
                        activeSession.fsync(out)
                    }
                }

                // Commit with pending intent
                val pendingIntent = PendingIntent.getBroadcast(context, id, intent, flags)
                activeSession.commit(pendingIntent.intentSender)
            }
        }
}
```

### Unarchive Support (Android 15+)

```kotlin
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM
    && installItem.unarchiveId != null) {
    sessionParams.setUnarchiveId(installItem.unarchiveId)
}
```

## ShizukuInstaller

**File:** `installer/installers/shizuku/ShizukuInstaller.kt`

Uses Shizuku for privileged installation via shell commands.

### Requirements
- Shizuku service running
- User granted permission to app

### Implementation

```kotlin
class ShizukuInstaller(private val context: Context) : Installer {

    override suspend fun install(installItem: InstallItem): InstallState =
        suspendCancellableCoroutine { cont ->
            file.inputStream().use {
                // Create install session
                val createCommand = "pm install-create --user current -i $installerPackage -S $fileSize"
                val createResult = exec(createCommand)
                val sessionId = SESSION_ID_REGEX.find(createResult.out)?.value

                // Write APK
                val writeResult = exec("pm install-write -S $fileSize $sessionId base -", it)

                // Commit
                val commitResult = exec("pm install-commit $sessionId")
            }
        }

    private fun exec(command: String, stdin: InputStream? = null): ShellResult {
        val process = Shizuku.newProcess(arrayOf("sh", "-c", command), null, null)
        if (stdin != null) {
            process.outputStream.use { stdin.copyTo(it) }
        }
        return ShellResult(process.waitFor(), process.inputStream.readText())
    }
}
```

## RootInstaller

**File:** `installer/installers/root/RootInstaller.kt`

Uses root shell for installation via libsu.

### Requirements
- Rooted device
- Root permission granted

### Implementation

```kotlin
class RootInstaller(private val context: Context) : Installer {

    override suspend fun install(installItem: InstallItem): InstallState =
        suspendCancellableCoroutine { cont ->
            val installCommand = INSTALL_COMMAND.format(
                releaseFile.absolutePath,
                currentUser(),
                context.packageName,
                releaseFile.length(),
            )
            Shell.cmd(installCommand).submit { shellResult ->
                cont.resume(if (shellResult.isSuccess) Installed else Failed)

                // Cleanup APK via root
                Shell.cmd(DELETE_COMMAND.format(utilBox(), releaseFile.absolutePath)).submit()
            }
        }
}

private const val INSTALL_COMMAND = "cat %s | pm install --user %s -i %s -t -r -S %s"
```

### Helper Functions

```kotlin
// Find toybox or busybox for file operations
private fun utilBox(): String {
    listOf("toybox", "busybox").forEach {
        val out = Shell.cmd("which $it").exec().out
        if (out.isNotEmpty() && !out.first().contains("not found"))
            return out.first()
    }
    return ""
}

// Get current Android user
private fun currentUser() = if (SdkCheck.isOreo) {
    Shell.cmd("am get-current-user").exec().out[0]
} else {
    // Parse from activity dump for older Android
    Shell.cmd("dumpsys activity | grep -E \"mUserLru\"")
        .exec().out[0].extractUserId()
}
```

## LegacyInstaller

**File:** `installer/installers/LegacyInstaller.kt`

Uses traditional `ACTION_INSTALL_PACKAGE` intent.

### Use Cases
- MIUI devices where session installer fails
- User prefers manual confirmation
- Custom installer app selection

### Implementation

```kotlin
class LegacyInstaller(
    private val context: Context,
    private val settingsRepository: SettingsRepository
) : Installer {

    override suspend fun install(installItem: InstallItem): InstallState {
        val fileUri = if (SdkCheck.isNougat) {
            Cache.getReleaseUri(context, installItem.installFileName)  // FileProvider
        } else {
            Cache.getReleaseFile(context, installItem.installFileName).toUri()
        }

        val comp = settingsRepository.get { legacyInstallerComponent }.firstOrNull()

        return suspendCancellableCoroutine { cont ->
            val intent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
                setDataAndType(fileUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION

                when (comp) {
                    is LegacyInstallerComponent.Component -> {
                        component = ComponentName(comp.clazz, comp.activity)
                    }
                    // For AlwaysChoose or null, don't set component
                }
            }

            val installIntent = when (comp) {
                LegacyInstallerComponent.AlwaysChoose ->
                    Intent.createChooser(intent, getString(R.string.select_installer))
                else -> intent
            }

            context.startActivity(installIntent)
        }
    }
}
```

### LegacyInstallerComponent

```kotlin
sealed class LegacyInstallerComponent {
    object Unspecified : LegacyInstallerComponent()
    object AlwaysChoose : LegacyInstallerComponent()
    data class Component(val clazz: String, val activity: String) : LegacyInstallerComponent()
}
```

## Uninstall Helper

Common uninstall implementation for non-privileged installers:

```kotlin
suspend fun Context.uninstallPackage(packageName: PackageName) =
    suspendCancellableCoroutine { cont ->
        startActivity(
            intent(Intent.ACTION_UNINSTALL_PACKAGE) {
                data = "package:${packageName.name}".toUri()
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        )
        cont.resume(Unit)
    }
```

## Comparison

| Installer | Root Required | Silent Install | User Interaction |
|-----------|---------------|----------------|------------------|
| Session | No | Android 12+ | Confirmation dialog |
| Shizuku | Shizuku app | Yes | None |
| Root | Yes | Yes | None |
| Legacy | No | No | System installer UI |
