# Broadcast Receivers

Broadcast receivers that handle system-level events and inter-component communication.

## Overview

**Location:** `app/src/main/kotlin/com/looker/droidify/receivers/`

| Receiver | Purpose |
|----------|---------|
| `CopyErrorReceiver` | Copies error details to clipboard from notifications |
| `InstalledAppReceiver` | Tracks app install/uninstall events |
| `UnarchivePackageReceiver` | Handles app unarchiving (Android 15+) |

## CopyErrorReceiver

Handles the "Copy Error" action from installation failure notifications.

**File:** `receivers/CopyErrorReceiver.kt`

### Constants

```kotlin
const val ACTION_COPY_ERROR = "com.looker.droidify.intent.action.COPY_ERROR"
const val EXTRA_ERROR_DETAILS = "error_details"
const val EXTRA_NOTIFICATION_TAG = "notification_tag"
const val EXTRA_NOTIFICATION_ID = "notification_id"
```

### Behavior

1. Receives `ACTION_COPY_ERROR` intent
2. Extracts error details from `EXTRA_ERROR_DETAILS`
3. Copies error text to clipboard via `copyToClipboard()`
4. Dismisses the notification using tag and ID

### Usage

Triggered from notification action button when installation fails:

```kotlin
// In InstallNotification
val copyIntent = Intent(context, CopyErrorReceiver::class.java).apply {
    action = CopyErrorReceiver.ACTION_COPY_ERROR
    putExtra(CopyErrorReceiver.EXTRA_ERROR_DETAILS, errorMessage)
    putExtra(CopyErrorReceiver.EXTRA_NOTIFICATION_TAG, notificationTag)
    putExtra(CopyErrorReceiver.EXTRA_NOTIFICATION_ID, notificationId)
}
```

## InstalledAppReceiver

Tracks package installations and removals to keep the installed apps database in sync.

**File:** `receivers/InstalledAppReceiver.kt`

### Constructor

```kotlin
class InstalledAppReceiver(
    private val packageManager: PackageManager,
) : BroadcastReceiver()
```

### Handled Actions

- `Intent.ACTION_PACKAGE_ADDED` - App installed
- `Intent.ACTION_PACKAGE_REMOVED` - App uninstalled

### Behavior

```kotlin
override fun onReceive(context: Context, intent: Intent) {
    val packageName = intent.data?.schemeSpecificPart
    when (intent.action) {
        ACTION_PACKAGE_ADDED, ACTION_PACKAGE_REMOVED -> {
            val packageInfo = packageManager.getPackageInfoCompat(packageName)
            if (packageInfo != null) {
                Database.InstalledAdapter.put(packageInfo.toInstalledItem())
            } else {
                Database.InstalledAdapter.delete(packageName)
            }
        }
    }
}
```

### Registration

Registered dynamically in `MainApplication`:

```kotlin
registerReceiver(
    installedAppReceiver,
    IntentFilter().apply {
        addAction(Intent.ACTION_PACKAGE_ADDED)
        addAction(Intent.ACTION_PACKAGE_REMOVED)
        addDataScheme("package")
    }
)
```

## UnarchivePackageReceiver

Handles Android 15+ app archiving feature. When user tries to open an archived app, this receiver triggers re-download.

**File:** `receivers/UnarchivePackageReceiver.kt`

### Requirements

- Android 15 (API 35) or higher
- `@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)`

### Annotation

```kotlin
@AndroidEntryPoint  // Hilt injection support
@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
```

### Intent Extras

| Extra | Description |
|-------|-------------|
| `EXTRA_UNARCHIVE_PACKAGE_NAME` | Package name to unarchive |
| `EXTRA_UNARCHIVE_ID` | System-assigned unarchive ID |
| `EXTRA_UNARCHIVE_ALL_USERS` | Whether to unarchive for all users |

### Behavior

```kotlin
override fun onReceive(context: Context, intent: Intent) {
    if (intent.action != ACTION_UNARCHIVE_PACKAGE) return

    val packageName = intent.getStringExtra(EXTRA_UNARCHIVE_PACKAGE_NAME)
    val unarchiveId = intent.getIntExtra(EXTRA_UNARCHIVE_ID, -1)
    val allUsers = intent.getBooleanExtra(EXTRA_UNARCHIVE_ALL_USERS, false)

    UnarchiveWorker.updateNow(context, packageName, unarchiveId, allUsers)
}
```

### Manifest Declaration

```xml
<receiver
    android:name=".receivers.UnarchivePackageReceiver"
    android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.UNARCHIVE_PACKAGE" />
    </intent-filter>
</receiver>
```

## SessionInstallerReceiver

Receives callbacks from PackageInstaller sessions.

**File:** `installer/installers/session/SessionInstallerReceiver.kt`

See [Session Installer](installer/session-installer.md) for details.

## Flow Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                      System Events                          │
└─────────────────────────────────────────────────────────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        ▼                     ▼                     ▼
┌───────────────┐    ┌───────────────┐    ┌───────────────────┐
│ Package       │    │ Notification  │    │ Unarchive         │
│ Added/Removed │    │ Action        │    │ Request (API 35+) │
└───────┬───────┘    └───────┬───────┘    └─────────┬─────────┘
        │                    │                      │
        ▼                    ▼                      ▼
┌───────────────┐    ┌───────────────┐    ┌───────────────────┐
│InstalledApp   │    │CopyError      │    │UnarchivePackage   │
│Receiver       │    │Receiver       │    │Receiver           │
└───────┬───────┘    └───────┬───────┘    └─────────┬─────────┘
        │                    │                      │
        ▼                    ▼                      ▼
┌───────────────┐    ┌───────────────┐    ┌───────────────────┐
│Database       │    │Clipboard +    │    │UnarchiveWorker    │
│.InstalledAdap │    │Dismiss Notif  │    │                   │
└───────────────┘    └───────────────┘    └───────────────────┘
```

## Best Practices

1. **Keep receivers lightweight** - Offload heavy work to workers/services
2. **Handle null data** - Always validate intent data before processing
3. **Use AndroidEntryPoint** - For Hilt dependency injection when needed
4. **Check API levels** - Use `@RequiresApi` for version-specific receivers
