# Settings Exporter

Export and import settings to/from JSON files.

## Overview

**File:** `datastore/exporter/SettingsExporter.kt`

Allows users to backup and restore their settings configuration.

## Implementation

```kotlin
class SettingsExporter(
    private val context: Context,
    private val scope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher,
    private val json: Json
) : Exporter<Settings> {

    override suspend fun export(item: Settings, target: Uri) {
        scope.launch(ioDispatcher) {
            context.contentResolver.openOutputStream(target, "wt").use {
                if (it != null) json.encodeToStream(item, it)
            }
        }
    }

    override suspend fun import(target: Uri): Settings = withContext(ioDispatcher) {
        context.contentResolver.openInputStream(target).use {
            checkNotNull(it) { "Null input stream for import file" }
            json.decodeFromStream(it)
        }
    }
}
```

## Exporter Interface

```kotlin
interface Exporter<T> {
    suspend fun export(item: T, target: Uri)
    suspend fun import(target: Uri): T
}
```

## Usage in Repository

```kotlin
class PreferenceSettingsRepository(
    private val dataStore: DataStore<Preferences>,
    private val exporter: Exporter<Settings>
) : SettingsRepository {

    override suspend fun export(target: Uri) {
        exporter.export(getInitial(), target)
    }

    override suspend fun import(target: Uri) {
        val imported = exporter.import(target)
        // Apply all imported settings
        setTheme(imported.theme)
        setDynamicTheme(imported.dynamicTheme)
        setInstallerType(imported.installerType)
        // ... more settings
    }
}
```

## JSON Format

Exported settings are stored as JSON:

```json
{
  "language": "system",
  "incompatibleVersions": false,
  "notifyUpdate": true,
  "theme": "SYSTEM",
  "dynamicTheme": true,
  "installerType": "SESSION",
  "autoSync": "WIFI_ONLY",
  "sortOrder": "UPDATED",
  "proxy": {
    "type": "DIRECT",
    "host": "localhost",
    "port": 9050
  },
  "favouriteApps": ["org.mozilla.firefox", "org.fdroid.fdroid"],
  "cleanUpInterval": "PT12H"
}
```

## DI Configuration

```kotlin
@Provides
fun provideSettingsExporter(
    context: Context,
    scope: CoroutineScope,
    dispatcher: CoroutineDispatcher
): Exporter<Settings> = SettingsExporter(
    context = context,
    scope = scope,
    ioDispatcher = dispatcher,
    json = Json {
        encodeDefaults = true
        prettyPrint = true
    }
)
```
