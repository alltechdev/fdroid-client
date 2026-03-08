# Deep Links Removal

Removed all deep link handling from the app.

## Date
2026-03-07

## Branch
main

## Reason

Simplify the app by removing external link handling. The app is meant to be used directly, not opened via external links.

## Components Removed

### Intent Filters (AndroidManifest.xml)

| Scheme/Host | Purpose |
|-------------|---------|
| `fdroid.app://` | F-Droid app scheme |
| `market://details` | Play Store compatibility |
| `market://search` | Play Store search compatibility |
| `https://f-droid.org/*` | F-Droid website links |
| `https://www.f-droid.org/*` | F-Droid website links |
| `https://staging.f-droid.org/*` | F-Droid staging links |
| `https://apt.izzysoft.de/*` | IzzyOnDroid links |
| `https://droidify.eu.org/*` | Legacy Droidify links |
| `https://droidify.app/*` | Droidify links |
| `fdroidrepo://`, `fdroidrepos://` | Repository add links |

### Code

| Component | File | Description |
|-----------|------|-------------|
| `DeeplinkType` | `Deeplinks.kt` | Sealed interface for deep link types |
| `deeplinkType()` | `Deeplinks.kt` | Intent extension to parse deep links |
| `InvalidDeeplink` | `Deeplinks.kt` | Exception class |
| `ACTION_VIEW` handler | `MainActivity.kt` | Deep link intent handling |

### Removed Constants

- `LEGACY_HOST`
- `PERSONAL_HOST`
- `httpScheme`
- `fdroidRepoScheme`
- `supportedExternalHosts`

## Files Changed

| File | Change |
|------|--------|
| `app/src/main/AndroidManifest.xml` | Removed all ACTION_VIEW intent filters |
| `app/src/debug/AndroidManifest.xml` | Removed all ACTION_VIEW intent filters from MainComposeActivity |
| `MainActivity.kt` | Removed ACTION_VIEW case from handleIntent, removed DeeplinkType imports |
| `Deeplinks.kt` | Simplified to only contain `getInstallPackageName` extension |

## Kept

- `getInstallPackageName` extension - used for internal install handling
- `ACTION_SHOW_APP_INFO` intent filter - system app info integration

## Removed

| Feature | Removal Doc |
|---------|-------------|
| Package: `com.looker.droidify` | [package-rename.md](../changes/package-rename.md) |
