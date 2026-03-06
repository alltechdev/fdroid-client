# Index V2 Format

The V2 index format is the modern F-Droid repository format. It uses a two-stage approach with an entry file and supports incremental diff updates.

## File Structure

```
Repository
├── entry.jar           (small, always downloaded)
│   └── entry.json
├── index-v2.json       (full index)
└── diffs/
    ├── 1699999999999.json   (diff from timestamp)
    └── 1699888888888.json
```

## Implementation

**File:** `sync/v2/EntrySyncable.kt`

```kotlin
class EntrySyncable(
    private val context: Context,
    private val downloader: Downloader,
    private val dispatcher: CoroutineDispatcher,
) : Syncable<Entry>
```

## Sync Flow

```
1. Download entry.jar (small file)
         │
         ▼
2. Parse entry.json
         │
         ▼
3. Check if update needed
         │
    ┌────┴────┐
    │         │
    ▼         ▼
No update  Update needed
    │         │
    │    ┌────┴────┐
    │    │         │
    │    ▼         ▼
    │  Has diff?  No diff
    │    │         │
    │    ▼         ▼
    │  Download   Download
    │  diff.json  index-v2.json
    │    │         │
    │    ▼         │
    │  Apply diff  │
    │    │         │
    └────┴─────────┘
         │
         ▼
4. Return IndexV2
```

## Entry File

**File:** `sync/v2/model/Entry.kt`

```kotlin
@Serializable
data class Entry(
    val timestamp: Long,        // Current repo timestamp
    val version: Long,          // Index format version
    val index: EntryFile,       // Full index reference
    val diffs: Map<Long, EntryFile>  // Available diffs by timestamp
)

@Serializable
data class EntryFile(
    val name: String,           // "/index-v2.json" or "/diff/123.json"
    val sha256: String,         // File hash for verification
    val size: Long,             // File size in bytes
    val numPackages: Long,      // Number of packages (for UI)
)
```

### Diff Selection Logic

```kotlin
fun getDiff(timestamp: Long?): EntryFile? {
    return if (this.timestamp == timestamp) null  // Up to date
    else diffs[timestamp] ?: index                // Use diff or full index
}
```

## Sync Logic

### Step 1: Download Entry

```kotlin
val jar = downloader.downloadIndex(
    context = context,
    repo = repo,
    url = repo.address + "/entry.jar",
    fileName = ENTRY_V2_NAME,
    onProgress = { bytes, total ->
        block(SyncState.IndexDownload.Progress(repo.id, percent))
    },
)
```

### Step 2: Parse Entry and Verify

```kotlin
with(jar.toJarScope<Entry>()) {
    val entry = json()
    val fingerprint = fingerprint
        ?: invalid("Jar entry does not contain a fingerprint")

    if (repo.fingerprint != null && !repo.fingerprint.assert(fingerprint)) {
        invalid("Fingerprint mismatch")
    }
}
```

### Step 3: Determine Update Strategy

```kotlin
val diffRef = entry.getDiff(repo.versionInfo?.timestamp)
if (diffRef == null) {
    // Already up to date
    block(SyncState.JsonParsing.Success(repo.id, fingerprint, null))
    return
}
```

### Step 4a: Apply Diff (if available)

```kotlin
if (diffRef != entry.index && indexFile.exists()) {
    // Download diff file
    val diffFile = downloader.downloadIndex(...)

    // Load existing index and diff
    val diff = JsonParser.decodeFromString<IndexV2Diff>(diffFile.readText())
    val oldIndex = JsonParser.decodeFromString<IndexV2>(indexFile.readText())

    // Apply diff to get new index
    val newIndex = diff.patchInto(oldIndex) { index ->
        // Save patched index for next sync
        Json.encodeToStream(index, indexFile.outputStream())
    }
}
```

### Step 4b: Download Full Index (no diff available)

```kotlin
else {
    val newIndexFile = downloader.downloadIndex(
        context = context,
        repo = repo,
        url = indexPath,
        fileName = INDEX_V2_NAME,
        onProgress = { ... },
    )
    val indexV2 = JsonParser.decodeFromString<IndexV2>(newIndexFile.readText())
}
```

