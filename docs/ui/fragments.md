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
        ProductAll,
        CollapseSearchView,
        HideSections,
        None,
    }

    private val viewModel: TabsViewModel by viewModels()

    // ViewPager with tabs: Explore, Installed, Updates
    private var viewPager: ViewPager2? = null

    // Category/section selection
    private var sectionsAdapter: SectionsAdapter? = null
    private var showSections = false
}
```

### Features
- ViewPager2 with 3 tabs (Explore, Installed, Updates)
- Search integration with FocusSearchView
- Sort order menu
- Category/section filtering
- Sync service binding

### State Preservation

```kotlin
companion object {
    private const val STATE_SEARCH_FOCUSED = "searchFocused"
    private const val STATE_SEARCH_QUERY = "searchQuery"
    private const val STATE_SHOW_SECTIONS = "showSections"
}
```

## AppListFragment

**File:** `ui/appList/AppListFragment.kt`

Displays list of apps with filtering.

```kotlin
@AndroidEntryPoint
class AppListFragment : ScreenFragment() {
    private val viewModel: AppListViewModel by viewModels()
    private var adapter: AppListAdapter? = null

    enum class Source {
        AVAILABLE, INSTALLED, UPDATES
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

## RepositoriesFragment

**File:** `ui/repository/RepositoriesFragment.kt`

Lists all configured repositories.

```kotlin
@AndroidEntryPoint
class RepositoriesFragment : ScreenFragment() {
    private var adapter: RepositoriesAdapter? = null
}
```

## RepositoryFragment

**File:** `ui/repository/RepositoryFragment.kt`

Shows repository details.

```kotlin
@AndroidEntryPoint
class RepositoryFragment : ScreenFragment() {
    private val viewModel: RepositoryViewModel by viewModels()

    companion object {
        private const val EXTRA_REPOSITORY_ID = "repositoryId"
    }
}

fun RepositoryFragment(repositoryId: Long)
```

## EditRepositoryFragment

**File:** `ui/repository/EditRepositoryFragment.kt`

Add or edit repository.

```kotlin
class EditRepositoryFragment : ScreenFragment() {
    companion object {
        private const val EXTRA_REPOSITORY_ID = "repositoryId"
        private const val EXTRA_REPO_ADDRESS = "repoAddress"
    }
}

fun EditRepositoryFragment(repositoryId: Long?, repoAddress: String?)
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
mainActivity?.navigateRepositories()
mainActivity?.navigatePreferences()
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
