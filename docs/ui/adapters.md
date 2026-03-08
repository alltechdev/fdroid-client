# RecyclerView Adapters

Custom adapters for displaying app and repository lists.

## Overview

**Directory:** `ui/*/`

The app uses custom RecyclerView adapters with various base classes.

## AppListAdapter

**File:** `ui/appList/AppListAdapter.kt`

Displays list of apps with icon, name, and status.

```kotlin
class AppListAdapter(
    private val source: AppListFragment.Source,
    private val onClick: (packageName: String) -> Unit,
) : CursorRecyclerAdapter<AppListAdapter.ViewType, RecyclerView.ViewHolder>()
```

### View Types

```kotlin
enum class ViewType { PRODUCT, LOADING, EMPTY }
```

### ProductViewHolder

```kotlin
private inner class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val name = itemView.findViewById<TextView>(R.id.name)
    val status = itemView.findViewById<TextView>(R.id.status)
    val summary = itemView.findViewById<TextView>(R.id.summary)
    val icon = itemView.findViewById<ShapeableImageView>(R.id.icon)

    init {
        itemView.setOnClickListener {
            onClick(getPackageName(absoluteAdapterPosition))
        }
    }
}
```

### Loading State

```kotlin
private class LoadingViewHolder(context: Context) : RecyclerView.ViewHolder(FrameLayout(context)) {
    init {
        // Centered ProgressBar
        addView(ProgressBar(context), LayoutParams(...).apply { gravity = Gravity.CENTER })
    }
}
```

## AppDetailAdapter

**File:** `ui/appDetail/AppDetailAdapter.kt`

Complex adapter for app detail screen with multiple section types.

```kotlin
class AppDetailAdapter(private val callbacks: Callbacks) :
    StableRecyclerAdapter<AppDetailAdapter.ViewType, RecyclerView.ViewHolder>()
```

### View Types

```kotlin
enum class ViewType {
    APP_INFO,         // Icon, name, summary
    DOWNLOAD_STATUS,  // Download progress
    INSTALL_BUTTON,   // Install/update button
    SCREENSHOT,       // Horizontal screenshot carousel
    SECTION,          // Section header
    EXPAND,           // Show more button
    TEXT,             // Description text
    LINK,             // URL link (donate links)
    RELEASE,          // Release version item
    EMPTY,            // Empty state
}
```

### Callbacks Interface

```kotlin
interface Callbacks {
    fun onActionClick(action: Action)
    fun onPreferenceChanged(preference: ProductPreference)
    fun onScreenshotClick(position: Int)
    fun onReleaseClick(release: Release)
    fun onRequestAddRepository(address: String)
    fun onUriClick(uri: Uri, shouldConfirm: Boolean): Boolean
}
```

### Actions

```kotlin
enum class Action(@StringRes val titleResId: Int, @DrawableRes val iconResId: Int) {
    INSTALL(R.string.install, R.drawable.ic_download),
    UPDATE(R.string.update, R.drawable.ic_download),
    LAUNCH(R.string.launch, R.drawable.ic_launch),
    DETAILS(R.string.details, R.drawable.ic_tune),
    UNINSTALL(R.string.uninstall, R.drawable.ic_delete),
    CANCEL(R.string.cancel, R.drawable.ic_cancel),
}
```

### Section Display

Limited to first 5 releases by default:
```kotlin
companion object {
    private const val MAX_RELEASE_ITEMS = 5
}
```

## ScreenshotsAdapter

**File:** `ui/appDetail/ScreenshotsAdapter.kt`

Horizontal carousel for app screenshots.

```kotlin
class ScreenshotsAdapter(
    private val onClick: (Int) -> Unit,
) : RecyclerView.Adapter<ScreenshotsAdapter.ViewHolder>()
```

## Base Classes

### CursorRecyclerAdapter

Database cursor-backed adapter (see `docs/ui/widgets.md`).

### StableRecyclerAdapter

Adapter with stable IDs from string descriptors (see `docs/ui/widgets.md`).

## Icon Loading

Uses Coil for async image loading:

```kotlin
icon.load(iconUrl) {
    placeholder(R.drawable.ic_placeholder)
    error(R.drawable.ic_placeholder)
    authentication(repository.authentication)
}
```

## View Type Patterns

### Multi-Type Adapter

```kotlin
override fun getItemViewType(position: Int): Int {
    return when (items[position]) {
        is HeaderItem -> ViewType.HEADER.ordinal
        is TextItem -> ViewType.TEXT.ordinal
        is ReleaseItem -> ViewType.RELEASE.ordinal
        // ...
    }
}

override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    return when (ViewType.values()[viewType]) {
        ViewType.HEADER -> HeaderViewHolder(parent.inflate(R.layout.item_header))
        ViewType.TEXT -> TextViewHolder(parent.inflate(R.layout.item_text))
        // ...
    }
}
```

### Click Handling

```kotlin
init {
    itemView.setOnClickListener {
        val position = bindingAdapterPosition
        if (position != RecyclerView.NO_POSITION) {
            onClick(items[position])
        }
    }
}
```

## Performance

- Uses `DiffUtil` for efficient updates
- Stable IDs enable item animations
- ViewHolder recycling minimizes allocations
- Coil caches loaded images

## Removed

| Feature | Removal Doc |
|---------|-------------|
| `RepositoriesAdapter` | [repository-management.md](../removal/repository-management.md) |
| `CustomButtonsAdapter` | [custom-buttons-and-settings.md](../removal/custom-buttons-and-settings.md) |
| `FavouriteFragmentAdapter` | [favourites.md](../removal/favourites.md) |
| `ViewType.SWITCH` | [version-settings.md](../removal/version-settings.md) |
| `ViewType.PERMISSIONS` | [app-detail-permissions.md](../removal/app-detail-permissions.md) |
| `ViewType.CUSTOM_BUTTONS` | [custom-buttons-and-settings.md](../removal/custom-buttons-and-settings.md) |
| `onPermissionsClick` | [app-detail-permissions.md](../removal/app-detail-permissions.md) |
| `onFavouriteToggled` | [favourites.md](../removal/favourites.md) |
| `Action.Custom` | [custom-buttons-and-settings.md](../removal/custom-buttons-and-settings.md) |
| `Action.SHARE` | [share-source-actions.md](../removal/share-source-actions.md) |
| `Action.SOURCE` | [share-source-actions.md](../removal/share-source-actions.md) |
| `ViewType.VERSION` | [versions-antifeatures.md](../removal/versions-antifeatures.md) |
| `ViewType.EMPTY` | [versions-antifeatures.md](../removal/versions-antifeatures.md) |
| `VersionViewHolder` | [versions-antifeatures.md](../removal/versions-antifeatures.md) |
| `Item.VersionItem` | [versions-antifeatures.md](../removal/versions-antifeatures.md) |
| `Action.DETAILS` | [app-info-uninstall-actions.md](../removal/app-info-uninstall-actions.md) |
| `Action.UNINSTALL` | [app-info-uninstall-actions.md](../removal/app-info-uninstall-actions.md) |
