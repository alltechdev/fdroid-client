# Repository Updater

The `RepositoryUpdater` orchestrates the entire repository sync process for the legacy sync system, including downloading, parsing, validating, and committing index data.

## Overview

**File:** `index/RepositoryUpdater.kt`

This is the legacy sync implementation used by `SyncService`. The newer Room-based sync uses `Syncable` implementations instead.

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                  RepositoryUpdater                       │
│                                                          │
│  ┌───────────────────────────────────────────────────┐  │
│  │                    update()                        │  │
│  │                        │                           │  │
│  │    ┌───────────────────┼───────────────────┐      │  │
│  │    ▼                   ▼                   ▼      │  │
│  │ DOWNLOAD           PROCESS              MERGE     │  │
│  │ (index.jar)     (parse JSON)     (merge products) │  │
│  │    │                   │                   │      │  │
│  │    └───────────────────┼───────────────────┘      │  │
│  │                        ▼                          │  │
│  │                    COMMIT                         │  │
│  │              (save to database)                   │  │
│  └───────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
```

## Stages

```kotlin
enum class Stage {
    DOWNLOAD,  // Downloading index-v1.jar
    PROCESS,   // Parsing JSON content
    MERGE,     // Merging products with releases
    COMMIT     // Saving to database
}
```

| Stage | Description | Progress |
|-------|-------------|----------|
| `DOWNLOAD` | Download index JAR file | Bytes downloaded / total |
| `PROCESS` | Parse JSON index content | Bytes parsed / file size |
| `MERGE` | Merge products in batches | Products merged / total |
| `COMMIT` | Commit to database | Indeterminate |

## Error Types

```kotlin
enum class ErrorType {
    NETWORK,     // Connection failed
    HTTP,        // HTTP error (4xx, 5xx)
    VALIDATION,  // Fingerprint/signature invalid
    PARSING      // JSON parsing failed
}

