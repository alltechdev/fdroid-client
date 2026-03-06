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
    HEADER,           // Icon, name, summary
    SWITCH,           // Ignore updates toggle
    SCREENSHOTS,      // Horizontal screenshot carousel
    SECTION,          // Section header
    EXPAND,           // Show more button
    TEXT,             // Description text
    LINK,             // URL link
    PERMISSIONS,      // Permission list
    RELEASE,          // Release version item
    EMPTY,            // Empty state
    CUSTOM_BUTTONS,   // User-defined action buttons
}
```

### Callbacks Interface

```kotlin
interface Callbacks {
    fun onActionClick(action: Action)
    fun onFavouriteToggled()
    fun onPreferenceChanged(preference: ProductPreference)
    fun onPermissionsClick(permissions: List<String>, group: String)
    fun onScreenshotClick(index: Int, screenshots: List<Product.Screenshot>)
    fun onReleaseClick(release: Release)
}
```

### Actions

```kotlin
sealed interface Action {
    data object Install : Action
    data object Update : Action
    data object Uninstall : Action
    data object Cancel : Action
    data object Launch : Action
    data object Details : Action
    data object Share : Action
    data class Custom(val button: CustomButton) : Action
}
```

### Section Display

Limited to first 5 releases by default:
```kotlin
companion object {
    private const val MAX_RELEASE_ITEMS = 5
}
```

## RepositoriesAdapter

**File:** `ui/repository/RepositoriesAdapter.kt`

Lists repositories with enable/disable switch.

```kotlin
class RepositoriesAdapter(
    private val onSwitch: (Repository, Boolean) -> Unit,
    private val onClick: (Repository) -> Unit,
) : RecyclerView.Adapter<RepositoriesAdapter.ViewHolder>()
```

## ScreenshotsAdapter

**File:** `ui/appDetail/ScreenshotsAdapter.kt`

Horizontal carousel for app screenshots.

```kotlin
class ScreenshotsAdapter(
    private val onClick: (Int) -> Unit,
) : RecyclerView.Adapter<ScreenshotsAdapter.ViewHolder>()
```

## CustomButtonsAdapter

**File:** `ui/appDetail/CustomButtonsAdapter.kt`

User-defined action buttons in app detail.

```kotlin
class CustomButtonsAdapter(
    private val onClick: (CustomButton) -> Unit,
) : RecyclerView.Adapter<CustomButtonsAdapter.ViewHolder>()
```

## FavouriteFragmentAdapter

**File:** `ui/favourites/FavouriteFragmentAdapter.kt`

Lists bookmarked apps.

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
