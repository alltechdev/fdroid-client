# File Validation

The validation module ensures downloaded files are authentic and unmodified. This includes hash verification, signature checking, and metadata validation.

## Overview

**Key Files:**
- `network/validation/FileValidator.kt` - Validator interface
- `network/validation/ValidationException.kt` - Validation error
- `service/ReleaseFileValidator.kt` - APK validation implementation
- `data/encryption/Sha256.kt` - SHA-256 hashing

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    File Download                         │
│                         │                                │
│                         ▼                                │
│  ┌─────────────────────────────────────────────────────┐│
│  │                  FileValidator                       ││
│  │                                                      ││
│  │  ┌───────────────┐  ┌───────────────┐              ││
│  │  │ Hash Check    │  │ Signature     │              ││
│  │  │ (SHA-256)     │  │   Check       │              ││
│  │  └───────┬───────┘  └───────┬───────┘              ││
│  │          │                  │                       ││
│  │          ▼                  ▼                       ││
│  │  ┌───────────────┐  ┌───────────────┐              ││
│  │  │ Metadata      │  │ Permissions   │              ││
│  │  │  Check        │  │   Check       │              ││
│  │  └───────────────┘  └───────────────┘              ││
│  └─────────────────────────────────────────────────────┘│
│                         │                                │
│                         ▼                                │
│              Pass ──► Continue                           │
│              Fail ──► Delete file + throw exception      │
└─────────────────────────────────────────────────────────┘
```

## FileValidator Interface

```kotlin
interface FileValidator {
    @Throws(ValidationException::class)
    suspend fun validate(file: File)
}
```

### Implementation Pattern

```kotlin
class MyValidator : FileValidator {
    override suspend fun validate(file: File) {
        // Check condition
        if (!isValid(file)) {
            invalid("Validation failed: reason")
        }
        // More checks...
    }
}
```

## ValidationException

Thrown when validation fails:

```kotlin
class ValidationException(override val message: String) : Exception(message)

