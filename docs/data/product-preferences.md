# Product Preferences

Per-app preferences for update behavior, stored separately from the main database.

## Overview

**File:** `app/src/main/kotlin/com/looker/droidify/content/ProductPreferences.kt`

Product preferences allow users to ignore updates for specific apps, either entirely or for specific versions.

## ProductPreference Model

**File:** `model/ProductPreference.kt`

```kotlin
data class ProductPreference(
    val ignoreUpdates: Boolean,
    val ignoreVersionCode: Long
)
```

| Field | Description |
|-------|-------------|
| `ignoreUpdates` | If true, never show updates for this app |
| `ignoreVersionCode` | Ignore updates up to this version code |

## ProductPreferences Object

Singleton object managing per-app preferences.

### Initialization

```kotlin
fun init(context: Context, scope: CoroutineScope) {
    preferences = context.getSharedPreferences("product_preferences", Context.MODE_PRIVATE)
    Database.LockAdapter.putAll(
        preferences.all.keys.mapNotNull { packageName ->
            this[packageName].databaseVersionCode?.let { Pair(packageName, it) }
        }
    )
    scope.launch {
        subject.collect { (packageName, versionCode) ->
            if (versionCode != null) {
                Database.LockAdapter.put(Pair(packageName, versionCode))
            } else {
                Database.LockAdapter.delete(packageName)
            }
        }
    }
}
```

### Getting Preferences

```kotlin
operator fun get(packageName: String): ProductPreference {
    return if (preferences.contains(packageName)) {
        try {
            Json.factory.createParser(preferences.getString(packageName, "{}"))
                .use { it.parseDictionary { productPreference() } }
        } catch (e: Exception) {
            defaultProductPreference
        }
    } else {
        defaultProductPreference
    }
}
```

### Setting Preferences

```kotlin
operator fun set(packageName: String, productPreference: ProductPreference) {
    val oldProductPreference = this[packageName]
    preferences.edit().putString(
        packageName,
        ByteArrayOutputStream().apply {
            Json.factory.createGenerator(this)
                .use { it.writeDictionary(productPreference::serialize) }
        }.toByteArray().toString(Charset.defaultCharset())
    ).apply()

    if (oldProductPreference.ignoreUpdates != productPreference.ignoreUpdates ||
        oldProductPreference.ignoreVersionCode != productPreference.ignoreVersionCode) {
        mutableSubject.tryEmit(Pair(packageName, productPreference.databaseVersionCode))
    }
}
```

## Database Integration

### Lock Adapter

The `Database.LockAdapter` stores version locks for efficient SQL queries:

```kotlin
private val ProductPreference.databaseVersionCode: Long?
    get() = when {
        ignoreUpdates -> 0L           // 0 = ignore all
        ignoreVersionCode > 0L -> ignoreVersionCode  // specific version
        else -> null                  // no lock
    }
```

### Update Query Integration

In `Database.ProductAdapter.query()`:

```sql
COALESCE(lock.version_code, -1) NOT IN (0, product.version_code)
```

- If lock is `0`, all updates are ignored
- If lock equals product version, that specific update is ignored
- If lock is `-1` (no lock), updates are shown

## Usage Patterns

### Ignore All Updates

```kotlin
ProductPreferences[packageName] = ProductPreference(
    ignoreUpdates = true,
    ignoreVersionCode = 0L
)
```

### Ignore Specific Version

```kotlin
ProductPreferences[packageName] = ProductPreference(
    ignoreUpdates = false,
    ignoreVersionCode = 12345L  // Ignore updates until this version
)
```

### Clear Ignore

```kotlin
ProductPreferences[packageName] = ProductPreference(
    ignoreUpdates = false,
    ignoreVersionCode = 0L
)
```

## Storage Format

Stored in `product_preferences` SharedPreferences as JSON:

```json
{
    "ignoreUpdates": false,
    "ignoreVersionCode": 12345
}
```

## Flow Diagram

```
┌─────────────────┐     ┌─────────────────┐
│  User Action    │────▶│ ProductPref     │
│  (ignore app)   │     │ [packageName]=  │
└─────────────────┘     └────────┬────────┘
                                 │
         ┌───────────────────────┼───────────────────────┐
         ▼                       ▼                       ▼
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│ SharedPref      │     │ MutableShared   │     │ Database.Lock   │
│ (persistent)    │     │ Flow (emit)     │     │ Adapter.put     │
└─────────────────┘     └─────────────────┘     └─────────────────┘
                                                         │
                                                         ▼
                                                ┌─────────────────┐
                                                │ Product Query   │
                                                │ (filters locked │
                                                │ apps)           │
                                                └─────────────────┘
```

## Serialization

Uses Jackson for JSON serialization:

**File:** `utility/serialization/ProductPreferenceSerialization.kt`

```kotlin
fun ProductPreference.serialize(generator: JsonGenerator) {
    generator.writeBooleanField("ignoreUpdates", ignoreUpdates)
    generator.writeNumberField("ignoreVersionCode", ignoreVersionCode)
}

fun JsonParser.productPreference(): ProductPreference {
    var ignoreUpdates = false
    var ignoreVersionCode = 0L
    forEachKey { key ->
        when {
            key.boolean("ignoreUpdates") -> ignoreUpdates = valueAsBoolean
            key.number("ignoreVersionCode") -> ignoreVersionCode = valueAsLong
            else -> skipChildren()
        }
    }
    return ProductPreference(ignoreUpdates, ignoreVersionCode)
}
```

## Thread Safety

- SharedPreferences operations are synchronous with `apply()`
- Database updates are handled via coroutine scope
- Flow emissions are non-blocking with `tryEmit()`

## UI Integration

The app detail screen provides UI to:
1. Ignore all updates (toggle)
2. Ignore current version (skip this update)
3. Clear ignored status

These actions call `ProductPreferences[packageName] = ...` to update.
