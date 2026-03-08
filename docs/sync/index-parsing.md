# Index Parsing

Low-level parsing of F-Droid repository index files.

## Overview

**Location:** `app/src/main/kotlin/com/atd/store/index/`

| File | Purpose |
|------|---------|
| `IndexV1Parser.kt` | Parses index-v1.json format |
| `IndexMerger.kt` | Merges products and releases during sync |
| `OemRepositoryParser.kt` | Parses OEM-provided repository configs |
| `RepositoryUpdater.kt` | Orchestrates sync process |

## IndexV1Parser

Streaming JSON parser for the V1 index format.

**File:** `index/IndexV1Parser.kt`

### Callback Interface

```kotlin
interface Callback {
    fun onRepository(
        mirrors: List<String>,
        name: String,
        description: String,
        version: Int,
        timestamp: Long
    )
    fun onProduct(product: Product)
    fun onReleases(packageName: String, releases: List<Release>)
}
```

### Parse Method

```kotlin
fun parse(repositoryId: Long, inputStream: InputStream, callback: Callback)
```

### Index Structure

```json
{
    "repo": {
        "name": "F-Droid",
        "address": "https://f-droid.org/repo",
        "mirrors": [...],
        "description": "...",
        "version": 21,
        "timestamp": 1234567890
    },
    "apps": [
        { "packageName": "...", "name": "...", ... }
    ],
    "packages": {
        "com.example.app": [
            { "versionName": "1.0", "versionCode": 1, ... }
        ]
    }
}
```

### Localization Handling

Uses locale-aware field selection:

```kotlin
private fun <T> Map<String, Localized>.findLocalized(callback: (Localized) -> T?): T? {
    return getBestLocale(getLocales(Resources.getSystem().configuration))
        ?.let { callback(it) }
}

private fun <T> Map<String, T>?.getBestLocale(localeList: LocaleListCompat): T? {
    if (isNullOrEmpty()) return null
    val firstMatch = localeList.getFirstMatch(keys.toTypedArray()) ?: return null
    val tag = firstMatch.toLanguageTag()
    return get(tag)
        ?: getOrStartsWith("${firstMatch.language}-${firstMatch.country}")
        ?: getOrStartsWith(firstMatch.language)
        ?: get("en-US") ?: get("en")
        ?: values.first()
}
```

### Donate Sorting

Orders donation types by preference:

```kotlin
internal object DonateComparator : Comparator<Product.Donate> {
    private val classes = listOf(
        Regular::class,
        Bitcoin::class,
        Litecoin::class,
        Liberapay::class,
        OpenCollective::class
    )
}
```

### Permission Handling

Filters permissions by SDK version:

```kotlin
private fun JsonParser.collectPermissions(permissions: LinkedHashSet<String>, minSdk: Int) {
    forEach(JsonToken.START_ARRAY) {
        val permission = if (nextToken() == JsonToken.VALUE_STRING) valueAsString else ""
        val maxSdk = if (nextToken() == JsonToken.VALUE_NUMBER_INT) valueAsInt else 0

        if (permission.isNotEmpty() &&
            SdkCheck.sdk >= minSdk &&
            (maxSdk <= 0 || SdkCheck.sdk <= maxSdk)) {
            permissions.add(permission)
        }
    }
}
```

## IndexMerger

Temporary SQLite database for merging products with their releases.

**File:** `index/IndexMerger.kt`

### Purpose

During V1 sync, apps and packages are in separate sections of the index. IndexMerger stores them temporarily and joins them for final output.

### Schema

```kotlin
db.execSQL(
    "CREATE TABLE product (" +
        "package_name TEXT PRIMARY KEY," +
        "description TEXT NOT NULL, " +
        "data BLOB NOT NULL)"
)
db.execSQL("CREATE TABLE releases (package_name TEXT PRIMARY KEY, data BLOB NOT NULL)")
```

### Usage Flow

```kotlin
val merger = IndexMerger(tempFile)

// Add products as they're parsed
merger.addProducts(products)

// Add releases as they're parsed
merger.addReleases(pairs)

// Iterate merged results
merger.forEach(repositoryId, windowSize = 100) { products, total ->
    Database.UpdaterAdapter.putTemporary(products)
}

merger.close()
```

### Performance Optimizations

```kotlin
db.execWithResult("PRAGMA synchronous = OFF")
db.execWithResult("PRAGMA journal_mode = OFF")
db.beginTransaction()
```

### Windowed Processing

Processes products in batches to avoid memory issues:

