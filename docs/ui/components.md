# UI Components

Reusable Compose components used throughout the Droid-ify app.

## Overview

**Location:** `compose/components/`

| File | Description |
|------|-------------|
| `BackButton.kt` | Standard back navigation button |
| `ButtonColors.kt` | Custom button color schemes |

## BackButton

Standard back navigation button for top app bars:

```kotlin
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(width = (24 + 12).dp, height = 40.dp),
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}
```

### Usage

```kotlin
@Composable
fun MyScreen(onBackClick: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Screen") },
                navigationIcon = { BackButton(onClick = onBackClick) },
            )
        },
    ) { padding ->
        // Content
    }
}
```

### Features

- Uses `Icons.AutoMirrored.Filled.ArrowBack` for RTL support
- Fixed size for consistent touch target
- Standard Material 3 styling

## Button Colors

Custom button color extensions:

```kotlin
@Composable
fun ButtonDefaults.errorButtonColors() = buttonColors(
    containerColor = MaterialTheme.colorScheme.error,
    contentColor = MaterialTheme.colorScheme.onError,
)
```

### Usage

```kotlin
@Composable
fun DeleteButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.errorButtonColors(),
    ) {
        Text("Delete")
    }
}
```

## Common Patterns

### Screen with Back Button

```kotlin
@Composable
fun DetailScreen(
    title: String,
    onBackClick: () -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = { BackButton(onClick = onBackClick) },
            )
        },
    ) { padding ->
        content(padding)
    }
}
```

### Action Buttons Row

```kotlin
@Composable
fun ActionRow(
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedButton(onClick = onCancel) {
            Text("Cancel")
        }
        Button(onClick = onConfirm) {
            Text("Confirm")
        }
    }
}
```

### Destructive Action Button

```kotlin
@Composable
fun DestructiveButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.errorButtonColors(),
        modifier = modifier,
    ) {
        Text(text)
    }
}
```

## Feature-Specific Components

### AppDetail Components

Located in `compose/appDetail/components/`:

| Component | Purpose |
|-----------|---------|
| `CustomButtonsRow` | Quick action buttons |
| `PackageItem` | Version list item |

### Settings Components

Located in `compose/settings/components/`:

| Component | Purpose |
|-----------|---------|
| `SettingHeader` | Section header |
| `SwitchSettingItem` | Boolean toggle |
| `DropdownSettingItem` | Enum selection |
| `TextInputSettingItem` | Text input |
| `SelectionSettingItem` | Radio selection |
| `ActionSettingItem` | Clickable action |
| `CustomButtonsSettingItem` | Custom buttons editor |
| `WarningBanner` | Warning message |

## Creating New Components

### Guidelines

1. **Keep it simple** - One component, one purpose
2. **Accept Modifier** - Always accept `modifier` parameter
3. **Use Material 3** - Follow Material Design guidelines
4. **Preview support** - Include `@Preview` functions

### Template

```kotlin
@Composable
fun MyComponent(
    // Required parameters first
    title: String,
    onClick: () -> Unit,
    // Optional parameters with defaults
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    // Implementation
}

@Preview
@Composable
private fun MyComponentPreview() {
    AtdTheme {
        MyComponent(
            title = "Preview",
            onClick = {},
        )
    }
}
```

## Best Practices

1. **State hoisting** - Keep components stateless when possible
2. **Slots** - Use slots for flexible composition
3. **Consistent naming** - Use descriptive names (e.g., `AppListItem`, not `Item`)
4. **Documentation** - Add KDoc for public components

## Removed

| Component | Removal Doc |
|-----------|-------------|
| Package: `com.looker.droidify` | [package-rename.md](../changes/package-rename.md) |
| `CustomButtonsRow` | [custom-buttons-and-settings.md](../removal/custom-buttons-and-settings.md) |
| `CustomButtonsSettingItem` | [custom-buttons-and-settings.md](../removal/custom-buttons-and-settings.md) |
| `TextInputSettingItem` | [proxy-and-backup.md](../removal/proxy-and-backup.md) |

## Changes

| Change | Change Doc |
|--------|------------|
| Class renames (AtdStore, AtdDatabase, AtdTheme) | [package-rename.md](../changes/package-rename.md) |
| App branding Droid-ify → ATD Store | [app-branding.md](../changes/app-branding.md) |
