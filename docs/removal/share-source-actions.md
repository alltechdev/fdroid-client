# Share and Source Actions Removal

Removed the Share and Source Code actions from the app detail page menu.

## Date
2026-03-07

## Branch
main

## Reason

Simplify the app detail UI by removing rarely-used menu actions. Users can find source code links via the app's website or repository page if needed.

## Components Removed

### Actions

| Component | File | Description |
|-----------|------|-------------|
| `Action.SHARE` | `AppDetailAdapter.kt` | Share app link action |
| `Action.SOURCE` | `AppDetailAdapter.kt` | View source code action |
| `Action.SHARE` | `AppDetailFragment.kt` | Fragment action enum entry |
| `Action.SOURCE` | `AppDetailFragment.kt` | Fragment action enum entry |

### Action Handlers

| File | Description |
|------|-------------|
| `AppDetailFragment.kt` | Removed SHARE case from `onActionClick` |
| `AppDetailFragment.kt` | Removed SOURCE case from `onActionClick` |

### Utilities

| Component | File | Description |
|-----------|------|-------------|
| `shareUrl()` | `Deeplinks.kt` | URL builder for sharing app links |

### Resources

| Resource | Type | Description |
|----------|------|-------------|
| `ic_share.xml` | Drawable | Share action icon |
| `ic_source_code.xml` | Drawable | Source code action icon |
| `share` | String | "Share" action label |
| `source_code` | String | "Source code" action label |

## Files Changed

| File | Change |
|------|--------|
| `AppDetailAdapter.kt` | Removed `Action.SHARE` and `Action.SOURCE` enum values |
| `AppDetailFragment.kt` | Removed SHARE/SOURCE from Action enum, actions set, and onActionClick handler |
| `Deeplinks.kt` | Removed `shareUrl()` function |
| `drawable/ic_share.xml` | Deleted |
| `drawable/ic_source_code.xml` | Deleted |
| `values*/strings.xml` | Removed `share` and `source_code` strings from all locales |
