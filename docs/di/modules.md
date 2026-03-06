# Dependency Injection Modules

Hilt DI modules for app-wide dependency injection.

## Overview

**Directory:** `di/`

All modules use `@InstallIn(SingletonComponent::class)` for app-scoped singletons.

## CoroutinesModule

Provides coroutine dispatchers and scopes.

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

```kotlin
@Qualifier annotation class IoDispatcher
@Qualifier annotation class DefaultDispatcher
@Qualifier annotation class ApplicationScope
```

Usage:
```kotlin
class MyRepository @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @ApplicationScope private val scope: CoroutineScope
)
```

## NetworkModule

Configures HTTP client and downloader.

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Singleton
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

    @Singleton
    @Provides
    fun provideDownloader(
        httpClient: HttpClient,
        @IoDispatcher dispatcher: CoroutineDispatcher,
    ): Downloader = KtorDownloader(client = httpClient, dispatcher = dispatcher)
}
```

### Proxy Configuration

```kotlin
private fun ProxyPreference.toProxy(): Proxy {
    val socketAddress = when (type) {
        ProxyType.DIRECT -> null
        ProxyType.HTTP, ProxyType.SOCKS -> {
            InetSocketAddress.createUnresolved(host, port)
        }
    }
    val androidProxyType = when (type) {
        ProxyType.DIRECT -> Proxy.Type.DIRECT
        ProxyType.HTTP -> Proxy.Type.HTTP
        ProxyType.SOCKS -> Proxy.Type.SOCKS
    }
    return socketAddress?.let { Proxy(androidProxyType, it) } ?: Proxy.NO_PROXY
}
```

## DatabaseModule

Provides Room database and DAOs.

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Singleton
    @Provides
    fun provideDatabase(context: Context): DroidifyDatabase =
        droidifyDatabase(context)

    @Singleton @Provides
    fun provideAppDao(db: DroidifyDatabase): AppDao = db.appDao()

    @Singleton @Provides
    fun provideRepoDao(db: DroidifyDatabase): RepoDao = db.repoDao()

    @Singleton @Provides
    fun provideAuthDao(db: DroidifyDatabase): AuthDao = db.authDao()

    @Singleton @Provides
    fun provideInstallDao(db: DroidifyDatabase): InstalledDao = db.installedDao()

    @Singleton @Provides
    fun provideIndexDao(db: DroidifyDatabase): IndexDao = db.indexDao()

    @Singleton @Provides
    fun provideRBLogDao(db: DroidifyDatabase): RBLogDao = db.rbLogDao()

    @Singleton @Provides
    fun provideDownloadStatsDao(db: DroidifyDatabase): DownloadStatsDao =
        db.downloadStatsDao()

    @Singleton @Provides
    fun providePrivacyRepository(
        rblDao: RBLogDao,
        downloadStatsDao: DownloadStatsDao,
        settingsRepository: SettingsRepository,
    ): PrivacyRepository = PrivacyRepository(rblDao, downloadStatsDao, settingsRepository)
}
```

## DatastoreModule

Settings storage configuration.

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DatastoreModule {

    // Legacy Proto DataStore (for migration)
    @Singleton
    @Provides
    fun provideProtoDatastore(context: Context): DataStore<Settings> =
        DataStoreFactory.create(serializer = SettingsSerializer) {
            context.dataStoreFile("settings_file")
        }

    // Current Preferences DataStore
    @Singleton
    @Provides
    fun providePreferenceDatastore(
        context: Context,
        oldDatastore: DataStore<Settings>,
    ): DataStore<Preferences> = PreferenceDataStoreFactory.create(
        migrations = listOf(ProtoToPreferenceMigration(oldDatastore))
    ) {
        context.preferencesDataStoreFile("settings")
    }

    @Singleton
    @Provides
    fun provideEncryptionStorage(
        dataStore: DataStore<Preferences>,
        @IoDispatcher dispatcher: CoroutineDispatcher
    ): EncryptionStorage = EncryptionStorage(dataStore, dispatcher)

    @Singleton
    @Provides
    fun provideSettingsRepository(
        dataStore: DataStore<Preferences>,
        exporter: Exporter<Settings>
    ): SettingsRepository = PreferenceSettingsRepository(dataStore, exporter)
}
```

## RepoModule

Repository layer providers.

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object RepoModule {

    @Provides
    fun provideRepoRepository(
        repoDao: RepoDao,
        appDao: AppDao,
        authDao: AuthDao,
        indexDao: IndexDao,
        settingsRepository: SettingsRepository,
        encryptionStorage: EncryptionStorage,
        downloader: Downloader,
        context: Context,
        @IoDispatcher syncDispatcher: CoroutineDispatcher,
    ): RepoRepository = RepoRepository(...)

    @Provides
    fun provideAppRepository(
        appDao: AppDao,
        repoDao: RepoDao,
        settingsRepository: SettingsRepository,
    ): AppRepository = AppRepository(appDao, repoDao, settingsRepository)

    @Provides
    fun provideInstalledRepository(
        installedDao: InstalledDao,
    ): InstalledRepository = InstalledRepository(installedDao)
}
```

## InstallModule

Installation manager provider.

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object InstallModule {

    @Singleton
    @Provides
    fun providesInstaller(
        context: Context,
        settingsRepository: SettingsRepository
    ): InstallManager = InstallManager(context, settingsRepository)
}
```

## SyncableModule

Sync infrastructure provider.

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object SyncableModule {

    @Singleton
    @Provides
    fun provideSyncable(context: Context): Syncable<IndexV2> = LocalSyncable(context)
}
```

## HandlersModule

ViewModel-scoped handlers.

```kotlin
@Module
@InstallIn(ViewModelComponent::class)
object HandlersModule {

    @Provides
    @ViewModelScoped
    fun provideStringHandler(context: Context): StringHandler = StringHandler(context)
}
```

Note: Uses `ViewModelComponent` not `SingletonComponent` for ViewModel lifecycle.

## Dependency Graph

```
┌─────────────────────────────────────────────────────────────┐
│                    CoroutinesModule                          │
│  IoDispatcher, DefaultDispatcher, ApplicationScope          │
└─────────────────────────┬───────────────────────────────────┘
                          │
         ┌────────────────┼────────────────┐
         ▼                ▼                ▼
┌─────────────┐  ┌─────────────┐  ┌─────────────────┐
│NetworkModule│  │DatabaseModule│  │DatastoreModule  │
│ HttpClient  │  │ DAOs        │  │ Settings        │
│ Downloader  │  │ Database    │  │ Encryption      │
└──────┬──────┘  └──────┬──────┘  └────────┬────────┘
       │                │                   │
       └────────────────┼───────────────────┘
                        ▼
┌─────────────────────────────────────────────────────────────┐
│                      RepoModule                              │
│  RepoRepository, AppRepository, InstalledRepository         │
└─────────────────────────┬───────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                 InstallModule / SyncableModule               │
│  InstallManager, Syncable                                   │
└─────────────────────────────────────────────────────────────┘
```
