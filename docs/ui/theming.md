# Theming

Droid-ify uses Material 3 theming with support for light/dark modes, dynamic colors (Material You), and contrast levels.

## Theme Files

| File | Purpose |
|------|---------|
| `compose/theme/Theme.kt` | Theme composable and color schemes |
| `compose/theme/Color.kt` | Color palette definitions |
| `compose/theme/Type.kt` | Typography definitions |

## DroidifyTheme

The main theme composable:

```kotlin
@Composable
fun DroidifyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
)
```

### Parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `darkTheme` | `Boolean` | System setting | Use dark color scheme |
| `dynamicColor` | `Boolean` | `false` | Use Material You colors |
| `content` | `@Composable` | - | Theme content |

### Usage

```kotlin
// In MainComposeActivity
setContent {
    DroidifyTheme {
        // App content
    }
}

// With user preferences
setContent {
    val settings by settingsRepository.data.collectAsStateWithLifecycle()

    DroidifyTheme(
        darkTheme = when (settings.theme) {
            Theme.LIGHT -> false
            Theme.DARK, Theme.AMOLED -> true
            Theme.SYSTEM, Theme.SYSTEM_BLACK -> isSystemInDarkTheme()
        },
        dynamicColor = settings.dynamicTheme,
    ) {
        // App content
    }
}
```

## Color Schemes

### Static Schemes

The app defines 6 color schemes:

| Scheme | Description |
|--------|-------------|
| `lightScheme` | Standard light theme |
| `darkScheme` | Standard dark theme |
| `mediumContrastLightColorScheme` | Light with enhanced contrast |
| `highContrastLightColorScheme` | Light with high contrast |
| `mediumContrastDarkColorScheme` | Dark with enhanced contrast |
| `highContrastDarkColorScheme` | Dark with high contrast |

### Dynamic Colors (Material You)

On Android 12+, dynamic colors extract from the user's wallpaper:

```kotlin
val colorScheme = when {
    dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context)
        else dynamicLightColorScheme(context)
    }
    darkTheme -> darkScheme
    else -> lightScheme
}
```

## Color Palette

The app uses a green primary color with orange tertiary:

### Light Theme

| Role | Color | Hex |
|------|-------|-----|
| Primary | Green | `#416835` |
| Secondary | Muted Green | `#54634D` |
| Tertiary | Orange | `#845416` |
| Background | Light Green-Gray | `#F8FAF0` |
| Surface | Light Green-Gray | `#F8FAF0` |
| Error | Red | `#BA1A1A` |

### Dark Theme

| Role | Color | Hex |
|------|-------|-----|
| Primary | Light Green | `#A6D394` |
| Secondary | Light Muted Green | `#BBCBB1` |
| Tertiary | Orange | `#FABA73` |
| Background | Dark Gray | `#11140F` |
| Surface | Dark Gray | `#11140F` |
| Error | Light Red | `#FFB4AB` |

## Using Theme Colors

### In Composables

```kotlin
@Composable
fun MyComponent() {
    val colorScheme = MaterialTheme.colorScheme

    Text(
        text = "Hello",
        color = colorScheme.onSurface,
    )

    Surface(
        color = colorScheme.surfaceContainer,
    ) {
        // Content
    }
}
```

### Common Color Roles

| Role | Usage |
|------|-------|
| `primary` | Primary brand color, buttons |
| `onPrimary` | Content on primary |
| `primaryContainer` | Less prominent primary surfaces |
| `secondary` | Secondary actions |
| `surface` | Card backgrounds |
| `surfaceContainer` | Elevated surfaces |
| `onSurface` | Text on surfaces |
| `onSurfaceVariant` | Secondary text |
| `outline` | Borders, dividers |
| `error` | Error states |

## Typography

Defined in `compose/theme/Type.kt`:

```kotlin
val Typography = Typography(
    displayLarge = TextStyle(...),
    displayMedium = TextStyle(...),
    // ...
    bodyLarge = TextStyle(...),
    bodyMedium = TextStyle(...),
    bodySmall = TextStyle(...),
    // ...
    labelLarge = TextStyle(...),
    labelMedium = TextStyle(...),
    labelSmall = TextStyle(...),
)
```

### Usage

```kotlin
Text(
    text = "Title",
    style = MaterialTheme.typography.headlineMedium,
)

Text(
    text = "Description",
    style = MaterialTheme.typography.bodyMedium,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
)
```

## Theme Options (User Settings)

Users can configure theming via settings:

### Theme Enum

```kotlin
enum class Theme {
    SYSTEM,        // Follow system
    SYSTEM_BLACK,  // Follow system, use AMOLED black in dark
    LIGHT,         // Always light
    DARK,          // Always dark
    AMOLED,        // Always AMOLED black
}
```

### Dynamic Theme Toggle

```kotlin
// In settings
SwitchSettingItem(
    title = stringResource(R.string.material_you),
    description = stringResource(R.string.material_you_desc),
    checked = settings.dynamicTheme,
    onCheckedChange = viewModel::setDynamicTheme,
)
```

Only shown on Android 12+ (`SdkCheck.isSnowCake`).

## Adding Custom Colors

### Step 1: Add to Color.kt

```kotlin
// Add new color values
val myCustomColorLight = Color(0xFF123456)
val myCustomColorDark = Color(0xFF654321)
```

### Step 2: Add to Color Schemes

For custom colors not in Material 3, use `CompositionLocal`:

```kotlin
val LocalMyCustomColor = staticCompositionLocalOf { Color.Unspecified }

@Composable
fun DroidifyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val myCustomColor = if (darkTheme) myCustomColorDark else myCustomColorLight

    CompositionLocalProvider(
        LocalMyCustomColor provides myCustomColor,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content,
        )
    }
}
```

### Step 3: Use in Composables

```kotlin
@Composable
fun MyComponent() {
    val customColor = LocalMyCustomColor.current
    // Use customColor
}
```

## Shapes

Material 3 shapes are available via `MaterialTheme.shapes`:

```kotlin
Box(
    modifier = Modifier
        .clip(MaterialTheme.shapes.small)   // 4dp rounded
        .clip(MaterialTheme.shapes.medium)  // 8dp rounded
        .clip(MaterialTheme.shapes.large)   // 16dp rounded
)
```

## Preview Support

Include theme in previews:

```kotlin
@Preview(name = "Light")
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MyComponentPreview() {
    DroidifyTheme {
        MyComponent()
    }
}
```
