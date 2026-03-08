# Dependency Injection

Droid-ify uses [Hilt](https://dagger.dev/hilt/) for dependency injection. All modules are located in the `di/` package.

## Module Overview

| Module | Scope | Purpose |
|--------|-------|---------|
| `CoroutinesModule` | Singleton | Coroutine dispatchers and scopes |
| `DatabaseModule` | Singleton | Room database and DAOs |
| `DatastoreModule` | Singleton | DataStore, settings, encryption |
| `NetworkModule` | Singleton | HTTP client and downloader |
| `RepoModule` | (none) | Repository classes |
| `InstallModule` | Singleton | Install manager |
| `SyncableModule` | Singleton | Sync implementations |
| `HandlersModule` | ViewModel | String handler for ViewModels |

## CoroutinesModule

Provides coroutine infrastructure.

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object CoroutinesModule {

    @Provides
    @IoDispatcher
    fun providesIODispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @DefaultDispatcher
    fun providesDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default

    @Provides
    @Singleton
    @ApplicationScope
    fun providesCoroutineScope(
        @DefaultDispatcher dispatcher: CoroutineDispatcher
    ): CoroutineScope = CoroutineScope(SupervisorJob() + dispatcher)
}
```

### Qualifiers

| Qualifier | Type | Usage |
|-----------|------|-------|
| `@IoDispatcher` | `CoroutineDispatcher` | I/O operations (network, database, files) |
| `@DefaultDispatcher` | `CoroutineDispatcher` | CPU-intensive work |
| `@ApplicationScope` | `CoroutineScope` | App-lifetime coroutines |

### Usage

```kotlin
class MyRepository @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    suspend fun fetchData() = withContext(ioDispatcher) {
        // I/O work here
    }
}
```

## DatabaseModule

Provides Room database and all DAOs.

**Provided dependencies:**

| Type | Scope | Description |
|------|-------|-------------|
| `AtdDatabase` | Singleton | Main Room database |
| `AppDao` | Singleton | App queries |
| `RepoDao` | Singleton | Repository queries |
| `AuthDao` | Singleton | Authentication data |
| `InstalledDao` | Singleton | Installed apps |
| `IndexDao` | Singleton | Index insertion |
| `RBLogDao` | Singleton | Reproducible build logs |
| `DownloadStatsDao` | Singleton | Download statistics |
| `PrivacyRepository` | Singleton | Privacy-related data |

### Adding a New DAO

1. Create the DAO interface in `data/local/dao/`
2. Add abstract function to `AtdDatabase`
3. Add provider in `DatabaseModule`:

```kotlin
@Singleton
@Provides
fun provideMyDao(db: AtdDatabase): MyDao = db.myDao()
```

## DatastoreModule

Provides preferences and settings infrastructure.

**Provided dependencies:**

| Type | Scope | Description |
|------|-------|-------------|
| `DataStore<Settings>` | Singleton | Proto DataStore (legacy, for migration) |
| `DataStore<Preferences>` | Singleton | Preferences DataStore |
| `Exporter<Settings>` | Singleton | Settings import/export |
| `EncryptionStorage` | Singleton | Encrypted key storage |
| `SettingsRepository` | Singleton | Settings access interface |

### Migration

The module handles migration from Proto DataStore to Preferences DataStore:

```kotlin
@Provides
fun providePreferenceDatastore(
    @ApplicationContext context: Context,
    oldDatastore: DataStore<Settings>,  // Proto
): DataStore<Preferences> = PreferenceDataStoreFactory.create(
    migrations = listOf(
        ProtoToPreferenceMigration(oldDatastore)
    )
) {
    context.preferencesDataStoreFile(SETTINGS)
}
```

## NetworkModule

Provides HTTP client and downloader.

**Provided dependencies:**

| Type | Scope | Description |
|------|-------|-------------|
| `HttpClient` | Singleton | Ktor HTTP client with OkHttp engine |
| `Downloader` | Singleton | File download abstraction |

### HTTP Client Configuration

```kotlin
@Provides
fun provideHttpClient(settingsRepository: SettingsRepository): HttpClient {
    val proxyPreference = runBlocking { settingsRepository.getInitial().proxy }
    val engine = OkHttp.create { proxy = proxyPreference.toProxy() }
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
```

**Note:** Proxy settings are read at app startup. Changing proxy requires app restart.

## RepoModule

Provides repository classes (not singleton-scoped for flexibility).

**Provided dependencies:**

| Type | Description |
|------|-------------|
| `RepoRepository` | F-Droid repository management |
| `AppRepository` | App data access |
| `InstalledRepository` | Installed app tracking |

## InstallModule

Provides installation infrastructure.

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object InstallModule {

    @Singleton
    @Provides
    fun providesInstaller(
        @ApplicationContext context: Context,
        settingsRepository: SettingsRepository
    ): InstallManager = InstallManager(context, settingsRepository)
}
```

## SyncableModule

Provides sync implementations.

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object SyncableModule {

    @Singleton
    @Provides
    fun provideSyncable(
        @ApplicationContext context: Context,
    ): Syncable<IndexV2> = LocalSyncable(context)
}
```

## HandlersModule

ViewModel-scoped utilities.

```kotlin
@Module
@InstallIn(ViewModelComponent::class)
object HandlersModule {
    @Provides
    @ViewModelScoped
    fun provideStringHandler(
        @ApplicationContext context: Context,
    ): StringHandler = StringHandler(context)
}
```

`StringHandler` allows ViewModels to access string resources without holding Activity context.

## Adding New Dependencies

### Step 1: Choose the Right Module

| Dependency Type | Module | Scope |
|-----------------|--------|-------|
| Database/DAO | `DatabaseModule` | Singleton |
| Network-related | `NetworkModule` | Singleton |
| Settings/Preferences | `DatastoreModule` | Singleton |
| Repository | `RepoModule` | Unscoped |
| ViewModel utility | `HandlersModule` | ViewModelScoped |

### Step 2: Add Provider

```kotlin
@Provides
@Singleton  // if singleton-scoped
fun provideMyDependency(
    // inject other dependencies as parameters
): MyDependency = MyDependencyImpl(...)
```

### Step 3: Inject in Consumer

```kotlin
class MyClass @Inject constructor(
    private val myDependency: MyDependency,
)
```

## Testing

For testing, use Hilt's testing utilities:

```kotlin
@HiltAndroidTest
class MyTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var myDependency: MyDependency

    @Before
    fun setup() {
        hiltRule.inject()
    }
}
```

Replace dependencies in tests:

```kotlin
@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [NetworkModule::class]
)
object FakeNetworkModule {
    @Provides
    fun provideDownloader(): Downloader = FakeDownloader()
}
```

## Removed

| Feature | Removal Doc |
|---------|-------------|
| Package: `com.looker.droidify` | [package-rename.md](../changes/package-rename.md) |
| `ProxyPreference` injection | [proxy-and-backup.md](../removal/proxy-and-backup.md) |
| `SettingsExporter` injection | [proxy-and-backup.md](../removal/proxy-and-backup.md) |
| `CustomButtonRepository` injection | [custom-buttons-and-settings.md](../removal/custom-buttons-and-settings.md) |

## Changes

| Change | Change Doc |
|--------|------------|
| Class renames (AtdStore, AtdDatabase, AtdTheme) | [package-rename.md](../changes/package-rename.md) |
| App branding Droid-ify → ATD Store | [app-branding.md](../changes/app-branding.md) |
