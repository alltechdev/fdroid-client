# Versions and Anti-Features Removal

Removed the versions section and anti-features from both UI and data layer.

## Date
2026-03-07

## Branch
main

## Reason

Simplify the app by removing:
- **Versions section**: Users typically only need the latest compatible version. The install/update button handles this automatically.
- **Anti-features**: This information was synced and stored but never displayed after UI removal. Complete removal of data layer.

## Components Removed

### Database Entities (Deleted)

| File | Description |
|------|-------------|
| `AntiFeatureEntity.kt` | Room entity for anti-features, repo relations, app relations |

### Database Changes

- Removed `AntiFeatureEntity`, `AntiFeatureAppRelation`, `AntiFeatureRepoRelation` from entities list
- Database version reset to 1 (destructive migration)

### DAO Changes

| File | Changes |
|------|---------|
| `IndexDao.kt` | Removed anti-feature insertion methods and logic |
| `AppDao.kt` | Removed anti-feature filtering parameters from queries |

### Repository Changes

| File | Changes |
|------|---------|
| `AppRepository.kt` | Removed `antiFeaturesToInclude`/`antiFeaturesToExclude` parameters |

### Model Changes

| File | Changes |
|------|---------|
| `Package.kt` | Removed `antiFeatures: List<String>` field |
| `VersionEntity.kt` | Simplified `versionEntities()` to return `List<VersionEntity>` instead of `Map<VersionEntity, List<AntiFeatureAppRelation>>` |
| `Repo.kt` | Removed unused `AntiFeature` data class |

### UI Components (Legacy)

| Component | File | Description |
|-----------|------|-------------|
| `ViewType.VERSION` | `AppDetailAdapter.kt` | ViewType enum value for version items |
| `VersionViewHolder` | `AppDetailAdapter.kt` | ViewHolder for version items |
| `Item.VersionItem` | `AppDetailAdapter.kt` | Item class for version display |
| `SectionType.VERSIONS` | `AppDetailAdapter.kt` | Section type enum value |
| `ExpandType.VERSIONS` | `AppDetailAdapter.kt` | Expand type enum value |
| `ViewType.EMPTY` | `AppDetailAdapter.kt` | ViewType for empty states |
| `EmptyViewHolder` | `AppDetailAdapter.kt` | ViewHolder for empty state |
| `Item.EmptyItem` | `AppDetailAdapter.kt` | Item class for empty state |

### UI Components (Compose)

| Component | File | Description |
|-----------|------|-------------|
| `ReleaseItem.kt` | `compose/appDetail/` | Compose component for version display (deleted) |
| Version-related composables | `AppDetailScreen.kt` | Version list rendering code |
| Anti-features section | `AppDetailScreen.kt` | Anti-features display code |

### String Resources Removed

| Resource | Description |
|----------|-------------|
| `anti_features` | "Anti-Features" section title |
| `compiled_for_debugging` | Anti-feature label |
| `contains_non_free_media` | Anti-feature label |
| `show_older_versions` | Button to expand versions list |
| `versions` | Section title |

These strings were removed from `values/strings.xml` and all 62 locale files.

### Drawable Resources Removed

| Resource | Description |
|----------|-------------|
| `ic_sort.xml` | Sort icon (was used for versions) |

## Files Changed

| File | Change |
|------|--------|
| `AppDetailAdapter.kt` | Removed VERSION ViewType, ViewHolder, Item, and all version-related binding code |
| `AppDetailScreen.kt` | Removed versions list and anti-features section |
| `ReleaseItem.kt` | Deleted |
| `values*/strings.xml` | Removed `anti_features`, `compiled_for_debugging`, `contains_non_free_media`, `show_older_versions`, `versions` from all locales |

## Impact

- Users can no longer see older versions of apps
- Users can no longer see anti-feature warnings before installation
- The app detail page is significantly simpler and faster to render
