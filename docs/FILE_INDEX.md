# Complete File Index

Every Kotlin source file in the project (255 files total).

## Root Files

| File | Purpose |
|------|---------|
| `Droidify.kt` | Application class - DI init, receivers, startup |
| `MainActivity.kt` | Legacy UI host - fragment navigation, theming |

## compose/

### compose/appDetail/
| File | Purpose |
|------|---------|
| `AppDetailScreen.kt` | Compose app detail screen |
| `AppDetailViewModel.kt` | ViewModel for app details |
| `ReleaseItem.kt` | Release list item composable |
| `components/CustomButtonsRow.kt` | Custom buttons row |
| `components/PackageItem.kt` | Package version item |
| `navigation/AppDetailNavigation.kt` | Navigation graph entry |

### compose/appList/
| File | Purpose |
|------|---------|
| `AppListScreen.kt` | Compose app list screen |
| `AppListViewModel.kt` | ViewModel for app listing |
| `navigation/AppListNavigation.kt` | Navigation graph entry |

### compose/components/
| File | Purpose |
|------|---------|
| `BackButton.kt` | Back navigation button |
| `ButtonColors.kt` | Material button color utilities |

### compose/home/
| File | Purpose |
|------|---------|
| `HomeScreen.kt` | Home screen composable |
| `navigation/HomeNavigation.kt` | Navigation graph entry |

### compose/repoDetail/
| File | Purpose |
|------|---------|
| `RepoDetailScreen.kt` | Repository detail screen |
| `RepoDetailViewModel.kt` | ViewModel for repo details |
| `components/LastUpdatedCard.kt` | Last sync timestamp card |
| `components/UnsyncedRepoState.kt` | Unsynced repo indicator |
| `navigation/RepoDetailNavigation.kt` | Navigation entry |

### compose/repoEdit/
| File | Purpose |
|------|---------|
| `RepoEditScreen.kt` | Add/edit repository screen |
| `RepoEditViewModel.kt` | ViewModel for editing repos |
| `navigation/RepoEditNavigation.kt` | Navigation entry |

### compose/repoList/
| File | Purpose |
|------|---------|
| `RepoListScreen.kt` | Repository list screen |
| `RepoListViewModel.kt` | ViewModel for repo list |
| `navigation/RepoListNavigation.kt` | Navigation entry |

### compose/settings/
| File | Purpose |
|------|---------|
| `SettingsScreen.kt` | Settings screen composable |
| `SettingsViewModel.kt` | All settings logic |
| `navigation/SettingsNavigation.kt` | Navigation entry |
| `components/ActionSettingItem.kt` | Clickable action item |
| `components/CustomButtonEditor.kt` | Custom button editor dialog |
| `components/CustomButtonsSettingItem.kt` | Custom buttons list setting |
| `components/DropdownSettingItem.kt` | Dropdown selection |
| `components/SelectionSettingItem.kt` | Multi-option selection |
| `components/SettingHeader.kt` | Section header |
| `components/SwitchSettingItem.kt` | Toggle switch setting |
| `components/TextInputSettingItem.kt` | Text input setting |
| `components/WarningBanner.kt` | Warning message banner |

### compose/theme/
| File | Purpose |
|------|---------|
| `Color.kt` | Color definitions |
| `Theme.kt` | DroidifyTheme composable |
| `Type.kt` | Typography definitions |

### compose/
| File | Purpose |
|------|---------|
| `MainComposeActivity.kt` | Compose activity with NavHost |

## content/

| File | Purpose |
|------|---------|
| `ProductPreferences.kt` | Per-app update preferences (ignore version) |

## data/

### data/ (root)
| File | Purpose |
|------|---------|
| `AppRepository.kt` | App data repository |
| `InstalledRepository.kt` | Installed apps repository |
| `PrivacyRepository.kt` | RB logs + download stats |
| `RepoRepository.kt` | Repository management |
| `StringHandler.kt` | String resource handler for ViewModels |

### data/encryption/
| File | Purpose |
|------|---------|
| `AesEncryption.kt` | AES-256-CBC encryption |
| `EncryptionStorage.kt` | Encrypted credential storage |
| `Sha256.kt` | SHA-256 hashing utility |

### data/local/
| File | Purpose |
|------|---------|
| `DroidifyDatabase.kt` | Room database definition |

### data/local/converters/
| File | Purpose |
|------|---------|
| `Converters.kt` | LocalizedString/StringList converters |
| `PermissionConverter.kt` | PermissionV2 list converter |

