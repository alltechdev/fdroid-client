# Legacy Models

Data models used by the legacy database and sync systems.

## Overview

**Location:** `app/src/main/kotlin/com/looker/droidify/model/`

These models are used with the legacy SQLite database. New code should use Room entities.

## Product

Represents an app from a repository.

**File:** `model/Product.kt`

```kotlin
data class Product(
    var repositoryId: Long,
    val packageName: String,
    val name: String,
    val summary: String,
    var description: String,
    val whatsNew: String,
    val icon: String,
    val metadataIcon: String,
    val author: Author,
    val source: String,
    val changelog: String,
    val web: String,
    val tracker: String,
    val added: Long,
    val updated: Long,
    val suggestedVersionCode: Long,
    val categories: List<String>,
    val antiFeatures: List<String>,
    val licenses: List<String>,
    val donates: List<Donate>,
    val screenshots: List<Screenshot>,
    val releases: List<Release>,
)
```

### Nested Classes

#### Author

```kotlin
data class Author(
    val name: String,
    val email: String,
    val web: String
)
```

#### Donate

```kotlin
sealed class Donate {
    data class Regular(val url: String) : Donate()
    data class Bitcoin(val address: String) : Donate()
    data class Litecoin(val address: String) : Donate()
    data class Liberapay(val id: String) : Donate()
    data class OpenCollective(val id: String) : Donate()
}
```

#### Screenshot

```kotlin
class Screenshot(
    val locale: String,
    val type: Type,
    val path: String
) {
    enum class Type(val jsonName: String) {
        VIDEO("video"),
        PHONE("phone"),
        SMALL_TABLET("smallTablet"),
        LARGE_TABLET("largeTablet"),
        WEAR("wear"),
        TV("tv")
    }

    fun url(context: Context, repository: Repository, packageName: String): Any
}
```

### Computed Properties

```kotlin
val selectedReleases: List<Release>  // Releases matching device
val displayRelease: Release?         // First selected or first available
val version: String                  // Display version
val versionCode: Long               // Numeric version
val compatible: Boolean             // Has compatible release
val signatures: List<String>        // Signing certificates

fun canUpdate(installedItem: InstalledItem?): Boolean
fun item(): ProductItem  // Convert to list item
```

### Finding Suggested Version

```kotlin
fun List<Pair<Product, Repository>>.findSuggested(
    installedItem: InstalledItem?,
): Pair<Product, Repository>? = maxWithOrNull(
    compareBy(
        { (product, _) ->
            val isSameSignature = installedItem?.signature in product.signatures
            product.compatible && isSameSignature
        },
        { (product, _) -> product.versionCode }
    )
)
```

## Release

Represents a specific version/APK of an app.

**File:** `model/Release.kt`

```kotlin
data class Release(
    val selected: Boolean,
    val version: String,
    val versionCode: Long,
    val added: Long,
    val size: Long,
    val minSdkVersion: Int,
    val targetSdkVersion: Int,
    val maxSdkVersion: Int,
    val source: String,
    val release: String,
    val hash: String,
    val hashType: String,
    val signature: String,
    val obbMain: String,
    val obbMainHash: String,
    val obbMainHashType: String,
    val obbPatch: String,
    val obbPatchHash: String,
    val obbPatchHashType: String,
    val permissions: List<String>,
    val features: List<String>,
    val platforms: List<String>,
    val incompatibilities: List<Incompatibility>,
)
```

### Incompatibility

```kotlin
sealed class Incompatibility {
    data object MinSdk : Incompatibility()
    data object MaxSdk : Incompatibility()
    data object Platform : Incompatibility()
    data class Feature(val feature: String) : Incompatibility()
}
```

## Repository

Represents an F-Droid repository.

**File:** `model/Repository.kt`

```kotlin
data class Repository(
    var id: Long,
    val address: String,
    val mirrors: List<String>,
    val name: String,
    val description: String,
    val version: Int,
    val enabled: Boolean,
    val fingerprint: String,
    val lastModified: String,
    val entityTag: String,
    val updated: Long,
    val timestamp: Long,
    val authentication: String,
)
```

### Methods

```kotlin
fun edit(address: String, fingerprint: String, authentication: String): Repository
fun update(mirrors: List<String>, name: String, ...): Repository
fun enable(enabled: Boolean): Repository
```

### Default Repositories

```kotlin
companion object {
    fun newRepository(address: String, fingerprint: String, authentication: String): Repository
    fun defaultRepository(...): Repository

    val defaultRepositories: List<Repository>  // Built-in repos
}
```

## ProductItem

Lightweight version of Product for list display.

**File:** `model/ProductItem.kt`

```kotlin
data class ProductItem(
    var repositoryId: Long,
    var packageName: String,
    var name: String,
    var summary: String,
    val icon: String,
    val metadataIcon: String,
    val version: String,
    var installedVersion: String,
    var compatible: Boolean,
    var canUpdate: Boolean,
    var matchRank: Int,
)
```

## InstalledItem

Represents an installed app.

**File:** `model/InstalledItem.kt`

```kotlin
data class InstalledItem(
    val packageName: String,
    val version: String,
    val versionCode: Long,
    val signature: String,
)
```

## ProductPreference

Per-app update preferences.

**File:** `model/ProductPreference.kt`

```kotlin
data class ProductPreference(
    val ignoreUpdates: Boolean,
    val ignoreVersionCode: Long
)
```

## Model Relationships

```
Repository (1) ──────────────── (N) Product
     │                               │
     │                               ├── Author
     │                               ├── Donate (N)
     │                               ├── Screenshot (N)
     │                               └── Release (N)
     │                                        │
     │                                        └── Incompatibility (N)
     │
InstalledItem ─────────────────────────── ProductPreference
(per device)                               (per package)
```

## Serialization

All models have JSON serialization in `utility/serialization/`:

| Model | File |
|-------|------|
| Product | `ProductSerialization.kt` |
| Release | `ReleaseSerialization.kt` |
| Repository | `RepositorySerialization.kt` |
| ProductItem | `ProductItemSerialization.kt` |
| ProductPreference | `ProductPreferenceSerialization.kt` |

See [Serialization](serialization.md) for details.

## Migration to Room

These legacy models are being replaced by Room entities in `data/local/model/`:

| Legacy Model | Room Entity |
|--------------|-------------|
| Product | AppEntity |
| Release | VersionEntity |
| Repository | RepoEntity |
| InstalledItem | InstalledEntity |

The new models use proper foreign keys and are documented in [Models](../data/models.md).

## Removed

| Model | Removal Doc |
|-------|-------------|
| `ProductItem.Section` | [category-filtering.md](../removal/category-filtering.md) |
