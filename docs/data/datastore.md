# DataStore & Settings

Droid-ify uses Jetpack DataStore with Preferences for persisting user settings. Settings are reactive and observable via Flow.

## Overview

**Key Files:**
- `datastore/Settings.kt` - Settings data class and serializer
- `datastore/SettingsRepository.kt` - Repository interface
- `datastore/PreferenceSettingsRepository.kt` - Preferences DataStore implementation
- `datastore/model/*.kt` - Setting enum types

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    Settings Flow                         │
│                                                          │
│  ┌─────────────────────────────────────────────────────┐│
│  │              SettingsRepository                      ││
│  │                  (interface)                         ││
│  │                                                      ││
│  │  val data: Flow<Settings>                           ││
│  │  suspend fun setXxx(value: T)                       ││
│  └─────────────────────────────────────────────────────┘│
│                         │                                │
│                         ▼                                │
│  ┌─────────────────────────────────────────────────────┐│
│  │          PreferenceSettingsRepository                ││
│  │                                                      ││
│  │  DataStore<Preferences> ──► Settings                ││
│  │       ▲                          │                   ││
│  │       │                          ▼                   ││
│  │  preferences.json         Flow<Settings>            ││
│  └─────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────┘
```

## Settings Data Class

```kotlin
@Serializable
@OptIn(ExperimentalTime::class)
data class Settings(
    val language: String = "system",
    val incompatibleVersions: Boolean = false,
    val notifyUpdate: Boolean = true,
    val unstableUpdate: Boolean = false,
    val ignoreSignature: Boolean = false,
    val theme: Theme = Theme.SYSTEM,
    val dynamicTheme: Boolean = false,
    val installerType: InstallerType = InstallerType.Default,
    val legacyInstallerComponent: LegacyInstallerComponent? = null,
    val autoUpdate: Boolean = false,
    val autoSync: AutoSync = AutoSync.WIFI_ONLY,
    val sortOrder: SortOrder = SortOrder.UPDATED,
    val proxy: ProxyPreference = ProxyPreference(),
    val cleanUpInterval: Duration = 12.hours,
    @Contextual
    val lastCleanup: Instant? = null,
    val lastRbLogFetch: Long? = null,
    val lastModifiedDownloadStats: Long? = null,
    val favouriteApps: Set<String> = emptySet(),
    val homeScreenSwiping: Boolean = true,
    val enabledRepoIds: Set<Int> = emptySet(),
    val deleteApkOnInstall: Boolean = false,
)
```

## SettingsRepository Interface

```kotlin
interface SettingsRepository {
    val data: Flow<Settings>

    suspend fun getInitial(): Settings

    suspend fun export(target: Uri)
    suspend fun import(target: Uri)

    // Theme
    suspend fun setTheme(theme: Theme)
    suspend fun setDynamicTheme(enable: Boolean)

    // Updates
    suspend fun enableNotifyUpdates(enable: Boolean)
    suspend fun enableUnstableUpdates(enable: Boolean)
    suspend fun setAutoUpdate(allow: Boolean)

    // Installer
    suspend fun setInstallerType(installerType: InstallerType)
    suspend fun setLegacyInstallerComponent(component: LegacyInstallerComponent?)

    // Sync
    suspend fun setAutoSync(autoSync: AutoSync)
    suspend fun setSortOrder(sortOrder: SortOrder)

    // Proxy
    suspend fun setProxyType(proxyType: ProxyType)
    suspend fun setProxyHost(proxyHost: String)
    suspend fun setProxyPort(proxyPort: Int)

    // Repository management
    suspend fun setRepoEnabled(repoId: Int, enabled: Boolean)
    fun getEnabledRepoIds(): Flow<Set<Int>>
    suspend fun isRepoEnabled(repoId: Int): Boolean

    // Favorites
    suspend fun toggleFavourites(packageName: String)

