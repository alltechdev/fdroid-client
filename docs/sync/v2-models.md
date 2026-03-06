# V2 Index Models

Data models for F-Droid v2 repository index format.

## Overview

**Directory:** `sync/v2/model/`

These models are based on F-Droid's official v2 index specification (GPL 3.0 licensed).

## IndexV2

Top-level index structure:

```kotlin
@Serializable
data class IndexV2(
    val repo: RepoV2,
    val packages: Map<String, PackageV2>  // packageName -> package
)
```

### Diff Support

```kotlin
@Serializable
data class IndexV2Diff(
    val repo: RepoV2Diff,
    val packages: Map<String, PackageV2Diff?>  // null = remove
) {
    fun patchInto(index: IndexV2, saveIndex: (IndexV2) -> Unit): IndexV2
}
```

Diff updates allow incremental sync instead of downloading full index.

## RepoV2

Repository metadata:

```kotlin
@Serializable
data class RepoV2(
    val address: String,
    val webBaseUrl: String? = null,
    val icon: LocalizedIcon? = null,
    val name: LocalizedString = emptyMap(),
    val description: LocalizedString = emptyMap(),
    val antiFeatures: Map<Tag, AntiFeatureV2> = emptyMap(),
    val categories: Map<DefaultName, CategoryV2> = emptyMap(),
    val mirrors: List<MirrorV2> = emptyList(),
    val timestamp: Long,
)
```

### MirrorV2

```kotlin
@Serializable
data class MirrorV2(
    val url: String,
    val isPrimary: Boolean? = null,
    val countryCode: String? = null
)
```

### CategoryV2

```kotlin
@Serializable
data class CategoryV2(
    val icon: LocalizedIcon = emptyMap(),
    val name: LocalizedString,
    val description: LocalizedString = emptyMap(),
)
```

### AntiFeatureV2

```kotlin
@Serializable
data class AntiFeatureV2(
    val icon: LocalizedIcon = emptyMap(),
    val name: LocalizedString,
    val description: LocalizedString = emptyMap(),
)
```

## PackageV2

App package data:

```kotlin
@Serializable
data class PackageV2(
    val metadata: MetadataV2,
    val versions: Map<String, VersionV2>,  // hash -> version
)
```

## MetadataV2

App metadata:

```kotlin
@Serializable
data class MetadataV2(
    val name: LocalizedString,
    val summary: LocalizedString? = null,
    val description: LocalizedString? = null,
    val icon: LocalizedIcon? = null,
    val added: Long,
    val lastUpdated: Long,

    // Author info
    val authorEmail: String? = null,
    val authorName: String? = null,
    val authorPhone: String? = null,
    val authorWebSite: String? = null,

    // Donation
    val bitcoin: String? = null,
    val donate: List<String> = emptyList(),
    val flattrID: String? = null,
    val liberapay: String? = null,
    val litecoin: String? = null,
    val openCollective: String? = null,

    // Links
    val changelog: String? = null,
    val issueTracker: String? = null,
    val license: String? = null,
    val sourceCode: String? = null,
    val translation: String? = null,
    val webSite: String? = null,
    val video: LocalizedString? = null,

    // Graphics
    val featureGraphic: LocalizedIcon? = null,
    val promoGraphic: LocalizedIcon? = null,
    val screenshots: ScreenshotsV2? = null,
    val tvBanner: LocalizedIcon? = null,

    // Other
    val categories: List<String> = emptyList(),
    val preferredSigner: String? = null,
)
```

## VersionV2

App release version:

```kotlin
@Serializable
data class VersionV2(
    val added: Long,
    val file: ApkFileV2,
    val src: FileV2? = null,
    val whatsNew: LocalizedString = emptyMap(),
    val manifest: ManifestV2,
    val antiFeatures: Map<Tag, AntiFeatureReason> = emptyMap(),
)
```

### ManifestV2

