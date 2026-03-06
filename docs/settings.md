# Settings Page Documentation

This document explains the architecture of the Settings screen and how to add, modify, or remove settings entries.

## Architecture Overview

The settings system consists of three main layers:

```
SettingsScreen.kt (UI)
       ↓
SettingsViewModel.kt (Logic)
       ↓
SettingsRepository.kt → PreferenceSettingsRepository.kt (Persistence)
       ↓
Settings.kt (Data Model)
```

### Key Files

| File | Purpose |
|------|---------|
| `compose/settings/SettingsScreen.kt` | Compose UI for the settings page |
| `compose/settings/SettingsViewModel.kt` | ViewModel handling settings logic |
| `datastore/Settings.kt` | Data class defining all settings fields |
| `datastore/SettingsRepository.kt` | Interface for settings operations |
| `datastore/PreferenceSettingsRepository.kt` | DataStore-based implementation |
| `compose/settings/components/` | Reusable UI components for settings items |

## Available Setting Components

### SwitchSettingItem
For boolean on/off settings.

```kotlin
SwitchSettingItem(
    title = stringResource(R.string.my_setting),
    description = stringResource(R.string.my_setting_desc),
    checked = settings.mySetting,
    onCheckedChange = viewModel::setMySetting,
)
```

### SelectionSettingItem
For selecting from a list of options.

```kotlin
SelectionSettingItem(
    title = stringResource(R.string.my_selection),
    selectedValue = settings.myEnum,
    values = MyEnum.entries,
    onValueSelected = viewModel::setMyEnum,
    valueToString = { enum -> stringResource(enum.labelRes) },
)
```

### TextInputSettingItem
For text input settings.

```kotlin
TextInputSettingItem(
    title = stringResource(R.string.proxy_host),
    value = settings.proxy.host,
    onValueChange = viewModel::setProxyHost,
)
```

### ActionSettingItem
For clickable actions (export, import, etc.).

```kotlin
ActionSettingItem(
    title = stringResource(R.string.export_settings),
    description = stringResource(R.string.export_settings_desc),
    onClick = { /* action */ },
)
```

### SettingHeader
Section headers to group related settings.

```kotlin
SettingHeader(title = stringResource(R.string.updates))
```

## Adding a New Setting

### Step 1: Add field to Settings data class

In `datastore/Settings.kt`:

```kotlin
@Serializable
data class Settings(
    // ... existing fields
    val myNewSetting: Boolean = false,  // Add with default value
)
```

### Step 2: Add preference key and mapping

In `datastore/PreferenceSettingsRepository.kt`:

```kotlin
companion object PreferencesKeys {
    // ... existing keys
    val MY_NEW_SETTING = booleanPreferencesKey("key_my_new_setting")
}

private fun mapSettings(preferences: Preferences): Settings {
    // ... existing mappings
    val myNewSetting = preferences[MY_NEW_SETTING] ?: false

    return Settings(
        // ... existing fields
        myNewSetting = myNewSetting,
    )
}
```

### Step 3: Add repository method

In `datastore/SettingsRepository.kt`:

```kotlin
interface SettingsRepository {
    // ... existing methods
    suspend fun setMyNewSetting(enabled: Boolean)
}
```

In `datastore/PreferenceSettingsRepository.kt`:

```kotlin
override suspend fun setMyNewSetting(enabled: Boolean) =
    MY_NEW_SETTING.update(enabled)
```

Also update `MutablePreferences.setting()` for import/export support:

```kotlin
fun MutablePreferences.setting(settings: Settings): Preferences {
    // ... existing settings
    set(MY_NEW_SETTING, settings.myNewSetting)
    return this.toPreferences()
}
```

### Step 4: Add ViewModel method

In `compose/settings/SettingsViewModel.kt`:

```kotlin
fun setMyNewSetting(enabled: Boolean) {
    viewModelScope.launch {
        settingsRepository.setMyNewSetting(enabled)
    }
}
```

