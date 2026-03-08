# Notifications

Droid-ify uses Android notifications to inform users about updates, downloads, installations, and sync progress.

## Overview

**Key Files:**
- `utility/notifications/InstallNotification.kt` - Installation status
- `utility/notifications/UpdateNotification.kt` - Available updates
- `utility/notifications/WorkerNotification.kt` - Background task progress
- `utility/common/Constants.kt` - Notification IDs and channels

## Notification Channels

| Channel ID | Name | Purpose |
|------------|------|---------|
| `NOTIFICATION_CHANNEL_SYNCING` | Syncing | Repository sync progress |
| `NOTIFICATION_CHANNEL_UPDATES` | Updates | Available app updates |
| `NOTIFICATION_CHANNEL_DOWNLOADING` | Downloading | Download progress |
| `NOTIFICATION_CHANNEL_INSTALL` | Install | Installation status |

### Creating Channels

```kotlin
fun Context.createNotificationChannel(
    id: String,
    name: String,
    showBadge: Boolean = false,
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            id,
            name,
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            setShowBadge(showBadge)
        }
        notificationManager?.createNotificationChannel(channel)
    }
}
```

## Update Notifications

Shows when updates are available after sync:

```kotlin
fun updatesAvailableNotification(
    context: Context,
    productItems: List<ProductItem>,
) = NotificationCompat
    .Builder(context, Constants.NOTIFICATION_CHANNEL_UPDATES)
    .setSmallIcon(R.drawable.ic_new_releases)
    .setContentTitle(context.getString(R.string.new_updates_available))
    .setContentText(
        context.resources.getQuantityString(
            R.plurals.new_updates_DESC_FORMAT,
            productItems.size,
            productItems.size,
        ),
    )
    .setContentIntent(
        PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java)
                .setAction(MainActivity.ACTION_UPDATES),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        ),
    )
    .setStyle(
        NotificationCompat.InboxStyle().also {
            for (productItem in productItems.take(MAX_UPDATE_NOTIFICATION)) {
                it.addLine("${productItem.name} ${productItem.version}")
            }
            if (productItems.size > MAX_UPDATE_NOTIFICATION) {
                it.addLine(context.getString(
                    R.string.plus_more_FORMAT,
                    productItems.size - MAX_UPDATE_NOTIFICATION,
                ))
            }
        },
    )
    .build()
```

### Features

- Lists up to 5 apps with available updates
- Shows "+N more" for additional updates
- Tapping opens updates view in app
- Auto-triggered after sync completion

## Install Notifications

Shows installation status for each app:

```kotlin
fun Context.createInstallNotification(
    appName: String,
    state: InstallState,
    isUninstall: Boolean = false,
    autoCancel: Boolean = true,
    block: NotificationCompat.Builder.() -> Unit = {},
): Notification
```

### Install States

| State | Icon | Title | Description |
|-------|------|-------|-------------|
| `Pending` | Download | "Downloaded: {app}" | "Tap to install" |
| `Installing` | Download | "Installing" | App name + progress |
| `Installed` | Check | "Installed" | App name |
| `Failed` | Bug | "Installation failed" | App name + error |

### State-Specific Content

```kotlin
when (state) {
    InstallState.Pending -> {
        setSmallIcon(R.drawable.ic_download)
        getString(R.string.downloaded_FORMAT, appName) to
            getString(R.string.tap_to_install_DESC)
    }

    InstallState.Installing -> {
        setSmallIcon(R.drawable.ic_download)
        setProgress(-1, -1, true)  // Indeterminate
        getString(R.string.installing) to appName
    }

    InstallState.Installed -> {
        setTimeoutAfter(SUCCESS_TIMEOUT)  // Auto-dismiss after 5s
        setSmallIcon(R.drawable.ic_check)
        getString(R.string.installed) to appName
    }

    InstallState.Failed -> {
        setSmallIcon(R.drawable.ic_bug_report)
        getString(R.string.installation_failed) to
            getString(R.string.installation_failed_DESC, appName)
    }
}
```

### Notification Manager Extensions

```kotlin
fun NotificationManager.installNotification(
    packageName: String,
    notification: Notification,
) {
    notify(
        installTag(packageName),
        NOTIFICATION_ID_INSTALL,
        notification
    )
}

fun NotificationManager.removeInstallNotification(packageName: String) {
    cancel(installTag(packageName), NOTIFICATION_ID_INSTALL)
}

private fun installTag(name: String): String =
    "install-${name.trim().replace(' ', '_')}"
```

## Download Notifications

Progress notifications during APK download:

```kotlin
// In DownloadService
stateNotificationBuilder
    .setContentTitle(getString(R.string.downloading_FORMAT, taskName))
    .setContentText("${bytesRead} / ${totalBytes}")
    .setProgress(100, percentComplete, false)
```

### Download States

