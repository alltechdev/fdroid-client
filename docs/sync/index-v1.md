# Index V1 Format

The V1 index format is the legacy F-Droid repository format. It uses a signed JAR file containing a JSON index.

## File Structure

```
index-v1.jar
└── index-v1.json     (signed JSON index)
```

## Implementation

**File:** `sync/v1/V1Syncable.kt`

```kotlin
class V1Syncable(
    private val context: Context,
    private val downloader: Downloader,
    private val dispatcher: CoroutineDispatcher,
) : Syncable<IndexV1>
```

## Sync Flow

```
1. Download index-v1.jar
         │
         ▼
2. Extract and verify JAR signature
         │
         ▼
3. Parse index-v1.json to IndexV1
         │
         ▼
4. Convert IndexV1 to IndexV2 (common format)
         │
         ▼
5. Return success with index data
```

### Code Flow

```kotlin
override suspend fun sync(repo: Repo, block: (SyncState) -> Unit) {
    // 1. Download
    val jar = downloader.downloadIndex(
        context = context,
        repo = repo,
        url = repo.address + "/index-v1.jar",
        fileName = INDEX_V1_NAME,
        onProgress = { bytes, total ->
            block(SyncState.IndexDownload.Progress(repo.id, percent))
        },
    )

    // 2. Parse JAR and extract fingerprint
    with(jar.toJarScope<IndexV1>()) {
        // 3. Verify fingerprint
        if (repo.fingerprint != null && !repo.fingerprint.assert(fingerprint)) {
            block(SyncState.JarParsing.Failure(repo.id, error))
            return
        }

        // 4. Parse JSON and convert to V2
        val indexV2 = json().toV2()

        // 5. Return success
        block(SyncState.JsonParsing.Success(repo.id, fingerprint, indexV2))
    }

    jar.delete()
}
```

## V1 Data Model

**File:** `sync/v1/model/IndexV1.kt`

### IndexV1

```kotlin
@Serializable
data class IndexV1(
    val repo: RepoV1,
    val apps: List<AppV1>,
    val packages: Map<String, List<PackageV1>>,
)
```

### RepoV1

```kotlin
@Serializable
data class RepoV1(
    val name: String,
    val address: String,
    val description: String?,
    val timestamp: Long,
    val mirrors: List<String>?,
)
```

### AppV1

```kotlin
@Serializable
data class AppV1(
    val packageName: String,
    val name: String,
    val summary: String?,
    val description: String?,
    val icon: String?,
    val license: String?,
    val categories: List<String>?,
    val webSite: String?,
    val sourceCode: String?,
    val issueTracker: String?,
    val changelog: String?,
    val donate: String?,
    val bitcoin: String?,
    val flattrID: String?,
    val liberapay: String?,
    val openCollective: String?,
    val suggestedVersionCode: Long?,
    val localized: Map<String, Localized>?,
    val antiFeatures: List<String>?,
    val added: Long?,
    val lastUpdated: Long?,
)
```

### PackageV1

```kotlin
@Serializable
data class PackageV1(
    val versionName: String,
    val versionCode: Long,
    val added: Long?,
    val size: Long?,
    val minSdkVersion: Int?,
    val targetSdkVersion: Int?,
    val maxSdkVersion: Int?,
    val sig: String?,
    val signer: String?,
    val hash: String?,
    val hashType: String?,
    val apkName: String?,
    val nativecode: List<String>?,
    val uses_permission: List<List<String>>?,
)
```

## V1 to V2 Conversion

**File:** `sync/common/IndexConverter.kt`

The V1 index is converted to V2 format for unified processing:

```kotlin
fun IndexV1.toV2(): IndexV2
```

This allows the database layer to work with a single format.

## Limitations

Compared to V2, V1 has:

| Limitation | Impact |
|------------|--------|
| No incremental updates | Full index download every sync |
| Single locale in index | Limited localization in index itself |
| Larger file size | More bandwidth usage |
| No diff support | Can't minimize data transfer |

## When V1 is Used

V1 is used when:
1. Repository doesn't support V2 (no `entry.jar`)
2. V2 download fails (fallback)

## JAR Verification

The JAR file signature is verified during parsing:

```kotlin
val jarFile = JarFile(file, true)  // true = verify
```

The certificate fingerprint is extracted from the JAR entry:

```kotlin
val fingerprint = entry.fingerprint()
```

This is compared against the stored repository fingerprint to ensure authenticity.

## Example V1 Index

```json
{
  "repo": {
    "name": "F-Droid",
    "address": "https://f-droid.org/repo",
    "description": "The official F-Droid repository",
    "timestamp": 1699999999999
  },
  "apps": [
    {
      "packageName": "org.example.app",
      "name": "Example App",
      "summary": "An example application",
      "categories": ["Development"],
      "lastUpdated": 1699999999999
    }
  ],
  "packages": {
    "org.example.app": [
      {
        "versionName": "1.0.0",
        "versionCode": 1,
        "apkName": "org.example.app_1.apk",
        "hash": "abc123...",
        "hashType": "sha256",
        "size": 1234567
      }
    ]
  }
}
```
