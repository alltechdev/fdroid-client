# Cache System

File caching utilities for APKs, images, and temporary files with automatic cleanup.

## Overview

**File:** `app/src/main/kotlin/com/atd/store/utility/common/cache/Cache.kt`

The Cache object manages all cached files with automatic cleanup based on age.

## Directory Structure

```
cache/
├── releases/      # Downloaded APK files
├── partial/       # Partially downloaded files
├── images/        # App icons and screenshots
├── index/         # Repository index files
└── temporary/     # Temporary files
```

### Directory Constants

```kotlin
private const val RELEASE_DIR = "releases"
private const val PARTIAL_DIR = "partial"
private const val IMAGES_DIR = "images"
private const val INDEX_DIR = "index"
private const val TEMP_DIR = "temporary"
```

## File Access Methods

### Release Files (APKs)

```kotlin
fun getReleaseFile(context: Context, cacheFileName: String): File {
    return File(ensureCacheDir(context, RELEASE_DIR), cacheFileName).apply {
        sdkAbove(Build.VERSION_CODES.N) {
            // Make readable for package installer
            val cacheDir = context.cacheDir.parentFile!!.parentFile!!
            generateSequence(this) { it.parentFile!! }
                .takeWhile { it != cacheDir }
                .forEach {
                    when {
                        it.isDirectory -> applyOrMode(it, 0b001001001)
                        it.isFile -> applyOrMode(it, 0b100100100)
                    }
                }
        }
    }
}
```

The permission modification ensures PackageInstaller can read APK files.

### Partial Files (Resumable Downloads)

```kotlin
fun getPartialReleaseFile(context: Context, cacheFileName: String): File {
    return File(ensureCacheDir(context, PARTIAL_DIR), cacheFileName)
}
```

### Image Files

```kotlin
fun getImagesDir(context: Context): File {
    return ensureCacheDir(context, IMAGES_DIR)
}
```

### Index Files

```kotlin
fun getIndexFile(context: Context, indexName: String): File {
    return File(ensureCacheDir(context, INDEX_DIR), indexName)
}
```

### Temporary Files

```kotlin
fun getTemporaryFile(context: Context): File {
    return File(ensureCacheDir(context, TEMP_DIR), UUID.randomUUID().toString())
}
```

## Content URI Generation

For sharing APKs with PackageInstaller via ContentProvider:

```kotlin
fun getReleaseUri(context: Context, cacheFileName: String): Uri {
    val file = getReleaseFile(context, cacheFileName)
    val packageInfo = context.packageManager.getPackageInfoCompat(
        packageName = context.packageName,
        signatureFlag = PackageManager.GET_PROVIDERS
    )
    val authority = packageInfo?.providers
        ?.find { it.name == Provider::class.java.name }!!.authority

    return Uri.Builder()
        .scheme("content")
        .authority(authority)
        .encodedPath(file.path.drop(context.cacheDir.path.length))
        .build()
}
```

## Cache Cleanup

### Cleanup Invocation

```kotlin
fun cleanup(context: Context) {
    thread {
        cleanup(
            context,
            Pair(IMAGES_DIR, 7.days),
            Pair(INDEX_DIR, Duration.INFINITE),
            Pair(PARTIAL_DIR, 1.days),
            Pair(RELEASE_DIR, 1.days),
            Pair(TEMP_DIR, 1.hours),
        )
    }
}
```

### Retention Periods

| Directory | Retention |
|-----------|-----------|
| `images/` | 7 days |
| `index/` | Forever (never cleaned) |
| `partial/` | 1 day |
| `releases/` | 1 day |
| `temporary/` | 1 hour |

### Cleanup Implementation

```kotlin
private fun cleanupDir(dir: File, duration: Duration) {
    dir.listFiles()?.forEach {
        val older = duration <= Duration.ZERO || run {
            val olderThan = System.currentTimeMillis() / 1000L - duration.inWholeSeconds
            try {
                val stat = Os.lstat(it.path)
                stat.st_atime < olderThan  // Check access time
            } catch (e: Exception) {
                false
            }
        }
        if (older) {
            if (it.isDirectory) {
                cleanupDir(it, duration)
                it.delete()
            } else {
                it.delete()
            }
        }
    }
}
```

Uses `st_atime` (access time) to determine file age.

## ContentProvider

Exposes cached release files to system PackageInstaller.

### Provider Implementation

```kotlin
class Provider : ContentProvider() {
    override fun getType(uri: Uri): String = "application/vnd.android.package-archive"

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        val openMode = when (mode) {
            "r" -> ParcelFileDescriptor.MODE_READ_ONLY
            "w", "wt" -> MODE_WRITE_ONLY or MODE_CREATE or MODE_TRUNCATE
            "wa" -> MODE_WRITE_ONLY or MODE_CREATE or MODE_APPEND
            "rw" -> MODE_READ_WRITE or MODE_CREATE
            "rwt" -> MODE_READ_WRITE or MODE_CREATE or MODE_TRUNCATE
            else -> throw IllegalArgumentException()
        }
        val file = getFileAndTypeForUri(uri).first
        return ParcelFileDescriptor.open(file, openMode)
    }

    override fun query(...): Cursor {
        val file = getFileAndTypeForUri(uri).first
        return MatrixCursor(arrayOf(DISPLAY_NAME, SIZE)).apply {
            addRow(arrayOf(file.name, file.length()))
        }
    }
}
```

### Manifest Declaration

```xml
<provider
    android:name=".utility.common.cache.Cache$Provider"
    android:authorities="${applicationId}.cache"
    android:exported="false"
    android:grantUriPermissions="true" />
```

## Utility Functions

### Check Available Space

```kotlin
fun getEmptySpace(context: Context): Long {
    val dir = context.cacheDir
    return min(dir.usableSpace, dir.freeSpace)
}
```

### Ensure Directory Exists

```kotlin
private fun ensureCacheDir(context: Context, name: String): File {
    return File(context.cacheDir, name).apply {
        isDirectory || mkdirs() || throw RuntimeException()
    }
}
```

### Apply File Permissions

```kotlin
private fun applyOrMode(file: File, mode: Int) {
    val oldMode = Os.stat(file.path).st_mode and 0b111111111111
    val newMode = oldMode or mode
    if (newMode != oldMode) {
        Os.chmod(file.path, newMode)
    }
}
```

## Usage Examples

### Download Manager

```kotlin
// Get partial file for resume
val partialFile = Cache.getPartialReleaseFile(context, fileName)

// On completion, move to release
val releaseFile = Cache.getReleaseFile(context, fileName)
partialFile.renameTo(releaseFile)

// Get URI for installer
val uri = Cache.getReleaseUri(context, fileName)
```

### Image Loading

```kotlin
val imagesDir = Cache.getImagesDir(context)
val iconFile = File(imagesDir, "${packageName}_${hash}.png")
```

## Thread Safety

- File operations are thread-safe
- Cleanup runs on background thread via `thread {}`
- Directory creation uses atomic `mkdirs()` check
