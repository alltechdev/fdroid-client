# V1 Index Models

Legacy F-Droid index format models and sync implementation.

## Overview

**Directory:** `sync/v1/`

The V1 index format is the legacy JSON format used by F-Droid repositories. All models use kotlinx.serialization for JSON parsing.

## IndexV1

**File:** `sync/v1/model/IndexV1.kt`

Root container for the V1 index.

```kotlin
@Serializable
data class IndexV1(
    val repo: RepoV1,
    val apps: List<AppV1> = emptyList(),
    val packages: Map<String, List<PackageV1>> = emptyMap(),
)
```

### Structure

- `repo` - Repository metadata
- `apps` - List of app metadata
- `packages` - Map of packageName to list of versions

## RepoV1

**File:** `sync/v1/model/RepoV1.kt`

Repository information.

```kotlin
@Serializable
data class RepoV1(
    val address: String,
    val icon: String,
    val name: String,
    val description: String,
    val timestamp: Long,
    val version: Int,
    val mirrors: List<String> = emptyList(),
)
```

## AppV1

**File:** `sync/v1/model/AppV1.kt`

App metadata in V1 format.

```kotlin
@Serializable
data class AppV1(
    val packageName: String,
    val icon: String? = null,
    val name: String? = null,
    val description: String? = null,
    val summary: String? = null,
    val added: Long? = null,
    val antiFeatures: List<String> = emptyList(),
    val authorEmail: String? = null,
    val authorName: String? = null,
    val authorPhone: String? = null,
    val authorWebSite: String? = null,
    val binaries: String? = null,
    val bitcoin: String? = null,
    val categories: List<String> = emptyList(),
    val changelog: String? = null,
    val donate: String? = null,
    val flattrID: String? = null,
    val issueTracker: String? = null,
    val lastUpdated: Long? = null,
    val liberapay: String? = null,
    val liberapayID: String? = null,
    val license: String,
    val litecoin: String? = null,
    val localized: Map<String, Localized>? = null,
    val openCollective: String? = null,
    val sourceCode: String? = null,
    val suggestedVersionCode: String? = null,
    val translation: String? = null,
    val webSite: String? = null,
)
```

### Donation Fields

V1 supports multiple donation methods:
- `donate` - Generic donation URL
- `bitcoin` - Bitcoin address
- `litecoin` - Litecoin address
- `flattrID` - Flattr ID
- `liberapay` / `liberapayID` - Liberapay
- `openCollective` - Open Collective ID

## PackageV1

**File:** `sync/v1/model/PackageV1.kt`

Individual package version/release.

```kotlin
@Serializable
data class PackageV1(
    val added: Long? = null,
    val apkName: String,
    val hash: String,
    val hashType: String,
    val minSdkVersion: Int? = null,
    val maxSdkVersion: Int? = null,
    val targetSdkVersion: Int? = minSdkVersion,
    val packageName: String,
    val sig: String? = null,
    val signer: String? = null,
    val size: Long,
    @SerialName("srcname")
    val srcName: String? = null,
    @SerialName("uses-permission")
    val usesPermission: List<PermissionV1> = emptyList(),
    @SerialName("uses-permission-sdk-23")
    val usesPermission23: List<PermissionV1> = emptyList(),
    val versionCode: Long? = null,
    val versionName: String,
    @SerialName("nativecode")
    val nativeCode: List<String>? = null,
    val features: List<String>? = null,
    val antiFeatures: List<String>? = null,
)
```

### PermissionV1

```kotlin
typealias PermissionV1 = Array<String?>

val PermissionV1.name: String get() = first()!!
val PermissionV1.maxSdk: Int? get() = getOrNull(1)?.toInt()
```

Permissions are represented as arrays: `["android.permission.INTERNET", "23"]` where the second element is optional maxSdk.

## Localized

**File:** `sync/v1/model/Localized.kt`

Localized metadata per locale.

```kotlin
@Serializable
data class Localized(
    val icon: String? = null,
    val name: String? = null,
    val description: String? = null,
    val summary: String? = null,
    val featureGraphic: String? = null,
    val phoneScreenshots: List<String>? = null,
    val promoGraphic: String? = null,
    val sevenInchScreenshots: List<String>? = null,
    val tenInchScreenshots: List<String>? = null,
    val tvBanner: String? = null,
    val tvScreenshots: List<String>? = null,
    val video: String? = null,
    val wearScreenshots: List<String>? = null,
    val whatsNew: String? = null,
)
```

### Screenshot Types

| Field | Description |
|-------|-------------|
| `phoneScreenshots` | Phone-sized screenshots |
| `sevenInchScreenshots` | 7" tablet screenshots |
| `tenInchScreenshots` | 10" tablet screenshots |
| `tvScreenshots` | Android TV screenshots |
| `wearScreenshots` | Wear OS screenshots |

## V1Syncable

**File:** `sync/v1/V1Syncable.kt`

Implements `Syncable<IndexV1>` for V1 index synchronization.

```kotlin
class V1Syncable(
    private val context: Context,
    private val downloader: Downloader,
    private val dispatcher: CoroutineDispatcher,
) : Syncable<IndexV1>
```

### Sync Flow

```kotlin
override suspend fun sync(repo: Repo, block: (SyncState) -> Unit) {
    // 1. Download index-v1.jar
    val jar = downloader.downloadIndex(
        context = context,
        repo = repo,
        url = repo.address.removeSuffix("/") + "/$INDEX_V1_NAME",
        fileName = INDEX_V1_NAME,
        onProgress = { bytes, total ->
            block(SyncState.IndexDownload.Progress(repo.id, percent))
        },
    )

    // 2. Parse JAR and verify fingerprint
    with(jar.toJarScope<IndexV1>()) {
        when {
            fingerprint == null -> // Error: no fingerprint
            repo.fingerprint != null && !repo.fingerprint.assert(fingerprint) ->
                // Error: fingerprint mismatch
            else ->
                // Success: convert to V2 and return
                block(SyncState.JsonParsing.Success(
                    repo.id,
                    fingerprint,
                    json().toV2()  // Convert V1 to V2 format
                ))
        }
    }

    // 3. Clean up
    jar.delete()
}
```

### Key Points

- Downloads `index-v1.jar` from repository
- Verifies JAR signature and fingerprint
- Converts V1 format to V2 internally using `toV2()` extension
- Reports progress via `SyncState` callbacks

## V1 vs V2 Comparison

| Feature | V1 | V2 |
|---------|----|----|
| File | `index-v1.jar` | `entry.jar` + `index-v2.json` |
| Format | Single JSON | Multiple JSON files |
| Diff support | No | Yes |
| Size | Larger | Smaller (with diffs) |
| Parsing | Parse entire file | Incremental |

## Conversion to V2

V1 models are converted to V2 internally via extension functions:

```kotlin
fun IndexV1.toV2(): IndexV2
```

This allows the rest of the app to work with a unified V2 model regardless of the repository's actual format.
