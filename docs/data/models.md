# Data Models

This document describes the domain models used throughout Droid-ify for representing apps, packages, repositories, and related data.

## Overview

**Key Files:**
- `data/model/App.kt` - App and metadata models
- `data/model/Package.kt` - Package/APK models
- `data/model/Fingerprint.kt` - Cryptographic fingerprint
- `data/model/PackageName.kt` - Package name value class
- `data/model/FilePath.kt` - URL/path wrapper
- `data/model/Html.kt` - HTML content wrapper

## Core Models

### App

Full app information including all metadata:

```kotlin
@Immutable
data class App(
    val repoId: Long,
    val appId: Long,
    val categories: List<String>,
    val links: Links?,
    val metadata: Metadata,
    val author: Author?,
    val screenshots: Screenshots?,
    val graphics: Graphics?,
    val donation: Donation?,
    val preferredSigner: String = "",
    val packages: List<Package>?,
)
```

### AppMinimal

Lightweight app representation for lists:

```kotlin
@Immutable
data class AppMinimal(
    val appId: Long,
    val packageName: PackageName,
    val name: String,
    val summary: String?,
    val icon: FilePath?,
    val suggestedVersion: String,
) {
    val fallbackIcon: FilePath? = icon?.path?.let { current ->
        FilePath(current.substringBeforeLast("/") + "/icon.png")
    }
}

// Conversion
fun App.minimal() = AppMinimal(
    appId = appId,
    packageName = metadata.packageName,
    name = metadata.name,
    summary = metadata.summary,
    icon = metadata.icon,
    suggestedVersion = metadata.suggestedVersionName,
)
```

### Metadata

App metadata and localized information:

```kotlin
@Immutable
data class Metadata(
    val name: String,
    val packageName: PackageName,
    val added: Long,
    val description: Html,
    val icon: FilePath?,
    val lastUpdated: Long,
    val license: String,
    val suggestedVersionCode: Long,
    val suggestedVersionName: String,
    val summary: String,
)
```

## Package Models

### Package

Represents a specific APK version:

```kotlin
data class Package(
    val id: Long,
    val installed: Boolean,
    val added: Long,
    val apk: ApkFile,
    val platforms: Platforms,
    val features: List<String>,
    val antiFeatures: List<String>,
    val manifest: Manifest,
    val whatsNew: String
)
```

### ApkFile

APK file metadata:

```kotlin
data class ApkFile(
    override val name: String,
    override val hash: String,
    override val size: DataSize
) : DataFile

interface DataFile {
    val name: String
    val hash: String
    val size: DataSize
}
```

### Manifest

APK manifest information:

```kotlin
data class Manifest(
    val versionCode: Long,
    val versionName: String,
    val usesSDKs: SDKs,
    val signer: Set<String>,
    val permissions: List<Permission>
)

data class SDKs(
    val min: Int = -1,
    val max: Int = -1,
    val target: Int = -1
)

data class Permission(
    val name: String,
    val sdKs: SDKs  // SDK range where permission applies
)
```

### Platforms

CPU architectures supported:

```kotlin
@JvmInline
value class Platforms(val value: List<String>)

// Example: ["arm64-v8a", "armeabi-v7a", "x86", "x86_64"]
```

## Supporting Models

### Author

App author information:

```kotlin
@Immutable
data class Author(
    val id: Int,
    val name: String?,
    val email: String?,
    val phone: String?,
    val web: String?,
)
```

### Links

App-related URLs:

```kotlin
@Immutable
data class Links(
    val changelog: String? = null,
    val issueTracker: String? = null,
    val sourceCode: String? = null,
    val translation: String? = null,
    val webSite: String? = null,
)
```

### Screenshots

Screenshot URLs by device type:

```kotlin
data class Screenshots(
    val phone: List<FilePath> = emptyList(),
    val sevenInch: List<FilePath> = emptyList(),
    val tenInch: List<FilePath> = emptyList(),
    val tv: List<FilePath> = emptyList(),
    val wear: List<FilePath> = emptyList(),
)
```

### Graphics

Promotional graphics:

```kotlin
@Immutable
data class Graphics(
    val featureGraphic: FilePath? = null,
    val promoGraphic: FilePath? = null,
    val tvBanner: FilePath? = null,
    val video: FilePath? = null,
)
```