## V2 Data Model

**File:** `sync/v2/model/IndexV2.kt`

### IndexV2

```kotlin
@Serializable
data class IndexV2(
    val repo: RepoV2,
    val packages: Map<String, PackageV2>,
)
```

### RepoV2

```kotlin
@Serializable
data class RepoV2(
    val name: Map<String, String>,           // Localized names
    val description: Map<String, String>,    // Localized descriptions
    val icon: Map<String, FileV2>,           // Localized icons
    val address: String,
    val webBaseUrl: String?,
    val timestamp: Long,
    val mirrors: List<Mirror>?,
    val antiFeatures: Map<String, AntiFeature>?,
    val categories: Map<String, Category>?,
)
```

### PackageV2

```kotlin
@Serializable
data class PackageV2(
    val metadata: Metadata,
    val versions: Map<String, Version>,
)

@Serializable
data class Metadata(
    val name: Map<String, String>?,          // Localized names
    val summary: Map<String, String>?,       // Localized summaries
    val description: Map<String, String>?,   // Localized descriptions
    val icon: Map<String, FileV2>?,          // Localized icons
    val categories: List<String>?,
    val license: String?,
    val webSite: String?,
    val sourceCode: String?,
    // ... more fields
)
```

## Diff Format

**File:** `sync/v2/model/IndexV2Diff.kt`

Diffs contain only changed data:

```kotlin
@Serializable
data class IndexV2Diff(
    val repo: RepoV2?,                        // Repo changes (if any)
    val packages: Map<String, PackageV2?>?,   // Package changes
                                              // null value = package removed
)
```

### Applying Diffs

```kotlin
fun IndexV2Diff.patchInto(
    old: IndexV2,
    onPatched: (IndexV2) -> Unit
): IndexV2 {
    val newRepo = repo ?: old.repo
    val newPackages = old.packages.toMutableMap()

    packages?.forEach { (packageName, packageData) ->
        if (packageData == null) {
            newPackages.remove(packageName)  // Removed
        } else {
            newPackages[packageName] = packageData  // Added/updated
        }
    }

    val patched = IndexV2(newRepo, newPackages)
    onPatched(patched)  // Save for next sync
    return patched
}
```

## Benefits Over V1

| Feature | V1 | V2 |
|---------|----|----|
| Incremental updates | No | Yes (diffs) |
| Localization | Limited | Full support |
| Update check | Download full index | Download small entry.jar |
| Bandwidth | High | Low (with diffs) |

## Caching Strategy

V2 caches the full index locally:

```kotlin
val indexFile = Cache.getIndexFile(context, "repo_${repo.id}_$INDEX_V2_NAME")
```

This enables:
1. Quick "already up to date" checks
2. Diff application without re-downloading full index

## Example Entry.json

```json
{
  "timestamp": 1699999999999,
  "version": 20002,
  "index": {
    "name": "/index-v2.json",
    "sha256": "abc123...",
    "size": 5242880,
    "numPackages": 3500
  },
  "diffs": {
    "1699888888888": {
      "name": "/diffs/1699888888888.json",
      "sha256": "def456...",
      "size": 10240,
      "numPackages": 5
    }
  }
}
```

## Example Index-v2.json (partial)

```json
{
  "repo": {
    "name": { "en-US": "F-Droid" },
    "description": { "en-US": "The official repository" },
    "address": "https://f-droid.org/repo",
    "timestamp": 1699999999999
  },
  "packages": {
    "org.example.app": {
      "metadata": {
        "name": { "en-US": "Example App" },
        "summary": { "en-US": "An example" },
        "categories": ["Development"]
      },
      "versions": {
        "abc123": {
          "versionName": "1.0.0",
          "versionCode": 1,
          "file": {
            "name": "/org.example.app_1.apk",
            "sha256": "...",
            "size": 1234567
          }
        }
      }
    }
  }
}
```
