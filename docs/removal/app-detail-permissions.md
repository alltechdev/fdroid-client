# App Detail Permissions Section Removal

Removed the permissions section from the app detail page.

## Date
2026-03-07

## Branch
main

## Reason

Simplify the app detail UI by removing the permissions section. Users can view permissions in system settings after installation.

## Components Removed

### UI Components

| Component | File | Description |
|-----------|------|-------------|
| `ViewType.PERMISSIONS` | `AppDetailAdapter.kt` | ViewType enum value |
| `PermissionsViewHolder` | `AppDetailAdapter.kt` | ViewHolder for permissions items |
| `Item.PermissionsItem` | `AppDetailAdapter.kt` | Item class for permissions |
| `SectionType.PERMISSIONS` | `AppDetailAdapter.kt` | Section type enum value |
| `ExpandType.PERMISSIONS` | `AppDetailAdapter.kt` | Expand type enum value |
| `DotSpan` | `AppDetailAdapter.kt` | Custom span for word-wrap in permission names |
| `onPermissionsClick` | `AppDetailAdapter.Callbacks` | Callback interface method |

### Dialog Components

| Component | File | Description |
|-----------|------|-------------|
| `Message.Permissions` | `MessageDialog.kt` | Permissions dialog message type |
| Permissions dialog case | `MessageDialog.kt` | Dialog display logic for permissions |

### Utility Classes

| File | Description |
|------|-------------|
| `PackageItemResolver.kt` | Permission label/description resolution utility |

### Resources

| Resource | Type | Description |
|----------|------|-------------|
| `ic_perm_device_information.xml` | Drawable | Permissions section icon |
| `permissions` | String | "Permissions" section title |
| `other` | String | "Other" category label |
| `no_description_available_DESC` | String | Fallback description text |

## Files Changed

| File | Change |
|------|--------|
| `AppDetailAdapter.kt` | Removed permissions ViewType, ViewHolder, Item, binding code, DotSpan class |
| `AppDetailFragment.kt` | Removed `onPermissionsClick` override |
| `MessageDialog.kt` | Removed `Message.Permissions` class and dialog case |
| `PackageItemResolver.kt` | Deleted (no longer used) |
| `drawable/ic_perm_device_information.xml` | Deleted |
| `values*/strings.xml` | Removed `permissions`, `other`, `no_description_available_DESC` strings from all locales |

## Imports Removed

| File | Imports |
|------|---------|
| `AppDetailAdapter.kt` | `PermissionGroupInfo`, `PermissionInfo`, `PackageItemResolver`, `ReplacementSpan` |
| `MessageDialog.kt` | `PackageItemResolver`, `nullIfEmpty` |
