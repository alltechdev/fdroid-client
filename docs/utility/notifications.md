# Notification Utilities

Notification creation and management helpers.

## Overview

**Files:**
- `utility/common/Notification.kt` - Channel setup
- `utility/notifications/InstallNotification.kt` - Install notifications
- `utility/notifications/UpdateNotification.kt` - Update notifications
- `utility/notifications/WorkerNotification.kt` - Background worker notifications

## Notification Channels

**File:** `utility/common/Notification.kt`

### Channel IDs

```kotlin
object Constants {
    const val NOTIFICATION_CHANNEL_SYNCING = "syncing"
    const val NOTIFICATION_CHANNEL_UPDATES = "updates"
    const val NOTIFICATION_CHANNEL_DOWNLOADING = "downloading"
    const val NOTIFICATION_CHANNEL_INSTALL = "install"
}
```

### Channel Creation

```kotlin
fun Context.createNotificationChannels() {
    if (!SdkCheck.isOreo) return

    val channels = listOf(
        NotificationChannel(
            NOTIFICATION_CHANNEL_SYNCING,
            getString(R.string.syncing),
            NotificationManager.IMPORTANCE_LOW
        ),
        NotificationChannel(
            NOTIFICATION_CHANNEL_UPDATES,
            getString(R.string.updates),
            NotificationManager.IMPORTANCE_DEFAULT
        ),
        NotificationChannel(
            NOTIFICATION_CHANNEL_DOWNLOADING,
            getString(R.string.downloading),
            NotificationManager.IMPORTANCE_LOW
        ),
        NotificationChannel(
            NOTIFICATION_CHANNEL_INSTALL,
            getString(R.string.installation),
            NotificationManager.IMPORTANCE_HIGH
        )
    )

    notificationManager?.createNotificationChannels(channels)
}
```

## InstallNotification

**File:** `utility/notifications/InstallNotification.kt`

### Notification IDs

Uses package name hash for unique per-app IDs:

```kotlin
private fun packageNotificationId(packageName: String): Int =
    packageName.hashCode()
```

### Success Notification

```kotlin
fun Context.showInstallSuccessNotification(packageName: String, appName: String) {
    val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_INSTALL)
        .setSmallIcon(R.drawable.ic_check)
        .setContentTitle(getString(R.string.installed))
        .setContentText(appName)
        .setAutoCancel(true)
        .setContentIntent(openAppPendingIntent(packageName))
        .build()

    notificationManager?.notify(
        packageName,
        packageNotificationId(packageName),
        notification
    )
}
```

### Failure Notification with Copy Action

```kotlin
fun Context.showInstallFailureNotification(
    packageName: String,
    appName: String,
    errorMessage: String
) {
    val notificationId = packageNotificationId(packageName)

    val copyIntent = Intent(this, CopyErrorReceiver::class.java).apply {
        action = CopyErrorReceiver.ACTION_COPY_ERROR
        putExtra(CopyErrorReceiver.EXTRA_ERROR_DETAILS, errorMessage)
        putExtra(CopyErrorReceiver.EXTRA_NOTIFICATION_TAG, packageName)
        putExtra(CopyErrorReceiver.EXTRA_NOTIFICATION_ID, notificationId)
    }

    val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_INSTALL)
        .setSmallIcon(R.drawable.ic_error)
        .setContentTitle(getString(R.string.installation_failed))
        .setContentText(appName)
        .setStyle(NotificationCompat.BigTextStyle().bigText(errorMessage))
        .addAction(
            R.drawable.ic_copy,
            getString(R.string.copy_error),
            PendingIntent.getBroadcast(this, notificationId, copyIntent, FLAGS)
        )
        .build()

    notificationManager?.notify(packageName, notificationId, notification)
}
```

## UpdateNotification

**File:** `utility/notifications/UpdateNotification.kt`

### Updates Available

