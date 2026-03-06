# RecyclerView Widgets

Custom RecyclerView adapters and utilities for the legacy View-based UI.

## Overview

**Location:** `app/src/main/kotlin/com/looker/droidify/widget/`

| Widget | Purpose |
|--------|---------|
| `EnumRecyclerAdapter` | Base adapter with enum-based view types |
| `CursorRecyclerAdapter` | Adapter backed by database cursor |
| `StableRecyclerAdapter` | Adapter with stable IDs from string descriptors |
| `DividerItemDecoration` | Configurable list dividers |
| `FocusSearchView` | SearchView with controllable focus behavior |

## EnumRecyclerAdapter

Abstract base adapter that uses enum values for view types instead of integers.

**File:** `widget/EnumRecyclerAdapter.kt`

### Type Parameters

```kotlin
abstract class EnumRecyclerAdapter<VT : Enum<VT>, VH : RecyclerView.ViewHolder>
```

| Parameter | Description |
|-----------|-------------|
| `VT` | Enum type representing view types |
| `VH` | ViewHolder type |

### Abstract Members

```kotlin
abstract val viewTypeClass: Class<VT>
abstract fun getItemEnumViewType(position: Int): VT
abstract fun onCreateViewHolder(parent: ViewGroup, viewType: VT): VH
```

### Implementation Details

Uses a `SparseArray` to map ordinal values to enum names:

```kotlin
private val names = SparseArray<String>()

final override fun getItemViewType(position: Int): Int {
    val enum = getItemEnumViewType(position)
    names.put(enum.ordinal, enum.name)
    return enum.ordinal
}
```

### Usage

```kotlin
enum class ViewType { HEADER, ITEM, FOOTER }

class MyAdapter : EnumRecyclerAdapter<ViewType, MyViewHolder>() {
    override val viewTypeClass = ViewType::class.java

    override fun getItemEnumViewType(position: Int) = when {
        position == 0 -> ViewType.HEADER
        position == itemCount - 1 -> ViewType.FOOTER
        else -> ViewType.ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: ViewType) = when (viewType) {
        ViewType.HEADER -> HeaderViewHolder(...)
        ViewType.ITEM -> ItemViewHolder(...)
        ViewType.FOOTER -> FooterViewHolder(...)
    }
}
```

## CursorRecyclerAdapter

Extends `EnumRecyclerAdapter` to work with database cursors.

**File:** `widget/CursorRecyclerAdapter.kt`

### Properties

```kotlin
var cursor: Cursor? = null
    set(value) {
        field?.close()  // Close old cursor
        field = value
        rowIdIndex = value?.getColumnIndexOrThrow("_id") ?: 0
        notifyDataSetChanged()
    }
```

### Key Features

1. **Stable IDs** - Enabled by default, uses `_id` column
2. **Auto-close** - Closes previous cursor when new one is set
3. **Position helper** - `moveTo(position)` returns cursor at position

### Implementation

```kotlin
override fun getItemCount(): Int = cursor?.count ?: 0

override fun getItemId(position: Int): Long = moveTo(position).getLong(rowIdIndex)

fun moveTo(position: Int): Cursor {
    val cursor = cursor!!
    cursor.moveToPosition(position)
    return cursor
}
```

### Usage

```kotlin
class ProductAdapter : CursorRecyclerAdapter<ViewType, ProductViewHolder>() {
    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val cursor = moveTo(position)
        val name = cursor.getString(cursor.getColumnIndexOrThrow("name"))
        holder.bind(name)
    }
}

// Set data
adapter.cursor = database.query(...)
```

## StableRecyclerAdapter

Provides stable IDs using string descriptors instead of database IDs.

**File:** `widget/StableRecyclerAdapter.kt`

### Abstract Method

```kotlin
abstract fun getItemDescriptor(position: Int): String
```

### ID Generation

```kotlin
private var nextId = 1L
private val descriptorToId = mutableMapOf<String, Long>()

override fun getItemId(position: Int): Long {
    val descriptor = getItemDescriptor(position)
    return descriptorToId[descriptor] ?: run {
        val id = nextId++
        descriptorToId[descriptor] = id
        id
    }
}
```

### Usage

```kotlin
class CategoryAdapter : StableRecyclerAdapter<ViewType, CategoryViewHolder>() {
    private var categories = listOf<String>()

    override fun getItemDescriptor(position: Int) = categories[position]

    fun setData(newCategories: List<String>) {
        categories = newCategories
        notifyDataSetChanged()
    }
}
```

## DividerItemDecoration

Highly configurable divider decoration with RTL support.

**File:** `widget/DividerItemDecoration.kt`

### Extension Function

```kotlin
fun RecyclerView.addDivider(
    configure: (context: Context, position: Int, configuration: DividerConfiguration) -> Unit
)
```

### Configuration Interface

```kotlin
fun interface DividerConfiguration {
    fun set(needDivider: Boolean, toTop: Boolean, paddingStart: Int, paddingEnd: Int)
}
```

| Parameter | Description |
|-----------|-------------|
| `needDivider` | Whether to show divider after this item |
| `toTop` | Draw divider at top of next item instead of bottom |
| `paddingStart` | Start padding (respects RTL) |
| `paddingEnd` | End padding (respects RTL) |

### Usage

```kotlin
recyclerView.addDivider { context, position, config ->
    when {
        position == 0 -> config.set(false, false, 0, 0)  // No divider after header
        else -> config.set(true, false, 72.dp, 0)       // Divider with indent
    }
}
```

### Features

1. **RTL support** - Automatically swaps padding for RTL layouts
2. **Alpha animation** - Divider alpha matches item alpha
3. **Translation support** - Follows item translation during animations
4. **Position-based** - Configure per-position divider behavior

## FocusSearchView

Custom SearchView with controllable focus and keyboard behavior.

**File:** `widget/FocusSearchView.kt`

### Properties

```kotlin
var allowFocus = true
```

### Behavior

1. **Back press handling** - Clears focus on back key:

```kotlin
override fun dispatchKeyEventPreIme(event: KeyEvent): Boolean {
    return if (hasFocus() && event.keyCode == KeyEvent.KEYCODE_BACK) {
        if (event.action == KeyEvent.ACTION_UP) {
            clearFocus()
        }
        true
    } else {
        super.dispatchKeyEventPreIme(event)
    }
}
```

2. **Focus control** - Prevents auto-focus when `allowFocus` is false:

```kotlin
override fun setIconified(iconify: Boolean) {
    super.setIconified(iconify)
    if (!iconify && !allowFocus) {
        clearFocus()
    }
}
```

### Usage

```kotlin
<com.looker.droidify.widget.FocusSearchView
    android:id="@+id/search_view"
    android:layout_width="match_parent"
    android:layout_height="wrap_content" />
```

```kotlin
searchView.allowFocus = false  // Don't show keyboard when expanded
searchView.setIconified(false)  // Expand without focus
```

## Adapter Hierarchy

```
RecyclerView.Adapter
        │
        ▼
EnumRecyclerAdapter<VT, VH>
        │
        ├─────────────────────┐
        ▼                     ▼
CursorRecyclerAdapter    StableRecyclerAdapter
(database cursors)       (in-memory lists)
```

## Migration to Compose

These widgets are part of the legacy View-based UI. New features should use Compose equivalents:

| Legacy Widget | Compose Equivalent |
|--------------|-------------------|
| `EnumRecyclerAdapter` | `LazyColumn` with sealed classes |
| `CursorRecyclerAdapter` | `LazyColumn` with Flow |
| `DividerItemDecoration` | `HorizontalDivider` component |
| `FocusSearchView` | `SearchBar` with focus requester |
