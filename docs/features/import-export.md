# Import/Export

Droid-ify supports importing and exporting various data types for backup and sharing purposes.

## Overview

**Key Files:**
- `utility/common/Exporter.kt` - Export interface
- `database/RepositoryExporter.kt` - Repository export/import
- `datastore/exporter/SettingsExporter.kt` - Settings export/import
- `datastore/CustomButtonRepository.kt` - Custom buttons export/import

## Exporter Interface

```kotlin
interface Exporter<T> {
    suspend fun export(item: T, target: Uri)
    suspend fun import(target: Uri): T
}
```

All exporters write to and read from a user-selected URI using Android's Storage Access Framework.

## Exportable Data

| Data Type | Format | File Extension | Exporter |
|-----------|--------|----------------|----------|
| Repositories | JSON | `.json` | `RepositoryExporter` |
| Settings | JSON | `.json` | `SettingsExporter` |
| Custom Buttons | JSON | `.json` | `CustomButtonRepository` |

## Repository Export/Import

### RepositoryExporter

```kotlin
@Singleton
class RepositoryExporter @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:ApplicationScope private val scope: CoroutineScope,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : Exporter<List<Repository>>
```

### Export Format

```json
{
    "repositories": [
        {
            "name": "F-Droid",
            "address": "https://f-droid.org/repo",
            "fingerprint": "43238D512C1E5EB2D6569F4A3AFBF5523418B82E0A3ED1552770ABB9A9C9CCAB",
            "enabled": true,
            "description": "Main F-Droid repository"
        },
        {
            "name": "IzzyOnDroid",
            "address": "https://apt.izzysoft.de/fdroid/repo",
            "fingerprint": "3BF0D6ABFEAE2F401707B6D966BE743BF0EEE49C2561B9BA39073711F628937A",
            "enabled": true
        }
    ]
}
```

### Export Logic

```kotlin
override suspend fun export(item: List<Repository>, target: Uri) {
    scope.launch(ioDispatcher) {
        val stream = context.contentResolver.openOutputStream(target, "wt")
        Json.factory.createGenerator(stream).use { generator ->
            generator.writeDictionary {
                writeArray("repositories") {
                    item.map {
                        it.copy(
                            id = -1,  // Don't export internal ID
                            mirrors = if (it.enabled) it.mirrors else emptyList(),
                            lastModified = "",  // Don't export sync state
                            entityTag = ""
                        )
                    }.forEach { repo ->
                        writeDictionary { repo.serialize(this) }
                    }
                }
            }
        }
    }
}
```

### Import Logic

```kotlin
override suspend fun import(target: Uri): List<Repository> = withContext(ioDispatcher) {
    val list = mutableListOf<Repository>()
    val stream = context.contentResolver.openInputStream(target)
    Json.factory.createParser(stream).use { parser ->
        parser?.parseDictionary {
            forEachKey {
                if (it.array("repositories")) {
                    forEach(JsonToken.START_OBJECT) {
                        val repo = repository()
                        list.add(repo)
                    }
                }
            }
        }
    }
    list
}
```

## Settings Export/Import

### SettingsExporter

```kotlin
@OptIn(ExperimentalSerializationApi::class)
class SettingsExporter(
    private val context: Context,
    private val scope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher,
    private val json: Json
) : Exporter<Settings>
```

### Export Format

```json
{
    "language": "system",
    "incompatibleVersions": false,
    "notifyUpdate": true,
    "unstableUpdate": false,
    "ignoreSignature": false,
    "theme": "SYSTEM",
    "dynamicTheme": true,
    "installerType": "SESSION",
    "autoUpdate": false,
    "autoSync": "WIFI_ONLY",
    "sortOrder": "UPDATED",
    "proxy": {
        "type": "DIRECT",
        "host": "",
        "port": 0
    },
    "cleanUpInterval": "PT12H",
    "favouriteApps": [
        "com.termux",
        "org.fdroid.fdroid"
    ],
    "homeScreenSwiping": true,
    "deleteApkOnInstall": false
}
```

### Export

```kotlin
override suspend fun export(item: Settings, target: Uri) {
    scope.launch(ioDispatcher) {
        try {
            context.contentResolver.openOutputStream(target, "wt").use {
                if (it != null) json.encodeToStream(item, it)
            }
        } catch (e: SerializationException) {
            e.printStackTrace()
            cancel()
        }
    }
}
```

### Import

```kotlin
override suspend fun import(target: Uri): Settings = withContext(ioDispatcher) {
    context.contentResolver.openInputStream(target).use {
        checkNotNull(it) { "Null input stream for import file" }
        json.decodeFromStream(it)
    }
}
```

