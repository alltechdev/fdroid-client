# Compose ViewModels

Hilt ViewModels for Compose screens.

## Overview

**Directory:** `compose/*/`

Each Compose screen has a corresponding ViewModel for state management.

## SettingsViewModel

**File:** `compose/settings/SettingsViewModel.kt`

Manages all app settings.

```kotlin
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val repositoryExporter: RepositoryExporter,
    private val customButtonRepository: CustomButtonRepository,
    private val handler: StringHandler,
) : ViewModel()
```

### State Flows

```kotlin
val settings = settingsRepository.data.asStateFlow(Settings())
val customButtons: StateFlow<List<CustomButton>> = customButtonRepository.buttons
    .asStateFlow(emptyList())
val isBackgroundAllowed = MutableStateFlow(true).asStateFlow()
```

### Theme Settings

```kotlin
fun setTheme(theme: Theme)
fun setDynamicTheme(enabled: Boolean)
fun setLanguage(language: String) {
    val appLocale = LocaleListCompat.create(language.toLocale())
    AppCompatDelegate.setApplicationLocales(appLocale)
    settingsRepository.setLanguage(language)
}
```

### Installer Settings

```kotlin
fun setInstaller(context: Context, installerType: InstallerType) {
    when (installerType) {
        InstallerType.SHIZUKU -> handleShizukuInstaller(context, installerType)
        InstallerType.ROOT -> handleRootInstaller(installerType)
        else -> settingsRepository.setInstallerType(installerType)
    }
}

private suspend fun handleShizukuInstaller(context: Context, installerType: InstallerType) {
    if (isShizukuInstalled(context) || initSui(context)) {
        when {
            !isShizukuAlive() -> showSnackbar(R.string.shizuku_not_alive)
            isShizukuGranted() -> settingsRepository.setInstallerType(installerType)
            else -> {
                if (requestPermissionListener()) {
                    settingsRepository.setInstallerType(installerType)
                }
            }
        }
    } else {
        showSnackbar(R.string.shizuku_not_installed)
    }
}
```

### Import/Export

```kotlin
fun exportSettings(uri: Uri)
fun importSettings(uri: Uri)
fun exportRepos(uri: Uri)
fun importRepos(uri: Uri)
fun exportCustomButtons(uri: Uri)
fun importCustomButtons(uri: Uri)
```

### Cleanup Intervals

```kotlin
companion object {
    val cleanUpIntervals: List<Duration> = listOf(
        6.hours, 12.hours, 18.hours, 1.days, 2.days, Duration.INFINITE
    )
    val localeCodesList: List<String> = BuildConfig.DETECTED_LOCALES.toList()
}
```

## AppDetailViewModel

**File:** `compose/appDetail/AppDetailViewModel.kt`

Shows app details and available versions.

```kotlin
@HiltViewModel
class AppDetailViewModel @Inject constructor(
    private val appRepository: AppRepository,
    private val repoRepository: RepoRepository,
    private val customButtonRepository: CustomButtonRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel()
```

### Navigation Arguments

```kotlin
val packageName: String = requireNotNull(savedStateHandle["packageName"])
```

### State

```kotlin
sealed interface AppDetailState {
    object Loading : AppDetailState
    data class Error(val message: String) : AppDetailState
    data class Success(
        val app: App,
        val packages: List<Pair<Package, Repo>>,
    ) : AppDetailState
}

val state: StateFlow<AppDetailState> = appRepository
    .getApp(PackageName(packageName))
    .map { apps ->
        when {
            apps.isEmpty() -> AppDetailState.Error("No app found")
            else -> AppDetailState.Success(
                app = apps.first(),
                packages = apps.flatMap { ... }
                    .sortedByDescending { (pkg, _) -> pkg.manifest.versionCode }
            )
        }
    }
    .onStart { emit(AppDetailState.Loading) }
    .asStateFlow(AppDetailState.Loading)
```

## AppListViewModel

**File:** `compose/appList/AppListViewModel.kt`

Lists apps with filtering and sorting.

```kotlin
@HiltViewModel
class AppListViewModel @Inject constructor(
    private val appRepository: AppRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel()
```

### Query Parameters

```kotlin
val apps: StateFlow<List<AppMinimal>>
val searchQuery: MutableStateFlow<String>
val sortOrder: StateFlow<SortOrder>
val categories: StateFlow<List<String>>
```

## RepoListViewModel

**File:** `compose/repoList/RepoListViewModel.kt`

Lists repositories with enable/disable.

```kotlin
@HiltViewModel
class RepoListViewModel @Inject constructor(
    private val repoRepository: RepoRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel()

val repos: StateFlow<List<RepoWithEnabled>>
fun toggleRepo(repoId: Int, enabled: Boolean)
```

## RepoDetailViewModel

**File:** `compose/repoDetail/RepoDetailViewModel.kt`

Shows repository details and app count.

```kotlin
@HiltViewModel
class RepoDetailViewModel @Inject constructor(
    private val repoRepository: RepoRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel()

val repoId: Int = requireNotNull(savedStateHandle["repoId"])
val repo: StateFlow<RepoDetail?>
```

## RepoEditViewModel

**File:** `compose/repoEdit/RepoEditViewModel.kt`

Add or edit repository.

```kotlin
@HiltViewModel
class RepoEditViewModel @Inject constructor(
    private val repoRepository: RepoRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel()

val repoId: Int? = savedStateHandle["repoId"]
val address: MutableStateFlow<String>
val fingerprint: MutableStateFlow<String>
val username: MutableStateFlow<String>
val password: MutableStateFlow<String>

fun save()
fun delete()
```

## Common Patterns

### SavedStateHandle for Navigation Args

```kotlin
val packageName: String = requireNotNull(savedStateHandle["packageName"])
```

### asStateFlow Extension

```kotlin
val state = flow.asStateFlow(initialValue)
// Converts Flow to StateFlow with initial value
```

### Snackbar Host

```kotlin
val snackbarHostState = SnackbarHostState()

fun showSnackbar(@StringRes messageRes: Int) {
    viewModelScope.launch {
        snackbarHostState.showSnackbar(handler.getString(messageRes))
    }
}
```

## State Management Flow

```
┌─────────────────────────────────────────────────────────────┐
│                    Repository Layer                          │
│  AppRepository, RepoRepository, SettingsRepository          │
└─────────────────────────┬───────────────────────────────────┘
                          │ Flow<Data>
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                    ViewModel                                 │
│  - Transforms Flow to StateFlow                             │
│  - Handles user actions                                     │
│  - Manages UI state                                         │
└─────────────────────────┬───────────────────────────────────┘
                          │ StateFlow<UiState>
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                    Compose Screen                            │
│  - collectAsState()                                         │
│  - Renders UI based on state                                │
│  - Calls ViewModel methods                                  │
└─────────────────────────────────────────────────────────────┘
```
