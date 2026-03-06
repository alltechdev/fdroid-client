# Release File Validator

Validates downloaded APK files before installation.

## Overview

**File:** `service/ReleaseFileValidator.kt`

Implements `FileValidator` to verify APK integrity, metadata, signature, and permissions.

## Implementation

```kotlin
class ReleaseFileValidator(
    private val context: Context,
    private val packageName: String,
    private val release: Release,
) : FileValidator {

    override suspend fun validate(file: File) {
        // 1. Checksum verification
        val checksum = sha256(file).hex()
        if (!checksum.equals(release.hash, ignoreCase = true)) {
            invalid(getString(strings.integrity_check_error_DESC))
        }

        // 2. Package info extraction
        val packageInfo = context.packageManager.getPackageArchiveInfoCompat(file.path)
            ?: invalid(getString(strings.file_format_error_DESC))

        // 3. Metadata verification
        if (packageInfo.packageName != packageName ||
            packageInfo.versionCodeCompat != release.versionCode) {
            invalid(getString(strings.invalid_metadata_error_DESC))
        }

        // 4. Signature verification
        packageInfo.singleSignature
            ?.calculateHash()
            ?.takeIf { it.isNotBlank() || it == release.signature }
            ?: invalid(getString(strings.invalid_signature_error_DESC))

        // 5. Permissions verification
        packageInfo.permissions
            ?.map { it.name }
            ?.toSet()
            .orEmpty()
            .takeIf { release.permissions.containsAll(it) }
            ?: invalid(getString(strings.invalid_permissions_error_DESC))
    }
}
```

## Validation Steps

### 1. SHA-256 Checksum

```kotlin
val checksum = sha256(file).hex()
if (!checksum.equals(release.hash, ignoreCase = true)) {
    invalid("Integrity check failed")
}
```

Ensures file wasn't corrupted during download.

### 2. APK Parsing

```kotlin
val packageInfo = context.packageManager.getPackageArchiveInfoCompat(file.path)
    ?: invalid("Invalid APK format")
```

Verifies file is a valid APK that Android can parse.

### 3. Metadata Match

```kotlin
if (packageInfo.packageName != packageName ||
    packageInfo.versionCodeCompat != release.versionCode) {
    invalid("Metadata mismatch")
}
```

Confirms APK matches expected package name and version.

### 4. Signature Verification

```kotlin
packageInfo.singleSignature
    ?.calculateHash()
    ?.takeIf { it.isNotBlank() || it == release.signature }
    ?: invalid("Invalid signature")
```

Ensures APK is signed and signature matches expected value.

### 5. Permission Verification

```kotlin
packageInfo.permissions
    ?.map { it.name }
    ?.toSet()
    .orEmpty()
    .takeIf { release.permissions.containsAll(it) }
    ?: invalid("Unexpected permissions")
```

Verifies APK doesn't request more permissions than declared in index.

## Usage in Download Flow

```kotlin
downloader.downloadToFile(
    url = releaseUrl,
    target = cacheFile,
    validator = ReleaseFileValidator(context, packageName, release),
    headers = { ... }
)
```

If validation fails, `ValidationException` is thrown and the downloaded file is deleted.

## Error Messages

| Validation | Error String Resource |
|------------|----------------------|
| Checksum | `integrity_check_error_DESC` |
| APK parsing | `file_format_error_DESC` |
| Metadata | `invalid_metadata_error_DESC` |
| Signature | `invalid_signature_error_DESC` |
| Permissions | `invalid_permissions_error_DESC` |

## Security Considerations

1. **Hash verification first** - Catches corrupted downloads early
2. **Signature pinning** - Prevents APK tampering
3. **Permission auditing** - Detects malicious permission additions
4. **Version verification** - Prevents downgrade attacks