```kotlin
when (state) {
    is State.Connecting -> {
        setContentText(getString(R.string.connecting))
        setProgress(1, 0, true)  // Indeterminate
    }

    is State.Downloading -> {
        if (state.total != null) {
            setContentText("${state.read} / ${state.total}")
            setProgress(100, state.read percentBy state.total, false)
        } else {
            setContentText(state.read.toString())
            setProgress(0, 0, true)  // Indeterminate (unknown size)
        }
    }
}
```

## Sync Notifications

Shows repository sync progress:

```kotlin
// In SyncService
when (state) {
    is State.Connecting -> {
        setContentText(getString(R.string.connecting))
        setProgress(0, 0, true)
    }

    is State.Syncing -> {
        when (state.stage) {
            Stage.DOWNLOAD -> {
                setContentText("${state.read} / ${state.total}")
                setProgress(100, percent, false)
            }
            Stage.PROCESS -> {
                setContentText(getString(R.string.processing_FORMAT, "$percent%"))
            }
            Stage.MERGE -> {
                setContentText(getString(R.string.merging_FORMAT, "${state.read}/${state.total}"))
            }
            Stage.COMMIT -> {
                setContentText(getString(R.string.saving_details))
                setProgress(0, 0, true)
            }
        }
    }
}
```

## Worker Notifications

Background task notifications:

```kotlin
// Download stats
fun Context.createDownloadStatsNotification(): Notification {
    return NotificationCompat
        .Builder(this, NOTIFICATION_CHANNEL_SYNCING)
        .setSmallIcon(android.R.drawable.stat_sys_download)
        .setContentTitle(getString(R.string.downloading))
        .setContentText(getString(R.string.downloading_download_stats))
        .setProgress(-1, -1, true)
        .build()
}

// Reproducible build logs
fun Context.createRbNotification(): Notification {
    return NotificationCompat
        .Builder(this, NOTIFICATION_CHANNEL_SYNCING)
        .setSmallIcon(android.R.drawable.stat_sys_download)
        .setContentTitle(getString(R.string.downloading))
        .setContentText(getString(R.string.downloading_rb_logs))
        .setProgress(-1, -1, true)
        .build()
}
```

## Cancel Actions

Notifications with cancel actions:

```kotlin
// Cancel button in notification
.addAction(
    0,
    getString(R.string.cancel),
    PendingIntent.getService(
        this,
        0,
        Intent(this, DownloadService::class.java)
            .setAction(ACTION_CANCEL),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    ),
)
```

## Error Notifications

Sync and download errors:

```kotlin
fun showNotificationError(repository: Repository, exception: Exception) {
    val description = when (exception) {
        is RepositoryUpdater.UpdateException -> getString(
            when (exception.errorType) {
                ErrorType.NETWORK -> R.string.network_error_DESC
                ErrorType.HTTP -> R.string.http_error_DESC
                ErrorType.VALIDATION -> R.string.validation_index_error_DESC
                ErrorType.PARSING -> R.string.parsing_index_error_DESC
            },
        )
        else -> getString(R.string.unknown_error_DESC)
    }

    // Copy error details action
    val copyIntent = Intent(this, CopyErrorReceiver::class.java).apply {
        action = CopyErrorReceiver.ACTION_COPY_ERROR
        putExtra(CopyErrorReceiver.EXTRA_ERROR_DETAILS, fullErrorDetails)
    }

    notificationManager?.notify(
        "repository-${repository.id}",
        NOTIFICATION_ID_SYNCING,
        NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_SYNCING)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle(getString(R.string.could_not_sync_FORMAT, repository.name))
            .setContentText(description)
            .addAction(0, getString(R.string.copy_error_details), copyPendingIntent)
            .setAutoCancel(true)
            .build(),
    )
}
```

## Notification IDs

```kotlin
object Constants {
    const val NOTIFICATION_ID_SYNCING = 1
    const val NOTIFICATION_ID_UPDATES = 2
    const val NOTIFICATION_ID_DOWNLOADING = 3
    const val NOTIFICATION_ID_INSTALL = 4
    const val NOTIFICATION_ID_STATS_DOWNLOAD = 5
    const val NOTIFICATION_ID_RB_DOWNLOAD = 6
}
```

## ForegroundInfo Helper

Converting notifications to ForegroundInfo for workers:

```kotlin
fun Notification.toForegroundInfo(id: Int): ForegroundInfo =
    ForegroundInfo(id, this)

// Usage
setForegroundAsync(notification.toForegroundInfo(NOTIFICATION_ID))
```

## Best Practices

1. **Use unique tags** - Use package names in tags to avoid conflicts
2. **Auto-dismiss success** - Success notifications should timeout
3. **Provide actions** - Include cancel/retry/open actions where appropriate
4. **Progress updates** - Sample progress updates to avoid excessive redraws
5. **Error details** - Include copy-to-clipboard for error details

## Changes

| Change | Change Doc |
|--------|------------|
| Package: `com.looker.droidify` → `com.atd.store` | [package-rename.md](../changes/package-rename.md) |
| App branding Droid-ify → ATD Store | [app-branding.md](../changes/app-branding.md) |
