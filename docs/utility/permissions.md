# Permissions

Runtime permission handling utilities.

## Overview

**File:** `app/src/main/kotlin/com/atd/store/utility/common/Permissions.kt`

Provides helpers for checking and requesting Android permissions.

## Key Permissions

| Permission | Purpose | Required From |
|------------|---------|---------------|
| `POST_NOTIFICATIONS` | Show notifications | Android 13+ |
| `WRITE_EXTERNAL_STORAGE` | Download to external | Pre-Android 10 |
| `REQUEST_INSTALL_PACKAGES` | Install APKs | Android 8+ |
| `INSTALL_PACKAGES` | System installer | Privileged only |

## Permission Checking

### Single Permission

```kotlin
fun Context.hasPermission(permission: String): Boolean {
    return ContextCompat.checkSelfPermission(this, permission) ==
        PackageManager.PERMISSION_GRANTED
}
```

### Notification Permission

```kotlin
fun Context.hasNotificationPermission(): Boolean {
    return if (SdkCheck.isTiramisu) {
        hasPermission(Manifest.permission.POST_NOTIFICATIONS)
    } else {
        true  // Always granted before Android 13
    }
}
```

### Install Permission

```kotlin
fun Context.canInstallPackages(): Boolean {
    return if (SdkCheck.isOreo) {
        packageManager.canRequestPackageInstalls()
    } else {
        true  // Always allowed before Android 8
    }
}
```

## Permission Requests

### Activity Extension

```kotlin
fun Activity.requestNotificationPermission(requestCode: Int) {
    if (SdkCheck.isTiramisu) {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            requestCode
        )
    }
}
```

### Install Permission Intent

```kotlin
fun Context.requestInstallPermission() {
    if (SdkCheck.isOreo) {
        startActivity(
            Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:$packageName")
            )
        )
    }
}
```

## Compose Integration

### Permission State

```kotlin
@Composable
fun rememberNotificationPermissionState(): PermissionState {
    return rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
}
```

### Permission Request

```kotlin
@Composable
fun NotificationPermissionRequest(
    onGranted: () -> Unit,
    onDenied: () -> Unit
) {
    val permissionState = rememberNotificationPermissionState()

    LaunchedEffect(permissionState.status) {
        when (permissionState.status) {
            is PermissionStatus.Granted -> onGranted()
            is PermissionStatus.Denied -> onDenied()
        }
    }

    if (permissionState.status is PermissionStatus.Denied) {
        // Show rationale UI
    }
}
```

## Settings UI Integration

In `SettingsScreen.kt`:

```kotlin
@Composable
fun NotificationSettings(viewModel: SettingsViewModel) {
    val context = LocalContext.current
    val hasPermission = remember { context.hasNotificationPermission() }

    if (!hasPermission) {
        WarningBanner(
            text = stringResource(R.string.notification_permission_required),
            action = { context.requestNotificationPermission() }
        )
    }
}
```

## Manifest Declarations

```xml
<!-- Notifications (Android 13+) -->
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

<!-- Install packages -->
<uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />

<!-- Network -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

<!-- Foreground service -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC" />

<!-- Boot receiver -->
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
```

## Permission Flow

```
┌───────────────────────────────────────────────────────────┐
│                    App Launch                              │
└─────────────────────────┬─────────────────────────────────┘
                          │
                          ▼
┌───────────────────────────────────────────────────────────┐
│              Check Required Permissions                    │
│                                                            │
│  ┌─────────────────┐    ┌─────────────────┐              │
│  │ Notifications   │    │ Install Pkgs    │              │
│  │ (Android 13+)   │    │ (Android 8+)    │              │
│  └────────┬────────┘    └────────┬────────┘              │
│           │                      │                        │
│           ▼                      ▼                        │
│  ┌─────────────────┐    ┌─────────────────┐              │
│  │ Request if      │    │ Check on        │              │
│  │ not granted     │    │ install attempt │              │
│  └─────────────────┘    └─────────────────┘              │
└───────────────────────────────────────────────────────────┘
```

## Best Practices

1. **Check before request** - Always check permission status first
2. **Show rationale** - Explain why permission is needed
3. **Handle denial** - Gracefully degrade functionality
4. **Version checks** - Use `SdkCheck` for version-specific permissions
5. **Minimal permissions** - Only request what's needed