// Helper function to throw
inline fun invalid(message: String): Nothing = throw ValidationException(message)
```

### Behavior on Failure

When validation fails:
1. `ValidationException` is thrown
2. Downloaded file is deleted
3. `NetworkResponse.Error.Validation` is returned
4. Error message shown in notification

## ReleaseFileValidator

The main APK validator used during app downloads:

```kotlin
class ReleaseFileValidator(
    private val context: Context,
    private val packageName: String,
    private val release: Release,
) : FileValidator
```

### Validation Steps

| Step | Check | Error Message |
|------|-------|---------------|
| 1 | SHA-256 hash matches | `integrity_check_error_DESC` |
| 2 | Valid APK format | `file_format_error_DESC` |
| 3 | Package name and version | `invalid_metadata_error_DESC` |
| 4 | Signature matches | `invalid_signature_error_DESC` |
| 5 | Permissions subset | `invalid_permissions_error_DESC` |

### Implementation

```kotlin
override suspend fun validate(file: File) {
    // 1. Hash verification
    val checksum = sha256(file).hex()
    if (!checksum.equals(release.hash, ignoreCase = true)) {
        invalid(getString(strings.integrity_check_error_DESC))
    }

    // 2. APK format check
    val packageInfo = context.packageManager.getPackageArchiveInfoCompat(file.path)
        ?: invalid(getString(strings.file_format_error_DESC))

    // 3. Metadata verification
    if (packageInfo.packageName != packageName ||
        packageInfo.versionCodeCompat != release.versionCode
    ) {
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
```

## SHA-256 Hashing

Located in `data/encryption/Sha256.kt`:

```kotlin
private val DIGEST = MessageDigest.getInstance("SHA-256")

fun sha256(data: String): ByteArray = DIGEST.digest(data.toByteArray())
fun sha256(data: ByteArray): ByteArray = DIGEST.digest(data)
fun sha256(data: File): ByteArray = data.inputStream().use(::sha256)

fun sha256(data: InputStream): ByteArray = synchronized(DIGEST) {
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var bytesRead = data.read(buffer)
    while (bytesRead >= 0) {
        DIGEST.update(buffer, 0, bytesRead)
        bytesRead = data.read(buffer)
    }
    DIGEST.digest()
}
```

### Thread Safety

The `MessageDigest` is synchronized for concurrent use:

```kotlin
synchronized(DIGEST) {
    // ... digest operations
}
```

## Signature Verification

APK signatures are extracted and verified:

```kotlin
// Extension for getting signature hash
val PackageInfo.singleSignature: Signature?
    get() = if (SdkCheck.isPie) {
        signingInfo?.apkContentsSigners?.firstOrNull()
    } else {
        @Suppress("DEPRECATION")
        signatures?.firstOrNull()
    }

// Calculate signature hash
fun Signature.calculateHash(): String =
    sha256(toByteArray()).hex()
```

### Hash Comparison

```kotlin
val downloadedSignature = packageInfo.singleSignature?.calculateHash()
val expectedSignature = release.signature

if (downloadedSignature != expectedSignature) {
    invalid("Signature mismatch")
}
```

## Fingerprint Model

Repository fingerprints for verification:

```kotlin
@JvmInline
value class Fingerprint(val value: String) {
    init {
        require(value.length == 64 && value.all { it.isHexDigit() }) {
            "Invalid fingerprint format"
        }
    }
}

// Hex conversion
fun ByteArray.hex(): String = joinToString("") { "%02X".format(it) }
```

## Usage in Downloads

The validator is passed to the downloader:

```kotlin
// In DownloadService
val releaseValidator = ReleaseFileValidator(
    context = this,
    packageName = task.packageName,
    release = task.release,
)

val response = downloader.downloadToFile(
    url = task.url,
    target = target,
    validator = releaseValidator,  // Validator passed here
)

// Validation runs after download completes
// On failure: file deleted, ValidationException thrown
```

## Error Handling

Validation errors are displayed to users:

```kotlin
when (response) {
    is NetworkResponse.Error.Validation -> {
        // Show error notification
        val errorType = ErrorType.Validation(response.exception)
        showNotificationError(task, errorType)
    }
}
```

### Error Notification

```kotlin
val description = when (errorType) {
    is ErrorType.Validation -> errorType.exception.message
    // Other errors...
}

notificationBuilder
    .setContentTitle(getString(R.string.could_not_validate_FORMAT, appName))
    .setContentText(description)
```

## Index Validation

F-Droid index files are validated using JAR signatures:

```kotlin
// In JarFile.kt
fun verify(jarFile: JarFile): Boolean {
    // JAR signature verification
    // Checks META-INF/*.SF, *.RSA/*.DSA
}
```

## Creating Custom Validators

```kotlin
class CustomValidator(
    private val expectedHash: String,
    private val minSize: Long,
) : FileValidator {

    override suspend fun validate(file: File) {
        // Size check
        if (file.length() < minSize) {
            invalid("File too small: ${file.length()} < $minSize")
        }

        // Hash check
        val actualHash = sha256(file).hex()
        if (!actualHash.equals(expectedHash, ignoreCase = true)) {
            invalid("Hash mismatch: expected $expectedHash, got $actualHash")
        }
    }
}
```

## Security Considerations

| Check | Protection Against |
|-------|-------------------|
| SHA-256 hash | File tampering, corruption |
| APK signature | Package substitution attacks |
| Package name | Wrong package delivered |
| Version code | Downgrade attacks |
| Permissions | Permission escalation |

## Best Practices

1. **Always validate downloads** - Never skip validation for untrusted sources
2. **Delete on failure** - Invalid files should be removed immediately
3. **Atomic operations** - Download to temp file, validate, then rename
4. **Clear error messages** - Help users understand validation failures