```kotlin
fun Context.showUpdatesNotification(updateCount: Int, updates: List<ProductItem>) {
    val style = NotificationCompat.InboxStyle()
        .setBigContentTitle(getString(R.string.updates_available, updateCount))

    updates.take(5).forEach { update ->
        style.addLine("${update.name} → ${update.version}")
    }

    if (updates.size > 5) {
        style.setSummaryText(getString(R.string.and_more, updates.size - 5))
    }

    val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_UPDATES)
        .setSmallIcon(R.drawable.ic_update)
        .setContentTitle(getString(R.string.updates_available, updateCount))
        .setStyle(style)
        .setContentIntent(openAppPendingIntent())
        .setAutoCancel(true)
        .build()

    notificationManager?.notify(NOTIFICATION_ID_UPDATES, notification)
}
```

## WorkerNotification

**File:** `utility/notifications/WorkerNotification.kt`

### Sync Progress

```kotlin
fun Context.createSyncNotification(
    repositoryName: String,
    progress: Int,
    max: Int
): Notification {
    return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_SYNCING)
        .setSmallIcon(R.drawable.ic_sync)
        .setContentTitle(getString(R.string.syncing))
        .setContentText(repositoryName)
        .setProgress(max, progress, progress == 0)
        .setOngoing(true)
        .build()
}
```

### Download Progress

```kotlin
fun Context.createDownloadNotification(
    appName: String,
    progress: Int,
    total: Long,
    downloaded: Long
): Notification {
    val progressText = if (total > 0) {
        "${downloaded.toFormattedSize()} / ${total.toFormattedSize()}"
    } else {
        downloaded.toFormattedSize()
    }

    return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_DOWNLOADING)
        .setSmallIcon(R.drawable.ic_download)
        .setContentTitle(appName)
        .setContentText(progressText)
        .setProgress(100, progress, progress == 0)
        .setOngoing(true)
        .build()
}
```

## Pending Intents

### Open App

```kotlin
private fun Context.openAppPendingIntent(
    packageName: String? = null
): PendingIntent {
    val intent = if (packageName != null) {
        packageManager.getLaunchIntentForPackage(packageName)
    } else {
        Intent(this, MainActivity::class.java)
    }

    return PendingIntent.getActivity(
        this,
        0,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}
```

### Cancel Download

```kotlin
fun Context.cancelDownloadPendingIntent(packageName: String): PendingIntent {
    val intent = Intent(this, DownloadService::class.java).apply {
        action = DownloadService.ACTION_CANCEL
        putExtra(DownloadService.EXTRA_PACKAGE_NAME, packageName)
    }

    return PendingIntent.getService(
        this,
        packageName.hashCode(),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}
```

## Notification Flow

```
┌────────────────────────────────────────────────────────────┐
│                    Notification Sources                     │
├──────────────┬──────────────┬──────────────┬───────────────┤
│  SyncWorker  │ DownloadSvc  │ InstallMgr   │  UpdateCheck  │
└──────┬───────┴──────┬───────┴──────┬───────┴───────┬───────┘
       │              │              │               │
       ▼              ▼              ▼               ▼
┌─────────────────────────────────────────────────────────────┐
│                    Notification Helpers                      │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────────────────┐│
│  │WorkerNotif  │ │WorkerNotif  │ │InstallNotification      ││
│  │(sync)       │ │(download)   │ │(success/failure)        ││
│  └─────────────┘ └─────────────┘ └─────────────────────────┘│
│                                  ┌─────────────────────────┐│
│                                  │UpdateNotification       ││
│                                  │(updates available)      ││
│                                  └─────────────────────────┘│
└────────────────────────────────┬────────────────────────────┘
                                 │
                                 ▼
┌─────────────────────────────────────────────────────────────┐
│                   NotificationManager                        │
│                                                              │
│  Channels:                                                   │
│  - syncing (LOW)                                            │
│  - downloading (LOW)                                        │
│  - updates (DEFAULT)                                        │
│  - install (HIGH)                                           │
└─────────────────────────────────────────────────────────────┘
```

## Best Practices

1. **Unique IDs** - Use package name hash for per-app notifications
2. **Immutable flags** - Always use `FLAG_IMMUTABLE` on Android 12+
3. **Auto-cancel** - Set for one-time notifications
4. **Ongoing** - Set for progress notifications
5. **Big text style** - Use for error messages
6. **Inbox style** - Use for update lists
