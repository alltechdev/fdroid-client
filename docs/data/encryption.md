# Encryption

Cryptographic utilities for secure storage and data integrity.

## Overview

**Location:** `app/src/main/kotlin/com/looker/droidify/data/encryption/`

| File | Purpose |
|------|---------|
| `AesEncryption.kt` | AES-256-CBC encryption for credentials |
| `Sha256.kt` | SHA-256 hashing utilities |
| `EncryptionStorage.kt` | Secure key storage |

## AES Encryption

**File:** `data/encryption/AesEncryption.kt`

### Configuration

```kotlin
private const val KEY_SIZE = 256
private const val IV_SIZE = 16
private const val ALGORITHM = "AES"
private const val TRANSFORMATION = "AES/CBC/PKCS5Padding"
```

### Key Class

```kotlin
@JvmInline
value class Key(val secretKey: ByteArray) {
    val spec: SecretKeySpec
        get() = SecretKeySpec(secretKey, ALGORITHM)

    fun encrypt(input: String): Pair<Encrypted, ByteArray> {
        val iv = generateIV()
        val ivSpec = IvParameterSpec(iv)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, spec, ivSpec)
        val encrypted = cipher.doFinal(input.toByteArray())
        return Encrypted(Base64.encode(encrypted)) to iv
    }
}

fun Key() = Key(
    with(KeyGenerator.getInstance(ALGORITHM)) {
        init(KEY_SIZE)
        generateKey().encoded
    }
)
```

### Encrypted Class

```kotlin
@JvmInline
value class Encrypted(val value: String) {
    fun decrypt(key: Key, iv: ByteArray): String {
        val ivSpec = IvParameterSpec(iv)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key.spec, ivSpec)
        val decrypted = cipher.doFinal(Base64.decode(value))
        return String(decrypted)
    }
}
```

### Usage

```kotlin
// Generate a new key
val key = Key()

// Encrypt data
val (encrypted, iv) = key.encrypt("secret password")

// Store encrypted value and IV
saveToStorage(encrypted.value, iv)

// Later, decrypt
val decrypted = encrypted.decrypt(key, iv)
```

## SHA-256 Hashing

**File:** `data/encryption/Sha256.kt`

### File Hashing

```kotlin
fun sha256(file: File): ByteArray {
    val digest = MessageDigest.getInstance("SHA-256")
    file.inputStream().use { input ->
        val buffer = ByteArray(8192)
        var read: Int
        while (input.read(buffer).also { read = it } != -1) {
            digest.update(buffer, 0, read)
        }
    }
    return digest.digest()
}
```

### Byte Array Hashing

```kotlin
fun sha256(bytes: ByteArray): ByteArray {
    return MessageDigest.getInstance("SHA-256").digest(bytes)
}
```

### Hex Conversion

```kotlin
fun ByteArray.hex(): String = joinToString("") { "%02x".format(it) }
```

### Usage

```kotlin
// Hash a downloaded APK
val fileHash = sha256(downloadedFile).hex()

// Verify against expected hash
if (!fileHash.equals(release.hash, ignoreCase = true)) {
    throw IntegrityException("Hash mismatch")
}
```

## Use Cases

### Repository Authentication

Credentials are encrypted before storage:

```kotlin
class AuthenticationEntity(
    val repoId: Long,
    val encryptedUsername: String,
    val encryptedPassword: String,
    val iv: ByteArray
)
```

### APK Integrity Verification

```kotlin
class ReleaseFileValidator(...) : FileValidator {
    override suspend fun validate(file: File) {
        val checksum = sha256(file).hex()
        if (!checksum.equals(release.hash, ignoreCase = true)) {
            invalid("Integrity check failed")
        }
    }
}
```

### Index Signature Verification

JAR file certificates are hashed:

```kotlin
val certHash = sha256(certificate.encoded).hex()
if (certHash != repo.fingerprint) {
    throw SignatureException("Fingerprint mismatch")
}
```

## Security Considerations

1. **Random IV** - Each encryption uses a fresh IV from SecureRandom
2. **Key Storage** - Keys stored in Android Keystore (EncryptionStorage)
3. **No Hardcoded Keys** - Keys generated at runtime
4. **CBC Mode** - Provides confidentiality with proper IV handling

## Integration

### With DataStore

```kotlin
// Encrypt credentials before saving
val (encrypted, iv) = key.encrypt(password)
dataStore.updateData { prefs ->
    prefs.copy(
        encryptedPassword = encrypted.value,
        passwordIv = iv
    )
}

// Decrypt when reading
val password = Encrypted(prefs.encryptedPassword)
    .decrypt(key, prefs.passwordIv)
```

### With Room

```kotlin
@Entity(tableName = "authentication")
data class AuthenticationEntity(
    @PrimaryKey val repoId: Long,
    val encryptedUsername: String,
    val encryptedPassword: String,
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val iv: ByteArray
)
```