    // Other
    suspend fun setLanguage(language: String)
    suspend fun enableIncompatibleVersion(enable: Boolean)
    suspend fun setIgnoreSignature(enable: Boolean)
    suspend fun setCleanUpInterval(interval: Duration)
    suspend fun setHomeScreenSwiping(value: Boolean)
    suspend fun setDeleteApkOnInstall(enable: Boolean)
}
```

### Helper Extension

```kotlin
inline fun <T> SettingsRepository.get(
    crossinline block: suspend Settings.() -> T
): Flow<T> {
    return data.map(block).distinctUntilChanged()
}

// Usage
val theme = settingsRepository.get { theme }
val autoSync = settingsRepository.get { autoSync }
```

## PreferenceSettingsRepository

Implementation using Preferences DataStore:

```kotlin
class PreferenceSettingsRepository(
    private val dataStore: DataStore<Preferences>,
    private val exporter: Exporter<Settings>,
) : SettingsRepository {

    override val data: Flow<Settings> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Log.e("PreferencesSettingsRepository", "Error reading preferences.", exception)
            } else {
                throw exception
            }
        }.map(::mapSettings)

    override suspend fun getInitial(): Settings = data.first()
}
```

### Preference Keys

```kotlin
companion object PreferencesKeys {
    // Strings
    val LANGUAGE = stringPreferencesKey("key_language")
    val PROXY_HOST = stringPreferencesKey("key_proxy_host")

    // Booleans
    val INCOMPATIBLE_VERSIONS = booleanPreferencesKey("key_incompatible_versions")
    val NOTIFY_UPDATES = booleanPreferencesKey("key_notify_updates")
    val UNSTABLE_UPDATES = booleanPreferencesKey("key_unstable_updates")
    val DYNAMIC_THEME = booleanPreferencesKey("key_dynamic_theme")
    val AUTO_UPDATE = booleanPreferencesKey("key_auto_updates")
    val HOME_SCREEN_SWIPING = booleanPreferencesKey("key_home_swiping")
    val DELETE_APK_ON_INSTALL = booleanPreferencesKey("key_delete_apk_on_install")

    // Integers
    val PROXY_PORT = intPreferencesKey("key_proxy_port")

    // Longs
    val CLEAN_UP_INTERVAL = longPreferencesKey("key_clean_up_interval")
    val LAST_CLEAN_UP = longPreferencesKey("key_last_clean_up_time")
    val LAST_RB_FETCH = longPreferencesKey("key_last_rb_logs_fetch_time")
    val LAST_MODIFIED_DS = longPreferencesKey("key_last_modified_download_stats")

    // String Sets
    val FAVOURITE_APPS = stringSetPreferencesKey("key_favourite_apps")
    val ENABLED_REPO_IDS = stringSetPreferencesKey("key_enabled_repo_ids")

    // Enums (stored as strings)
    val THEME = stringPreferencesKey("key_theme")
    val INSTALLER_TYPE = stringPreferencesKey("key_installer_type")
    val AUTO_SYNC = stringPreferencesKey("key_auto_sync")
    val SORT_ORDER = stringPreferencesKey("key_sort_order")
    val PROXY_TYPE = stringPreferencesKey("key_proxy_type")
}
```

## Updating Settings

```kotlin
private suspend inline fun <T> Preferences.Key<T>.update(newValue: T) {
    dataStore.edit { preferences ->
        preferences[this] = newValue
    }
}

// Usage
override suspend fun setTheme(theme: Theme) =
    THEME.update(theme.name)

override suspend fun enableNotifyUpdates(enable: Boolean) =
    NOTIFY_UPDATES.update(enable)
```

### Set Operations (Favorites)

```kotlin
override suspend fun toggleFavourites(packageName: String) {
    dataStore.edit { preference ->
        val currentSet = preference[FAVOURITE_APPS] ?: emptySet()
        val newSet = currentSet.updateAsMutable {
            if (!add(packageName)) remove(packageName)
        }
        preference[FAVOURITE_APPS] = newSet
    }
}
```

## Setting Types

### Theme

```kotlin
enum class Theme {
    SYSTEM,        // Follow system theme
    SYSTEM_BLACK,  // Follow system, AMOLED black for dark
    LIGHT,         // Always light
    DARK,          // Always dark
    AMOLED,        // Always AMOLED black
}
```

### InstallerType

```kotlin
enum class InstallerType {
    SESSION,   // Standard PackageInstaller session
    LEGACY,    // Intent-based installation
    ROOT,      // Root shell installation
    SHIZUKU;   // Shizuku service installation

