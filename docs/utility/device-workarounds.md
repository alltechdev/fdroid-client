# Device Workarounds

OEM-specific detection and workarounds for non-standard Android implementations.

## Overview

**Location:** `app/src/main/kotlin/com/atd/store/utility/common/device/`

Some Android device manufacturers modify the system in ways that break standard app installation. These utilities detect and work around such issues.

## Huawei EMUI

**File:** `device/Huawei.kt`

### Detection

```kotlin
object Huawei {
    val isHuaweiEmui: Boolean
        get() {
            return try {
                Class.forName("com.huawei.android.os.BuildEx")
                true
            } catch (e: Exception) {
                false
            }
        }
}
```

Detects EMUI by checking for Huawei-specific class `com.huawei.android.os.BuildEx`.

### Known Issues

1. **Package installer restrictions** - EMUI may block third-party app installations
2. **Background service limits** - More aggressive than stock Android
3. **Permission handling** - Custom permission dialogs

### Usage

```kotlin
if (Huawei.isHuaweiEmui) {
    // Apply EMUI-specific workarounds
    showHuaweiInstallGuide()
}
```

## Xiaomi MIUI

**File:** `device/Miui.kt`

### Detection

```kotlin
object Miui {
    val isMiui by lazy {
        getSystemProperty("ro.miui.ui.version.name")?.isNotEmpty() ?: false
    }
}
```

Checks system property `ro.miui.ui.version.name` for MIUI version.

### MIUI Optimization Check

```kotlin
fun isMiuiOptimizationDisabled(): Boolean {
    // Check system property
    val sysProp = getSystemProperty("persist.sys.miui_optimization")
    if (sysProp == "0" || sysProp == "false") {
        return true
    }

    // Check via reflection
    return try {
        Class.forName("android.miui.AppOpsUtils")
            .getDeclaredMethod("isXOptMode")
            .invoke(null) as Boolean
    } catch (e: Exception) {
        false
    }
}
```

### System Property Access

```kotlin
@SuppressLint("PrivateApi")
private fun getSystemProperty(key: String?): String? {
    return try {
        Class.forName("android.os.SystemProperties")
            .getDeclaredMethod("get", String::class.java)
            .invoke(null, key) as String
    } catch (e: Exception) {
        Log.e("Miui", "Unable to use SystemProperties.get()", e)
        null
    }
}
```

### Known MIUI Issues

1. **MIUI Optimization** - Blocks unsigned APK installations when enabled
2. **Install via USB only** - May require enabling "Install via USB"
3. **Background restrictions** - Aggressive battery optimization
4. **Notification permissions** - Custom notification channel handling

### Usage

```kotlin
if (Miui.isMiui && !Miui.isMiuiOptimizationDisabled()) {
    // Warn user about MIUI optimization
    showMiuiWarning()
}
```

## Integration Points

### Session Installer

The session installer may need workarounds:

```kotlin
// In SessionInstaller
if (Miui.isMiui) {
    // Use alternative installation method
} else if (Huawei.isHuaweiEmui) {
    // Handle EMUI restrictions
}
```

### Settings Screen

Display relevant warnings:

```kotlin
@Composable
fun InstallerWarnings() {
    if (Miui.isMiui && !Miui.isMiuiOptimizationDisabled()) {
        WarningBanner(
            text = stringResource(R.string.miui_optimization_warning)
        )
    }

    if (Huawei.isHuaweiEmui) {
        WarningBanner(
            text = stringResource(R.string.huawei_warning)
        )
    }
}
```

## Adding New Device Workarounds

### Template

```kotlin
object VendorName {
    val isVendorRom: Boolean by lazy {
        try {
            // Detection method 1: Check for vendor-specific class
            Class.forName("com.vendor.specific.Class")
            true
        } catch (e: Exception) {
            // Detection method 2: Check system property
            getSystemProperty("ro.vendor.name")?.isNotEmpty() ?: false
        }
    }

    private fun getSystemProperty(key: String): String? {
        return try {
            Class.forName("android.os.SystemProperties")
                .getDeclaredMethod("get", String::class.java)
                .invoke(null, key) as String
        } catch (e: Exception) {
            null
        }
    }
}
```

### Detection Methods

| Method | Reliability | Example |
|--------|------------|---------|
| Class existence | High | `Class.forName("com.vendor.Class")` |
| System property | High | `ro.miui.ui.version.name` |
| Build fields | Medium | `Build.MANUFACTURER`, `Build.BRAND` |
| Feature check | Low | Package manager features |

## Known Problematic ROMs

| Vendor | ROM | Issues |
|--------|-----|--------|
| Xiaomi | MIUI | Optimization blocks installs |
| Huawei | EMUI | Install restrictions |
| Samsung | OneUI | Aggressive Doze |
| Oppo | ColorOS | Background limits |
| Vivo | FunTouchOS | Permission issues |

## Best Practices

1. **Lazy detection** - Use `by lazy` to defer checks
2. **Graceful fallback** - Always catch exceptions
3. **Log failures** - Log detection failures for debugging
4. **User guidance** - Show clear instructions for workarounds
5. **Keep updated** - OEM behavior changes with ROM updates
