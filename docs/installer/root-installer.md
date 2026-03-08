# Root Installer

The `RootInstaller` uses root shell access (via libsu) to install apps silently without user interaction.

## Overview

**File:** `installer/installers/root/RootInstaller.kt`

**Dependency:** `com.github.topjohnwu.libsu:core`

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    RootInstaller                         │
│                                                          │
│  ┌───────────────────────────────────────────────────┐  │
│  │                  libsu Shell                       │  │
│  │                                                    │  │
│  │  Shell.cmd(command).submit { result -> }          │  │
│  │                                                    │  │
│  │  Commands:                                         │  │
│  │  - pm install (install APK)                       │  │
│  │  - rm (delete APK after install)                  │  │
│  └───────────────────────────────────────────────────┘  │
│                         │                                │
│                         ▼                                │
│  ┌───────────────────────────────────────────────────┐  │
│  │              Package Manager (pm)                  │  │
│  │                                                    │  │
│  │  Runs with root privileges                        │  │
│  │  Silent installation, no user prompt              │  │
│  └───────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
```

## Implementation

```kotlin
class RootInstaller(private val context: Context) : Installer {

    override suspend fun install(
        installItem: InstallItem,
    ): InstallState = suspendCancellableCoroutine { cont ->
        val releaseFile = Cache.getReleaseFile(context, installItem.installFileName)

        val installCommand = INSTALL_COMMAND.format(
            releaseFile.absolutePath,
            currentUser(),
            context.packageName,
            releaseFile.length(),
        )

        Shell.cmd(installCommand).submit { shellResult ->
            val result = if (shellResult.isSuccess) {
                InstallState.Installed
            } else {
                InstallState.Failed
            }
            cont.resume(result)

            // Clean up APK file
            val deleteCommand = DELETE_COMMAND.format(utilBox(), releaseFile.absolutePath)
            Shell.cmd(deleteCommand).submit()
        }
    }

    override suspend fun uninstall(packageName: PackageName) =
        context.uninstallPackage(packageName)

    override fun close() {}
}
```

## Shell Commands

### Install Command

```kotlin
private const val INSTALL_COMMAND = "cat %s | pm install --user %s -i %s -t -r -S %s"
```

| Part | Description |
|------|-------------|
| `cat %s` | Read APK file |
| `pm install` | Package manager install |
| `--user %s` | Install for specific user |
| `-i %s` | Set installer package name |
| `-t` | Allow test packages |
| `-r` | Replace existing app |
| `-S %s` | Specify file size (streaming) |

### Delete Command

```kotlin
private const val DELETE_COMMAND = "%s rm %s"
```

Uses `toybox` or `busybox` for the `rm` command.

## Helper Functions

### Get Current User

```kotlin
private fun currentUser() = if (SdkCheck.isOreo) {
    Shell.cmd("am get-current-user")
        .exec()
        .out[0]
} else {
    Shell.cmd("dumpsys activity | grep -E \"mUserLru\"")
        .exec()
        .out[0]
        .trim()
        .removePrefix("mUserLru: [")
        .removeSuffix("]")
}
```

### Find Util Box

```kotlin
private fun utilBox(): String {
    listOf("toybox", "busybox").forEach {
        val out = Shell.cmd("which $it").exec().out
        if (out.isEmpty()) return ""
        if (out.first().contains("not found")) return ""
        return out.first()
    }
    return ""
}
```

## Uninstall

Uses the standard uninstall package helper:

```kotlin
suspend fun Context.uninstallPackage(packageName: PackageName) {
    val intent = Intent(Intent.ACTION_DELETE).apply {
        data = Uri.parse("package:${packageName.name}")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    startActivity(intent)
}
```

Note: Even with root, uninstall still uses standard intent for user safety.

## Requirements

1. **Root Access** - Device must be rooted
2. **Magisk/SuperSU** - Root management app installed
3. **Grant Permission** - User must grant root to Droid-ify

## Root Check

Before using, check if root is available:

```kotlin
val isRootAvailable = Shell.isAppGrantedRoot() == true
```

## Advantages

- Silent installation (no prompts)
- Can install system apps (with additional commands)
- Fastest installation method
- Works even when screen is off

## Limitations

- Requires rooted device
- User must grant root permission once
- Some ROMs may have restrictions
- Cannot install to `/system` without remounting

## Error Handling

```kotlin
Shell.cmd(installCommand).submit { shellResult ->
    if (shellResult.isSuccess) {
        // Installation successful
        cont.resume(InstallState.Installed)
    } else {
        // Check shellResult.err for error details
        cont.resume(InstallState.Failed)
    }
}
```

## Security Considerations

- Only use with trusted APKs (validated hashes)
- Root access should be granted consciously by user
- APK files are deleted after installation
- Installer package name is set for accountability

## Changes

| Change | Change Doc |
|--------|------------|
| Package: `com.looker.droidify` → `com.atd.store` | [package-rename.md](../changes/package-rename.md) |
| App branding Droid-ify → ATD Store | [app-branding.md](../changes/app-branding.md) |
