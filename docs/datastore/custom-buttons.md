# Custom Buttons (REMOVED)

> **Removed:** This feature has been removed from the app. The documentation below is kept for historical reference.

User-defined action buttons for quick access to frequent actions.

## Overview

**File:** `datastore/CustomButtonRepository.kt`

Manages user-created buttons stored as JSON file.

## Repository

```kotlin
@Singleton
class CustomButtonRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    companion object {
        private const val FILE_NAME = "custom_buttons.json"
    }

    private val mutex = Mutex()
    private var isLoaded = false
    private val _buttons = MutableStateFlow<List<CustomButton>>(emptyList())

    val buttons: Flow<List<CustomButton>> = flow {
        ensureLoaded()
        emitAll(_buttons)
    }
}
```

## Data Model

**File:** `datastore/model/CustomButton.kt`

```kotlin
@Serializable
data class CustomButton(
    val id: String,           // Unique identifier
    val label: String,        // Display text
    val action: String,       // Action to perform
    val iconRes: Int? = null  // Optional icon resource
)
```

## Operations

### Add Button
```kotlin
suspend fun addButton(button: CustomButton) {
    mutex.withLock {
        ensureLoadedInternal()
        val newButtons = _buttons.value + button
        saveToFile(newButtons)
        _buttons.value = newButtons
    }
}
```

### Update Button
```kotlin
suspend fun updateButton(button: CustomButton) {
    mutex.withLock {
        ensureLoadedInternal()
        val newButtons = _buttons.value.map {
            if (it.id == button.id) button else it
        }
        saveToFile(newButtons)
        _buttons.value = newButtons
    }
}
```

### Remove Button
```kotlin
suspend fun removeButton(buttonId: String) {
    mutex.withLock {
        ensureLoadedInternal()
        val newButtons = _buttons.value.filter { it.id != buttonId }
        saveToFile(newButtons)
        _buttons.value = newButtons
    }
}
```

### Reorder Buttons
```kotlin
suspend fun reorderButtons(buttons: List<CustomButton>) {
    mutex.withLock {
        saveToFile(buttons)
        _buttons.value = buttons
    }
}
```

## Import/Export

### Import from URI
```kotlin
suspend fun importFromUri(uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
    runCatching {
        val jsonString = context.contentResolver
            .openInputStream(uri)
            .bufferedReader()
            .readText()
        val importedButtons = json.decodeFromString<List<CustomButton>>(jsonString)

        mutex.withLock {
            // Merge avoiding duplicates
            val existingIds = _buttons.value.map { it.id }.toSet()
            val newButtons = importedButtons.filter { it.id !in existingIds }
            val mergedButtons = _buttons.value + newButtons
            saveToFile(mergedButtons)
            _buttons.value = mergedButtons
            newButtons.size  // Return count of imported
        }
    }
}
```

### Export to URI
```kotlin
suspend fun exportToUri(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
    runCatching {
        ensureLoaded()
        val jsonString = json.encodeToString(_buttons.value)
        context.contentResolver.openOutputStream(uri)?.use {
            it.write(jsonString.toByteArray())
        }
    }
}
```

## Lazy Loading

```kotlin
private suspend fun ensureLoaded() {
    if (!isLoaded) {
        mutex.withLock {
            ensureLoadedInternal()
        }
    }
}

private suspend fun ensureLoadedInternal() {
    if (!isLoaded) {
        _buttons.value = loadFromFile()
        isLoaded = true
    }
}

private suspend fun loadFromFile(): List<CustomButton> = withContext(Dispatchers.IO) {
    val file = File(context.filesDir, FILE_NAME)
    if (!file.exists()) return@withContext emptyList()
    try {
        json.decodeFromString(file.readText())
    } catch (e: Exception) {
        emptyList()
    }
}
```

## Thread Safety

- All mutations protected by `Mutex`
- File I/O on `Dispatchers.IO`
- `MutableStateFlow` for thread-safe state
- Lazy loading prevents blocking on init

## JSON Format

```json
[
  {
    "id": "btn_1",
    "label": "Sync All",
    "action": "sync_all",
    "iconRes": null
  },
  {
    "id": "btn_2",
    "label": "Check Updates",
    "action": "check_updates",
    "iconRes": 2131165284
  }
]
```

## Removed

| Feature | Removal Doc |
|---------|-------------|
| Package: `com.looker.droidify` | [package-rename.md](../changes/package-rename.md) |
| `CustomButtonRepository` | [custom-buttons-and-settings.md](../removal/custom-buttons-and-settings.md) |
