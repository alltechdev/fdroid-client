# Application Entry Points

Main entry points and navigation structure of the app.

## Overview

| Component | Purpose |
|-----------|---------|
| `MainApplication` | Application class, DI initialization |
| `MainActivity` | Legacy View-based UI host |
| `MainComposeActivity` | Compose UI host |

## MainActivity

**File:** `app/src/main/kotlin/com/looker/droidify/MainActivity.kt`

Legacy fragment-based UI host.

### Actions

```kotlin
companion object {
    const val ACTION_UPDATES = "${BuildConfig.APPLICATION_ID}.intent.action.UPDATES"
    const val ACTION_INSTALL = "${BuildConfig.APPLICATION_ID}.intent.action.INSTALL"
    const val EXTRA_CACHE_FILE_NAME = "${BuildConfig.APPLICATION_ID}.intent.extra.CACHE_FILE_NAME"
}
```

### Fragment Stack Management

Custom fragment stack for back navigation:

```kotlin
@Parcelize
private class FragmentStackItem(
    val className: String,
    val arguments: Bundle?,
    val savedState: Fragment.SavedState?,
) : Parcelable

private val fragmentStack = mutableListOf<FragmentStackItem>()

private fun pushFragment(fragment: Fragment) {
    currentFragment?.let {
        fragmentStack.add(FragmentStackItem(
            it::class.java.name,
            it.arguments,
            supportFragmentManager.saveFragmentInstanceState(it),
        ))
    }
    replaceFragment(fragment, true)
}

private fun popFragment(): Boolean {
    if (fragmentStack.isEmpty()) return false
    val stackItem = fragmentStack.removeAt(fragmentStack.size - 1)
    val fragment = Class.forName(stackItem.className).newInstance() as Fragment
    stackItem.arguments?.let(fragment::setArguments)
    stackItem.savedState?.let(fragment::setInitialSavedState)
    replaceFragment(fragment, false)
    return true
}
```

### Navigation Methods

```kotlin
fun navigateFavourites() = pushFragment(FavouritesFragment())
fun navigateProduct(packageName: String, repoAddress: String? = null) =
    pushFragment(AppDetailFragment(packageName, repoAddress))
fun navigateRepositories() = pushFragment(RepositoriesFragment())
fun navigatePreferences() = pushFragment(SettingsFragment.newInstance())
fun navigateAddRepository(repoAddress: String? = null) =
    pushFragment(EditRepositoryFragment(null, repoAddress))
fun navigateRepository(repositoryId: Long) =
    pushFragment(RepositoryFragment(repositoryId))
fun navigateEditRepository(repositoryId: Long) =
    pushFragment(EditRepositoryFragment(repositoryId, null))
```

### Intent Handling

```kotlin
private fun handleIntent(intent: Intent?) {
    when (intent?.action) {
        ACTION_UPDATES -> {
            navigateToTabsFragment()
            (currentFragment as TabsFragment).selectUpdates()
        }
        ACTION_INSTALL -> {
            val packageName = intent.getInstallPackageName ?: return
            navigateProduct(packageName)
            val cacheFile = intent.getStringExtra(EXTRA_CACHE_FILE_NAME) ?: return
            lifecycleScope.launch { installer install (packageName installFrom cacheFile) }
        }
        Intent.ACTION_VIEW -> {
            when (val deeplink = intent.deeplinkType()) {
                is DeeplinkType.AppDetail -> navigateProduct(deeplink.packageName)
                is DeeplinkType.AppSearch -> doSearchInTabsFragment(deeplink.query)
                is DeeplinkType.AddRepository -> navigateAddRepository(deeplink.address)
            }
        }
    }
}
```

### Theme Handling

```kotlin
private fun collectChange() {
    val hiltEntryPoint = EntryPointAccessors.fromApplication(this, ...)
    val newSettings = hiltEntryPoint.settingsRepository().get { theme to dynamicTheme }

    runBlocking {
        val theme = newSettings.first()
        setTheme(resources.configuration.getThemeRes(theme.first, theme.second))
    }

    lifecycleScope.launch {
        newSettings.drop(1).collect { (theme, dynamic) ->
            setTheme(resources.configuration.getThemeRes(theme, dynamic))
            recreate()
        }
    }
}
```

### CursorOwner

Manages database cursors across configuration changes:

```kotlin
lateinit var cursorOwner: CursorOwner

override fun onCreate(savedInstanceState: Bundle?) {
    if (savedInstanceState == null) {
        cursorOwner = CursorOwner()
        supportFragmentManager.commit {
            add(cursorOwner, CursorOwner::class.java.name)
        }
    } else {
        cursorOwner = supportFragmentManager
            .findFragmentByTag(CursorOwner::class.java.name) as CursorOwner
    }
}
```

## MainComposeActivity

**File:** `app/src/main/kotlin/com/looker/droidify/compose/MainComposeActivity.kt`

Compose-based UI host for newer screens.

### Navigation Setup

```kotlin
@AndroidEntryPoint
class MainComposeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DroidifyTheme {
                val navController = rememberNavController()
                NavHost(navController, startDestination = "home") {
                    homeScreen(navController)
                    appListScreen(navController)
                    appDetailScreen(navController)
                    settingsScreen(navController)
                    repoListScreen(navController)
                    repoDetailScreen(navController)
                    repoEditScreen(navController)
                }
            }
        }
    }
}
```

## Lifecycle Flow

```
┌─────────────────────────────────────────────────────────────┐
│                   App Launch                                 │
└─────────────────────────┬───────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                MainApplication.onCreate()                    │
│  - Initialize Hilt                                          │
│  - Initialize Database                                      │
│  - Register InstalledAppReceiver                            │
│  - Initialize ProductPreferences                            │
│  - Start sync if enabled                                    │
└─────────────────────────┬───────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                 MainActivity.onCreate()                      │
│  - Apply theme                                              │
│  - Request notification permission                          │
│  - Setup CursorOwner                                        │
│  - Handle deeplinks                                         │
│  - Show TabsFragment                                        │
└─────────────────────────┬───────────────────────────────────┘
                          │
             ┌────────────┴────────────┐
             ▼                         ▼
┌───────────────────────┐   ┌───────────────────────┐
│    TabsFragment       │   │    Handle Intent      │
│  (default home)       │   │  (deeplink/action)    │
└───────────────────────┘   └───────────────────────┘
```

## Back Navigation

### OnBackPressedCallback

```kotlin
private var onBackPressedCallback: OnBackPressedCallback? = null

private fun backHandler() {
    if (onBackPressedCallback == null) {
        onBackPressedCallback = object : OnBackPressedCallback(enabled = false) {
            override fun handleOnBackPressed() {
                hideKeyboard()
                popFragment()
            }
        }
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback!!)
    }
    onBackPressedCallback?.isEnabled = fragmentStack.isNotEmpty()
}
```

## Edge-to-Edge UI

```kotlin
if (SdkCheck.isR) {
    window.statusBarColor = resources.getColor(android.R.color.transparent, theme)
    window.navigationBarColor = resources.getColor(android.R.color.transparent, theme)
    WindowCompat.setDecorFitsSystemWindows(window, false)
}
```

## Dependency Injection

```kotlin
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject
    lateinit var installer: InstallManager

    // Access settings without injection
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface CustomUserRepositoryInjector {
        fun settingsRepository(): SettingsRepository
    }
}
```
