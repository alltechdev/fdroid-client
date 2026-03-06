# SDK Version Checks

Utilities for Android SDK version checks and conditional execution.

## Overview

**File:** `app/src/main/kotlin/com/looker/droidify/utility/common/SdkCheck.kt`

Provides type-safe SDK version checks with proper Android lint annotations.

## Helper Function

Execute code only on specific SDK versions:

```kotlin
@ChecksSdkIntAtLeast(parameter = 0, lambda = 1)
inline fun sdkAbove(sdk: Int, onSuccessful: () -> Unit) {
    if (Build.VERSION.SDK_INT >= sdk) onSuccessful()
}
```

### Usage

```kotlin
sdkAbove(Build.VERSION_CODES.N) {
    // Nougat+ only code
}
```

## SdkCheck Object

### Current SDK

```kotlin
val sdk: Int = Build.VERSION.SDK_INT
```

### Version Checks

| Property | API Level | Android Version |
|----------|-----------|-----------------|
| `isNougat` | 24+ | 7.0+ |
| `isOreo` | 26+ | 8.0+ |
| `isPie` | 28+ | 9+ |
| `isR` | 30+ | 11+ |
| `isSnowCake` | 31+ | 12+ |
| `isTiramisu` | 33+ | 13+ |

### Implementation

```kotlin
object SdkCheck {
    val sdk: Int = Build.VERSION.SDK_INT

    @get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.TIRAMISU)
    val isTiramisu: Boolean get() = sdk >= Build.VERSION_CODES.TIRAMISU

    @get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.R)
    val isR: Boolean get() = sdk >= Build.VERSION_CODES.R

    @get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.P)
    val isPie: Boolean get() = sdk >= Build.VERSION_CODES.P

    @get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.O)
    val isOreo: Boolean get() = sdk >= Build.VERSION_CODES.O

    @get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.S)
    val isSnowCake: Boolean get() = sdk >= Build.VERSION_CODES.S

    @get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.N)
    val isNougat: Boolean get() = sdk >= Build.VERSION_CODES.N
}
```

### Auto-Install Check

```kotlin
fun canAutoInstall(targetSdk: Int) = targetSdk >= sdk - 1 && isSnowCake
```

Returns true if:
1. Running Android 12+ (Snow Cake)
2. APK's target SDK is within 1 of current SDK

## SDK Name Mapping

Human-readable Android version names:

```kotlin
val sdkName by lazy {
    mapOf(
        1 to "1.0",
        // ...
        24 to "7.0",
        25 to "7.1",
        26 to "8.0",
        27 to "8.1",
        28 to "9",
        29 to "10",
        30 to "11",
        31 to "12",
        32 to "12L",
        33 to "13",
        34 to "14",
        35 to "15",
        36 to "16",
    )
}
```

### Usage

```kotlin
val versionName = sdkName[Build.VERSION.SDK_INT] ?: "Unknown"
// Returns "14" for API 34
```

## Usage Examples

### Conditional Feature Use

```kotlin
if (SdkCheck.isTiramisu) {
    // Use Android 13+ notification permission
    requestNotificationPermission()
}
```

### Root Installer Command

```kotlin
val installCommand = if (SdkCheck.isNougat) {
    "pm install-create --user current -i $installerPackage -S $fileSize"
} else {
    "pm install-create -i $installerPackage -S $fileSize"
}
```

### Cache Permissions

```kotlin
sdkAbove(Build.VERSION_CODES.N) {
    // Make APK readable for package installer on N+
    applyOrMode(file, 0b100100100)
}
```

## Lint Integration

The `@ChecksSdkIntAtLeast` annotation enables Android Lint to:
1. Suppress false-positive warnings in version-guarded code
2. Properly smart-cast SDK checks
3. Enable IDE code analysis

```kotlin
@get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.O)
val isOreo: Boolean get() = sdk >= Build.VERSION_CODES.O

// After checking isOreo, Lint knows API 26+ methods are safe
if (SdkCheck.isOreo) {
    notificationChannel.importance = IMPORTANCE_HIGH  // API 26+
}
```

## Comparison Patterns

### Pattern 1: Property Check

```kotlin
if (SdkCheck.isOreo) {
    createNotificationChannel()
}
```

### Pattern 2: Inline Lambda

```kotlin
sdkAbove(Build.VERSION_CODES.O) {
    createNotificationChannel()
}
```

### Pattern 3: When Expression

```kotlin
val result = when {
    SdkCheck.isTiramisu -> useAndroid13Feature()
    SdkCheck.isR -> useAndroid11Feature()
    else -> useLegacyFeature()
}
```

## Best Practices

1. Use `SdkCheck` properties for readability
2. Use `sdkAbove()` for simple conditional blocks
3. Prefer properties over direct `Build.VERSION.SDK_INT` comparisons
4. Always annotate custom version checks with `@ChecksSdkIntAtLeast`
