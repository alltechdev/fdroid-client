# Index Converter

Converts V1 index format to V2 format for unified processing.

## Overview

**File:** `app/src/main/kotlin/com/looker/droidify/sync/common/IndexConverter.kt`

The converter enables using V2 processing logic for repositories that only provide V1 format.

## Main Function

```kotlin
internal fun IndexV1.toV2(): IndexV2
```

Converts an entire V1 index to V2 format.

## Conversion Process

### Repository Conversion

```kotlin
private fun RepoV1.toRepoV2(
    categories: List<String>,
    antiFeatures: List<String>,
): RepoV2 = RepoV2(
    address = address,
    timestamp = timestamp,
    icon = mapOf(V1_LOCALE to FileV2("/icons/$icon")),
    name = mapOf(V1_LOCALE to name),
    description = mapOf(V1_LOCALE to description),
    mirrors = mirrors.toMutableList()
        .apply { add(0, address) }
        .map { MirrorV2(url = it, isPrimary = (it == address).takeIf { it }) },
    antiFeatures = antiFeatures.associateWith { name ->
        AntiFeatureV2(name = mapOf(V1_LOCALE to name), ...)
    },
    categories = categories.associateWith { name ->
        CategoryV2(name = mapOf(V1_LOCALE to name), ...)
    },
    webBaseUrl = address,
)
```

### App Conversion

```kotlin
private fun AppV1.toV2(preferredSigner: String?): MetadataV2 = MetadataV2(
    added = added ?: 0L,
    lastUpdated = lastUpdated ?: 0L,
    icon = localized?.localizedIcon(packageName, icon) { it.icon },
    name = localized?.localizedString(name) { it.name } ?: emptyMap(),
    description = localized?.localizedString(description) { it.description },
    summary = localized?.localizedString(summary) { it.summary },
    authorEmail = authorEmail,
    authorName = authorName,
    // ... more fields
    screenshots = localized?.screenshotV2(packageName),
)
```

### Package/Version Conversion

```kotlin
private fun PackageV1.toVersionV2(
    whatsNew: LocalizedString?,
    packageAntiFeatures: List<String>,
): VersionV2 = VersionV2(
    added = added ?: 0L,
    file = ApkFileV2(
        name = "/$apkName",
        sha256 = hash,
        size = size,
    ),
    src = srcName?.let { FileV2("/$it") },
    whatsNew = whatsNew ?: emptyMap(),
    antiFeatures = packageAntiFeatures.associateWith { mapOf(V1_LOCALE to it) },
    manifest = ManifestV2(
        versionName = versionName,
        versionCode = versionCode ?: 0L,
        signer = signer?.let { SignerV2(listOf(it)) },
        usesSdk = sdkV2(),
        usesPermission = usesPermission.map { PermissionV2(it.name, it.maxSdk) },
        features = features?.map { FeatureV2(it) } ?: emptyList(),
        nativecode = nativeCode ?: emptyList()
    ),
)
```

## Localization Handling

V1 stores localized content differently than V2. The converter normalizes this:

### Localized String Extraction

```kotlin
private inline fun Map<String, Localized>.localizedString(
    default: String?,
    crossinline block: (Localized) -> String?,
): LocalizedString? {
    if (isEmpty() && default != null) {
        return mapOf(V1_LOCALE to default)  // "en-US"
    }
    val checkDefault = get(V1_LOCALE)?.let { block(it) }
    if (checkDefault == null && default != null) {
        return mapOf(V1_LOCALE to default)
    }
    return mapValuesNotNull { (_, localized) ->
        block(localized)
    }.takeIf { it.isNotEmpty() }
}
```

### Localized Icon Paths

```kotlin
private inline fun Map<String, Localized>.localizedIcon(
    packageName: String,
    default: String? = null,
    crossinline block: (Localized) -> String?,
): LocalizedIcon? {
    // Build paths like "/$packageName/$locale/$iconName"
    return mapValuesNotNull { (locale, localized) ->
        block(localized)?.let {
            FileV2("/$packageName/$locale/$it")
        }
    }
}
```

### Screenshot Conversion

```kotlin
private fun Map<String, Localized>.screenshotV2(packageName: String): ScreenshotsV2? =
    ScreenshotsV2(
        phone = localizedScreenshots { locale, screenshot ->
            screenshot.phoneScreenshots?.map {
                "/$packageName/$locale/phoneScreenshots/$it"
            }
        },
        sevenInch = localizedScreenshots { locale, screenshot ->
            screenshot.sevenInchScreenshots?.map {
                "/$packageName/$locale/sevenInchScreenshots/$it"
            }
        },
        // ... more screenshot types
    ).takeIf { !it.isNull }
```

## Key Differences Handled

| V1 | V2 | Conversion |
|----|----|----|
| Flat string fields | Localized maps | Wrap in `mapOf("en-US" to value)` |
| Separate packages array | Versions map inside package | Group by package name |
| String antiFeatures | AntiFeature objects | Create AntiFeatureV2 with name |
| String categories | Category objects | Create CategoryV2 with name |
| Icon filename | FileV2 with path | Prepend `/icons/` |
| Screenshot arrays | Nested structure | Build full paths |

## V1 Locale Default

```kotlin
private const val V1_LOCALE = "en-US"
```

V1 doesn't have locale-specific fields at the top level, so they're assigned to "en-US".

## Usage

```kotlin
// In sync process
val indexV1 = parseV1Index(inputStream)
val indexV2 = indexV1.toV2()

// Now process using V2 logic
processIndex(indexV2)
```

## Data Flow

```
┌─────────────────────────────────────────────────────────────┐
│                    index-v1.json                             │
│                                                              │
│  repo: { name, description, ... }                           │
│  apps: [ { packageName, name, localized, ... } ]           │
│  packages: { "pkg.name": [ { versionCode, ... } ] }        │
└─────────────────────────────┬───────────────────────────────┘
                              │
                              ▼ IndexV1.toV2()
┌─────────────────────────────────────────────────────────────┐
│                    IndexV2 (in memory)                       │
│                                                              │
│  repo: RepoV2 {                                             │
│    name: { "en-US": "F-Droid" }                             │
│    categories: { "Internet": CategoryV2 { ... } }           │
│  }                                                          │
│  packages: {                                                │
│    "pkg.name": PackageV2 {                                  │
│      metadata: MetadataV2 { ... }                           │
│      versions: { "hash": VersionV2 { ... } }                │
│    }                                                        │
│  }                                                          │
└─────────────────────────────────────────────────────────────┘
```
