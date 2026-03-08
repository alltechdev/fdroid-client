# Favourites Feature Removal

Removed the ability to favourite/bookmark apps.

## Date
2026-03-06

## Branch
main

## Reason

Simplify app by removing favourites functionality. Users can rely on the installed apps list instead.

## What Was Removed

### Model
- `favouriteApps` field from `Settings` data class

### Settings
- `toggleFavourites()` method from `SettingsRepository` interface
- `toggleFavourites()` implementation from `PreferenceSettingsRepository`
- `FAVOURITE_APPS` preference key

### UI - Legacy
- `FavouritesFragment` - deleted entirely
- `FavouritesViewModel` - deleted entirely
- `FavouriteFragmentAdapter` - deleted entirely
- Favourites menu item from `TabsFragment` three-dot menu
- `navigateFavourites()` from `MainActivity`
- Favourite button from `AppDetailAdapter`
- `onFavouriteClicked()` callback from `AppDetailAdapter.Callbacks`
- `isFavourite` parameter from `setProducts()`
- `setFavouriteState()` from `AppDetailViewModel`
- `isFavourite` field from `AppDetailUiState`

### UI - Compose
- Favourites filter chip from `AppListScreen`
- `favouritesOnly` and `favouriteApps` state from `AppListViewModel`
- `toggleFavouritesOnly()` from `AppListViewModel`
- Favourite toggle button from `AppDetailScreen` HeaderSection

### Data
- `addToFavourite()` from `AppRepository`

### Resources
- `ic_favourite.xml` drawable
- `ic_favourite_checked.xml` drawable
- `favourite_icon.xml` drawable
- `favourite_icon_color.xml` color selector
- `FavouriteTheme` style
- Favourite button from `app_detail_header.xml` layout

### Strings Removed
- `favourites`

### Tests
- Removed `toggleFavourites` test from `PreferenceSettingsRepositoryTest`

## Behavior Change

| Before | After |
|--------|-------|
| Users could favourite apps | No favourites feature |
| Favourites accessible from menu | Menu item removed |
| Filter by favourites in app list | Filter removed |
| Favourite button on app detail | Button removed |

## Files Changed

| File | Change |
|------|--------|
| `datastore/Settings.kt` | Removed `favouriteApps` field |
| `datastore/SettingsRepository.kt` | Removed `toggleFavourites()` |
| `datastore/PreferenceSettingsRepository.kt` | Removed implementation |
| `ui/favourites/` | Deleted directory |
| `MainActivity.kt` | Removed `navigateFavourites()` |
| `ui/tabsFragment/TabsFragment.kt` | Removed menu item |
| `ui/appDetail/AppDetailAdapter.kt` | Removed favourite button |
| `ui/appDetail/AppDetailFragment.kt` | Removed callback |
| `ui/appDetail/AppDetailViewModel.kt` | Removed state |
| `compose/appList/AppListScreen.kt` | Removed filter chip |
| `compose/appList/AppListViewModel.kt` | Removed favourites state |
| `compose/appDetail/AppDetailScreen.kt` | Removed toggle button |
| `data/AppRepository.kt` | Removed `addToFavourite()` |
| `res/layout/app_detail_header.xml` | Removed button |
| `res/drawable/` | Deleted favourite icons |
| `res/color/favourite_icon_color.xml` | Deleted |
| `res/values/styles.xml` | Removed `FavouriteTheme` |
| `res/values*/strings.xml` | Removed `favourites` string |