class UpdateException(
    val errorType: ErrorType,
    message: String,
    cause: Exception? = null
) : Exception(message, cause)
```

## Public API

### Initialization

```kotlin
object RepositoryUpdater {
    fun init(scope: CoroutineScope, downloader: Downloader)
}
```

Called from `Droidify.kt` during app startup:

```kotlin
appScope.launch {
    RepositoryUpdater.init(this, downloader)
}
```

### Update Repository

```kotlin
suspend fun update(
    context: Context,
    repository: Repository,
    unstable: Boolean,
    callback: (Stage, Long, Long?) -> Unit,
): Result<Boolean>
```

**Parameters:**
- `context` - Android context
- `repository` - Repository to update
- `unstable` - Include unstable versions
- `callback` - Progress callback (stage, progress, total)

**Returns:** `Result<Boolean>` - Success with `true` if index was modified

### Await Completion

```kotlin
fun await()
```

Blocks until current update completes. Used when cancelling.

## Update Flow

### 1. Download Index

```kotlin
private suspend fun downloadIndex(
    context: Context,
    repository: Repository,
    indexType: IndexType,
    callback: (Stage, Long, Long?) -> Unit,
): Result<IndexFile>
```

- Downloads `index-v1.jar` from repository
- Supports conditional requests (`If-Modified-Since`, `ETag`)
- Returns `IndexFile` with metadata and file handle

### 2. Process File

```kotlin
fun processFile(
    context: Context,
    repository: Repository,
    indexType: IndexType,
    unstable: Boolean,
    file: File,
    lastModified: String,
    entityTag: String,
    callback: (Stage, Long, Long?) -> Unit,
): Boolean
```

Processing steps:

1. **Open JAR and validate signature**
   ```kotlin
   val jarFile = JarFile(file, true)
   val indexEntry = jarFile.getEntry(indexType.contentName) as JarEntry
   ```

2. **Parse JSON with callbacks**
   ```kotlin
   IndexV1Parser.parse(repository.id, entryStream, object : Callback {
       override fun onRepository(...) { }
       override fun onProduct(product: Product) { }
       override fun onReleases(packageName: String, releases: List<Release>) { }
   })
   ```

3. **Merge products in batches**
   ```kotlin
   indexMerger.forEach(repository.id, 50) { products, totalCount ->
       Database.UpdaterAdapter.putTemporary(
           products.map { transformProduct(it, features, unstable) }
       )
   }
   ```

4. **Validate fingerprint**
   ```kotlin
   val fingerprint = indexEntry.codeSigner.certificate.fingerprint()
   if (!workRepository.fingerprint.equals(fingerprint, ignoreCase = true)) {
       throw UpdateException(ErrorType.VALIDATION, "Certificate fingerprints do not match")
   }
   ```

5. **Commit to database**
   ```kotlin
   Database.UpdaterAdapter.finishTemporary(commitRepository, true)
   ```

## Product Transformation

Releases are filtered and marked for compatibility:

```kotlin
private fun transformProduct(
    product: Product,
    features: Set<String>,
    unstable: Boolean,
): Product {
    val releasePairs = product.releases
        .distinctBy { it.identifier }
        .sortedByDescending { it.versionCode }
        .map { release ->
            val incompatibilities = mutableListOf<Release.Incompatibility>()

            // Check SDK compatibility
            if (release.minSdkVersion > 0 && SdkCheck.sdk < release.minSdkVersion) {
                incompatibilities += Release.Incompatibility.MinSdk
            }
            if (release.maxSdkVersion > 0 && SdkCheck.sdk > release.maxSdkVersion) {
                incompatibilities += Release.Incompatibility.MaxSdk
            }

            // Check platform compatibility
            if (release.platforms.isNotEmpty() &&
                (release.platforms intersect Android.platforms).isEmpty()
            ) {
                incompatibilities += Release.Incompatibility.Platform
            }

            // Check feature requirements
            incompatibilities += (release.features - features)
                .map { Release.Incompatibility.Feature(it) }

            Pair(release, incompatibilities)
        }

    // Select best compatible release
    val firstSelected = releasePairs.firstOrNull {
        it.second.isEmpty() && predicate(it.first)
    }

    return product.copy(releases = releases)
}
```

## Index Types

```kotlin
enum class IndexType(
    val jarName: String,
    val contentName: String,
) {
    INDEX_V1("index-v1.jar", "index-v1.json")
}
```

## Signature Validation

JAR must be signed by a single code signer:

```kotlin
private val JarEntry.codeSigner: CodeSigner
    get() = codeSigners?.singleOrNull()
        ?: throw UpdateException(
            ErrorType.VALIDATION,
            "index.jar must be signed by a single code signer"
        )

private val CodeSigner.certificate: Certificate
    get() = signerCertPath?.certificates?.singleOrNull()
        ?: throw UpdateException(
            ErrorType.VALIDATION,
            "index.jar code signer should have only one certificate"
        )
```

## Cleanup

Deleted repositories are cleaned up automatically:

```kotlin
fun init(scope: CoroutineScope, downloader: Downloader) {
    scope.launch {
        Database.RepositoryAdapter
            .getAllRemovedStream()
            .drop(1)  // Skip initial state
            .filter { it.isNotEmpty() }
            .collect(Database.RepositoryAdapter::cleanup)
    }
}
```

## Thread Safety

Uses synchronized blocks for updates:

```kotlin
private val updaterLock = Any()
private val cleanupLock = Any()

fun processFile(...): Boolean {
    return synchronized(updaterLock) {
        // ... processing
        synchronized(cleanupLock) {
            Database.UpdaterAdapter.finishTemporary(commitRepository, true)
        }
    }
}
```

## Usage in SyncService

```kotlin
// In SyncService
val response = RepositoryUpdater.update(
    context = this,
    repository = repository,
    unstable = unstableUpdates,
) { stage, progress, total ->
    launch {
        syncState.emit(State.Syncing(
            appName = repository.name,
            stage = stage,
            read = DataSize(progress),
            total = total?.let { DataSize(it) },
        ))
    }
}
```

## IndexFile

Result of download operation:

```kotlin
data class IndexFile(
    val isUnmodified: Boolean,  // 304 Not Modified
    val lastModified: String,
    val entityTag: String,
    val statusCode: Int,
    val file: File,
)
```
