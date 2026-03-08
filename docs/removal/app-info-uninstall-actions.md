# Removed: App Info and Uninstall Toolbar Actions

**Date:** 2026-03-07
**Branch:** main

## Overview

Removed the "Details" (App Info) and "Uninstall" action buttons from the app detail screen toolbar.

## What was removed

### Details (App Info) Action

| Component | File | Description |
|-----------|------|-------------|
| Action enum | `ui/appDetail/AppDetailAdapter.kt` | `DETAILS` action entry |
| Action enum | `ui/appDetail/AppDetailFragment.kt` | `DETAILS` action mapping |
| Action handler | `ui/appDetail/AppDetailFragment.kt` | Intent to `ACTION_APPLICATION_DETAILS_SETTINGS` |
| Drawable | `res/drawable/ic_tune.xml` | **Deleted** |

### Uninstall Action

| Component | File | Description |
|-----------|------|-------------|
| Action enum | `ui/appDetail/AppDetailAdapter.kt` | `UNINSTALL` action entry |
| Action enum | `ui/appDetail/AppDetailFragment.kt` | `UNINSTALL` action mapping |
| Action handler | `ui/appDetail/AppDetailFragment.kt` | Call to `viewModel.uninstallPackage()` |
| ViewModel method | `ui/appDetail/AppDetailViewModel.kt` | `uninstallPackage()` function **Deleted** |

### Strings removed

- `details` - removed from all 63 locale files
- `uninstall` - removed from all 63 locale files

## What was kept

- `ic_delete.xml` drawable - still used by `InstallNotification` for uninstall notifications
- `uninstalled_application` and `uninstalled_application_DESC` strings - used for notifications
- Installer uninstall functionality - still exists in installer implementations

## User impact

- Users can no longer open Android's app info screen directly from the app detail toolbar
- Users can no longer uninstall apps directly from the app detail toolbar
- To uninstall apps, users must use Android system settings or long-press the app icon

## Technical notes

- The `ACTION_SHOW_APP_INFO` intent handler in `MainActivity` was kept - this handles incoming requests from other apps to show app details
- The toolbar now only shows: Install, Update, Launch, and Cancel actions