## Custom Buttons Export/Import

### Export Format

```json
[
    {
        "id": "exodus",
        "label": "Exodus Privacy",
        "urlTemplate": "https://reports.exodus-privacy.eu.org/en/reports/{{package_name}}/latest/",
        "icon": "PRIVACY"
    },
    {
        "id": "custom_1",
        "label": "Source Code",
        "urlTemplate": "https://github.com/search?q={{package_name}}",
        "icon": "CODE"
    }
]
```

### Export

```kotlin
suspend fun exportToUri(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
    runCatching {
        ensureLoaded()
        val jsonString = json.encodeToString(
            ListSerializer(CustomButton.serializer()),
            _buttons.value
        )
        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            outputStream.write(jsonString.toByteArray())
        } ?: throw IllegalStateException("Cannot open output stream")
    }
}
```

### Import with Merge

```kotlin
suspend fun importFromUri(uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
    runCatching {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("Cannot open input stream")
        val jsonString = inputStream.bufferedReader().use { it.readText() }
        val importedButtons = json.decodeFromString<List<CustomButton>>(jsonString)

        mutex.withLock {
            ensureLoadedInternal()
            // Only import buttons with new IDs (no duplicates)
            val existingIds = _buttons.value.map { it.id }.toSet()
            val newButtons = importedButtons.filter { it.id !in existingIds }
            val mergedButtons = _buttons.value + newButtons
            saveToFile(mergedButtons)
            _buttons.value = mergedButtons
            newButtons.size  // Return count of new buttons
        }
    }
}
```

## UI Integration

### Export Flow

```kotlin
// In SettingsViewModel
fun exportSettings() {
    viewModelScope.launch {
        val settings = settingsRepository.getInitial()
        settingsExporter.export(settings, targetUri)
    }
}

fun exportRepositories() {
    viewModelScope.launch {
        val repos = Database.RepositoryAdapter.getAll()
        repositoryExporter.export(repos, targetUri)
    }
}
```

### Import Flow

```kotlin
fun importSettings(uri: Uri) {
    viewModelScope.launch {
        try {
            val settings = settingsExporter.import(uri)
            settingsRepository.import(settings)
        } catch (e: Exception) {
            // Show error
        }
    }
}

fun importRepositories(uri: Uri) {
    viewModelScope.launch {
        val repos = repositoryExporter.import(uri)
        repos.forEach { repo ->
            Database.RepositoryAdapter.put(repo)
        }
    }
}
```

### File Picker

```kotlin
// Create document for export
val createDocument = rememberLauncherForActivityResult(
    ActivityResultContracts.CreateDocument("application/json")
) { uri ->
    uri?.let { viewModel.exportSettings(it) }
}

// Open document for import
val openDocument = rememberLauncherForActivityResult(
    ActivityResultContracts.OpenDocument()
) { uri ->
    uri?.let { viewModel.importSettings(it) }
}

Button(onClick = { createDocument.launch("droidify_settings.json") }) {
    Text("Export Settings")
}

Button(onClick = { openDocument.launch(arrayOf("application/json")) }) {
    Text("Import Settings")
}
```

## Error Handling

```kotlin
override suspend fun import(target: Uri): Settings = withContext(ioDispatcher) {
    try {
        context.contentResolver.openInputStream(target).use {
            checkNotNull(it) { "Null input stream for import file" }
            json.decodeFromStream(it)
        }
    } catch (e: SerializationException) {
        e.printStackTrace()
        throw IllegalStateException("Invalid settings file format: ${e.message}")
    } catch (e: IOException) {
        e.printStackTrace()
        throw IllegalStateException("Could not read file: ${e.message}")
    }
}
```

## Best Practices

1. **Strip internal IDs** - Don't export database IDs; they should be regenerated on import
2. **Exclude transient state** - Don't export sync timestamps or ETags
3. **Handle duplicates** - Check for existing items before importing
4. **Validate on import** - Ensure data integrity before committing
5. **Use SAF** - Always use Storage Access Framework for file access
6. **Error feedback** - Provide clear messages on import/export failures

## Removed

| Feature | Removal Doc |
|---------|-------------|
| Package: `com.looker.droidify` | [package-rename.md](../changes/package-rename.md) |
| `Settings Import/Export` | [proxy-and-backup.md](../removal/proxy-and-backup.md) |
| `Custom Buttons Import/Export` | [custom-buttons-and-settings.md](../removal/custom-buttons-and-settings.md) |

## Changes

| Change | Change Doc |
|--------|------------|
| App branding Droid-ify → ATD Store | [app-branding.md](../changes/app-branding.md) |