### Donation

Donation options:

```kotlin
data class Donation(
    val regularUrl: List<String>? = null,
    val bitcoinAddress: String? = null,
    val flattrId: String? = null,
    val litecoinAddress: String? = null,
    val openCollectiveId: String? = null,
    val liberapayId: String? = null,
)
```

## Value Classes

### PackageName

Type-safe package name wrapper:

```kotlin
@JvmInline
value class PackageName(val name: String) {
    override fun toString(): String = name
}
```

### FilePath

URL/path wrapper:

```kotlin
@JvmInline
value class FilePath(val path: String) {
    override fun toString(): String = path
}
```

### Html

HTML content wrapper:

```kotlin
@JvmInline
value class Html(val html: String) {
    override fun toString(): String = html
}
```

### Fingerprint

Cryptographic fingerprint (64-character hex):

```kotlin
@JvmInline
value class Fingerprint(val value: String) {
    inline val isValid: Boolean
        get() = value.isNotBlank() && value.length == Length

    inline fun assert(other: Fingerprint): Boolean =
        other.value.equals(value, ignoreCase = true)

    companion object {
        const val Length = 64
    }
}
```

### Fingerprint Generation

```kotlin
inline fun Certificate.fingerprint(): Fingerprint? {
    val bytes = this.encoded.takeIf { it.size >= 256 } ?: return null
    return Fingerprint(sha256(bytes).hex().uppercase()).takeIf { it.isValid }
}

inline fun ByteArray.hex(): String = joinToString(separator = "") { byte ->
    "%02x".format(Locale.US, byte.toInt() and 0xff)
}

inline fun Fingerprint.formattedString(): String = value.chunked(2)
    .take(Fingerprint.Length / 2).joinToString(separator = " ") { it.uppercase(Locale.US) }
```

## Legacy Models

These models are used by the legacy database system:

### Product (Legacy)

```kotlin
data class Product(
    val repositoryId: Long,
    val packageName: String,
    val name: String,
    val summary: String,
    val description: String,
    val whatsNew: String,
    val icon: String,
    val metadataIcon: String,
    val authorName: String,
    val authorEmail: String,
    val authorWeb: String,
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

### ProductItem (Legacy)

List item representation:

```kotlin
data class ProductItem(
    val repositoryId: Long,
    val packageName: String,
    val name: String,
    val summary: String,
    val icon: String,
    val metadataIcon: String,
    val version: String,
    var installedVersion: String,
    val compatible: Boolean,
    var canUpdate: Boolean,
    val matchRank: Int,
)
```

### Release (Legacy)

APK release information:

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
    val obbPatch: String,
    val obbPatchHash: String,
    val permissions: List<String>,
    val features: List<String>,
    val platforms: List<String>,
    val incompatibilities: List<Incompatibility>,
)
```

## Model Relationships

```
App
├── Metadata
│   ├── PackageName
│   ├── Html (description)
│   └── FilePath (icon)
├── Author
├── Links
├── Screenshots
│   └── List<FilePath>
├── Graphics
│   └── FilePath
├── Donation
└── List<Package>
    ├── ApkFile
    │   └── DataSize
    ├── Manifest
    │   ├── SDKs
    │   └── List<Permission>
    └── Platforms
```

## Immutability

Many models use `@Immutable` annotation for Compose stability:

```kotlin
@Immutable
data class App(...)

@Immutable
data class AppMinimal(...)

@Immutable
data class Metadata(...)
```

This enables Compose to skip recomposition when values haven't changed.

## Usage Examples

### Fetching App with Packages

```kotlin
val app: App = appRepository.getApp(PackageName("com.example.app"))
val latestPackage = app.packages?.maxByOrNull { it.manifest.versionCode }
```

### Converting to Minimal

```kotlin
val apps: List<App> = appRepository.getAllApps()
val minimalApps = apps.map { it.minimal() }
```

### Validating Fingerprint

```kotlin
val expected = Fingerprint("43238D512C1E5EB2D6569F4A3AFBF5523418B82E0A3ED1552770ABB9A9C9CCAB")
val actual = certificate.fingerprint()

if (actual != null && expected.assert(actual)) {
    // Signature matches
}
```
