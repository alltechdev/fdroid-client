# Exporter Interface

Generic interface for data export/import operations.

## Overview

**File:** `app/src/main/kotlin/com/looker/droidify/utility/common/Exporter.kt`

Provides a standardized interface for exporting and importing various data types.

## Interface Definition

```kotlin
interface Exporter<T> {
    suspend fun export(data: T): Uri
    suspend fun import(uri: Uri): T
}
```

## Implementations

### SettingsExporter

**File:** `datastore/SettingsExporter.kt`

Exports/imports user settings:

```kotlin
class SettingsExporter @Inject constructor(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
) : Exporter<Settings> {

    override suspend fun export(data: Settings): Uri {
        val json = Json.encodeToString(data)
        val file = File(context.cacheDir, "settings_backup.json")
        file.writeText(json)
        return FileProvider.getUriForFile(context, authority, file)
    }

    override suspend fun import(uri: Uri): Settings {
        val json = context.contentResolver.openInputStream(uri)
            ?.bufferedReader()
            ?.readText()
            ?: throw IOException("Cannot read file")
        return Json.decodeFromString(json)
    }
}
```

### RepositoryExporter

**File:** `database/RepositoryExporter.kt`

Exports/imports repository list:

```kotlin
class RepositoryExporter @Inject constructor(
    private val context: Context,
) : Exporter<List<Repository>> {

    override suspend fun export(data: List<Repository>): Uri {
        val exportData = data.map { repo ->
            ExportedRepository(
                address = repo.address,
                fingerprint = repo.fingerprint,
                authentication = repo.authentication
            )
        }
        val json = Json.encodeToString(exportData)
        val file = File(context.cacheDir, "repositories_backup.json")
        file.writeText(json)
        return FileProvider.getUriForFile(context, authority, file)
    }

    override suspend fun import(uri: Uri): List<Repository> {
        val json = context.contentResolver.openInputStream(uri)
            ?.bufferedReader()
            ?.readText()
            ?: throw IOException("Cannot read file")
        val exportedList: List<ExportedRepository> = Json.decodeFromString(json)
        return exportedList.map { exported ->
            Repository.newRepository(
                address = exported.address,
                fingerprint = exported.fingerprint,
                authentication = exported.authentication
            )
        }
    }
}
```

## Export Format

### Settings JSON

```json
{
    "theme": "SYSTEM",
    "dynamicColors": true,
    "language": "en",
    "autoSync": "WIFI_ONLY",
    "notifyUpdates": true,
    "updateUnstable": false,
    "incompatibleVersions": false,
    "proxy": {
        "type": "DIRECT",
        "host": "localhost",
        "port": 9050
    },
    "installer": "SESSION",
    "customButtons": []
}
```

### Repositories JSON

```json
[
    {
        "address": "https://f-droid.org/repo",
        "fingerprint": "43238D512C1E5EB2D6569F4A3AFBF5523418B82E0A3ED1552770ABB9A9C9CCAB",
        "authentication": ""
    },
    {
        "address": "https://apt.izzysoft.de/fdroid/repo",
        "fingerprint": "3BF0D6ABFEAE2F401707B6D966BE743BF0EEE49C2561B9BA39073711F628937A",
        "authentication": ""
    }
]
```

## Usage in ViewModel

```kotlin
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsExporter: SettingsExporter,
    private val repoExporter: RepositoryExporter,
) : ViewModel() {

    fun exportSettings(launcher: ActivityResultLauncher<String>) {
        viewModelScope.launch {
            val settings = settingsRepository.getInitial()
            val uri = settingsExporter.export(settings)
            // Share URI
        }
    }

    fun importSettings(uri: Uri) {
        viewModelScope.launch {
            val settings = settingsExporter.import(uri)
            settingsRepository.updateAll(settings)
        }
    }

    fun exportRepositories(launcher: ActivityResultLauncher<String>) {
        viewModelScope.launch {
            val repos = Database.RepositoryAdapter.getAll()
            val uri = repoExporter.export(repos)
            // Share URI
        }
    }

    fun importRepositories(uri: Uri) {
        viewModelScope.launch {
            val repos = repoExporter.import(uri)
            Database.RepositoryAdapter.importRepos(repos)
        }
    }
}
```

## Activity Result Contracts

### Create Document

```kotlin
val createSettingsFile = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CreateDocument("application/json")
) { uri ->
    uri?.let { viewModel.saveExportedSettings(it) }
}
```

### Open Document

```kotlin
val openSettingsFile = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
) { uri ->
    uri?.let { viewModel.importSettings(it) }
}
```

## Error Handling

```kotlin
sealed class ExportResult {
    data class Success(val uri: Uri) : ExportResult()
    data class Error(val exception: Exception) : ExportResult()
}

suspend fun safeExport(): ExportResult {
    return try {
        ExportResult.Success(export(data))
    } catch (e: Exception) {
        ExportResult.Error(e)
    }
}
```

## File Provider Configuration

```xml
<provider
    android:name="androidx.core.content.FileProvider"
    android:authorities="${applicationId}.fileprovider"
    android:exported="false"
    android:grantUriPermissions="true">
    <meta-data
        android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/file_paths" />
</provider>
```

**file_paths.xml:**
```xml
<paths>
    <cache-path name="exports" path="/" />
</paths>
```

## Best Practices

1. **Use ContentResolver** - For reading imported files
2. **FileProvider** - For sharing exported files
3. **Coroutine scope** - All operations are suspend functions
4. **Validation** - Validate imported data before applying
5. **Error handling** - Wrap in try-catch for user feedback

## Removed

| Feature | Removal Doc |
|---------|-------------|
| `Exporter<T>` interface | [proxy-and-backup.md](../removal/proxy-and-backup.md) |
| `RepositoryExporter` | [proxy-and-backup.md](../removal/proxy-and-backup.md) |
| `SettingsExporter` | [proxy-and-backup.md](../removal/proxy-and-backup.md) |
| Settings import/export UI | [proxy-and-backup.md](../removal/proxy-and-backup.md) |
