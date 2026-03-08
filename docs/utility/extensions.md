# Kotlin Extensions

Extension functions organized by type and purpose.

## Overview

**Location:** `app/src/main/kotlin/com/atd/store/utility/`

Extensions are organized into two packages:
- `utility/common/extension/` - General-purpose extensions
- `utility/extension/` - App-specific extensions

## Context Extensions

**File:** `common/extension/Context.kt`

### Clipboard

```kotlin
fun Context.copyToClipboard(text: String) {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(null, text))
}
```

### System Services

```kotlin
val Context.notificationManager: NotificationManager?
    get() = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

val Context.connectivityManager: ConnectivityManager?
    get() = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
```

### Resources

```kotlin
fun Context.getColorFromAttr(@AttrRes attr: Int): ColorStateList {
    val typedValue = TypedValue()
    theme.resolveAttribute(attr, typedValue, true)
    return ColorStateList.valueOf(typedValue.data)
}

val Context.divider: Drawable
    get() = getDrawable(R.drawable.divider)!!

val Context.videoPlaceHolder: Drawable
    get() = getDrawable(R.drawable.ic_play)!!
```

## PackageInfo Extensions

**Files:** `common/extension/PackageInfo.kt`, `extension/PackageInfo.kt`

### Get Package Info

```kotlin
fun PackageManager.getPackageInfoCompat(
    packageName: String,
    signatureFlag: Int = 0
): PackageInfo? {
    return try {
        if (SdkCheck.isTiramisu) {
            getPackageInfo(packageName, PackageInfoFlags.of(signatureFlag.toLong()))
        } else {
            @Suppress("DEPRECATION")
            getPackageInfo(packageName, signatureFlag)
        }
    } catch (e: Exception) {
        null
    }
}
```

### Convert to InstalledItem

```kotlin
fun PackageInfo.toInstalledItem(): InstalledItem {
    val versionCode = if (SdkCheck.isPie) {
        longVersionCode
    } else {
        @Suppress("DEPRECATION")
        versionCode.toLong()
    }

    val signature = signatures?.firstOrNull()
        ?.let { MessageDigest.getInstance("SHA-256").digest(it.toByteArray()) }
        ?.toHexString()
        .orEmpty()

    return InstalledItem(packageName, versionName.orEmpty(), versionCode, signature)
}
```

## Intent Extensions

**File:** `common/extension/Intent.kt`

### Query Parameter Access

```kotlin
operator fun Uri.get(key: String): String? = getQueryParameter(key)
```

### Intent Extras

```kotlin
inline fun <reified T : Parcelable> Intent.parcelable(key: String): T? {
    return if (SdkCheck.isTiramisu) {
        getParcelableExtra(key, T::class.java)
    } else {
        @Suppress("DEPRECATION")
        getParcelableExtra(key)
    }
}
```

## View Extensions

**File:** `common/extension/View.kt`

### Visibility

```kotlin
fun View.show() { visibility = View.VISIBLE }
fun View.hide() { visibility = View.GONE }
fun View.invisible() { visibility = View.INVISIBLE }
```

### Click Listeners

```kotlin
inline fun View.setOnSafeClickListener(
    debounceTime: Long = 500L,
    crossinline action: (View) -> Unit
) {
    var lastClickTime = 0L
    setOnClickListener {
        val currentTime = SystemClock.elapsedRealtime()
        if (currentTime - lastClickTime >= debounceTime) {
            lastClickTime = currentTime
            action(it)
        }
    }
}
```

## Cursor Extensions

**File:** `common/extension/Cursor.kt`

### Iteration

```kotlin
fun Cursor.asSequence(): Sequence<Cursor> = sequence {
    while (moveToNext()) yield(this@asSequence)
}

fun Cursor.firstOrNull(): Cursor? = if (moveToFirst()) this else null
```

### Safe Column Access

```kotlin
fun Cursor.getStringOrNull(columnIndex: Int): String? =
    if (isNull(columnIndex)) null else getString(columnIndex)

fun Cursor.getLongOrNull(columnIndex: Int): Long? =
    if (isNull(columnIndex)) null else getLong(columnIndex)
```

## Flow Extensions

**Files:** `common/extension/Flow.kt`, `extension/Flow.kt`

### Collection Helpers

```kotlin
fun <T> Flow<List<T>>.filterNotEmpty(): Flow<List<T>> =
    filter { it.isNotEmpty() }
```

