# Removed: Repository Management

**Date:** 2026-03-06
**Branch:** main

## Overview

Removed all repository management functionality. The app now uses a single hardcoded repository: `https://repo.dietdroid.com/fdroid/repo`

## Compose Screens Deleted

| Directory | Contents |
|-----------|----------|
| `compose/repoList/` | RepoListScreen, RepoListViewModel, navigation |
| `compose/repoDetail/` | RepoDetailScreen, RepoDetailViewModel, components, navigation |
| `compose/repoEdit/` | RepoEditScreen, RepoEditViewModel, navigation |

## Legacy UI Deleted

| Directory | Contents |
|-----------|----------|
| `ui/repository/` | RepositoriesFragment, RepositoryFragment, EditRepositoryFragment, RepositoriesAdapter, RepositoryViewModel |

## Navigation Removed

| File | Change |
|------|--------|
| `MainComposeActivity.kt` | Removed repo navigation imports and NavHost entries |
| `HomeScreen.kt` | Removed "Repos" button |
| `HomeNavigation.kt` | Removed `onNavigateToRepos` parameter |
| `AppListScreen.kt` | Removed repo icon from top bar |
| `AppListNavigation.kt` | Removed `onNavigateToRepos` parameter |
| `TabsFragment.kt` | Removed "Repositories" menu item |
| `MainActivity.kt` | Removed `navigateRepositories`, `navigateAddRepository`, `navigateRepository`, `navigateEditRepository` |

## Model Changes

| File | Change |
|------|--------|
| `Repository.kt` | `defaultRepositories` now contains only DietDroid repo |
| `Repository.kt` | Added `HARDCODED_REPO_ADDRESS` constant |
| `MessageDialog.kt` | Removed `DeleteRepositoryConfirm`, `CantEditSyncing` messages |

## Hardcoded Repository

```kotlin
const val HARDCODED_REPO_ADDRESS = "https://repo.dietdroid.com/fdroid/repo"

val defaultRepositories = listOf(
    defaultRepository(
        address = HARDCODED_REPO_ADDRESS,
        name = "DietDroid",
        description = "DietDroid F-Droid repository",
        enabled = true,
        fingerprint = "",
    ),
)
```

## Initialization

`MainComposeActivity.onCreate()` handles repo initialization:
- Checks if repos list is empty
- Inserts the hardcoded repo if needed
- Enables it automatically

## Files Modified

- `compose/MainComposeActivity.kt`
- `compose/home/HomeScreen.kt`
- `compose/home/navigation/HomeNavigation.kt`
- `compose/appList/AppListScreen.kt`
- `compose/appList/navigation/AppListNavigation.kt`
- `ui/tabsFragment/TabsFragment.kt`
- `ui/MessageDialog.kt`
- `ui/appDetail/AppDetailFragment.kt`
- `model/Repository.kt`
- `MainActivity.kt`

## Data Layer

`RepoRepository` and related DAOs remain but are simplified in usage - always operating on the single repo.
