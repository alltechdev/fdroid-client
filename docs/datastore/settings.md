# Settings System

User preferences and app settings management using Jetpack DataStore.

## Overview

**Files:**
- `datastore/Settings.kt` - Settings data class
- `datastore/SettingsRepository.kt` - Repository interface
- `datastore/PreferenceSettingsRepository.kt` - Implementation

## Settings Data Class

```kotlin
@Serializable
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
    val lastCleanup: Instant? = null,
    val lastRbLogFetch: Long? = null,
    val lastModifiedDownloadStats: Long? = null,
    val favouriteApps: Set<String> = emptySet(),
    val homeScreenSwiping: Boolean = true,
    val enabledRepoIds: Set<Int> = emptySet(),
    val deleteApkOnInstall: Boolean = false,
)
```

### Setting Categories

| Category | Settings |
|----------|----------|
| Display | `theme`, `dynamicTheme`, `language`, `homeScreenSwiping` |
| Updates | `notifyUpdate`, `unstableUpdate`, `autoUpdate`, `incompatibleVersions` |
| Installation | `installerType`, `legacyInstallerComponent`, `ignoreSignature`, `deleteApkOnInstall` |
| Sync | `autoSync`, `sortOrder`, `enabledRepoIds` |
| Network | `proxy` (type, host, port) |
| Data | `favouriteApps`, `cleanUpInterval`, `lastCleanup` |

## Repository Interface

```kotlin
interface SettingsRepository {
    val data: Flow<Settings>

    suspend fun getInitial(): Settings
    suspend fun export(target: Uri)
    suspend fun import(target: Uri)

    // Individual setters for each setting
    suspend fun setTheme(theme: Theme)
    suspend fun setInstallerType(installerType: InstallerType)
    suspend fun setAutoSync(autoSync: AutoSync)
    suspend fun toggleFavourites(packageName: String)
    suspend fun setRepoEnabled(repoId: Int, enabled: Boolean)
    // ... more setters
}
```

### Convenience Extension

```kotlin
inline fun <T> SettingsRepository.get(
    crossinline block: suspend Settings.() -> T
): Flow<T> = data.map(block).distinctUntilChanged()

// Usage
settingsRepository.get { theme to dynamicTheme }
    .collect { (theme, dynamic) -> applyTheme(theme, dynamic) }
```

## Settings Serializer

```kotlin
object SettingsSerializer : Serializer<Settings> {
    private val json = Json { encodeDefaults = true }

    override val defaultValue: Settings = Settings()

    override suspend fun readFrom(input: InputStream): Settings =
        json.decodeFromStream(input)

    override suspend fun writeTo(t: Settings, output: OutputStream) =
        json.encodeToStream(t, output)
}
```

## Settings Models

### Theme

```kotlin
enum class Theme {
    SYSTEM,        // Follow system setting
    SYSTEM_BLACK,  // Follow system, black in dark mode
    LIGHT,
    DARK,
    AMOLED         // Pure black for OLED screens
}
```

### AutoSync

```kotlin
enum class AutoSync {
    ALWAYS,         // Sync on any connection
    WIFI_ONLY,      // Only on WiFi
    WIFI_PLUGGED_IN, // WiFi + charging
    NEVER           // Manual only
}
```

### InstallerType

```kotlin
enum class InstallerType {
    LEGACY,   // Legacy PackageInstaller intent
    SESSION,  // PackageInstaller session API
    SHIZUKU,  // Shizuku privileged install
    ROOT;     // Root shell install

    companion object {
        val Default: InstallerType
            get() = if (Miui.isMiui) {
                if (Miui.isMiuiOptimizationDisabled()) SESSION else LEGACY
            } else {
                SESSION
            }
    }
}
```

### ProxyPreference

```kotlin
@Serializable
data class ProxyPreference(
    val type: ProxyType = ProxyType.DIRECT,
    val host: String = "localhost",
    val port: Int = 9050  // Default Tor port
)

enum class ProxyType { DIRECT, HTTP, SOCKS }
```

### SortOrder

```kotlin
enum class SortOrder {
    UPDATED,  // Recently updated first
    ADDED,    // Newest additions first
    NAME,     // Alphabetical
    SIZE      // Largest first
}
```

## Theme Resolution

**File:** `datastore/extension/Preferences.kt`

```kotlin
fun Configuration.getThemeRes(theme: Theme, dynamicTheme: Boolean) = when (theme) {
    Theme.SYSTEM -> {
        if ((uiMode and Configuration.UI_MODE_NIGHT_YES) != 0) {
            if (SdkCheck.isSnowCake && dynamicTheme)
                styleRes.Theme_Main_DynamicDark
            else
                styleRes.Theme_Main_Dark
        } else {
            if (SdkCheck.isSnowCake && dynamicTheme)
                styleRes.Theme_Main_DynamicLight
            else
                styleRes.Theme_Main_Light
        }
    }
    Theme.AMOLED -> if (SdkCheck.isSnowCake && dynamicTheme)
        styleRes.Theme_Main_DynamicAmoled
    else
        styleRes.Theme_Main_Amoled
    // ... other cases
}
```

Dynamic themes require Android 12+ (Snow Cake).

## Data Flow

```
┌─────────────────────────────────────────────────────────────┐
│                    DataStore<Preferences>                    │
│  - File: settings_file.preferences_pb                       │
│  - Key-value storage                                        │
└─────────────────────────┬───────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│              PreferenceSettingsRepository                    │
│  - Reads/writes preferences                                 │
│  - Emits Flow<Settings>                                     │
│  - Handles export/import                                    │
└─────────────────────────┬───────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                    UI / ViewModels                          │
│  - Observe settings changes                                 │
│  - Call setters to update                                   │
└─────────────────────────────────────────────────────────────┘
```
