# Graphics Utilities

Custom drawable wrappers for consistent icon rendering.

## Overview

**Location:** `app/src/main/kotlin/com/looker/droidify/graphics/`

| Class | Purpose |
|-------|---------|
| `DrawableWrapper` | Base wrapper for drawable delegation |
| `PaddingDrawable` | Adds padding and aspect ratio control |

## DrawableWrapper

An open base class that wraps another `Drawable` and delegates all operations to it. Useful for extending drawable behavior without modifying the original.

**File:** `graphics/DrawableWrapper.kt`

### Implementation

```kotlin
open class DrawableWrapper(val drawable: Drawable) : Drawable() {
    init {
        drawable.callback = object : Callback {
            override fun invalidateDrawable(who: Drawable) {
                callback?.invalidateDrawable(who)
            }
            override fun scheduleDrawable(who: Drawable, what: Runnable, `when`: Long) {
                callback?.scheduleDrawable(who, what, `when`)
            }
            override fun unscheduleDrawable(who: Drawable, what: Runnable) {
                callback?.unscheduleDrawable(who, what)
            }
        }
    }
}
```

### Key Features

1. **Callback forwarding** - Propagates invalidation callbacks to parent
2. **Bounds delegation** - Automatically sets wrapped drawable bounds
3. **Intrinsic size** - Returns wrapped drawable's dimensions
4. **Alpha/ColorFilter** - Delegates to wrapped drawable

### Delegated Methods

| Method | Behavior |
|--------|----------|
| `onBoundsChange()` | Sets bounds on wrapped drawable |
| `getIntrinsicWidth()` | Returns `drawable.intrinsicWidth` |
| `getIntrinsicHeight()` | Returns `drawable.intrinsicHeight` |
| `draw()` | Calls `drawable.draw(canvas)` |
| `setAlpha()` | Sets `drawable.alpha` |
| `setColorFilter()` | Sets `drawable.colorFilter` |
| `getOpacity()` | Returns `drawable.opacity` |

### Usage

```kotlin
class TintedDrawable(drawable: Drawable, tint: Int) : DrawableWrapper(drawable) {
    init {
        drawable.setTint(tint)
    }
}
```

## PaddingDrawable

Extends `DrawableWrapper` to add configurable padding around a drawable with aspect ratio control.

**File:** `graphics/PaddingDrawable.kt`

### Constructor

```kotlin
class PaddingDrawable(
    drawable: Drawable,
    private val horizontalFactor: Float,
    private val aspectRatio: Float = 16f / 9f
) : DrawableWrapper(drawable)
```

| Parameter | Description |
|-----------|-------------|
| `drawable` | The drawable to wrap |
| `horizontalFactor` | Scale factor for width (e.g., 1.5 = 50% wider) |
| `aspectRatio` | Height ratio relative to width (default: 16:9) |

### Size Calculation

```kotlin
override fun getIntrinsicWidth(): Int =
    (horizontalFactor * super.getIntrinsicWidth()).roundToInt()

override fun getIntrinsicHeight(): Int =
    ((horizontalFactor * aspectRatio) * super.getIntrinsicHeight()).roundToInt()
```

### Bounds Calculation

Centers the wrapped drawable within the padded bounds:

```kotlin
override fun onBoundsChange(bounds: Rect) {
    val width = (bounds.width() / horizontalFactor).roundToInt()
    val height = (bounds.height() / (horizontalFactor * aspectRatio)).roundToInt()
    val left = (bounds.width() - width) / 2
    val top = (bounds.height() - height) / 2
    drawable.setBounds(
        bounds.left + left,
        bounds.top + top,
        bounds.left + left + width,
        bounds.top + top + height
    )
}
```

### Visual Explanation

```
┌─────────────────────────────────────┐
│           PaddingDrawable           │
│   ┌───────────────────────────┐     │
│   │                           │     │
│   │     Wrapped Drawable      │     │
│   │     (centered)            │     │
│   │                           │     │
│   └───────────────────────────┘     │
│                                     │
└─────────────────────────────────────┘
```

### Usage Example

```kotlin
// Create a padded version of an app icon for screenshots
val appIcon = packageManager.getApplicationIcon(packageName)
val paddedIcon = PaddingDrawable(
    drawable = appIcon,
    horizontalFactor = 1.2f,  // 20% wider
    aspectRatio = 1f          // Square
)
imageView.setImageDrawable(paddedIcon)
```

## Use Cases

### App Icons in Lists

Consistent spacing around app icons regardless of source icon padding:

```kotlin
val icon = PaddingDrawable(originalIcon, 1.1f, 1f)
```

### Screenshot Thumbnails

Maintaining aspect ratio for video placeholders:

```kotlin
val placeholder = PaddingDrawable(videoIcon, 1.0f, 16f / 9f)
```

## Comparison with Android Drawables

| Feature | InsetDrawable | PaddingDrawable |
|---------|---------------|-----------------|
| Fixed insets | Yes | No |
| Aspect ratio | No | Yes |
| Dynamic scaling | No | Yes |
| Centering | Manual | Automatic |

## Integration

These drawables are used internally for:
- Screenshot display with proper aspect ratios
- Icon rendering in product lists
- Video placeholder images
