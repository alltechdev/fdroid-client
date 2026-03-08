# UI Fragments

Legacy View-based fragment architecture.

## Overview

**Directory:** `ui/`

The app uses a fragment-based navigation system with ViewBinding.

## ScreenFragment (Base)

**File:** `ui/ScreenFragment.kt`

Base class for all fragments providing toolbar access.

```kotlin
open class ScreenFragment : Fragment() {
    private var _fragmentBinding: FragmentBinding? = null
    val fragmentBinding get() = _fragmentBinding!!
    val toolbar: MaterialToolbar get() = fragmentBinding.toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _fragmentBinding = FragmentBinding.inflate(layoutInflater)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = fragmentBinding.root

    override fun onDestroyView() {
        super.onDestroyView()
        _fragmentBinding = null
    }
}
```

## TabsFragment

**File:** `ui/tabsFragment/TabsFragment.kt`

Main home screen with ViewPager2 tabs.

```kotlin
@AndroidEntryPoint
class TabsFragment : ScreenFragment() {

    enum class BackAction {
        CollapseSearchView,
        None,
    }

    private val viewModel: TabsViewModel by viewModels()

    // ViewPager with tabs: Explore, Installed, Updates
    private var viewPager: ViewPager2? = null
}
```

### Features
- ViewPager2 with 3 tabs (Explore, Installed, Updates)
- Search integration with FocusSearchView
- Sort order menu
- Sync service binding

### State Preservation

```kotlin
companion object {
    private const val STATE_SEARCH_FOCUSED = "searchFocused"
    private const val STATE_SEARCH_QUERY = "searchQuery"
}
```

## AppListFragment

**File:** `ui/appList/AppListFragment.kt`

Displays list of apps.

```kotlin
@AndroidEntryPoint
class AppListFragment : ScreenFragment() {
    private val viewModel: AppListViewModel by viewModels()
    private var adapter: AppListAdapter? = null

    enum class Source(
        val titleResId: Int,
        val updateAll: Boolean,
    ) {
        AVAILABLE(R.string.available, false),
        INSTALLED(R.string.installed, false),
        UPDATES(R.string.updates, true),
    }
}
```

### Usage

```kotlin
fun newInstance(source: Source): AppListFragment
```

## AppDetailFragment

**File:** `ui/appDetail/AppDetailFragment.kt`

Shows app details with install options.

```kotlin
@AndroidEntryPoint
class AppDetailFragment : ScreenFragment() {
    private val viewModel: AppDetailViewModel by viewModels()
    private var adapter: AppDetailAdapter? = null

    companion object {
        private const val EXTRA_PACKAGE_NAME = "packageName"
        private const val EXTRA_REPO_ADDRESS = "repoAddress"
    }
}

// Constructor helper
fun AppDetailFragment(packageName: String, repoAddress: String? = null)
```

### Components
- Header with icon, name, summary
- Screenshots carousel
- Version list with install buttons
- Anti-features display
- Links section
- Custom buttons row

## FavouritesFragment

**File:** `ui/favourites/FavouritesFragment.kt`

Shows bookmarked apps.

```kotlin
@AndroidEntryPoint
class FavouritesFragment : ScreenFragment() {
    private val viewModel: FavouritesViewModel by viewModels()
    private var adapter: FavouriteFragmentAdapter? = null
}
```

## SettingsFragment

**File:** `ui/settings/SettingsFragment.kt`

App preferences screen.

```kotlin
class SettingsFragment : ScreenFragment() {
    companion object {
        fun newInstance(): SettingsFragment = SettingsFragment()
    }
}
```

## MessageDialog

**File:** `ui/MessageDialog.kt`

Simple dialog for messages.

```kotlin
class MessageDialog : DialogFragment() {
    companion object {
        fun newInstance(message: String): MessageDialog
    }
}
```

## Fragment Navigation

All navigation goes through `MainActivity`:

```kotlin
// In fragment
mainActivity?.navigateProduct(packageName)
mainActivity?.navigatePreferences()
mainActivity?.navigateFavourites()
```

## ViewModel Integration

All fragments use Hilt-injected ViewModels:

```kotlin
@AndroidEntryPoint
class MyFragment : ScreenFragment() {
    private val viewModel: MyViewModel by viewModels()
}
```

## Lifecycle Flow

```
┌─────────────────────────────────────────────────────────────┐
│                    MainActivity                              │
│  - Fragment stack management                                │
│  - Navigation methods                                       │
└─────────────────────────┬───────────────────────────────────┘
                          │ pushFragment()/popFragment()
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                    ScreenFragment                            │
│  - Base class with toolbar binding                          │
│  - ViewBinding setup                                        │
└─────────────────────────┬───────────────────────────────────┘
                          │ extends
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                    Concrete Fragments                        │
│  TabsFragment, AppListFragment, AppDetailFragment, etc.     │
└─────────────────────────────────────────────────────────────┘
```

## Removed

| Fragment | Removal Doc |
|----------|-------------|
| `RepositoriesFragment` | [repository-management.md](../removal/repository-management.md) |
| `RepositoryFragment` | [repository-management.md](../removal/repository-management.md) |
| `EditRepositoryFragment` | [repository-management.md](../removal/repository-management.md) |
| `RepositoriesAdapter` | [repository-management.md](../removal/repository-management.md) |
| `RepositoryViewModel` | [repository-management.md](../removal/repository-management.md) |
| Category/Section Filtering | [category-filtering.md](../removal/category-filtering.md) |
| Sort order menu in `TabsFragment` | [sort-order-ui.md](../removal/sort-order-ui.md) |
| `Action.DETAILS` in `AppDetailFragment` | [app-info-uninstall-actions.md](../removal/app-info-uninstall-actions.md) |
| `Action.UNINSTALL` in `AppDetailFragment` | [app-info-uninstall-actions.md](../removal/app-info-uninstall-actions.md) |
