# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Simplified F-Droid client for Android, forked from Droid-ify. Streamlined experience focused on core functionality:

- Browse and install apps from a single hardcoded repository
- Automatic background sync when network is available
- Multiple installation methods (Session, Root, Shizuku)
- Minimal UI with no advanced settings

**Removed features** (see `docs/removal/`): proxy settings, backup/restore, repository management, favorites, custom buttons, sort order UI, deep links, TV support, and more.

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
- **NEVER use sed** - use the Edit tool for all file modifications
- **NEVER suppress warnings** - always fix the underlying issue
- **Use parallel agents for bulk string operations** - when removing strings from multiple locale files, spawn multiple agents in parallel (each handling ~8 files)

## Commit Convention

Use conventional commits: `<type>: <description>`

Types: `feat`, `fix`, `docs`, `style`, `refactor`, `chore`

## Testing

- JUnit 5 for unit tests with Robolectric for Android components
- MockK for mocking
- Turbine for Flow testing
- Room in-memory database for DAO tests (see `BaseDatabaseTest`)

## Pre-Commit Verification

**MANDATORY: Run these checks before every commit. Fix any errors.**

```bash
# 1. Build - must pass with no errors
./gradlew assembleDebug

# 2. Unit tests - must pass
./gradlew :app:testDebugUnitTest

# 3. Lint - fix all errors
./gradlew lintDebug

# 4. Check lint report for errors/warnings
cat app/build/reports/lint-results-debug.txt | tail -20
```

**Important**: Don't just check if lint passes - review the actual report for errors and warnings. Fix any errors. Warnings from your changes should be addressed; pre-existing warnings can be left alone.

After code changes pass, update documentation per the Documentation Updates section below.

## Documentation Updates

The `docs/` directory has three special subdirectories for tracking codebase evolution:

- `docs/removal/` - Removed features
- `docs/changes/` - Modified features
- `docs/additions/` - New features

### When Removing Features

1. **Create removal documentation** in `docs/removal/`:
   - Create a file named after the feature (e.g., `feature-name.md`)
   - Document what was removed, why, and list all affected files/components
   - Follow the format of existing removal docs

2. **Update the removal index** at `docs/removal/README.md`:
   - Add an entry linking to the new removal doc

3. **Add "Removed" sections** to all affected documentation files:
   - Do NOT delete content from docs; instead add a `## Removed` section at the end
   - Use a table format linking to the removal documentation:
     ```markdown
     ## Removed

     | Feature | Removal Doc |
     |---------|-------------|
     | `FeatureName` | [feature-name.md](../removal/feature-name.md) |
     ```
   - Check all docs that reference the removed feature: architecture, UI, data layer, etc.

4. **Clean up orphaned resources**:
   - Remove unused string resources from all locale files
   - Remove unused layout files, drawables, etc.

5. **MANDATORY: Run verification before committing**:
   ```bash
   # Find all docs referencing the removed feature
   grep -rl "FeatureName" docs/

   # Verify each found doc has a ## Removed section
   grep -L "## Removed" <list of files>
   ```
   - Every doc that mentions the removed feature MUST have a `## Removed` section
   - Do not commit until all affected docs are updated

### When Modifying Features

1. **Create change documentation** in `docs/changes/`:
   - Document what changed from original implementation
   - Include rationale for the changes
   - Analyze impact on other parts of the codebase

2. **Update the changes index** at `docs/changes/README.md`

### When Adding Features

1. **Create addition documentation** in `docs/additions/`:
   - Document what was added and how it works
   - Include integration guide showing how it connects with existing code
   - Provide usage examples

2. **Update the additions index** at `docs/additions/README.md`

### Documentation Verification Checklist

Before committing any feature removal, modification, or addition:

1. **Search for all references**:
   ```bash
   grep -rl "FeatureName\|feature_name\|featureName" docs/
   ```

2. **Check these doc directories** (features often span multiple areas):
   - `docs/features/` - Feature-specific docs
   - `docs/datastore/` - Settings and data storage
   - `docs/data/` - Data layer docs
   - `docs/compose/` - Compose UI docs
   - `docs/ui/` - Legacy UI docs
   - `docs/architecture/` - Architecture docs

3. **Verify completeness**:
   - For removals: Every doc mentioning the feature has `## Removed` section
   - For changes: Affected docs reference the change documentation
   - For additions: New feature is documented and linked from relevant existing docs

4. **Validate links**: Ensure all relative links in `## Removed` tables resolve correctly
