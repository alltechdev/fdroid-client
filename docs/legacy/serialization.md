# JSON Serialization

Jackson-based serialization for legacy models.

## Overview

**Location:** `app/src/main/kotlin/com/looker/droidify/utility/serialization/`

Uses Jackson Streaming API for efficient JSON serialization/deserialization.

## Files

| File | Model |
|------|-------|
| `ProductSerialization.kt` | Product |
| `ReleaseSerialization.kt` | Release |
| `RepositorySerialization.kt` | Repository |
| `ProductItemSerialization.kt` | ProductItem |
| `ProductPreferenceSerialization.kt` | ProductPreference |

## JSON Factory

**File:** `utility/common/extension/Json.kt`

```kotlin
object Json {
    val factory: JsonFactory = JsonFactory()
}
```

## Serialization Pattern

### Serialize (Write)

```kotlin
fun Product.serialize(generator: JsonGenerator) {
    generator.writeNumberField("repositoryId", repositoryId)
    generator.writeStringField("packageName", packageName)
    generator.writeStringField("name", name)
    // ... more fields

    generator.writeArray("categories") {
        categories.forEach(::writeString)
    }

    generator.writeArray("releases") {
        releases.forEach { writeDictionary { it.serialize(this) } }
    }
}
```

### Deserialize (Read)

```kotlin
fun JsonParser.product(): Product {
    var repositoryId = 0L
    var packageName = ""
    var name = ""
    // ... more variables

    forEachKey { key ->
        when {
            key.string("repositoryId") -> repositoryId = valueAsLong
            key.string("packageName") -> packageName = valueAsString
            key.string("name") -> name = valueAsString
            key.array("categories") -> categories = collectNotNullStrings()
            key.array("releases") -> releases = collectNotNull(START_OBJECT) { release() }
            else -> skipChildren()
        }
    }

    return Product(repositoryId, packageName, name, ...)
}
```

## Extension Functions

### Dictionary Writing

```kotlin
fun JsonGenerator.writeDictionary(callback: JsonGenerator.() -> Unit) {
    writeStartObject()
    callback()
    writeEndObject()
}
```

### Array Writing

```kotlin
fun JsonGenerator.writeArray(fieldName: String, callback: JsonGenerator.() -> Unit) {
    writeArrayFieldStart(fieldName)
    callback()
    writeEndArray()
}
```

### Key Matching

```kotlin
fun String.string(name: String): Boolean = this == name && /* token check */
fun String.number(name: String): Boolean = this == name && /* token check */
fun String.boolean(name: String): Boolean = this == name && /* token check */
fun String.array(name: String): Boolean = this == name && /* token check */
```

### Collection Parsing

```kotlin
fun JsonParser.forEachKey(callback: (String) -> Unit) {
    while (nextToken() != JsonToken.END_OBJECT) {
        callback(currentName)
    }
}

fun <T> JsonParser.collectNotNull(token: JsonToken, callback: () -> T?): List<T> {
    return buildList {
        while (nextToken() != JsonToken.END_ARRAY) {
            if (currentToken == token) {
                callback()?.let(::add)
            }
        }
    }
}

fun JsonParser.collectNotNullStrings(): List<String> {
    return collectNotNull(JsonToken.VALUE_STRING) { valueAsString }
}
```

## Product Serialization

**File:** `ProductSerialization.kt`

### Fields

```kotlin
private const val REPOSITORYID = "repositoryId"
private const val PACKAGENAME = "packageName"
private const val NAME = "name"
private const val SUMMARY = "summary"
private const val DESCRIPTION = "description"
// ... 30+ fields
```

### Donate Serialization

```kotlin
when (donate) {
    is Product.Donate.Regular -> {
        writeStringField("type", "")
        writeStringField("url", donate.url)
    }
    is Product.Donate.Bitcoin -> {
        writeStringField("type", "bitcoin")
        writeStringField("address", donate.address)
    }
    // ... other types
}
```

### Screenshot Serialization

```kotlin
screenshots.forEach {
    writeDictionary {
        writeStringField("locale", it.locale)
        writeStringField("type", it.type.jsonName)
        writeStringField("path", it.path)
    }
}
```

## Release Serialization

**File:** `ReleaseSerialization.kt`

### Incompatibility Handling

```kotlin
generator.writeArray("incompatibilities") {
    incompatibilities.forEach {
        writeDictionary {
            when (it) {
                is Incompatibility.MinSdk -> writeStringField("type", "minSdk")
                is Incompatibility.MaxSdk -> writeStringField("type", "maxSdk")
                is Incompatibility.Platform -> writeStringField("type", "platform")
                is Incompatibility.Feature -> {
                    writeStringField("type", "feature")
                    writeStringField("feature", it.feature)
                }
            }
        }
    }
}
```

## Repository Serialization

**File:** `RepositorySerialization.kt`

```kotlin
fun Repository.serialize(generator: JsonGenerator) {
    generator.writeStringField("address", address)
    generator.writeArray("mirrors") { mirrors.forEach(::writeString) }
    generator.writeStringField("name", name)
    generator.writeStringField("description", description)
    generator.writeNumberField("version", version)
    generator.writeStringField("fingerprint", fingerprint)
    generator.writeStringField("lastModified", lastModified)
    generator.writeStringField("entityTag", entityTag)
    generator.writeNumberField("updated", updated)
    generator.writeNumberField("timestamp", timestamp)
    generator.writeStringField("authentication", authentication)
}
```

## Usage in Database

### Storing to Database

```kotlin
val data = Database.jsonGenerate { product.serialize(it) }
contentValues.put(Schema.Product.ROW_DATA, data)
```

### Reading from Database

```kotlin
val data = cursor.getBlob(cursor.getColumnIndexOrThrow(Schema.Product.ROW_DATA))
val product = data.jsonParse { it.product() }
```

## Performance Considerations

1. **Streaming API** - No DOM tree, low memory
2. **Field Constants** - Avoids string allocation
3. **Lazy Parsing** - Only parse needed fields
4. **Skip Unknown** - `skipChildren()` for forward compatibility

## Migration Notes

This serialization system is used for:
- Legacy SQLite storage (BLOB columns)

New Room-based storage uses native SQLite types and doesn't need this serialization.

## Removed

| Feature | Removal Doc |
|---------|-------------|
| Export/Import (backup files) | [proxy-and-backup.md](../removal/proxy-and-backup.md) |
