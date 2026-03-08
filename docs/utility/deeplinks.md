# Deeplink Handling

URL and intent parsing for app navigation via deeplinks.

## Overview

**File:** `app/src/main/kotlin/com/atd/store/utility/common/Deeplinks.kt`

Handles various URL schemes and intents to navigate to apps, repositories, and search.

## Supported Schemes

| Scheme | Example | Purpose |
|--------|---------|---------|
| `package` | `package:com.example` | Open app by package |
| `fdroid.app` | `fdroid.app:com.example` | Open app (F-Droid compat) |
| `fdroidrepo` | `fdroidrepo://example.com/repo` | Add HTTP repo |
| `fdroidrepos` | `fdroidrepos://example.com/repo` | Add HTTPS repo |
| `market` | `market://details?id=com.example` | Play Store compat |
| `http/https` | `https://droidify.app/app/?id=...` | Web URLs |

## DeeplinkType Sealed Interface

```kotlin
sealed interface DeeplinkType {
    class AddRepository(val address: String) : DeeplinkType
    class AppDetail(val packageName: String, val repoAddress: String? = null) : DeeplinkType
    class AppSearch(val query: String) : DeeplinkType
}
```

## Intent Parsing

### Main Function

```kotlin
fun Intent.deeplinkType(): DeeplinkType?
```

### Package/FDroid Scheme

```kotlin
"package", "fdroid.app" -> {
    val packageName = data.schemeSpecificPart?.nullIfEmpty()
        ?: invalidDeeplink("Invalid packageName: $data")
    DeeplinkType.AppDetail(packageName)
}
```

**Examples:**
- `package:com.atd.store` → AppDetail("com.atd.store")
- `fdroid.app:org.mozilla.firefox` → AppDetail("org.mozilla.firefox")

### FDroid Repo Scheme

```kotlin
in fdroidRepoScheme -> {
    val repoAddress = when (data.scheme) {
        "fdroidrepos" -> dataString!!.replaceFirst("fdroidrepos", "https")
        "fdroidrepo" -> dataString!!.replaceFirst("fdroidrepo", "https")
        else -> invalidDeeplink("No repo address: $data")
    }
    DeeplinkType.AddRepository(repoAddress)
}
```

**Examples:**
- `fdroidrepos://f-droid.org/repo` → AddRepository("https://f-droid.org/repo")
- `fdroidrepo://example.com/fdroid/repo` → AddRepository("https://example.com/fdroid/repo")

### Market Scheme

```kotlin
"market" if data.host == "details" -> {
    val packageName = data["id"]?.nullIfEmpty()
        ?: invalidDeeplink("Invalid packageName: $data")
    DeeplinkType.AppDetail(packageName)
}

"market" if data.host == "search" -> {
    val query = data["q"]?.nullIfEmpty()
        ?: invalidDeeplink("Invalid query: $data")
    DeeplinkType.AppSearch(query)
}
```

**Examples:**
- `market://details?id=com.example` → AppDetail("com.example")
- `market://search?q=browser` → AppSearch("browser")

### HTTP/HTTPS Scheme

```kotlin
in httpScheme -> {
    when (data.host) {
        PERSONAL_HOST, LEGACY_HOST -> {
            val repoAddress = data["repo_address"]
            if (data.path == "/app/") {
                val packageName = data["id"]?.nullIfEmpty()
                    ?: invalidDeeplink("Invalid packageName: $data")
                DeeplinkType.AppDetail(packageName, repoAddress)
            } else {
                invalidDeeplink("Unknown intent path")
            }
        }
        in supportedExternalHosts -> {
            val packageName = data.lastPathSegment?.nullIfEmpty()
                ?: invalidDeeplink("Invalid packageName: $data")
            DeeplinkType.AppDetail(packageName)
        }
        else -> null
    }
}
```

## Supported Hosts

### Droid-ify Hosts

```kotlin
const val LEGACY_HOST = "droidify.eu.org"
const val PERSONAL_HOST = "droidify.app"
```

### External Hosts

```kotlin
private val supportedExternalHosts = arrayOf(
    "f-droid.org",
    "www.f-droid.org",
    "staging.f-droid.org",
    "apt.izzysoft.de",
)
```

## Install Intent Helper

```kotlin
val Intent.getInstallPackageName: String?
    get() = if (data?.scheme == "package") data?.schemeSpecificPart?.nullIfEmpty() else null
```

## Error Handling

```kotlin
class InvalidDeeplink(override val message: String?) : IllegalStateException(message)

private inline fun invalidDeeplink(message: String): Nothing = throw InvalidDeeplink(message)
```

### Common Errors

| Error | Cause |
|-------|-------|
| "Invalid packageName" | Missing or empty package ID |
| "Invalid query" | Missing search query parameter |
| "No repo address" | Invalid fdroidrepo URL |
| "Unknown intent path" | Unrecognized URL path |

## Usage in MainActivity

```kotlin
override fun handleIntent(intent: Intent?) {
    when (val deeplink = intent?.deeplinkType()) {
        is DeeplinkType.AppDetail -> {
            navigateToAppDetail(deeplink.packageName, deeplink.repoAddress)
        }
        is DeeplinkType.AddRepository -> {
            navigateToAddRepo(deeplink.address)
        }
        is DeeplinkType.AppSearch -> {
            navigateToSearch(deeplink.query)
        }
        null -> {
            // Not a deeplink or unrecognized
        }
    }
}
```

## Manifest Configuration

```xml
<intent-filter>
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />

    <!-- F-Droid app scheme -->
    <data android:scheme="fdroid.app" />
    <data android:scheme="package" />
</intent-filter>

<intent-filter>
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />

    <!-- Repository URLs -->
    <data android:scheme="fdroidrepo" />
    <data android:scheme="fdroidrepos" />
</intent-filter>

<intent-filter>
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />

    <!-- Web URLs -->
    <data android:scheme="https" android:host="droidify.app" />
</intent-filter>
```

## Testing Deeplinks

### ADB Commands

```bash
# Open app detail
adb shell am start -a android.intent.action.VIEW \
    -d "package:com.atd.store"

# Add repository
adb shell am start -a android.intent.action.VIEW \
    -d "fdroidrepos://f-droid.org/repo"

# Search
adb shell am start -a android.intent.action.VIEW \
    -d "market://search?q=browser"

# Web URL
adb shell am start -a android.intent.action.VIEW \
    -d "https://droidify.app/app/?id=com.example"
```

## Removed

| Feature | Removal Doc |
|---------|-------------|
| Package: `com.looker.droidify` | [package-rename.md](../changes/package-rename.md) |
| `shareUrl()` | [share-source-actions.md](../removal/share-source-actions.md) |
| `DeeplinkType`, `deeplinkType()`, all intent filters | [deeplinks.md](../removal/deeplinks.md) |

## Changes

| Change | Change Doc |
|--------|------------|
| App branding Droid-ify → ATD Store | [app-branding.md](../changes/app-branding.md) |