    companion object {
        val Default = SESSION
    }
}
```

### AutoSync

```kotlin
enum class AutoSync {
    NEVER,      // Manual only
    WIFI_ONLY,  // WiFi networks only
    WIFI_PLUGGED_IN,  // WiFi + charging
    ALWAYS,     // Any network
}
```

### SortOrder

```kotlin
enum class SortOrder {
    NAME,      // Alphabetical by name
    UPDATED,   // Recently updated first
}
```

### ProxyPreference

```kotlin
@Serializable
data class ProxyPreference(
    val type: ProxyType = ProxyType.DIRECT,
    val host: String = "localhost",
    val port: Int = 9050,
)

enum class ProxyType {
    DIRECT,  // No proxy
    HTTP,    // HTTP proxy
    SOCKS,   // SOCKS proxy
}
```

## Dependency Injection

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DatastoreModule {

    @Provides
    @Singleton
    fun provideDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = PreferenceDataStoreFactory.create {
        context.preferencesDataStoreFile("settings")
    }

    @Provides
    @Singleton
    fun provideSettingsRepository(
        dataStore: DataStore<Preferences>,
        exporter: SettingsExporter,
    ): SettingsRepository = PreferenceSettingsRepository(dataStore, exporter)
}
```

## Using Settings in ViewModels

```kotlin
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    // Observe single setting
    val theme = settingsRepository.get { theme }
        .stateIn(viewModelScope, SharingStarted.Lazily, Theme.SYSTEM)

    // Observe all settings
    val settings = settingsRepository.data
        .stateIn(viewModelScope, SharingStarted.Lazily, Settings())

    fun setTheme(theme: Theme) {
        viewModelScope.launch {
            settingsRepository.setTheme(theme)
        }
    }
}
```

## Using Settings in Composables

```kotlin
@Composable
fun MyScreen(viewModel: MyViewModel = hiltViewModel()) {
    val theme by viewModel.theme.collectAsStateWithLifecycle()

    // Use theme value
    when (theme) {
        Theme.LIGHT -> LightContent()
        Theme.DARK -> DarkContent()
        else -> SystemContent()
    }
}
```

## Import/Export

```kotlin
override suspend fun export(target: Uri) {
    val currentSettings = getInitial()
    exporter.export(currentSettings, target)
}

override suspend fun import(target: Uri) {
    val importedSettings = exporter.import(target)
    // Merge favorites
    val updatedFavorites = importedSettings.favouriteApps + getInitial().favouriteApps
    val updatedSettings = importedSettings.copy(favouriteApps = updatedFavorites)
    dataStore.edit { it.setting(updatedSettings) }
}
```

## Storage Location

Settings are stored at:
```
/data/data/com.atd.store/files/datastore/settings.preferences_pb
```

## Removed

| Feature | Removal Doc |
|---------|-------------|
| Package: `com.looker.droidify` | [package-rename.md](../changes/package-rename.md) |
| `favouriteApps` | [favourites.md](../removal/favourites.md) |
| `ProxyPreference` | [proxy-and-backup.md](../removal/proxy-and-backup.md) |
| `AutoSync` | [auto-sync-setting.md](../removal/auto-sync-setting.md) |
| `Settings export/import` | [proxy-and-backup.md](../removal/proxy-and-backup.md) |
| `sortOrderName()`, `supportedSortOrders()` | [sort-order-ui.md](../removal/sort-order-ui.md) |

## Changes

| Change | Change Doc |
|--------|------------|
| App branding Droid-ify → ATD Store | [app-branding.md](../changes/app-branding.md) |