```kotlin
@Serializable
data class ManifestV2(
    val versionName: String,
    val versionCode: Long,
    val signer: SignerV2? = null,
    val usesSdk: UsesSdkV2? = null,
    val maxSdkVersion: Int? = null,
    val usesPermission: List<PermissionV2> = emptyList(),
    val usesPermissionSdk23: List<PermissionV2> = emptyList(),
    val features: List<FeatureV2> = emptyList(),
    val nativecode: List<String> = emptyList(),
)
```

### Supporting Types

```kotlin
@Serializable
data class UsesSdkV2(
    val minSdkVersion: Int,
    val targetSdkVersion: Int,
)

@Serializable
data class PermissionV2(
    val name: String,
    val maxSdkVersion: Int? = null,
)

@Serializable
data class FeatureV2(
    val name: String,
)

@Serializable
data class SignerV2(
    val sha256: List<String>,
    val hasMultipleSigners: Boolean = false,
)
```

## ScreenshotsV2

Multi-form-factor screenshots:

```kotlin
@Serializable
data class ScreenshotsV2(
    val phone: LocalizedFiles? = null,
    val sevenInch: LocalizedFiles? = null,
    val tenInch: LocalizedFiles? = null,
    val wear: LocalizedFiles? = null,
    val tv: LocalizedFiles? = null,
) {
    val isNull: Boolean =
        phone == null && sevenInch == null && tenInch == null && wear == null && tv == null
}
```

## Localization Types

**File:** `sync/v2/model/Localization.kt`

```kotlin
typealias LocalizedString = Map<String, String>       // locale -> text
typealias LocalizedIcon = Map<String, FileV2>         // locale -> file
typealias LocalizedFiles = Map<String, List<FileV2>>  // locale -> files
typealias NullableLocalizedString = Map<String, String?>

typealias Tag = String          // Anti-feature tag
typealias DefaultName = String  // Category default name
typealias AntiFeatureReason = LocalizedString
```

## FileV2

```kotlin
@Serializable
data class FileV2(
    val name: String,
    val sha256: String? = null,
    val size: Long? = null,
)

@Serializable
data class ApkFileV2(
    val name: String,
    val sha256: String,
    val size: Long,
)
```

## Diff Patching

### Package Diff

```kotlin
fun patchInto(pack: PackageV2): PackageV2 {
    val versionsToRemove = versions?.filterValues { it == null }?.keys ?: emptySet()
    val versionsToAdd = versions?.mapNotNull { ... }

    return pack.copy(
        metadata = pack.metadata.copy(
            lastUpdated = metadata?.lastUpdated ?: pack.metadata.lastUpdated,
            name = metadata?.name ?: pack.metadata.name,
            // ... patch each field
        ),
        versions = pack.versions
            .minus(versionsToRemove)
            .plus(versionsToAdd),
    )
}
```

### Null = Remove

In diff format, `null` values indicate removal:

```kotlin
packages: Map<String, PackageV2Diff?>  // null = remove package

// When applying:
val packagesToRemove = packages.filter { it.value == null }.keys
```

## Index Structure

```
IndexV2
├── repo: RepoV2
│   ├── address, timestamp
│   ├── name, description (localized)
│   ├── icon (localized)
│   ├── mirrors: List<MirrorV2>
│   ├── categories: Map<name, CategoryV2>
│   └── antiFeatures: Map<tag, AntiFeatureV2>
│
└── packages: Map<packageName, PackageV2>
    ├── metadata: MetadataV2
    │   ├── name, summary, description (localized)
    │   ├── icon, screenshots (localized)
    │   ├── author info
    │   ├── links (source, website, etc.)
    │   └── donation info
    │
    └── versions: Map<hash, VersionV2>
        ├── file: ApkFileV2 (name, sha256, size)
        ├── manifest: ManifestV2
        │   ├── versionName, versionCode
        │   ├── signer: SignerV2
        │   ├── usesSdk: UsesSdkV2
        │   ├── usesPermission: List<PermissionV2>
        │   └── nativecode: List<String>
        └── antiFeatures: Map<tag, reason>
```
