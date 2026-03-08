# Removed: Proxy and Backup/Restore Settings

**Date:** 2026-03-06
**Branch:** main

## Overview

Removed the Proxy configuration section and Backup/Restore (import/export) section from settings.

## Proxy Section

### What was removed

| Component | File | Description |
|-----------|------|-------------|
| UI (Compose) | `compose/settings/SettingsScreen.kt` | Proxy type selector, host/port inputs |
| UI (Legacy) | `res/layout/settings_page.xml` | Proxy section layout |
| ViewModel | `compose/settings/SettingsViewModel.kt` | `setProxyType`, `setProxyHost`, `setProxyPort` |
| Repository Interface | `datastore/SettingsRepository.kt` | Proxy setter methods |
| Repository Impl | `datastore/PreferenceSettingsRepository.kt` | Proxy keys and mapping |
| Settings Model | `datastore/Settings.kt` | `proxy` field |
| Network Module | `di/NetworkModule.kt` | Proxy configuration in HttpClient |
| Model | `datastore/model/ProxyPreference.kt` | **Deleted** |
| Model | `datastore/model/ProxyType.kt` | **Deleted** |
| Component | `compose/settings/components/TextInputSettingItem.kt` | **Deleted** (only used for proxy) |

### Strings removed

- `proxy`, `proxy_type`, `proxy_host`, `proxy_port`
- `proxy_restart_required`, `proxy_port_error_not_int`
- `no_proxy`, `http_proxy`, `socks_proxy`

### Network changes

`NetworkModule.kt` now creates HttpClient without proxy support:

```kotlin
// Before
val proxyPreference = runBlocking { settingsRepository.getInitial().proxy }
val engine = OkHttp.create { proxy = proxyPreference.toProxy() }

// After
HttpClient(OkHttp) { ... }
```

## Backup/Restore Section

### What was removed

| Component | File | Description |
|-----------|------|-------------|
| UI (Compose) | `compose/settings/SettingsScreen.kt` | Import/export buttons and launchers |
| UI (Legacy) | `res/layout/settings_page.xml` | Import/export section |
| ViewModel | `compose/settings/SettingsViewModel.kt` | `exportSettings`, `importSettings`, `exportRepos`, `importRepos` |
| Repository Interface | `datastore/SettingsRepository.kt` | `export()`, `import()` methods |
| Repository Impl | `datastore/PreferenceSettingsRepository.kt` | Export/import implementation |
| DI Module | `di/DatastoreModule.kt` | `SettingsExporter` provider |
| Exporter | `datastore/exporter/SettingsExporter.kt` | **Deleted** |

### Strings removed

- `import_export`
- `import_settings_title`, `import_settings_DESC`
- `export_settings_title`, `export_settings_DESC`
- `import_repos_title`, `import_repos_DESC`
- `export_repos_title`, `export_repos_DESC`

### Also deleted (confirmed unused)

| Component | File | Description |
|-----------|------|-------------|
| Interface | `utility/common/Exporter.kt` | **Deleted** |
| Exporter | `database/RepositoryExporter.kt` | **Deleted** |

## Migration notes

- Existing users with proxy settings configured will lose those settings
- No data migration needed - settings simply won't be read anymore
- PreferencesKeys for proxy still exist in stored preferences but are ignored
