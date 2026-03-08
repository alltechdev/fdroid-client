# Android TV Support Removal

Removed Android TV/Leanback support from the app.

## Date
2026-03-07

## Branch
main

## Reason

Simplify the app by removing TV support. The app is intended for phones/tablets only.

## Components Removed

### AndroidManifest.xml
- `android.software.leanback` uses-feature declaration
- `android:banner` attribute from application element
- `android.intent.category.LEANBACK_LAUNCHER` from MainActivity intent-filter

### Drawables
- `tv_banner.xml` - 1280x720 vector banner for TV launcher

## Files Changed

| File | Change |
|------|--------|
| `AndroidManifest.xml` | Removed leanback feature, banner attribute, LEANBACK_LAUNCHER category |
| `res/drawable/tv_banner.xml` | Deleted |