### State Flow

```kotlin
fun <T> MutableStateFlow<T>.update(transform: (T) -> T) {
    value = transform(value)
}
```

## Collection Extensions

**File:** `common/extension/Collections.kt`

### Safe Access

```kotlin
fun <T> List<T>.getOrNull(index: Int): T? =
    if (index in indices) this[index] else null
```

### Grouping

```kotlin
fun <T, K> Iterable<T>.groupByNotNull(keySelector: (T) -> K?): Map<K, List<T>> {
    return mapNotNull { item -> keySelector(item)?.let { it to item } }
        .groupBy({ it.first }, { it.second })
}
```

## DateTime Extensions

**File:** `common/extension/DateTime.kt`

### Formatting

```kotlin
fun Long.toFormattedDate(context: Context): String {
    val dateFormat = DateFormat.getDateFormat(context)
    return dateFormat.format(Date(this))
}

fun Long.toRelativeTime(context: Context): String {
    return DateUtils.getRelativeTimeSpanString(
        this,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS
    ).toString()
}
```

## File Extensions

**File:** `common/extension/File.kt`

### Size Formatting

```kotlin
fun Long.toFormattedSize(): String {
    return when {
        this < 1024 -> "$this B"
        this < 1024 * 1024 -> "${this / 1024} KB"
        this < 1024 * 1024 * 1024 -> "${this / (1024 * 1024)} MB"
        else -> "${this / (1024 * 1024 * 1024)} GB"
    }
}
```

### Hash Calculation

```kotlin
fun File.sha256(): String {
    val digest = MessageDigest.getInstance("SHA-256")
    inputStream().use { input ->
        val buffer = ByteArray(8192)
        var read: Int
        while (input.read(buffer).also { read = it } != -1) {
            digest.update(buffer, 0, read)
        }
    }
    return digest.digest().toHexString()
}
```

## Number Extensions

**File:** `common/extension/Number.kt`

### Byte Array to Hex

```kotlin
fun ByteArray.toHexString(): String =
    joinToString("") { "%02x".format(it) }
```

### Formatting

```kotlin
fun Int.toFormattedCount(): String = when {
    this < 1000 -> toString()
    this < 1_000_000 -> "${this / 1000}K"
    else -> "${this / 1_000_000}M"
}
```

## Insets Extensions

**File:** `common/extension/Insets.kt`

### Window Insets

```kotlin
fun View.applySystemWindowInsets(
    top: Boolean = false,
    bottom: Boolean = false,
    left: Boolean = false,
    right: Boolean = false
) {
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        view.updatePadding(
            top = if (top) systemBars.top else view.paddingTop,
            bottom = if (bottom) systemBars.bottom else view.paddingBottom,
            left = if (left) systemBars.left else view.paddingLeft,
            right = if (right) systemBars.right else view.paddingRight
        )
        insets
    }
}
```

## Service Extensions

**File:** `common/extension/Service.kt`

### Foreground Service

```kotlin
fun Service.startForegroundCompat(id: Int, notification: Notification, type: Int) {
    if (SdkCheck.isQ) {
        startForeground(id, notification, type)
    } else {
        startForeground(id, notification)
    }
}
```

## Exception Extensions

**File:** `common/extension/Exception.kt`

### Logging

```kotlin
fun Exception.log(tag: String = "ERROR") {
    Log.e(tag, message, this)
}
```

## Fragment Extensions

**File:** `extension/Fragment.kt`

### Safe Navigation

```kotlin
fun Fragment.navigateSafe(@IdRes resId: Int, args: Bundle? = null) {
    try {
        findNavController().navigate(resId, args)
    } catch (e: Exception) {
        e.log("Navigation")
    }
}
```

## Resources Extensions

**File:** `extension/Resources.kt`

### Drawable Tinting

```kotlin
fun Drawable.withTint(@ColorInt color: Int): Drawable {
    return mutate().apply { setTint(color) }
}
```

## Connection Extensions

**File:** `extension/Connection.kt`

### Network State

```kotlin
val Context.isNetworkAvailable: Boolean
    get() = connectivityManager?.activeNetwork != null
```

## Best Practices

1. **Nullability** - Extensions on nullable types use `?` receiver
2. **SDK Checks** - Version-specific code uses `SdkCheck`
3. **Naming** - Prefix with action (e.g., `toFormattedSize`)
4. **Scope** - Keep extensions focused and single-purpose
