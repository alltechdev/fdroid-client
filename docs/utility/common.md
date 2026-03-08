# Common Utilities

Core utility classes and extensions used throughout the app.

## Overview

**Directory:** `utility/common/`

Shared utilities for constants, results, extensions, device workarounds, and more.

## Constants

**File:** `utility/common/Constants.kt`

Application-wide constants.

```kotlin
object Constants {
    // Notification Channels
    const val NOTIFICATION_CHANNEL_SYNCING = "syncing"
    const val NOTIFICATION_CHANNEL_UPDATES = "updates"
    const val NOTIFICATION_CHANNEL_DOWNLOADING = "downloading"
    const val NOTIFICATION_CHANNEL_INSTALL = "install"

    // Notification IDs
    const val NOTIFICATION_ID_SYNCING = 1
    const val NOTIFICATION_ID_UPDATES = 2
    const val NOTIFICATION_ID_DOWNLOADING = 3
    const val NOTIFICATION_ID_INSTALL = 4
    const val NOTIFICATION_ID_RB_DOWNLOAD = 5
    const val NOTIFICATION_ID_STATS_DOWNLOAD = 6
    const val NOTIFICATION_ID_INDEX_DOWNLOAD = 7

    // Job IDs
    const val JOB_ID_SYNC = 1
}
```

## Result Type

**File:** `utility/common/result/Result.kt`

Generic result wrapper for operations.

```kotlin
sealed interface Result<out T> {
    data class Success<T>(val data: T) : Result<T>

    data class Error<T>(
        val exception: Throwable? = null,
        val data: T? = null  // Partial data on error
    ) : Result<T>
}
```

## Extensions

**Directory:** `utility/common/extension/`

### Flow Extensions

**File:** `extension/Flow.kt`

Kotlin Flow utilities.

```kotlin
context(viewModel: ViewModel)
fun <T> Flow<T>.asStateFlow(
    initialValue: T,
    scope: CoroutineScope = viewModel.viewModelScope,
    started: SharingStarted = SharingStarted.WhileSubscribed(5_000),
): StateFlow<T>
```

Usage in ViewModel:
```kotlin
val state: StateFlow<AppDetailState> = appRepository
    .getApp(packageName)
    .map { ... }
    .asStateFlow(AppDetailState.Loading)  // Context receiver provides ViewModel
```

### Channel Filter

```kotlin
context(scope: CoroutineScope)
fun <T> ReceiveChannel<T>.filter(
    block: suspend (T) -> Boolean,
): ReceiveChannel<T>
```

### Context Extensions

**File:** `extension/Context.kt`

System service accessors:
```kotlin
inline val Context.clipboardManager: ClipboardManager?
inline val Context.inputManager: InputMethodManager?
inline val Context.jobScheduler: JobScheduler?
inline val Context.notificationManager: NotificationManager?
inline val Context.powerManager: PowerManager?
```

Utility functions:
```kotlin
fun Context.copyToClipboard(clip: String)
fun Context.openLink(url: String)
```

Drawable helpers:
```kotlin
val Context.corneredBackground: Drawable
val Context.divider: Drawable
val Context.homeAsUp: Drawable
val Context.selectableBackground: Drawable
fun Context.getMutatedIcon(@DrawableRes id: Int): Drawable
fun Context.getDrawableCompat(@DrawableRes resId: Int): Drawable
fun Context.getColorFromAttr(@AttrRes attrResId: Int): ColorStateList
```

### Collection Extensions

**File:** `extension/Collections.kt`

Map windowing for batch processing:
```kotlin
inline fun <K, E> Map<K, E>.windowed(windowSize: Int, block: (Map<K, E>) -> Unit)
```

Mutable collection transformers:
```kotlin
inline fun <K, E> Map<K, E>.updateAsMutable(block: MutableMap<K, E>.() -> Unit): Map<K, E>
inline fun <T> Set<T>.updateAsMutable(block: MutableSet<T>.() -> Unit): Set<T>
inline fun <T> List<T>.updateAsMutable(block: MutableList<T>.() -> Unit): List<T>
```

Set operations:
```kotlin
inline fun <T> MutableSet<T>.addAndCompute(item: T, block: (isAdded: Boolean) -> Unit): Boolean
```

### Other Extensions

| File | Purpose |
|------|---------|
| `Cursor.kt` | Database cursor utilities |
| `DateTime.kt` | Date/time formatting |
| `Exception.kt` | Exception handling helpers |
| `File.kt` | File operations |
| `Insets.kt` | Window insets handling |
| `Intent.kt` | Intent building |
| `Json.kt` | JSON serialization |
| `Number.kt` | Number formatting |
| `PackageInfo.kt` | Package metadata extraction |
| `Service.kt` | Service utilities |
| `View.kt` | View manipulation |

## Device Workarounds

**Directory:** `utility/common/device/`

### Huawei

**File:** `device/Huawei.kt`

Workarounds for Huawei devices with EMUI.

### MIUI

**File:** `device/Miui.kt`

Workarounds for Xiaomi devices with MIUI.

## Other Utilities

### SdkCheck

**File:** `utility/common/SdkCheck.kt`

Android SDK version checks.

```kotlin
object SdkCheck {
    val isNougat: Boolean  // API 24+
    val isOreo: Boolean    // API 26+
    val isSnowCake: Boolean // API 31+ (Android 12)
    // etc.
}
```

### Cache

**File:** `utility/common/cache/Cache.kt`

Cache management utilities.

### Deeplinks

**File:** `utility/common/Deeplinks.kt`

Deep link parsing and handling.

### Notification

**File:** `utility/common/Notification.kt`

Notification building helpers.

### Permissions

**File:** `utility/common/Permissions.kt`

Runtime permission utilities.

### Scroller

**File:** `utility/common/Scroller.kt`

RecyclerView smooth scrolling utilities.

### Text

**File:** `utility/common/Text.kt`

Text formatting utilities.

## Removed

| Feature | Removal Doc |
|---------|-------------|
| `Exporter` interface | [proxy-and-backup.md](../removal/proxy-and-backup.md) |
