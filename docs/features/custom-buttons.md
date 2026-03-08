# Custom Buttons (REMOVED)

> **Removed:** This feature has been removed from the app. The documentation below is kept for historical reference.

Custom buttons allowed users to add quick-action links on app detail pages, enabling one-tap access to privacy reports, alternative stores, or any custom URL.

## Overview

**Key Files:**
- `datastore/model/CustomButton.kt` - Button data model
- `datastore/CustomButtonRepository.kt` - Button storage and management
- `compose/appDetail/components/CustomButtonsRow.kt` - UI component
- `compose/settings/components/CustomButtonsSettingItem.kt` - Settings editor
- `compose/settings/components/CustomButtonEditor.kt` - Button creation dialog

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    Custom Buttons                        │
│                                                          │
│  ┌─────────────────────────────────────────────────────┐│
│  │              CustomButtonRepository                  ││
│  │                                                      ││
│  │  - Store buttons in JSON file                       ││
│  │  - CRUD operations                                  ││
│  │  - Import/export                                    ││
│  └─────────────────────────────────────────────────────┘│
│                         │                                │
│                         ▼                                │
│  ┌─────────────────────────────────────────────────────┐│
│  │                CustomButtonsRow                      ││
│  │          (App Detail Screen)                         ││
│  │                                                      ││
│  │  [Exodus] [Plexus] [Play Store] [Custom...]         ││
│  │                                                      ││
│  │  Resolves URL template with app context             ││
│  └─────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────┘
```

## CustomButton Model

```kotlin
@Serializable
data class CustomButton(
    val id: String,
    val label: String,
    val urlTemplate: String,
    val icon: CustomButtonIcon = CustomButtonIcon.LINK,
)
```

### URL Template Variables

| Variable | Description | Example |
|----------|-------------|---------|
| `{{package_name}}` | App package name | `com.example.app` |
| `{{app_name}}` | App display name (URL encoded) | `My%20App` |
| `{{author_name}}` | Author name (URL encoded) | `John%20Doe` |

### URL Resolution

```kotlin
fun resolveUrl(
    packageName: String,
    appName: String,
    authorName: String,
): String {
    val encodedAppName = Uri.encode(appName)
    val encodedAuthorName = Uri.encode(authorName)

    return urlTemplate
        .replace("{{package_name}}", packageName)
        .replace("{{ package_name }}", packageName)  // Flexible spacing
        .replace("{{app_name}}", encodedAppName)
        .replace("{{ app_name }}", encodedAppName)
        .replace("{{author_name}}", encodedAuthorName)
        .replace("{{ author_name }}", encodedAuthorName)
}
```

### Button Icons

```kotlin
@Serializable
enum class CustomButtonIcon {
    LINK,       // Generic link
    SEARCH,     // Search/lookup
    PRIVACY,    // Privacy-related
    STORE,      // App store
    CODE,       // Source code
    DOWNLOAD,   // Download
    SHARE,      // Share
    BUG,        // Bug report
    INFO,       // Information
    EMAIL,      // Contact
    PERSON,     // Author
    HISTORY,    // Changelog
    SETTINGS,   // Settings
    TEXT_ONLY,  // Show first 2 letters instead
}
```

## Built-in Templates

```kotlin
val TEMPLATES = listOf(
    CustomButton(
        id = "exodus",
        label = "Exodus Privacy",
        urlTemplate = "https://reports.exodus-privacy.eu.org/en/reports/{{package_name}}/latest/",
        icon = CustomButtonIcon.PRIVACY,
    ),
    CustomButton(
        id = "plexus",
        label = "Plexus",
        urlTemplate = "https://plexus.techlore.tech/apps?q={{app_name}}",
        icon = CustomButtonIcon.PRIVACY,
    ),
    CustomButton(
        id = "playstore",
        label = "Play Store",
        urlTemplate = "https://play.google.com/store/apps/details?id={{package_name}}",
        icon = CustomButtonIcon.STORE,
    ),
    CustomButton(
        id = "alternativeto",
        label = "AlternativeTo",
        urlTemplate = "https://alternativeto.net/browse/search/?q={{app_name}}",
        icon = CustomButtonIcon.SEARCH,
    ),
    CustomButton(
        id = "apkmirror",
        label = "APKMirror",
        urlTemplate = "https://www.apkmirror.com/?post_type=app_release&searchtype=apk&s={{package_name}}",
        icon = CustomButtonIcon.SEARCH,
    ),
)
```

## CustomButtonRepository

Manages button storage in `custom_buttons.json`:

```kotlin
@Singleton
class CustomButtonRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    private val _buttons = MutableStateFlow<List<CustomButton>>(emptyList())
    val buttons: Flow<List<CustomButton>>

    // CRUD operations
    suspend fun getButtons(): List<CustomButton>
    suspend fun addButton(button: CustomButton)
    suspend fun updateButton(button: CustomButton)
    suspend fun removeButton(buttonId: String)
    suspend fun reorderButtons(buttons: List<CustomButton>)

    // Import/Export
    suspend fun importFromUri(uri: Uri): Result<Int>
    suspend fun exportToUri(uri: Uri): Result<Unit>
    suspend fun getButtonsJson(): String
}
```

### Thread Safety

```kotlin
private val mutex = Mutex()

