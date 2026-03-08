# Shizuku Installer

The `ShizukuInstaller` uses Shizuku to run privileged commands without full root access, enabling silent app installation.

## Overview

**File:** `installer/installers/shizuku/ShizukuInstaller.kt`

**Dependency:** `rikka.shizuku:api`

## What is Shizuku?

Shizuku is an app that provides a way to use system APIs directly with elevated privileges. It can be activated via:
- ADB (no root required)
- Root (if device is rooted)
- Wireless debugging (Android 11+)

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                   ShizukuInstaller                       │
│                                                          │
│  ┌───────────────────────────────────────────────────┐  │
│  │               Shizuku.newProcess()                 │  │
│  │                                                    │  │
│  │  Runs shell commands with ADB shell privileges    │  │
│  │                                                    │  │
│  │  Commands:                                         │  │
│  │  - pm install-create (create session)             │  │
│  │  - pm install-write  (write APK)                  │  │
│  │  - pm install-commit (commit session)             │  │
│  └───────────────────────────────────────────────────┘  │
│                         │                                │
│                         ▼                                │
│  ┌───────────────────────────────────────────────────┐  │
│  │              Package Manager (pm)                  │  │
│  │                                                    │  │
│  │  Running with ADB shell or root privileges        │  │
│  │  Silent installation via install sessions         │  │
│  └───────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
```

## Implementation

```kotlin
class ShizukuInstaller(private val context: Context) : Installer {

    companion object {
        private val SESSION_ID_REGEX = Regex("(?<=\\[).+?(?=])")
    }

    override suspend fun install(
        installItem: InstallItem,
    ): InstallState = suspendCancellableCoroutine { cont ->
        var sessionId: String? = null
        val file = Cache.getReleaseFile(context, installItem.installFileName)

        try {
            val fileSize = file.length()
            if (fileSize == 0L) {
                cont.cancel()
                error("File is not valid: Size ${file.size}")
            }

            val installerPackage = context.packageName

            file.inputStream().use { inputStream ->
                // Step 1: Create install session
                val createCommand = if (SdkCheck.isNougat) {
                    "pm install-create --user current -i $installerPackage -S $fileSize"
                } else {
                    "pm install-create -i $installerPackage -S $fileSize"
                }
                val createResult = exec(createCommand)
                sessionId = SESSION_ID_REGEX.find(createResult.out)?.value
                    ?: error("Failed to create install session")

                // Step 2: Write APK to session
                val writeResult = exec(
                    "pm install-write -S $fileSize $sessionId base -",
                    inputStream
                )
                if (writeResult.resultCode != 0) {
                    error("Failed to write APK to session $sessionId")
                }

                // Step 3: Commit session
                val commitResult = exec("pm install-commit $sessionId")
                if (commitResult.resultCode != 0) {
                    error("Failed to commit install session $sessionId")
                }

                cont.resume(InstallState.Installed)
            }
        } catch (_: Exception) {
            if (sessionId != null) {
                exec("pm install-abandon $sessionId")
            }
            cont.resume(InstallState.Failed)
        }
    }

    override suspend fun uninstall(packageName: PackageName) =
        context.uninstallPackage(packageName)

    override fun close() = Unit
}
```

## Shell Execution

```kotlin
private data class ShellResult(val resultCode: Int, val out: String)

private fun exec(command: String, stdin: InputStream? = null): ShellResult {
    val process = rikka.shizuku.Shizuku.newProcess(
        arrayOf("sh", "-c", command),
        null,  // environment
        null   // directory
    )

    if (stdin != null) {
        process.outputStream.use { stdin.copyTo(it) }
    }

    val output = process.inputStream.bufferedReader().use(BufferedReader::readText)
    val resultCode = process.waitFor()

    return ShellResult(resultCode, output)
}
```

## Install Session Commands

### Create Session

```bash
pm install-create --user current -i com.atd.store -S 12345678
```

Returns: `Success: created install session [1234567]`

The session ID is extracted using regex: `(?<=\[).+?(?=])`

### Write APK

```bash
pm install-write -S 12345678 1234567 base -
```

- `-S` specifies size for progress tracking
- `1234567` is the session ID
- `base` is the split name (base APK)
- `-` reads from stdin

### Commit Session

```bash
pm install-commit 1234567
```

Triggers the actual installation.

### Abandon Session (on failure)

```bash
pm install-abandon 1234567
```

Cleans up failed session.

## Uninstall

Uses standard intent (not Shizuku):

```kotlin
override suspend fun uninstall(packageName: PackageName) =
    context.uninstallPackage(packageName)
```

## Requirements

1. **Shizuku App** - Installed and running
2. **Permission** - User must grant permission to Droid-ify
3. **Shizuku Active** - Started via ADB, root, or wireless debugging

## Permission Check

Before using, verify Shizuku is available:

```kotlin
val isShizukuAvailable = Shizuku.pingBinder()
val hasPermission = Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
```

## Advantages

- Silent installation (no user prompts)
- Works without root
- Can be set up via ADB
- Persists across reboots (with root or wireless debugging)

## Limitations

- Requires Shizuku app
- User must set up Shizuku (one-time)
- ADB setup requires computer (unless using wireless debugging)
- Cannot install to system partition

## Error Handling

Each step checks for success:

```kotlin
val createResult = exec(createCommand)
sessionId = SESSION_ID_REGEX.find(createResult.out)?.value
    ?: run {
        cont.cancel()
        error("Failed to create install session")
    }

val writeResult = exec("pm install-write ...")
if (writeResult.resultCode != 0) {
    cont.cancel()
    error("Failed to write APK to session $sessionId")
}
```

On any failure, the session is abandoned to clean up:

```kotlin
} catch (_: Exception) {
    if (sessionId != null) {
        exec("pm install-abandon $sessionId")
    }
    cont.resume(InstallState.Failed)
}
```

## Comparison with Root

| Feature | Shizuku | Root |
|---------|---------|------|
| Requires root | No | Yes |
| Setup complexity | Medium | Low (if rooted) |
| Persistence | Varies | Permanent |
| System app install | Limited | Yes |
| Speed | Fast | Fastest |

## Changes

| Change | Change Doc |
|--------|------------|
| Package: `com.looker.droidify` → `com.atd.store` | [package-rename.md](../changes/package-rename.md) |
| App branding Droid-ify → ATD Store | [app-branding.md](../changes/app-branding.md) |