### data/local/dao/
| File | Purpose |
|------|---------|
| `AppDao.kt` | App queries with localization |
| `AuthDao.kt` | Repository authentication CRUD |
| `DownloadStatsDao.kt` | Download statistics DAO |
| `IndexDao.kt` | Bulk index insert operations |
| `InstalledDao.kt` | Installed apps tracking |
| `LogQueries.kt` | Debug query logging |
| `RBLogDao.kt` | Reproducible build logs |
| `RepoDao.kt` | Repository queries |

### data/local/model/
| File | Purpose |
|------|---------|
| `AntiFeatureEntity.kt` | Anti-feature Room entity |
| `AppEntity.kt` | App Room entity + relations |
| `AuthenticationEntity.kt` | Encrypted auth entity |
| `AuthorEntity.kt` | App author entity |
| `CategoryEntity.kt` | Category entity |
| `DonateEntity.kt` | Donation links entity |
| `DownloadStats.kt` | Download statistics entity |
| `GraphicEntity.kt` | Feature/promo graphics |
| `InstalledEntity.kt` | Installed app tracking |
| `LinksEntity.kt` | App links (source, website) |
| `LocalizedAppEntity.kt` | Localized app name/summary/desc/icon |
| `LocalizedRepoEntity.kt` | Localized repo name/desc/icon |
| `MirrorEntity.kt` | Repository mirror URLs |
| `RBLogEntity.kt` | Reproducible build log entry |
| `RepoEntity.kt` | Repository entity |
| `ScreenshotEntity.kt` | Screenshot paths |
| `VersionEntity.kt` | App version/release entity |

### data/model/
| File | Purpose |
|------|---------|
| `App.kt` | App domain model + nested types |
| `DataFile.kt` | File interface (name, hash, size) |
| `FilePath.kt` | URL path builder |
| `Fingerprint.kt` | Repository fingerprint value class |
| `Html.kt` | HTML string value class |
| `Package.kt` | Package/version domain model |
| `PackageName.kt` | Package name value class |
| `Repo.kt` | Repository domain model |

## database/ (Legacy)

| File | Purpose |
|------|---------|
| `CursorOwner.kt` | Retained fragment for cursor lifecycle |
| `Database.kt` | Legacy SQLite database + adapters |
| `ObservableCursor.kt` | Observable cursor wrapper |
| `QueryBuilder.kt` | SQL query builder |
| `QueryLoader.kt` | Background query loading |
| `RepositoryExporter.kt` | JSON export/import for repos |
| `table/DatabaseHelper.kt` | SQLite open helper |
| `table/Table.kt` | Table definitions (Schema, Installed, etc.) |

## datastore/

| File | Purpose |
|------|---------|
| `CustomButtonRepository.kt` | Custom buttons JSON storage |
| `PreferenceSettingsRepository.kt` | Preferences DataStore impl |
| `Settings.kt` | Settings data class + serializer |
| `SettingsRepository.kt` | Settings repository interface |
| `exporter/SettingsExporter.kt` | Settings JSON export/import |
| `extension/Preferences.kt` | Theme resolution + sort order names |
| `migration/ProtoToPreferenceMigration.kt` | DataStore migration |

### datastore/model/
| File | Purpose |
|------|---------|
| `AutoSync.kt` | AutoSync enum (ALWAYS, WIFI_ONLY, etc.) |
| `CustomButton.kt` | Custom button data class |
| `InstallerType.kt` | Installer enum + MIUI default |
| `LegacyInstallerComponent.kt` | Legacy installer selection |
| `ProxyPreference.kt` | Proxy settings data class |
| `ProxyType.kt` | Proxy type enum |
| `SortOrder.kt` | Sort order enum + supported list |
| `Theme.kt` | Theme enum |

## di/

| File | Purpose |
|------|---------|
| `CoroutinesModule.kt` | Dispatcher + scope providers |
| `DatabaseModule.kt` | Room database + DAOs |
| `DatastoreModule.kt` | DataStore + settings providers |
| `HandlerModule.kt` | StringHandler (ViewModel scoped) |
| `InstallModule.kt` | InstallManager provider |
| `NetworkModule.kt` | HttpClient + Downloader |
| `RepoModule.kt` | Repository layer providers |
| `SyncableModule.kt` | Syncable provider |

## graphics/

| File | Purpose |
|------|---------|
| `DrawableWrapper.kt` | Drawable delegation base class |
| `PaddingDrawable.kt` | Drawable with padding/aspect ratio |

## index/

| File | Purpose |
|------|---------|
| `IndexMerger.kt` | Merges index into legacy database |
| `IndexV1Parser.kt` | V1 index stream parser |
| `OemRepositoryParser.kt` | Pre-installed repo detection (Samsung, Huawei) |
| `RepositoryUpdater.kt` | Legacy sync orchestration |

## installer/

