# Sync System Overview

The sync system handles downloading, parsing, and storing F-Droid repository indexes. It supports both the legacy V1 format and the modern V2 format.

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    RepoRepository                        │
│                     sync(repo)                           │
└────────────────────────┬────────────────────────────────┘
                         │
         ┌───────────────┴───────────────┐
         ▼                               ▼
┌─────────────────┐             ┌─────────────────┐
│   EntrySyncable │             │   V1Syncable    │
│   (V2 Format)   │             │   (V1 Format)   │
└────────┬────────┘             └────────┬────────┘
         │                               │
         ▼                               ▼
┌─────────────────┐             ┌─────────────────┐
│  entry.jar      │             │  index-v1.jar   │
│  index-v2.json  │             │                 │
└────────┬────────┘             └────────┬────────┘
         │                               │
         └───────────────┬───────────────┘
                         ▼
              ┌─────────────────┐
              │    IndexDao     │
              │  insertIndex()  │
              └─────────────────┘
```

## Key Files

| File | Purpose |
|------|---------|
| `sync/Syncable.kt` | Interface for sync implementations |
| `sync/SyncState.kt` | State events during sync |
| `sync/v1/V1Syncable.kt` | V1 index sync implementation |
| `sync/v2/EntrySyncable.kt` | V2 index sync implementation |
| `sync/common/IndexDownloader.kt` | Download utilities |
| `sync/JarScope.kt` | JAR file parsing utilities |
| `sync/common/IndexConverter.kt` | V1 to V2 conversion |

## Syncable Interface

```kotlin
interface Syncable<T> {
    suspend fun sync(repo: Repo, block: (SyncState) -> Unit)
}
```

All sync implementations emit `SyncState` events to report progress.

## Sync States

The sync process emits these states:

```kotlin
sealed interface SyncState {
    val repoId: Int

    sealed interface IndexDownload : SyncState {
        data class Progress(repoId: Int, progress: Int)  // 0-100
        data class Success(repoId: Int)
        data class Failure(repoId: Int, error: Throwable)
    }

    sealed interface JarParsing : SyncState {
        data class Success(repoId: Int, fingerprint: Fingerprint)
        data class Failure(repoId: Int, error: Throwable)
    }

    sealed interface JsonParsing : SyncState {
        data class Success(repoId: Int, fingerprint: Fingerprint, index: IndexV2?)
        data class Failure(repoId: Int, error: Throwable)
    }
}
```

## Sync Flow

### 1. Trigger Sync

Sync can be triggered by:
- `SyncService` - Periodic background sync
- `SyncWorker` - One-time sync request
- User action - Pull-to-refresh or manual sync

### 2. Download Index

```kotlin
val jar = downloader.downloadIndex(
    context = context,
    repo = repo,
    url = repo.address + "/entry.jar",  // or index-v1.jar
    fileName = "entry.jar",
    onProgress = { bytes, total ->
        block(SyncState.IndexDownload.Progress(repo.id, percent))
    },
)
```

Features:
- Authentication support (Basic auth)
- Conditional requests (`If-Modified-Since`)
- Progress reporting

### 3. Parse JAR File

JAR files are signed and contain JSON data:

```kotlin
with(jar.toJarScope<Entry>()) {
    val entry = json()           // Parse JSON
    val fingerprint = fingerprint // Extract certificate fingerprint
}
```

The `JarScope` extracts:
- JSON content (`entry.json` or `index-v1.json`)
- Certificate fingerprint for verification

### 4. Verify Fingerprint

```kotlin
if (repo.fingerprint != null && !repo.fingerprint.assert(fingerprint)) {
    // Fingerprint mismatch - reject index
    block(SyncState.JarParsing.Failure(repo.id, error))
}
```

### 5. Store Index

```kotlin
indexDao.insertIndex(
    fingerprint = fingerprint,
    index = indexV2,
    expectedRepoId = repo.id,
)
```

## Index Formats

### V1 Format

- Single file: `index-v1.jar`
- Contains: `index-v1.json`
- Full index every sync
- See [index-v1.md](index-v1.md)

### V2 Format

- Entry file: `entry.jar` (small, always downloaded)
- Index file: `index-v2.json` (full index)
- Diff files: `diff_<timestamp>.json` (incremental updates)
- See [index-v2.md](index-v2.md)

## Error Handling

Sync failures are captured and reported:

```kotlin
try {
    // sync logic
} catch (t: Throwable) {
    block(SyncState.IndexDownload.Failure(repo.id, t))
}
```

Common errors:
- Network failures
- Invalid/corrupted JAR
- Fingerprint mismatch
- JSON parsing errors

## Conditional Sync

To avoid unnecessary downloads, the sync system uses:

1. **If-Modified-Since header**: Sends last sync timestamp
2. **Entry file (V2)**: Small file indicates if full sync needed
3. **Diff files (V2)**: Download only changes since last sync

```kotlin
if (repo.versionInfo != null && repo.versionInfo.timestamp > 0L) {
    ifModifiedSince(Date(repo.versionInfo.timestamp))
}
```

## Authentication

Repositories can require authentication:

```kotlin
if (repo.shouldAuthenticate) {
    with(requireNotNull(repo.authentication)) {
        authentication(username = username, password = password)
    }
}
```

Credentials are stored encrypted in the database.

## Cache Management

Index files are cached in `cache/index/`:

```kotlin
val indexFile = Cache.getIndexFile(context, "repo_${repo.id}_$fileName")
```

V2 caches the full index for diff application.

## Sync All Repositories

```kotlin
suspend fun syncAll(): Boolean = supervisorScope {
    val repos = getEnabledRepos().first()
    repos.forEach { repo -> launch { sync(repo) } }
    true
}
```

Uses `supervisorScope` so one repo failure doesn't cancel others.