suspend fun addButton(button: CustomButton) {
    mutex.withLock {
        ensureLoadedInternal()
        val newButtons = _buttons.value + button
        saveToFile(newButtons)
        _buttons.value = newButtons
    }
}
```

## UI Components

### CustomButtonsRow

Horizontal scrollable row of buttons:

```kotlin
@Composable
fun CustomButtonsRow(
    buttons: List<CustomButton>,
    packageName: String,
    appName: String,
    authorName: String?,
    onButtonClick: (url: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(buttons, key = { it.id }) { button ->
            CustomButtonItem(
                button = button,
                onClick = {
                    val resolvedUrl = button.resolveUrl(packageName, appName, authorName ?: "")
                    onButtonClick(resolvedUrl)
                },
            )
        }
    }
}
```

### CustomButtonItem

Individual button display:

```kotlin
@Composable
private fun CustomButtonItem(
    button: CustomButton,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(72.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            if (button.icon == CustomButtonIcon.TEXT_ONLY) {
                Text(
                    text = button.label.take(2).uppercase(),
                    style = MaterialTheme.typography.titleSmall,
                )
            } else {
                Icon(
                    painter = painterResource(button.icon.toDrawableRes()),
                    contentDescription = button.label,
                )
            }
        }

        Text(
            text = button.label,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 2,
        )
    }
}
```

## Settings Integration

Buttons are configured in Settings:

```kotlin
CustomButtonsSettingItem(
    buttons = customButtons,
    onAddButton = viewModel::addCustomButton,
    onEditButton = viewModel::updateCustomButton,
    onDeleteButton = viewModel::removeCustomButton,
    onReorder = viewModel::reorderButtons,
    onImport = viewModel::importButtons,
    onExport = viewModel::exportButtons,
)
```

## Import/Export

### Export Format

```json
[
    {
        "id": "exodus",
        "label": "Exodus Privacy",
        "urlTemplate": "https://reports.exodus-privacy.eu.org/en/reports/{{package_name}}/latest/",
        "icon": "PRIVACY"
    },
    {
        "id": "custom1",
        "label": "My Service",
        "urlTemplate": "https://example.com/app/{{package_name}}",
        "icon": "LINK"
    }
]
```

### Import Logic

```kotlin
suspend fun importFromUri(uri: Uri): Result<Int> {
    val inputStream = context.contentResolver.openInputStream(uri)
    val importedButtons = json.decodeFromString<List<CustomButton>>(jsonString)

    // Only import buttons with new IDs
    val existingIds = _buttons.value.map { it.id }.toSet()
    val newButtons = importedButtons.filter { it.id !in existingIds }
    val mergedButtons = _buttons.value + newButtons

    saveToFile(mergedButtons)
    return Result.success(newButtons.size)
}
```

## Adding a Custom Button

### Via Settings UI

1. Go to Settings > Custom Buttons
2. Tap "Add Button"
3. Fill in:
   - Label (e.g., "Check Source")
   - URL Template (e.g., `https://github.com/search?q={{package_name}}`)
   - Icon (select from predefined icons)
4. Save

### Programmatically

```kotlin
@Inject lateinit var customButtonRepository: CustomButtonRepository

val myButton = CustomButton(
    id = UUID.randomUUID().toString(),
    label = "My Service",
    urlTemplate = "https://example.com/app/{{package_name}}",
    icon = CustomButtonIcon.CODE,
)

customButtonRepository.addButton(myButton)
```

## Usage in App Detail

```kotlin
@Composable
fun AppDetailScreen(
    viewModel: AppDetailViewModel = hiltViewModel(),
) {
    val customButtons by viewModel.customButtons.collectAsStateWithLifecycle()
    val app by viewModel.app.collectAsStateWithLifecycle()

    CustomButtonsRow(
        buttons = customButtons,
        packageName = app.packageName,
        appName = app.name,
        authorName = app.authorName,
        onButtonClick = { url ->
            uriHandler.openUri(url)
        },
    )
}
```

## Removed

| Feature | Removal Doc |
|---------|-------------|
| `Custom Buttons` | [custom-buttons-and-settings.md](../removal/custom-buttons-and-settings.md) |