### Step 5: Add UI in SettingsScreen

In `compose/settings/SettingsScreen.kt`, add an `item` in the `LazyColumn`:

```kotlin
item {
    SwitchSettingItem(
        title = stringResource(R.string.my_new_setting),
        description = stringResource(R.string.my_new_setting_desc),
        checked = settings.myNewSetting,
        onCheckedChange = viewModel::setMyNewSetting,
    )
}
```

### Step 6: Add string resources

In `res/values/strings.xml`:

```xml
<string name="my_new_setting">My New Setting</string>
<string name="my_new_setting_desc">Description of what this setting does</string>
```

## Adding an Enum-Based Setting

For settings with multiple predefined options:

### Step 1: Create the enum

In `datastore/model/` create a new file or add to existing:

```kotlin
@Serializable
enum class MyOption {
    OPTION_A,
    OPTION_B,
    OPTION_C;
}
```

### Step 2: Follow steps 1-5 above

Use the enum type in `Settings.kt` and use `SelectionSettingItem` in the UI.

### Step 3: Add value-to-string mapping in UI

```kotlin
@Composable
private fun MyOptionSetting(
    selectedOption: MyOption,
    onOptionSelected: (MyOption) -> Unit,
) {
    SelectionSettingItem(
        title = stringResource(R.string.my_option),
        selectedValue = selectedOption,
        values = MyOption.entries,
        onValueSelected = onOptionSelected,
        valueToString = { option ->
            when (option) {
                MyOption.OPTION_A -> stringResource(R.string.option_a)
                MyOption.OPTION_B -> stringResource(R.string.option_b)
                MyOption.OPTION_C -> stringResource(R.string.option_c)
            }
        },
    )
}
```

## Removing a Setting

### Step 1: Remove from UI
Delete the `item { }` block from `SettingsScreen.kt`.

### Step 2: Remove ViewModel method
Delete the setter method from `SettingsViewModel.kt`.

### Step 3: Remove repository method
Delete from both `SettingsRepository.kt` interface and `PreferenceSettingsRepository.kt` implementation.

### Step 4: Keep field in Settings (for backwards compatibility)
Keep the field in `Settings.kt` with its default value to avoid deserialization errors for existing users. You can mark it as deprecated:

```kotlin
@Deprecated("No longer used")
val oldSetting: Boolean = false,
```

### Step 5: Remove string resources
Delete the related strings from `strings.xml` (ensure they're not used elsewhere first).

## Conditional Settings

Settings can be conditionally displayed based on other settings or device capabilities:

```kotlin
// Only show on Android 12+
if (SdkCheck.isSnowCake) {
    item {
        SwitchSettingItem(
            title = stringResource(R.string.material_you),
            // ...
        )
    }
}

// Only show when another setting has specific value
if (settings.installerType == InstallerType.LEGACY) {
    item {
        LegacyInstallerComponentSetting(/* ... */)
    }
}
```

## Settings Sections

Settings are grouped into sections using `SettingHeader`. Current sections:

1. **Personalization** - Language, theme, dynamic colors, home screen behavior
2. **Updates** - Auto-update, notifications, unstable/incompatible versions
3. **Sync Repositories** - Auto-sync mode, cleanup interval
4. **Install Types** - Installer selection, APK deletion
5. **Proxy** - Proxy type, host, port
6. **Import/Export** - Settings and repository backup
7. **Custom Buttons** - User-defined action buttons
8. **Credits** - About and attribution

## Reacting to Setting Changes

To react to setting changes elsewhere in the app, use the `get` extension function:

```kotlin
// In any component with access to SettingsRepository
settingsRepository.get { myNewSetting }.collect { value ->
    // React to changes
}
```

## Import/Export

Settings are automatically serialized to JSON via kotlinx.serialization. The `Settings` data class is annotated with `@Serializable`, and all fields are included in export/import operations through `PreferenceSettingsRepository.export()` and `import()` methods.
