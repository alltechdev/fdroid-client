# Service Connection

Generic service connection helper for bound services.

## Overview

**File:** `app/src/main/kotlin/com/looker/droidify/service/Connection.kt`

Provides a reusable pattern for binding to Android services.

## Class Definition

```kotlin
class Connection<B : IBinder, S : ConnectionService<B>>(
    private val serviceClass: Class<S>,
    private val onBind: ((Connection<B, S>, B) -> Unit)? = null,
    private val onUnbind: ((Connection<B, S>, B) -> Unit)? = null
) : ServiceConnection
```

### Type Parameters

| Parameter | Description |
|-----------|-------------|
| `B` | IBinder type returned by the service |
| `S` | Service class extending ConnectionService |

### Constructor Parameters

| Parameter | Description |
|-----------|-------------|
| `serviceClass` | Class of the service to bind to |
| `onBind` | Callback when service connects |
| `onUnbind` | Callback when service disconnects |

## Implementation

```kotlin
class Connection<B : IBinder, S : ConnectionService<B>>(...) : ServiceConnection {
    var binder: B? = null
        private set

    private fun handleUnbind() {
        binder?.let {
            binder = null
            onUnbind?.invoke(this, it)
        }
    }

    override fun onServiceConnected(componentName: ComponentName, binder: IBinder) {
        @Suppress("UNCHECKED_CAST")
        binder as B
        this.binder = binder
        onBind?.invoke(this, binder)
    }

    override fun onServiceDisconnected(componentName: ComponentName) {
        handleUnbind()
    }

    fun bind(context: Context) {
        context.bindService(Intent(context, serviceClass), this, Context.BIND_AUTO_CREATE)
    }

    fun unbind(context: Context) {
        context.unbindService(this)
        handleUnbind()
    }
}
```

## Usage

### With SyncService

```kotlin
class SyncManager {
    private val syncConnection = Connection<SyncService.Binder, SyncService>(
        SyncService::class.java,
        onBind = { connection, binder ->
            // Service connected
            binder.startSync()
        },
        onUnbind = { connection, binder ->
            // Service disconnected
        }
    )

    fun startSync(context: Context) {
        syncConnection.bind(context)
    }

    fun stopSync(context: Context) {
        syncConnection.unbind(context)
    }
}
```

### With DownloadService

```kotlin
class DownloadManager {
    private val downloadConnection = Connection<DownloadService.Binder, DownloadService>(
        DownloadService::class.java,
        onBind = { _, binder ->
            binder.download(packageName, url)
        }
    )

    fun startDownload(context: Context, packageName: String, url: String) {
        downloadConnection.bind(context)
    }
}
```

## ConnectionService Base Class

**File:** `service/ConnectionService.kt`

```kotlin
abstract class ConnectionService<B : IBinder> : Service() {
    abstract override fun onBind(intent: Intent): B
}
```

Services that use this connection pattern extend `ConnectionService`:

```kotlin
class SyncService : ConnectionService<SyncService.Binder>() {
    inner class Binder : android.os.Binder() {
        fun startSync(repos: List<Repository>) { ... }
        fun cancelSync() { ... }
    }

    override fun onBind(intent: Intent): Binder = Binder()
}
```

## Lifecycle

```
┌─────────────────────────────────────────────────────────────┐
│                    Client (Activity/Fragment)                │
└─────────────────────────┬───────────────────────────────────┘
                          │
                          │ connection.bind(context)
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                    Android System                            │
│  - Creates service if needed                                │
│  - Calls service.onBind()                                   │
│  - Returns IBinder                                          │
└─────────────────────────┬───────────────────────────────────┘
                          │
                          │ onServiceConnected(binder)
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                    Connection                                │
│  - Stores binder reference                                  │
│  - Calls onBind callback                                    │
└─────────────────────────┬───────────────────────────────────┘
                          │
                          │ Use binder methods
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                    Service                                   │
│  - Executes operations                                      │
│  - Reports progress via binder                              │
└─────────────────────────────────────────────────────────────┘
```

## Benefits

1. **Type Safety** - Generic types ensure correct binder type
2. **Reusable** - Single pattern for all bound services
3. **Callbacks** - Easy integration with UI via onBind/onUnbind
4. **Clean API** - Simple bind()/unbind() methods

## Example: Fragment Usage

```kotlin
class TabsFragment : Fragment() {
    private val syncConnection = Connection<SyncService.Binder, SyncService>(
        SyncService::class.java,
        onBind = { _, binder ->
            observeSyncState(binder)
        }
    )

    override fun onStart() {
        super.onStart()
        syncConnection.bind(requireContext())
    }

    override fun onStop() {
        super.onStop()
        syncConnection.unbind(requireContext())
    }

    private fun observeSyncState(binder: SyncService.Binder) {
        // Observe sync progress
    }
}
```
