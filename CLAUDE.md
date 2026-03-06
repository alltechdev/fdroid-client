# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Droid-ify is a modern F-Droid client for Android. It allows users to browse, install, and manage apps from F-Droid repositories with features like automatic background updates, multiple installation methods (Session, Root, Shizuku), and offline support.

## Build Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Install to connected device
./gradlew installDebug

# Run unit tests
./gradlew :app:testDebugUnitTest

# Run a single test class
./gradlew :app:testDebugUnitTest --tests "com.looker.droidify.data.local.dao.RepoDaoTest"

# Clean build
./gradlew clean --refresh-dependencies
```

APK output: `app/build/outputs/apk/`

## Architecture

### Layers
- **Presentation**: Jetpack Compose screens (`compose/`) and legacy Views (`ui/`) with ViewModels
- **Data**: Room database, DataStore preferences, repositories (`data/`)
- **Domain**: Business logic in sync, installer, and network modules

### Key Components

**Sync System** (`sync/`):
- `Syncable` interface with V1 and V2 implementations for F-Droid index formats
- `V1Syncable` - Legacy JSON index in JAR files
- `EntrySyncable` - Modern V2 index format with diff support
- `IndexConverter` - Transforms index data to database entities

**Installation** (`installer/`):
- `InstallManager` - Orchestrates download and installation queue
- Installer implementations: `SessionInstaller` (default), `RootInstaller`, `ShizukuInstaller`, `LegacyInstaller`

**Network** (`network/`):
- `Downloader` interface with `KtorDownloader` implementation
- Single shared `HttpClient` across the app (injected via Hilt)

**Data Layer** (`data/local/`):
- Room database `DroidifyDatabase` with DAOs for apps, repos, installed packages
- Repositories: `AppRepository`, `RepoRepository`, `InstalledRepository`

**Background Work**:
- `SyncService` - JobScheduler-based periodic sync
- WorkManager workers: `SyncWorker`, `CleanUpWorker`, `DownloadStatsWorker`

### Dependency Injection

Uses Hilt with modules in `di/`:
- `DatabaseModule`, `NetworkModule`, `SyncableModule`, `InstallModule`, `DatastoreModule`

### UI Structure

Two parallel UI implementations:
- **Compose** (`compose/`): New screens with navigation in `navigation/` subdirectories
- **Legacy Views** (`ui/`): Fragment-based screens being migrated to Compose

## Code Style

- Kotlin with coroutines throughout
- Context parameters enabled (`-Xcontext-parameters`)
- Prefer `val` over `var`
- Follow Kotlin coding conventions
- 4 spaces indentation, 120 character line limit

## Commit Convention

Use conventional commits: `<type>: <description>`

Types: `feat`, `fix`, `docs`, `style`, `refactor`, `chore`

## Testing

- JUnit 5 for unit tests with Robolectric for Android components
- MockK for mocking
- Turbine for Flow testing
- Room in-memory database for DAO tests (see `BaseDatabaseTest`)
