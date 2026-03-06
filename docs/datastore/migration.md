# DataStore Migration

Migration from Proto DataStore to Preferences DataStore.

## Overview

**File:** `datastore/migration/ProtoToPreferenceMigration.kt`

The app migrated from Proto DataStore (typed settings) to Preferences DataStore (key-value) for simpler schema management.

## Migration Class

```kotlin
class ProtoToPreferenceMigration(
    private val oldDataStore: DataStore<Settings>
) : DataMigration<Preferences> {

    override suspend fun shouldMigrate(currentData: Preferences): Boolean {
        // Migrate only if preferences are empty
        return currentData.asMap().isEmpty()
    }

    override suspend fun migrate(currentData: Preferences): Preferences {
        val settings = oldDataStore.data.first()
        val preferences = currentData.toMutablePreferences()
        preferences.setting(settings)
        return preferences
    }

    override suspend fun cleanUp() {
        // No cleanup needed - old file can remain
    }
}
```

## Migration Flow

```
┌─────────────────────────────────────────────────────────────┐
│                 First App Launch After Update               │
└─────────────────────────┬───────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│              shouldMigrate() called                          │
│  - Check if new preferences are empty                       │
│  - If empty → migration needed                              │
└─────────────────────────┬───────────────────────────────────┘
                          │ Yes, empty
                          ▼
┌─────────────────────────────────────────────────────────────┐
│              migrate() executed                              │
│  1. Read old Settings from Proto DataStore                  │
│  2. Convert to Preferences keys                             │
│  3. Return populated Preferences                            │
└─────────────────────────┬───────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│              New DataStore used                              │
│  - Future reads from Preferences DataStore                  │
│  - Old Proto file unused                                    │
└─────────────────────────────────────────────────────────────┘
```

## DI Configuration

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DatastoreModule {

    // Old Proto DataStore (kept for migration)
    @Provides
    fun provideProtoDatastore(context: Context): DataStore<Settings> =
        DataStoreFactory.create(serializer = SettingsSerializer) {
            context.dataStoreFile("settings_file")
        }

    // New Preferences DataStore with migration
    @Provides
    fun providePreferenceDatastore(
        context: Context,
        oldDatastore: DataStore<Settings>,
    ): DataStore<Preferences> = PreferenceDataStoreFactory.create(
        migrations = listOf(ProtoToPreferenceMigration(oldDatastore))
    ) {
        context.preferencesDataStoreFile("settings")
    }
}
```

## Benefits of Migration

| Proto DataStore | Preferences DataStore |
|-----------------|----------------------|
| Typed schema with protobuf | Simple key-value |
| Schema changes require migration | Add keys anytime |
| Complex serialization | Direct type support |
| Requires .proto file | Pure Kotlin |
