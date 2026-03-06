# Proxy Configuration

Droid-ify supports HTTP and SOCKS proxy configuration for all network requests.

## Overview

**Key Files:**
- `datastore/model/ProxyPreference.kt` - Proxy settings model
- `datastore/model/ProxyType.kt` - Proxy type enum
- `di/NetworkModule.kt` - Proxy application to HTTP client

## Proxy Types

```kotlin
enum class ProxyType {
    DIRECT,  // No proxy
    HTTP,    // HTTP/HTTPS proxy
    SOCKS    // SOCKS proxy (v4/v5)
}
```

## ProxyPreference Model

```kotlin
@Serializable
data class ProxyPreference(
    val type: ProxyType = ProxyType.DIRECT,
    val host: String = "localhost",
    val port: Int = 9050  // Default Tor port
) {
    fun update(
        newType: ProxyType? = null,
        newHost: String? = null,
        newPort: Int? = null
    ): ProxyPreference = copy(
        type = newType ?: type,
        host = newHost ?: host,
        port = newPort ?: port
    )
}
```

## HTTP Client Integration

Proxy is configured in `NetworkModule`:

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Singleton
    @Provides
    fun provideHttpClient(settingsRepository: SettingsRepository): HttpClient {
        val proxyPreference = runBlocking { settingsRepository.getInitial().proxy }
        val engine = OkHttp.create {
            proxy = proxyPreference.toProxy()
        }
        return HttpClient(engine) {
            install(UserAgent) {
                agent = "Droid-ify/${VERSION_NAME}-${BUILD_TYPE}"
            }
            install(HttpTimeout) {
                connectTimeoutMillis = 30_000L
                socketTimeoutMillis = 15_000L
            }
        }
    }
}
```

### Proxy Conversion

```kotlin
private fun ProxyPreference.toProxy(): Proxy {
    val socketAddress = when (type) {
        ProxyType.DIRECT -> null
        ProxyType.HTTP, ProxyType.SOCKS -> {
            try {
                InetSocketAddress.createUnresolved(host, port)
            } catch (e: IllegalArgumentException) {
                log(e)
                null
            }
        }
    }

    val androidProxyType = when (type) {
        ProxyType.DIRECT -> Proxy.Type.DIRECT
        ProxyType.HTTP -> Proxy.Type.HTTP
        ProxyType.SOCKS -> Proxy.Type.SOCKS
    }

    return socketAddress?.let {
        Proxy(androidProxyType, it)
    } ?: Proxy.NO_PROXY
}
```

## Settings Repository API

```kotlin
interface SettingsRepository {
    suspend fun setProxyType(proxyType: ProxyType)
    suspend fun setProxyHost(proxyHost: String)
    suspend fun setProxyPort(proxyPort: Int)
}
```

### Implementation

```kotlin
override suspend fun setProxyType(proxyType: ProxyType) =
    PROXY_TYPE.update(proxyType.name)

override suspend fun setProxyHost(proxyHost: String) =
    PROXY_HOST.update(proxyHost)

override suspend fun setProxyPort(proxyPort: Int) =
    PROXY_PORT.update(proxyPort)
```

## Settings Screen

Proxy is configured in Settings:

```kotlin
// In SettingsScreen.kt
SettingHeader(title = stringResource(R.string.proxy))

DropdownSettingItem(
    title = stringResource(R.string.proxy_type),
    currentValue = settings.proxy.type,
    values = ProxyType.entries,
    onValueChange = viewModel::setProxyType,
)

if (settings.proxy.type != ProxyType.DIRECT) {
    TextInputSettingItem(
        title = stringResource(R.string.proxy_host),
        value = settings.proxy.host,
        onValueChange = viewModel::setProxyHost,
    )

    TextInputSettingItem(
        title = stringResource(R.string.proxy_port),
        value = settings.proxy.port.toString(),
        onValueChange = { viewModel.setProxyPort(it.toIntOrNull() ?: 0) },
        keyboardType = KeyboardType.Number,
    )
}
```

## Common Proxy Configurations

### Tor (SOCKS5)

```
Type: SOCKS
Host: 127.0.0.1
Port: 9050
```

### Orbot (Tor on Android)

```
Type: SOCKS
Host: 127.0.0.1
Port: 9050  (or 9150 for Orbot's HTTP proxy)
```

### HTTP Proxy

```
Type: HTTP
Host: proxy.example.com
Port: 8080
```

### No Proxy

```
Type: DIRECT
```

## Persistence

Proxy settings are stored in DataStore:

```kotlin
companion object PreferencesKeys {
    val PROXY_TYPE = stringPreferencesKey("key_proxy_type")
    val PROXY_HOST = stringPreferencesKey("key_proxy_host")
    val PROXY_PORT = intPreferencesKey("key_proxy_port")
}
```

## Export/Import

Proxy settings are included in settings export:

```json
{
    "proxy": {
        "type": "SOCKS",
        "host": "127.0.0.1",
        "port": 9050
    }
}
```

## Error Handling

Invalid proxy configuration falls back to direct connection:

```kotlin
try {
    InetSocketAddress.createUnresolved(host, port)
} catch (e: IllegalArgumentException) {
    log(e)
    null  // Falls back to Proxy.NO_PROXY
}
```

## Limitations

1. **App Restart Required** - Proxy changes require app restart (HTTP client is a singleton)
2. **No Authentication** - Currently no support for proxy authentication
3. **All-or-Nothing** - Proxy applies to all requests (no per-repo proxy)

## Future Improvements

- Hot reload proxy without restart
- Proxy authentication support
- Per-repository proxy settings
- Proxy auto-detection
