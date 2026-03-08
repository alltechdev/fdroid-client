# Documentation Plan

This document outlines all documentation created for the Droid-ify codebase.

## Status: COMPLETE

All 78 documents have been created.

## Documentation Structure

```
docs/
├── DOCUMENTATION_PLAN.md    # This file
├── building.md              # Build instructions
├── settings.md              # Settings page documentation
├── receivers.md             # Broadcast receivers
├── graphics.md              # Drawable utilities
├── app-entry.md             # MainActivity and entry points
│
├── architecture/
│   ├── overview.md          # High-level architecture overview
│   ├── dependency-injection.md  # Hilt modules and DI setup
│   └── navigation.md        # App navigation structure
│
├── compose/
│   ├── viewmodels.md        # Compose ViewModels
│   └── screens.md           # Compose screens
│
├── data/
│   ├── database.md          # Room database overview
│   ├── room-database.md     # Room database details
│   ├── daos.md              # Data Access Objects
│   ├── repositories.md      # Repository pattern
│   ├── app-repository.md    # Data repositories detail
│   ├── datastore.md         # DataStore preferences
│   ├── models.md            # Domain models
│   ├── domain-models.md     # Compose domain models
│   ├── product-preferences.md # Per-app preferences
│   ├── encryption.md        # AES encryption
│   └── privacy-repository.md # RB logs & download stats
│
├── datastore/
│   ├── settings.md          # Settings system
│   ├── migration.md         # Proto to Preferences migration
│   ├── exporter.md          # Settings export/import
│   └── custom-buttons.md    # Custom buttons repository
│
├── di/
│   └── modules.md           # Hilt DI modules
│
├── sync/
│   ├── overview.md          # Sync system architecture
│   ├── index-v1.md          # V1 index format
│   ├── index-v2.md          # V2 index format
│   ├── v1-models.md         # V1 data models
│   ├── v2-models.md         # V2 data models
│   ├── index-parsing.md     # Low-level parsing
│   ├── index-converter.md   # V1 to V2 conversion
│   ├── jar-verification.md  # JAR signature verification
│   └── repository-updater.md # Legacy RepositoryUpdater
│
├── service/
│   ├── connection.md        # Service connection helper
│   ├── download-service.md  # Download service
│   ├── sync-service.md      # Sync service
│   └── release-validator.md # Release file validation
│
├── installer/
│   ├── overview.md          # Installation system
│   ├── backends.md          # Installer backends comparison
│   ├── install-manager.md   # Install queue management
│   ├── session-installer.md # PackageInstaller API
│   ├── root-installer.md    # Root installation
│   └── shizuku-installer.md # Shizuku installation
│
├── network/
│   ├── downloader.md        # Ktor downloader
│   ├── validation.md        # File validation
│   └── proxy.md             # Proxy configuration
│
├── ui/
│   ├── compose-screens.md   # Compose screens
│   ├── legacy-fragments.md  # Legacy fragments
│   ├── fragments.md         # Fragment details
│   ├── adapters.md          # RecyclerView adapters
│   ├── theming.md           # Theme system
│   ├── components.md        # UI components
│   └── widgets.md           # RecyclerView widgets
│
├── background/
│   ├── services.md          # Background services
│   └── workers.md           # WorkManager workers
│
├── features/
│   ├── custom-buttons.md    # Custom buttons
│   ├── favorites.md         # Favorites
│   ├── import-export.md     # Backup/restore
│   └── notifications.md     # Notifications
│
├── legacy/
│   ├── database.md          # Legacy SQLite
│   ├── models.md            # Legacy models
│   └── serialization.md     # Jackson JSON
│
├── work/
│   └── workers.md           # WorkManager workers
│
└── utility/
    ├── cache.md             # File caching
    ├── sdk-check.md         # SDK version checks
    ├── deeplinks.md         # URL/intent parsing
    ├── device-workarounds.md # OEM workarounds
    ├── text.md              # String utilities
    ├── extensions.md        # Kotlin extensions
    ├── common.md            # Common utilities
    ├── permissions.md       # Runtime permissions
    ├── notifications.md     # Notification helpers
    ├── exporter.md          # Export interface
    └── scroller.md          # Scroll utilities
```

## Documentation Summary

### Architecture (3 docs)
| Document | Status |
|----------|--------|
| `architecture/overview.md` | ✅ |
| `architecture/dependency-injection.md` | ✅ |
| `architecture/navigation.md` | ✅ |

### Data (5 docs)
| Document | Status |
|----------|--------|
| `data/database.md` | ✅ |
| `data/repositories.md` | ✅ |
| `data/datastore.md` | ✅ |
| `data/models.md` | ✅ |
| `data/product-preferences.md` | ✅ |

### Sync (5 docs)
| Document | Status |
|----------|--------|
| `sync/overview.md` | ✅ |
| `sync/index-v1.md` | ✅ |
| `sync/index-v2.md` | ✅ |
| `sync/index-parsing.md` | ✅ |
| `sync/repository-updater.md` | ✅ |

### Installer (5 docs)
| Document | Status |
|----------|--------|
| `installer/overview.md` | ✅ |
| `installer/install-manager.md` | ✅ |
| `installer/session-installer.md` | ✅ |
| `installer/root-installer.md` | ✅ |
| `installer/shizuku-installer.md` | ✅ |

### Network (3 docs)
| Document | Status |
|----------|--------|
| `network/downloader.md` | ✅ |
| `network/validation.md` | ✅ |
| `network/proxy.md` | ✅ |

### UI (5 docs)
| Document | Status |
|----------|--------|
| `ui/compose-screens.md` | ✅ |
| `ui/legacy-fragments.md` | ✅ |
| `ui/theming.md` | ✅ |
| `ui/components.md` | ✅ |
| `ui/widgets.md` | ✅ |

### Background (2 docs)
| Document | Status |
|----------|--------|
| `background/services.md` | ✅ |
| `background/workers.md` | ✅ |

### Features (4 docs)
| Document | Status |
|----------|--------|
| `features/custom-buttons.md` | ✅ |
| `features/favorites.md` | ✅ |
| `features/import-export.md` | ✅ |
| `features/notifications.md` | ✅ |

### Legacy (3 docs)
| Document | Status |
|----------|--------|
| `legacy/database.md` | ✅ |
| `legacy/models.md` | ✅ |
| `legacy/serialization.md` | ✅ |

### Utility (10 docs)
| Document | Status |
|----------|--------|
| `utility/cache.md` | ✅ |
| `utility/sdk-check.md` | ✅ |
| `utility/deeplinks.md` | ✅ |
| `utility/device-workarounds.md` | ✅ |
| `utility/text.md` | ✅ |
| `utility/extensions.md` | ✅ |
| `utility/permissions.md` | ✅ |
| `utility/notifications.md` | ✅ |
| `utility/exporter.md` | ✅ |
| `utility/scroller.md` | ✅ |

### Top-Level (3 docs)
| Document | Status |
|----------|--------|
| `building.md` | ✅ |
| `settings.md` | ✅ |
| `receivers.md` | ✅ |
| `graphics.md` | ✅ |

## Maintenance

- Update docs when making significant changes to related code
- Add new docs for new features before merging
- Review docs quarterly for accuracy
- Keep CLAUDE.md updated with key architecture changes

## Removed

| Feature | Removal Doc |
|---------|-------------|
| Package: `com.looker.droidify` | [package-rename.md](changes/package-rename.md) |

Note: Some features referenced in this plan have been removed. See `docs/removal/` for details.

## Changes

| Change | Change Doc |
|--------|------------|
| App branding Droid-ify → ATD Store | [app-branding.md](changes/app-branding.md) |
