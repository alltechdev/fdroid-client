# Architecture Overview

This document describes the high-level architecture of Droid-ify.

## Application Layers

```
┌─────────────────────────────────────────────────────────┐
│                    Presentation Layer                    │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────┐  │
│  │   Compose   │  │   Legacy    │  │   ViewModels    │  │
│  │   Screens   │  │  Fragments  │  │                 │  │
│  └─────────────┘  └─────────────┘  └─────────────────┘  │
├─────────────────────────────────────────────────────────┤
│                     Domain Layer                         │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────┐  │
│  │   Sync      │  │  Installer  │  │   Repositories  │  │
│  │   System    │  │   System    │  │                 │  │
│  └─────────────┘  └─────────────┘  └─────────────────┘  │
├─────────────────────────────────────────────────────────┤
│                      Data Layer                          │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────┐  │
│  │    Room     │  │  DataStore  │  │    Network      │  │
│  │   Database  │  │ Preferences │  │   (Ktor)        │  │
│  └─────────────┘  └─────────────┘  └─────────────────┘  │
└─────────────────────────────────────────────────────────┘
```

## Application Entry Point

### Droidify.kt

The `Droidify` class is the Application subclass and the entry point for the app.

**Key responsibilities:**

| Responsibility | Description |
|----------------|-------------|
| Database initialization | Initializes legacy `Database` and triggers migration if needed |
| Product preferences | Sets up `ProductPreferences` for app-specific settings |
| Repository updater | Initializes `RepositoryUpdater` with downloader |
| Package listener | Registers `InstalledAppReceiver` for install/uninstall events |
| Sync job scheduling | Sets up periodic repository sync via `JobScheduler` |
| Image loading | Configures Coil `ImageLoader` with Ktor network fetcher |
| WorkManager | Provides `HiltWorkerFactory` for dependency-injected workers |

**Initialization flow:**

```
onCreate()
    │
    ├── Database.init() ─────────────► Initialize legacy SQLite database
    │
    ├── ProductPreferences.init() ──► Load product-specific preferences
    │
    ├── RepositoryUpdater.init() ───► Setup repository update logic
    │
    ├── listenApplications() ───────► Register package install/remove receiver
    │
    ├── checkLanguage() ────────────► Sync language with system settings
    │
    ├── updatePreference() ─────────► Observe settings changes
    │       │
    │       ├── unstableUpdate ─────► Force sync when toggled
    │       ├── autoSync ───────────► Update sync job schedule
    │       └── cleanUpInterval ────► Schedule/cancel cleanup worker
    │
    └── installer() ────────────────► Start install manager
```

## Key Components

### Sync System (`sync/`)

Handles downloading and parsing F-Droid repository indexes.

- **V1 Syncable**: Legacy JSON index in signed JAR files
- **V2 Syncable**: Modern index format with diff/entry support
- **IndexConverter**: Transforms parsed index data to Room entities

### Installation System (`installer/`)

Manages app downloads and installations.

- **InstallManager**: Orchestrates download queue and installation
- **Installer implementations**: Session (default), Root, Shizuku, Legacy

### Services (`service/`)

Background services for long-running operations.

| Service | Purpose |
|---------|---------|
| `SyncService` | JobScheduler-based periodic repository sync |
| `DownloadService` | Foreground service for APK downloads |

### Workers (`work/`)

WorkManager workers for background tasks.

| Worker | Purpose |
|--------|---------|
| `SyncWorker` | One-time repository sync |
| `CleanUpWorker` | Periodic cache cleanup |
| `DownloadStatsWorker` | Fetch download statistics |
| `RBLogWorker` | Fetch reproducible build logs |
| `UnarchiveWorker` | Handle archived app restoration |

## Data Flow

### App List Loading

```
UI (AppListScreen)
    │
    ▼
ViewModel (AppListViewModel)
    │
    ▼
Repository (AppRepository.apps())
    │
    ▼
DAO (AppDao.query())
    │
    ▼
Room Database (SQLite)
```

### Repository Sync

```
SyncService/SyncWorker
    │
    ▼
RepoRepository.sync()
    │
    ├── V2 Syncable ──► Download entry.json
    │       │
    │       ▼
    │   Download index-v2.json (or diff)
    │       │
    │       ▼
    │   Parse JSON to IndexV2
    │
    ▼
IndexDao.insertIndex()
    │
    ▼
Room Database (apps, versions, categories, etc.)
```

### App Installation

```
UI (Install button clicked)
    │
    ▼
InstallManager.install()
    │
    ├── Download APK via Downloader
    │
    ├── Validate hash/signature
    │
    └── Install via selected Installer
            │
            ├── SessionInstaller (PackageInstaller API)
            ├── RootInstaller (su commands)
            ├── ShizukuInstaller (Shizuku API)
            └── LegacyInstaller (ACTION_INSTALL_PACKAGE intent)
```

## Dependency Injection

Hilt is used for dependency injection. All modules are in the `di/` package.

See [dependency-injection.md](dependency-injection.md) for details.

## Threading Model

| Context | Usage |
|---------|-------|
| `Dispatchers.IO` | Network, database, file operations |
| `Dispatchers.Default` | CPU-intensive work (parsing) |
| `Dispatchers.Main` | UI updates |

Coroutine dispatchers are provided via Hilt with `@IoDispatcher` and `@DefaultDispatcher` qualifiers.

## Configuration

### Build Variants

| Variant | Application ID Suffix | Description |
|---------|----------------------|-------------|
| `debug` | `.debug` | Development builds |
| `alpha` | `.alpha` | Testing builds with minification |
| `release` | (none) | Production builds |

### Feature Flags

Compile-time feature detection is done via `SdkCheck`:

```kotlin
if (SdkCheck.isSnowCake) {  // Android 12+
    // Use Material You
}
```
