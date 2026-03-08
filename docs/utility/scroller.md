# Scroller Utilities

Smooth scroll and fast scroller support for RecyclerView.

## Overview

**File:** `app/src/main/kotlin/com/atd/store/utility/common/Scroller.kt`

Provides utilities for scroll behavior in lists.

## Fast Scroller

Custom fast scroller implementation for RecyclerView.

### Setup

```kotlin
fun RecyclerView.setupFastScroller(
    thumbDrawable: Drawable,
    trackDrawable: Drawable? = null,
    popupStyle: (TextView) -> Unit = {}
) {
    val fastScroller = FastScroller(context).apply {
        setThumbDrawable(thumbDrawable)
        trackDrawable?.let { setTrackDrawable(it) }
        setPopupStyle(popupStyle)
    }
    addItemDecoration(fastScroller)
    addOnItemTouchListener(fastScroller)
}
```

### Popup Text Provider

For showing section headers during scroll:

```kotlin
interface PopupTextProvider {
    fun getPopupText(position: Int): String
}

class ProductAdapter : RecyclerView.Adapter<...>(), PopupTextProvider {
    override fun getPopupText(position: Int): String {
        return products[position].name.firstOrNull()?.uppercaseChar()?.toString() ?: "#"
    }
}
```

## Smooth Scroll

### Scroll to Position with Offset

```kotlin
fun RecyclerView.smoothScrollToPositionWithOffset(
    position: Int,
    offset: Int = 0,
    snapMode: Int = LinearSmoothScroller.SNAP_TO_START
) {
    val smoothScroller = object : LinearSmoothScroller(context) {
        override fun getVerticalSnapPreference() = snapMode
        override fun calculateDyToMakeVisible(view: View, snapPreference: Int): Int {
            return super.calculateDyToMakeVisible(view, snapPreference) - offset
        }
    }
    smoothScroller.targetPosition = position
    layoutManager?.startSmoothScroll(smoothScroller)
}
```

### Scroll Speed Control

```kotlin
fun RecyclerView.smoothScrollWithSpeed(
    position: Int,
    millisecondsPerInch: Float = 100f
) {
    val smoothScroller = object : LinearSmoothScroller(context) {
        override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics): Float {
            return millisecondsPerInch / displayMetrics.densityDpi
        }
    }
    smoothScroller.targetPosition = position
    layoutManager?.startSmoothScroll(smoothScroller)
}
```

## Compose Scroll Utilities

### Remember Scroll State

```kotlin
@Composable
fun rememberLazyListState(
    initialFirstVisibleItemIndex: Int = 0,
    initialFirstVisibleItemScrollOffset: Int = 0
): LazyListState {
    return rememberSaveable(saver = LazyListState.Saver) {
        LazyListState(initialFirstVisibleItemIndex, initialFirstVisibleItemScrollOffset)
    }
}
```

### Scroll to Top

```kotlin
@Composable
fun ScrollToTopFab(
    listState: LazyListState,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val showButton by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 3 }
    }

    AnimatedVisibility(
        visible = showButton,
        modifier = modifier
    ) {
        FloatingActionButton(
            onClick = {
                scope.launch {
                    listState.animateScrollToItem(0)
                }
            }
        ) {
            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Scroll to top")
        }
    }
}
```

### Infinite Scroll

```kotlin
@Composable
fun <T> InfiniteScrollList(
    items: List<T>,
    loadMore: () -> Unit,
    threshold: Int = 5,
    content: @Composable (T) -> Unit
) {
    val listState = rememberLazyListState()

    LaunchedEffect(listState) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisibleItem >= totalItems - threshold
        }
            .distinctUntilChanged()
            .filter { it }
            .collect { loadMore() }
    }

    LazyColumn(state = listState) {
        items(items) { item ->
            content(item)
        }
    }
}
```

## Nested Scroll Handling

### Collapsing Toolbar Support

```kotlin
val nestedScrollConnection = remember {
    object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            // Handle pre-scroll for toolbar collapse
            return Offset.Zero
        }

        override fun onPostScroll(
            consumed: Offset,
            available: Offset,
            source: NestedScrollSource
        ): Offset {
            // Handle post-scroll
            return Offset.Zero
        }
    }
}

Box(
    modifier = Modifier.nestedScroll(nestedScrollConnection)
) {
    LazyColumn { ... }
}
```

## Scroll State Persistence

### Save/Restore Position

```kotlin
class ScrollStateManager {
    private val positions = mutableMapOf<String, Int>()

    fun savePosition(key: String, position: Int) {
        positions[key] = position
    }

    fun restorePosition(key: String): Int {
        return positions[key] ?: 0
    }
}
```

### In ViewModel

```kotlin
@HiltViewModel
class ListViewModel @Inject constructor() : ViewModel() {
    private val scrollPosition = mutableStateOf(0)

    fun saveScrollPosition(position: Int) {
        scrollPosition.value = position
    }

    fun getScrollPosition(): Int = scrollPosition.value
}
```

## Performance Tips

1. **Use `key` parameter** - For stable item IDs in LazyColumn
2. **Avoid recomposition** - Use `derivedStateOf` for scroll calculations
3. **Debounce load more** - Prevent excessive API calls
4. **Remember scroller** - Don't recreate smooth scrollers