| File | Purpose |
|------|---------|
| `InstallManager.kt` | Central install/uninstall manager |
| `model/InstallItem.kt` | Install request data class |
| `model/InstallState.kt` | Install state enum |

### installer/installers/
| File | Purpose |
|------|---------|
| `Installer.kt` | Installer interface |
| `InstallerPermission.kt` | Shizuku/root permission helpers |
| `LegacyInstaller.kt` | ACTION_INSTALL_PACKAGE intent |
| `root/RootInstaller.kt` | Root shell installation |
| `session/SessionInstaller.kt` | PackageInstaller session API |
| `session/SessionInstallerReceiver.kt` | Session result receiver |
| `shizuku/ShizukuInstaller.kt` | Shizuku privileged install |

## model/ (Legacy)

| File | Purpose |
|------|---------|
| `InstalledItem.kt` | Installed app info |
| `Product.kt` | Legacy app model (with releases) |
| `ProductItem.kt` | Minimal app for lists |
| `ProductPreference.kt` | Per-app preferences |
| `Release.kt` | Legacy release/version model |
| `Repository.kt` | Legacy repo model + defaults |

## network/

| File | Purpose |
|------|---------|
| `DataSize.kt` | Byte size value class + formatting |
| `Downloader.kt` | Downloader interface |
| `KtorDownloader.kt` | Ktor HTTP client impl |
| `NetworkResponse.kt` | Response sealed interface |
| `header/HeadersBuilder.kt` | HTTP headers DSL |
| `header/KtorHeadersBuilder.kt` | Ktor headers impl |
| `validation/FileValidator.kt` | File validation interface |
| `validation/ValidationException.kt` | Validation error |

## receivers/

| File | Purpose |
|------|---------|
| `CopyErrorReceiver.kt` | Copy error to clipboard from notification |
| `InstalledAppReceiver.kt` | Package install/uninstall broadcasts |
| `UnarchivePackageReceiver.kt` | Android 15+ unarchive requests |

## service/

| File | Purpose |
|------|---------|
| `Connection.kt` | Generic service connection helper |
| `ConnectionService.kt` | Base class for bound services |
| `DownloadService.kt` | APK download service |
| `ReleaseFileValidator.kt` | APK validation (hash, sig, perms) |
| `SyncService.kt` | Repository sync service |

## sync/

| File | Purpose |
|------|---------|
| `JarScope.kt` | JAR file coroutine scope |
| `JsonParser.kt` | Kotlinx serialization JSON config |
| `LocalSyncable.kt` | Local file sync implementation |
| `Syncable.kt` | Sync interface |
| `SyncPreference.kt` | Repository sync preferences |
| `SyncState.kt` | Sync progress state |
| `common/IndexConverter.kt` | V1 to V2 index conversion |
| `common/IndexDownloader.kt` | Index file download logic |
| `utils/JarFile.kt` | JAR verification extensions |

### sync/v1/
| File | Purpose |
|------|---------|
| `V1Parser.kt` | V1 index JSON parser |
| `V1Syncable.kt` | V1 sync implementation |
| `model/AppV1.kt` | V1 app model |
| `model/IndexV1.kt` | V1 index model |
| `model/Localized.kt` | V1 localization model |
| `model/PackageV1.kt` | V1 package model |
| `model/RepoV1.kt` | V1 repo model |

### sync/v2/
| File | Purpose |
|------|---------|
| `EntrySyncable.kt` | V2 entry.json sync |
| `model/Entry.kt` | Entry point JSON model |
| `model/FileV2.kt` | File reference model |
| `model/IndexV2.kt` | V2 index + diff models |
| `model/Localization.kt` | Localization type aliases |
| `model/PackageV2.kt` | V2 package/metadata/version |
| `model/RepoV2.kt` | V2 repo/mirror/category/antifeature |

## ui/ (Legacy Fragments)

### ui/ (root)
| File | Purpose |
|------|---------|
| `MessageDialog.kt` | Simple message dialog |
| `ScreenFragment.kt` | Base fragment with toolbar |

### ui/appDetail/
| File | Purpose |
|------|---------|
| `AppDetailAdapter.kt` | Multi-section detail adapter |
| `AppDetailFragment.kt` | App detail fragment |
| `AppDetailViewModel.kt` | Legacy detail ViewModel |
| `CustomButtonsAdapter.kt` | Custom buttons RecyclerView |
| `ScreenshotsAdapter.kt` | Screenshot carousel |
| `ShizukuErrorDialog.kt` | Shizuku error dialog |

### ui/appList/
| File | Purpose |
|------|---------|
| `AppListAdapter.kt` | App list cursor adapter |
| `AppListFragment.kt` | App list fragment |
| `AppListViewModel.kt` | Legacy list ViewModel |

