# Legacy Fragments

This document describes the legacy View-based UI that is being migrated to Jetpack Compose.

## Overview

The legacy UI uses Android Fragments with ViewBinding and RecyclerView. It's accessed via `MainActivity` (as opposed to `MainComposeActivity` for the Compose UI).

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    MainActivity                          │
│                                                          │
│  ┌────────────────────────────────────────────────────┐ │
│  │                  TabsFragment                       │ │
│  │  ┌────────────┐ ┌────────────┐ ┌────────────────┐  │ │
│  │  │  Explore   │ │  Installed │ │   Favourites   │  │ │
│  │  │   (tab)    │ │    (tab)   │ │     (tab)      │  │ │
│  │  └─────┬──────┘ └──────┬─────┘ └───────┬────────┘  │ │
│  │        │               │               │           │ │
│  │        ▼               ▼               ▼           │ │
│  │  ┌────────────────────────────────────────────┐    │ │
│  │  │              AppListFragment                │    │ │
│  │  │           (different queries)               │    │ │
│  │  └────────────────────────────────────────────┘    │ │
│  └────────────────────────────────────────────────────┘ │
│                          │                               │
│                          ▼                               │
│  ┌────────────────────────────────────────────────────┐ │
│  │               AppDetailFragment                     │ │
│  │               RepositoryFragment                    │ │
│  │               SettingsFragment                      │ │
│  └────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────┘
```

## Fragment Overview

| Fragment | Purpose | Compose Equivalent |
|----------|---------|-------------------|
| `TabsFragment` | Main container with tabs | `AppListScreen` |
| `AppListFragment` | List of apps | `AppListScreen` |
| `AppDetailFragment` | App details & install | `AppDetailScreen` |
| `FavouritesFragment` | Favorite apps list | `AppListScreen` (filtered) |
| `RepositoriesFragment` | List of repos | `RepoListScreen` |
| `RepositoryFragment` | Single repo details | `RepoDetailScreen` |
| `EditRepositoryFragment` | Add/edit repo | `RepoEditScreen` |
| `SettingsFragment` | App settings | `SettingsScreen` |

## Key Files

### Fragments

| File | Description |
|------|-------------|
| `ui/ScreenFragment.kt` | Base fragment class |
| `ui/tabsFragment/TabsFragment.kt` | Main tabbed interface |
| `ui/appList/AppListFragment.kt` | App list display |
| `ui/appDetail/AppDetailFragment.kt` | App details |
| `ui/favourites/FavouritesFragment.kt` | Favorites list |
| `ui/repository/RepositoriesFragment.kt` | Repo list |
| `ui/repository/RepositoryFragment.kt` | Repo details |
| `ui/repository/EditRepositoryFragment.kt` | Repo editor |
| `ui/settings/SettingsFragment.kt` | Settings |

### Adapters

| File | Description |
|------|-------------|
| `ui/appList/AppListAdapter.kt` | RecyclerView adapter for apps |
| `ui/appDetail/AppDetailAdapter.kt` | App detail sections |
| `ui/appDetail/ScreenshotsAdapter.kt` | Screenshots carousel |
| `ui/appDetail/CustomButtonsAdapter.kt` | Custom action buttons |
| `ui/repository/RepositoriesAdapter.kt` | Repo list items |
| `ui/favourites/FavouriteFragmentAdapter.kt` | Favorites list |

### ViewModels

| File | Description |
|------|-------------|
| `ui/tabsFragment/TabsViewModel.kt` | Tabs state |
| `ui/appList/AppListViewModel.kt` | App list state |
| `ui/appDetail/AppDetailViewModel.kt` | App detail state |
| `ui/favourites/FavouritesViewModel.kt` | Favorites state |
| `ui/repository/RepositoryViewModel.kt` | Repo state |

## Fragment Structure

### Base Class

```kotlin
abstract class ScreenFragment : Fragment() {
    // Common fragment functionality
}
```

### Typical Fragment

```kotlin
@AndroidEntryPoint
class AppListFragment : Fragment() {

    private val viewModel: AppListViewModel by viewModels()
    private var _binding: FragmentAppListBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentAppListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = AppListAdapter { app ->
            // Navigate to detail
        }
        binding.recyclerView.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.apps.collect { apps ->
                adapter.submitList(apps)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
```

## RecyclerView Adapters

Adapters use `ListAdapter` with `DiffUtil`:

```kotlin
class AppListAdapter(
    private val onClick: (ProductItem) -> Unit,
) : ListAdapter<ProductItem, AppListAdapter.ViewHolder>(DiffCallback) {

    class ViewHolder(
        private val binding: ItemAppBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ProductItem) {
            binding.name.text = item.name
            binding.summary.text = item.summary
            // ...
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAppBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
        holder.itemView.setOnClickListener { onClick(getItem(position)) }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<ProductItem>() {
        override fun areItemsTheSame(old: ProductItem, new: ProductItem) =
            old.packageName == new.packageName

        override fun areContentsTheSame(old: ProductItem, new: ProductItem) =
            old == new
    }
}
```

## Navigation

Legacy navigation uses Fragment transactions:

```kotlin
// Navigate to detail
parentFragmentManager.commit {
    replace(R.id.fragment_container, AppDetailFragment.newInstance(packageName))
    addToBackStack(null)
}

// Navigate back
parentFragmentManager.popBackStack()
```

## Migration Status

| Feature | Legacy | Compose | Status |
|---------|--------|---------|--------|
| App List | `AppListFragment` | `AppListScreen` | Migrated |
| App Detail | `AppDetailFragment` | `AppDetailScreen` | Migrated |
| Repo List | `RepositoriesFragment` | `RepoListScreen` | Migrated |
| Repo Detail | `RepositoryFragment` | `RepoDetailScreen` | Migrated |
| Repo Edit | `EditRepositoryFragment` | `RepoEditScreen` | Migrated |
| Settings | `SettingsFragment` | `SettingsScreen` | Migrated |
| Favorites | `FavouritesFragment` | Integrated in AppList | Migrated |
| Tabs | `TabsFragment` | - | Removed (single list) |

## Migrating a Fragment to Compose

### Step 1: Identify State

Extract state management from Fragment to ViewModel if not already done.

### Step 2: Create Compose Screen

```kotlin
@Composable
fun MyFeatureScreen(
    viewModel: MyFeatureViewModel = hiltViewModel(),
    onNavigate: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // Compose UI
}
```

### Step 3: Create Navigation

```kotlin
@Serializable
object MyFeature

fun NavGraphBuilder.myFeature(onNavigate: () -> Unit) {
    composable<MyFeature> {
        MyFeatureScreen(onNavigate = onNavigate)
    }
}
```

### Step 4: Add to NavHost

Add the new route to `MainComposeActivity`.

### Step 5: Remove Legacy Code

After verification, remove:
- Fragment class
- Layout XML
- Adapter (if RecyclerView-based)
- Fragment-specific ViewBinding

## Coexistence

During migration, both UIs coexist:
- `MainActivity` - Legacy Fragment-based UI
- `MainComposeActivity` - New Compose UI

The app can be configured to launch either activity. Eventually, `MainActivity` and all legacy UI code will be removed.
