# Domain Models

Modern Compose-compatible data models.

## Overview

**Directory:** `data/model/`

Immutable data classes used by the repository layer and Compose UI.

## App Model

**File:** `data/model/App.kt`

Main app data structure.

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

### Metadata

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

### Author

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

```kotlin
data class Screenshots(
    val phone: List<FilePath> = emptyList(),
    val sevenInch: List<FilePath> = emptyList(),
    val tenInch: List<FilePath> = emptyList(),
    val tv: List<FilePath> = emptyList(),
    val wear: List<FilePath> = emptyList(),
)
```

### Donation

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

### Graphics

```kotlin
@Immutable
data class Graphics(
    val featureGraphic: FilePath? = null,
    val promoGraphic: FilePath? = null,
    val tvBanner: FilePath? = null,
    val video: FilePath? = null,
)
```

### AppMinimal

Lightweight version for lists:

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

fun App.minimal() = AppMinimal(...)
```

## Package Model

**File:** `data/model/Package.kt`

App version/release data.

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

```kotlin
data class ApkFile(
    override val name: String,
    override val hash: String,
    override val size: DataSize
) : DataFile
```

### Manifest

```kotlin
data class Manifest(
    val versionCode: Long,
    val versionName: String,
    val usesSDKs: SDKs,
    val signer: Set<String>,
    val permissions: List<Permission>
)
```

### SDKs

```kotlin
data class SDKs(
    val min: Int = -1,
    val max: Int = -1,
    val target: Int = -1
)
```

### Permission

```kotlin
data class Permission(
    val name: String,
    val sdKs: SDKs  // -1 means not specified
)
```

### Platforms

```kotlin
@JvmInline
value class Platforms(val value: List<String>)
// e.g., ["arm64-v8a", "armeabi-v7a"]
```

## Repo Model

**File:** `data/model/Repo.kt`

Repository configuration.

```kotlin
data class Repo(
    val id: Int,
    val enabled: Boolean,
    val address: String,
    val icon: FilePath?,
    val name: String,
    val description: Html,
    val fingerprint: Fingerprint?,
    val authentication: Authentication?,
    val versionInfo: VersionInfo?,
    val mirrors: List<String>,
) {
    val shouldAuthenticate = authentication != null

    fun update(fingerprint: Fingerprint, timestamp: Long?, etag: String?): Repo
}
```

### Authentication

```kotlin
data class Authentication(
    val username: String,
    val password: String,
)
```

### VersionInfo

```kotlin
data class VersionInfo(
    val timestamp: Long,
    val etag: String?,
)
```

### AntiFeature

```kotlin
data class AntiFeature(
    val id: Long,
    val name: String,
    val icon: String = "",
    val description: String = "",
)
```

### Category

```kotlin
data class Category(
    val id: Long,
    val name: String,
    val icon: String = "",
    val description: String = "",
)
```

## Value Types

### PackageName

```kotlin
@JvmInline
value class PackageName(val name: String)

fun String.toPackageName() = PackageName(this)
```

### Fingerprint

```kotlin
@JvmInline
value class Fingerprint(val value: String)
```

### FilePath

```kotlin
data class FilePath(
    val baseUrl: String,
    val path: String
) {
    constructor(path: String) : this("", path)

    val url: String get() = "$baseUrl$path"
}
```

### Html

```kotlin
@JvmInline
value class Html(val value: String)

fun String.hex(): String = ...
```

## Immutability

Models use `@Immutable` annotation for Compose stability:

```kotlin
@Immutable
data class App(...)
```

This allows Compose to skip recomposition when the reference hasn't changed.

## Model Hierarchy

```
App
├── metadata: Metadata
│   ├── packageName: PackageName
│   ├── icon: FilePath?
│   └── description: Html
├── author: Author?
├── links: Links?
├── screenshots: Screenshots?
├── graphics: Graphics?
├── donation: Donation?
└── packages: List<Package>?
    ├── apk: ApkFile
    ├── manifest: Manifest
    │   ├── usesSDKs: SDKs
    │   └── permissions: List<Permission>
    └── platforms: Platforms

Repo
├── fingerprint: Fingerprint?
├── authentication: Authentication?
├── versionInfo: VersionInfo?
└── mirrors: List<String>
```