### ui/favourites/
| File | Purpose |
|------|---------|
| `FavouriteFragmentAdapter.kt` | Favorites list adapter |
| `FavouritesFragment.kt` | Favorites fragment |
| `FavouritesViewModel.kt` | Favorites ViewModel |

### ui/repository/
| File | Purpose |
|------|---------|
| `EditRepositoryFragment.kt` | Add/edit repo fragment |
| `RepositoriesAdapter.kt` | Repo list adapter |
| `RepositoriesFragment.kt` | Repo list fragment |
| `RepositoryFragment.kt` | Repo detail fragment |
| `RepositoryViewModel.kt` | Repo detail ViewModel |

### ui/settings/
| File | Purpose |
|------|---------|
| `SettingsFragment.kt` | Settings fragment |

### ui/tabsFragment/
| File | Purpose |
|------|---------|
| `TabsFragment.kt` | Home screen with ViewPager2 |
| `TabsViewModel.kt` | Tabs ViewModel |

## utility/

### utility/ (root)
| File | Purpose |
|------|---------|
| `PackageItemResolver.kt` | Permission info resolver |
| `ProgressInputStream.kt` | Progress-reporting input stream |

### utility/common/
| File | Purpose |
|------|---------|
| `Constants.kt` | App-wide constants |
| `Deeplinks.kt` | URL/intent deeplink parsing |
| `Exporter.kt` | Generic export/import interface |
| `Notification.kt` | Notification channel creation |
| `Permissions.kt` | Runtime permission helpers |
| `Scroller.kt` | RecyclerView smooth scroll |
| `SdkCheck.kt` | SDK version check helpers |
| `Text.kt` | String utilities (nullIfEmpty, log) |
| `cache/Cache.kt` | File caching + cleanup |
| `device/Huawei.kt` | Huawei EMUI detection |
| `device/Miui.kt` | MIUI detection + optimization check |
| `result/Result.kt` | Result wrapper type |

### utility/common/extension/
| File | Purpose |
|------|---------|
| `Collections.kt` | Collection extensions |
| `Context.kt` | Context extensions (dp, colors, etc.) |
| `Cursor.kt` | Cursor reading extensions |
| `DateTime.kt` | Date/time formatting |
| `Exception.kt` | Exception utilities |
| `File.kt` | File extensions |
| `Flow.kt` | Flow extensions (asStateFlow) |
| `Insets.kt` | Window insets extensions |
| `Intent.kt` | Intent builder extensions |
| `Json.kt` | JSON extensions |
| `Number.kt` | Number formatting |
| `PackageInfo.kt` | PackageInfo extensions |
| `Service.kt` | Service extensions |
| `View.kt` | View extensions |

### utility/extension/
| File | Purpose |
|------|---------|
| `Android.kt` | Android version utilities |
| `Connection.kt` | Network connectivity check |
| `Flow.kt` | More Flow extensions |
| `Fragment.kt` | Fragment extensions (mainActivity) |
| `PackageInfo.kt` | PackageInfo compatibility |
| `Resources.kt` | Resource extensions |

### utility/notifications/
| File | Purpose |
|------|---------|
| `InstallNotification.kt` | Install progress notifications |
| `UpdateNotification.kt` | Update available notifications |
| `WorkerNotification.kt` | Background worker notifications |

### utility/serialization/
| File | Purpose |
|------|---------|
| `ProductItemSerialization.kt` | Legacy ProductItem serialization |
| `ProductPreferenceSerialization.kt` | ProductPreference serialization |
| `ProductSerialization.kt` | Product JSON serialization |
| `ReleaseSerialization.kt` | Release JSON serialization |
| `RepositorySerialization.kt` | Repository JSON serialization |

### utility/text/
| File | Purpose |
|------|---------|
| `AnnotatedString.kt` | AnnotatedString builder |
| `HtmlFormatter.kt` | HTML to styled text conversion |

## widget/

| File | Purpose |
|------|---------|
| `CursorRecyclerAdapter.kt` | Database cursor adapter |
| `DividerItemDecoration.kt` | Configurable list dividers |
| `EnumRecyclerAdapter.kt` | Enum-based view types |
| `FocusSearchView.kt` | SearchView with focus control |
| `StableRecyclerAdapter.kt` | Stable IDs from descriptors |

## work/

| File | Purpose |
|------|---------|
| `CleanUpWorker.kt` | Periodic cache cleanup |
| `DownloadStatsWorker.kt` | Download statistics fetch |
| `RBLogWorker.kt` | Reproducible build logs fetch |
| `SyncWorker.kt` | Repository sync worker |
| `UnarchiveWorker.kt` | Android 15+ app unarchive |

---

**Total: 255 Kotlin source files**