```kotlin
fun forEach(repositoryId: Long, windowSize: Int, callback: (List<Product>, Int) -> Unit) {
    closeTransaction()
    db.rawQuery(
        """SELECT product.description, product.data AS pd, releases.data AS rd
           FROM product
           LEFT JOIN releases ON product.package_name = releases.package_name""",
        null
    ).use { cursor ->
        cursor.asSequence()
            .map { /* parse product with releases */ }
            .windowed(windowSize, windowSize, true)
            .forEach { products -> callback(products, cursor.count) }
    }
}
```

## OemRepositoryParser

Parses OEM-provided repository configurations from system partitions.

**File:** `index/OemRepositoryParser.kt`

### Search Paths

```kotlin
private val rootDirs = arrayOf("/system", "/product", "/vendor", "/odm", "/oem")
private val supportedPackageNames = arrayOf("com.atd.store", "org.fdroid.fdroid")
private const val FILE_NAME = "additional_repos.xml"
```

Full paths searched:
- `/system/etc/com.atd.store/additional_repos.xml`
- `/system/etc/org.fdroid.fdroid/additional_repos.xml`
- `/product/etc/...`
- etc.

### XML Format

```xml
<resources>
    <string-array name="default_repos">
        <item>Repository Name</item>
        <item>https://repo.example.com</item>
        <item>Description text</item>
        <item>21</item>  <!-- version -->
        <item>1</item>   <!-- enabled: 0 or 1 -->
        <item></item>    <!-- unused -->
        <item>FINGERPRINT_HEX</item>
    </string-array>
</resources>
```

### Parse Method

```kotlin
fun getSystemDefaultRepos(): List<Repository>? = rootDirs.flatMap { rootDir ->
    supportedPackageNames.map { packageName -> "$rootDir/etc/$packageName/$FILE_NAME" }
}.flatMap { path ->
    val file = File(path)
    if (file.exists()) parse(file.inputStream()) else emptyList()
}.takeIf { it.isNotEmpty() }
```

### Fingerprint Handling

Handles both raw bytes and hex strings:

```kotlin
fingerprint = xml[6].let {
    if (it.length > 32) {
        // Raw certificate bytes - hash them
        val encoded = it.chunked(2)
            .mapNotNull { byteStr -> byteStr.toInt(16).toByte() }
            .toByteArray()
        sha256(encoded).hex()
    } else it
}
```

## Data Flow

```
┌─────────────────────────────────────────────────────────────┐
│                    index-v1.json                             │
└─────────────────────────┬───────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                   IndexV1Parser                              │
│                                                              │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐ │
│  │ onRepository│  │ onProduct   │  │ onReleases          │ │
│  └──────┬──────┘  └──────┬──────┘  └──────────┬──────────┘ │
└─────────┼────────────────┼─────────────────────┼────────────┘
          │                │                     │
          │                └──────────┬──────────┘
          │                           │
          │                           ▼
          │                ┌─────────────────────┐
          │                │    IndexMerger      │
          │                │   (temp SQLite)     │
          │                └──────────┬──────────┘
          │                           │
          │                           ▼
          │                ┌─────────────────────┐
          │                │  Merged Products    │
          │                │  (with releases)    │
          │                └──────────┬──────────┘
          │                           │
          └───────────────────────────┼─────────────────────┐
                                      │                     │
                                      ▼                     ▼
                           ┌─────────────────────┐ ┌─────────────────┐
                           │ Database.Updater   │ │ Repository.     │
                           │ (products/cats)    │ │ update()        │
                           └─────────────────────┘ └─────────────────┘
```

## Key Constants

### Product Fields

```kotlin
private const val KEY_PRODUCT_PACKAGENAME = "packageName"
private const val KEY_PRODUCT_NAME = "name"
private const val KEY_PRODUCT_LOCALIZED = "localized"
// ... many more
```

### Release Fields

```kotlin
private const val KEY_RELEASE_VERSIONNAME = "versionName"
private const val KEY_RELEASE_VERSIONCODE = "versionCode"
private const val KEY_RELEASE_HASH = "hash"
private const val KEY_RELEASE_SIGNER_SHA256 = "signer"
// ... many more
```

## Error Handling

### Icon Validation

```kotlin
private fun validateIcon(icon: String): String {
    return if (icon.endsWith(".xml")) "" else icon  // Skip vector icons
}
```

### Safe Parsing

```kotlin
key.string(KEY_PRODUCT_SUGGESTEDVERSIONCODE) ->
    suggestedVersionCode = valueAsString.toLongOrNull() ?: 0L
```

## See Also

- [Index V1](index-v1.md) - V1 format documentation
- [Index V2](index-v2.md) - V2 format documentation
- [Repository Updater](repository-updater.md) - Sync orchestration
