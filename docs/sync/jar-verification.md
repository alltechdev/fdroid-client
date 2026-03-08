# JAR Verification

Utilities for verifying signed JAR files from F-Droid repositories.

## Overview

**File:** `app/src/main/kotlin/com/atd/store/sync/utils/JarFile.kt`

Provides extension functions for working with signed JAR files and extracting code signer certificates.

## Extension Functions

### Open JAR File

```kotlin
fun File.toJarFile(verify: Boolean = true): JarFile = JarFile(this, verify)
```

Opens a file as a JAR with optional signature verification.

### Get Code Signer

```kotlin
inline val JarEntry.codeSignerOrNull: CodeSigner?
    get() = codeSigners?.singleOrNull()

inline val JarEntry.codeSigner: CodeSigner
    get() = codeSignerOrNull
        ?: error("index.jar must be signed by a single code signer")
```

F-Droid index JARs must be signed by exactly one signer.

### Get Certificate

```kotlin
inline val CodeSigner.certificateOrNull: Certificate?
    get() = signerCertPath?.certificates?.singleOrNull()

inline val CodeSigner.certificate: Certificate
    get() = certificateOrNull
        ?: error("index.jar code signer should have only one certificate")
```

## Usage in Sync

### Verifying Index JAR

```kotlin
suspend fun verifyAndParseIndex(jarFile: File, expectedFingerprint: String): IndexV1 {
    val jar = jarFile.toJarFile(verify = true)

    jar.use { jarFile ->
        val indexEntry = jarFile.getJarEntry("index-v1.json")
            ?: error("Missing index-v1.json in JAR")

        // Read entry to trigger verification
        jarFile.getInputStream(indexEntry).use { stream ->
            stream.readBytes()
        }

        // Verify fingerprint
        val certificate = indexEntry.codeSigner.certificate
        val fingerprint = sha256(certificate.encoded).hex()

        if (!fingerprint.equals(expectedFingerprint, ignoreCase = true)) {
            throw SignatureException("Repository fingerprint mismatch")
        }

        // Parse index
        jarFile.getInputStream(indexEntry).use { stream ->
            parseIndex(stream)
        }
    }
}
```

### In RepositoryUpdater

```kotlin
// Step 1: Download JAR
val jarFile = downloadIndex(repo)

// Step 2: Open and verify
jarFile.toJarFile().use { jar ->
    val entry = jar.getJarEntry("index-v1.json")

    // Reading triggers signature verification
    jar.getInputStream(entry).bufferedReader().use { reader ->
        val content = reader.readText()
    }

    // Extract certificate for fingerprint check
    val cert = entry.codeSigner.certificate
    val fingerprint = sha256(cert.encoded).hex()

    // Compare with stored fingerprint
    if (repo.fingerprint.isEmpty()) {
        // First sync - trust on first use
        updateRepoFingerprint(fingerprint)
    } else if (fingerprint != repo.fingerprint) {
        throw SecurityException("Fingerprint changed!")
    }
}
```

## Verification Process

```
┌─────────────────────────────────────────────────────────────┐
│                   index-v1.jar                               │
│                                                              │
│  ┌─────────────────────────────────────────────────────┐    │
│  │  META-INF/MANIFEST.MF                                │    │
│  │  - SHA-256 hashes of all entries                    │    │
│  └─────────────────────────────────────────────────────┘    │
│                                                              │
│  ┌─────────────────────────────────────────────────────┐    │
│  │  META-INF/CERT.SF                                    │    │
│  │  - Signature file (signed hashes)                   │    │
│  └─────────────────────────────────────────────────────┘    │
│                                                              │
│  ┌─────────────────────────────────────────────────────┐    │
│  │  META-INF/CERT.RSA                                   │    │
│  │  - Certificate and signature                        │    │
│  └─────────────────────────────────────────────────────┘    │
│                                                              │
│  ┌─────────────────────────────────────────────────────┐    │
│  │  index-v1.json                                       │    │
│  │  - Actual index content                             │    │
│  └─────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘

Verification Steps:
1. JarFile(verify=true) validates all signatures
2. Reading entry triggers hash verification
3. CodeSigner extracted from verified entry
4. Certificate fingerprint compared to stored value
```

## Error Handling

### Single Signer Requirement

```kotlin
inline val JarEntry.codeSigner: CodeSigner
    get() = codeSignerOrNull
        ?: error("index.jar must be signed by a single code signer, Current: $codeSigners")
```

Throws if:
- JAR is unsigned
- JAR has multiple signers (suspicious)

### Single Certificate Requirement

```kotlin
inline val CodeSigner.certificate: Certificate
    get() = certificateOrNull
        ?: error("index.jar code signer should have only one certificate")
```

Throws if certificate chain is malformed.

## Security Considerations

1. **Always verify** - Use `verify = true` when opening JARs
2. **Single signer** - Reject multi-signed JARs
3. **Fingerprint pinning** - Compare certificate hash to stored fingerprint
4. **Trust on first use** - Only accept new fingerprint on first sync
5. **Read before trusting** - Must read entry content to trigger verification
