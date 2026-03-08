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
    val notifyUpdate: Boolean = true,
    val theme: Theme = Theme.SYSTEM,
    val dynamicTheme: Boolean = false,
    val installerType: InstallerType = InstallerType.Default,
    val legacyInstallerComponent: LegacyInstallerComponent? = null,
    val autoUpdate: Boolean = false,
    val sortOrder: SortOrder = SortOrder.UPDATED,
    val lastCleanup: Instant? = null,
    val lastRbLogFetch: Long? = null,
    val lastModifiedDownloadStats: Long? = null,
    val favouriteApps: Set<String> = emptySet(),
    val homeScreenSwiping: Boolean = true,
    val enabledRepoIds: Set<Int> = emptySet(),
)
```

### Setting Categories

| Category | Settings |
|----------|----------|
| Display | `theme`, `dynamicTheme`, `language`, `homeScreenSwiping` |
| Updates | `notifyUpdate`, `autoUpdate` |
| Installation | `installerType`, `legacyInstallerComponent` |
| Sync | `sortOrder`, `enabledRepoIds` |
| Data | `lastCleanup` |

### Removed Settings

| Setting | Reason | Hardcoded Value |
|---------|--------|-----------------|
| `proxy` | Feature removed | N/A (no proxy support) |
| `cleanUpInterval` | Simplified | 6 hours |
| `deleteApkOnInstall` | Simplified | Always true |
| `unstableUpdate` | Simplified | Always false |
| `incompatibleVersions` | Simplified | Always false |
| `ignoreSignature` | Simplified | Always false |
| `autoSync` | Simplified | Always syncs when network available |
| `favouriteApps` | Feature removed | N/A |

## Repository Interface

```kotlin
interface SettingsRepository {
    val data: Flow<Settings>

    suspend fun getInitial(): Settings

    // Individual setters for each setting
    suspend fun setTheme(theme: Theme)
    suspend fun setInstallerType(installerType: InstallerType)
    suspend fun toggleFavourites(packageName: String)
    suspend fun setRepoEnabled(repoId: Int, enabled: Boolean)
    // ... more setters
}
```

### Removed Methods

- `export(target: Uri)` - Backup/restore removed
- `import(target: Uri)` - Backup/restore removed
- `setCleanUpInterval(interval: Duration)` - Hardcoded to 6 hours
- `setDeleteApkOnInstall(enable: Boolean)` - Hardcoded to true
- `enableUnstableUpdates(enable: Boolean)` - Hardcoded to false
- `enableIncompatibleVersion(enable: Boolean)` - Hardcoded to false
- `setIgnoreSignature(enable: Boolean)` - Hardcoded to false
- `setAutoSync(autoSync: AutoSync)` - Sync always enabled automatically
- `toggleFavourites(packageName: String)` - Feature removed

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

### AutoSync (REMOVED)

> **Removed:** Auto-sync setting has been removed. Sync now always runs automatically when network is available.

```kotlin
// Previously:
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

### ProxyPreference (REMOVED)

> **Removed:** Proxy support has been removed from the app.

```kotlin
// Previously:
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

## Removed

| Feature | Removal Doc |
|---------|-------------|
| Package: `com.looker.droidify` | [package-rename.md](../changes/package-rename.md) |
| `ProxyPreference` | [proxy-and-backup.md](../removal/proxy-and-backup.md) |
| `export()` / `import()` methods | [custom-buttons-and-settings.md](../removal/custom-buttons-and-settings.md) |
| `AutoSync` enum, `setAutoSync()` | [auto-sync-setting.md](../removal/auto-sync-setting.md) |
| `favouriteApps`, `toggleFavourites()` | [favourites.md](../removal/favourites.md) |
| `sortOrderName()`, `supportedSortOrders()` | [sort-order-ui.md](../removal/sort-order-ui.md) |
