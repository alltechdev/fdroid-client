# Downloader

The downloader module provides HTTP networking capabilities using Ktor, supporting file downloads with progress tracking, resume capability, and validation.

## Overview

**Key Files:**
- `network/Downloader.kt` - Interface definition
- `network/KtorDownloader.kt` - Ktor implementation
- `network/NetworkResponse.kt` - Response types
- `network/header/HeadersBuilder.kt` - Header construction
- `sync/common/IndexDownloader.kt` - Index-specific download helper

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                      Downloader                          │
│                     (interface)                          │
│                                                          │
│  ┌─────────────────────────────────────────────────────┐│
│  │                   KtorDownloader                     ││
│  │                                                      ││
│  │  HttpClient ──► Request ──► Response ──► File       ││
│  │       │                         │                    ││
│  │       │                         ▼                    ││
│  │       │                   FileValidator              ││
│  │       │                         │                    ││
│  │       ▼                         ▼                    ││
│  │  ProgressListener         NetworkResponse            ││
│  └─────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────┘
```

## Downloader Interface

```kotlin
interface Downloader {

    suspend fun headCall(
        url: String,
        headers: HeadersBuilder.() -> Unit = {},
    ): NetworkResponse

    suspend fun downloadToFile(
        url: String,
        target: File,
        validator: FileValidator? = null,
        headers: HeadersBuilder.() -> Unit = {},
        block: ProgressListener? = null,
    ): NetworkResponse
}

typealias ProgressListener = suspend (bytesReceived: DataSize, contentLength: DataSize?) -> Unit
```

### Methods

| Method | Purpose |
|--------|---------|
| `headCall` | HEAD request to check resource metadata |
| `downloadToFile` | Download file with optional validation and progress |

## KtorDownloader

The Ktor-based implementation with resume support:

```kotlin
internal class KtorDownloader(
    private val client: HttpClient,
    private val dispatcher: CoroutineDispatcher,
) : Downloader
```

### Download Flow

```
1. Open/Create target file
2. Check existing file size (for resume)
3. Set Range header if resuming
4. Execute GET request
5. Stream response to file
6. Run validator if provided
7. Return NetworkResponse
```

### Resume Support

Downloads automatically resume from the last position:

```kotlin
val fileSize = target.length()
val request = request(url = url, fileSize = fileSize) {
    if (fileSize > 0) inRange(fileSize)  // Range: bytes=fileSize-
    headers()
}
```

### Error Handling

```kotlin
try {
    // Download logic
} catch (e: SocketTimeoutException) {
    NetworkResponse.Error.SocketTimeout(e)
} catch (e: ConnectTimeoutException) {
    NetworkResponse.Error.ConnectionTimeout(e)
} catch (e: IOException) {
    NetworkResponse.Error.IO(e)
} catch (e: ValidationException) {
    target.delete()  // Delete invalid file
    NetworkResponse.Error.Validation(e)
} catch (e: Exception) {
    NetworkResponse.Error.Unknown(e)
}
```

## NetworkResponse

Sealed interface representing download outcomes:

```kotlin
sealed interface NetworkResponse {

    sealed interface Error : NetworkResponse {
        data class ConnectionTimeout(val exception: Exception) : Error
        data class SocketTimeout(val exception: Exception) : Error
        data class IO(val exception: Exception) : Error
        data class Validation(val exception: ValidationException) : Error
        data class Unknown(val exception: Exception) : Error
        data class Http(val statusCode: Int) : Error
    }

    data class Success(
        val statusCode: Int,
        val lastModified: Date?,
        val etag: String?
    ) : NetworkResponse
}
```

### Error Types

| Error | Cause |
|-------|-------|
| `ConnectionTimeout` | Connection could not be established |
| `SocketTimeout` | Read/write timeout during transfer |
| `IO` | General I/O error |
| `Validation` | File validation failed |
| `Http` | HTTP error (4xx, 5xx) |
| `Unknown` | Unexpected exception |

## Headers Builder

DSL for constructing HTTP headers:

```kotlin
interface HeadersBuilder {
    fun authentication(value: String)
    fun authentication(username: String, password: String)
    fun ifModifiedSince(date: Date)
    fun inRange(start: Long, end: Long? = null)
}
```

### Usage

```kotlin
downloader.downloadToFile(
    url = "https://example.com/file.apk",
    target = targetFile,
    headers = {
        authentication(username, password)
        ifModifiedSince(lastModified)
    }
)
```

## Index Downloader

Helper function for downloading F-Droid index files:

```kotlin
suspend fun Downloader.downloadIndex(
    context: Context,
    repo: Repo,
    fileName: String,
    url: String,
    diff: Boolean = false,
    onProgress: ProgressListener? = null,
): File
```

### Features

- Automatic authentication from repo settings
- Conditional requests using `If-Modified-Since`
- Caches to `Cache.getIndexFile()`

### Usage

```kotlin
val indexFile = downloader.downloadIndex(
    context = context,
    repo = repo,
    fileName = INDEX_V1_NAME,
    url = "${repo.address}/$INDEX_V1_NAME",
) { read, total ->
    // Progress callback
}
```

## Progress Tracking

Progress is reported via `ProgressListener`:

```kotlin
val response = downloader.downloadToFile(
    url = url,
    target = file,
) { bytesReceived, contentLength ->
    val percent = if (contentLength != null) {
        (bytesReceived.value * 100 / contentLength.value).toInt()
    } else {
        -1  // Unknown total
    }
    updateProgress(percent)
}
```

### DataSize

Wrapper for byte sizes with formatting:

```kotlin
@JvmInline
value class DataSize(val value: Long) {
    override fun toString(): String = formatSize(value)
}

// Extension for percentage calculation
infix fun Long.percentBy(total: Long): Int
infix fun DataSize.percentBy(total: DataSize?): Int
```

## Dependency Injection

Provided via Hilt in `NetworkModule`:

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideDownloader(
        client: HttpClient,
        @IoDispatcher dispatcher: CoroutineDispatcher,
    ): Downloader = KtorDownloader(client, dispatcher)
}
```

## Usage Example

```kotlin
@HiltViewModel
class DownloadViewModel @Inject constructor(
    private val downloader: Downloader,
) : ViewModel() {

    fun download(url: String, target: File) {
        viewModelScope.launch {
            val response = downloader.downloadToFile(
                url = url,
                target = target,
                validator = MyValidator(),
            ) { read, total ->
                _progress.value = read percentBy total
            }

            when (response) {
                is NetworkResponse.Success -> onSuccess()
                is NetworkResponse.Error.Validation -> onValidationError()
                is NetworkResponse.Error -> onError(response)
            }
        }
    }
}
```

## Thread Safety

- Downloads run on the provided `CoroutineDispatcher` (IO dispatcher)
- File operations use `NonCancellable` context for cleanup
- Each download is independent (no shared state)
