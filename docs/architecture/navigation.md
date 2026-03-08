# Navigation

Droid-ify uses [Jetpack Navigation Compose](https://developer.android.com/jetpack/compose/navigation) with type-safe routes.

## Navigation Graph

```
┌─────────────────────────────────────────────────────────┐
│                      NavHost                             │
│                  (startDestination: AppList)            │
│                                                          │
│  ┌─────────┐      ┌─────────────┐      ┌─────────────┐  │
│  │ AppList │─────►│  AppDetail  │      │  Settings   │  │
│  │         │      │ (packageName)│      │             │  │
│  └────┬────┘      └─────────────┘      └─────────────┘  │
│       │                                       ▲          │
│       └───────────────────────────────────────┘          │
└─────────────────────────────────────────────────────────┘
```

## Route Definitions

Routes are defined as `@Serializable` objects or data classes:

### Simple Routes (no parameters)

```kotlin
@Serializable
object AppList

@Serializable
object Settings

@Serializable
object Home
```

### Parameterized Routes

```kotlin
@Serializable
data class AppDetail(val packageName: String)
```

## Navigation Files

Each feature has a navigation file:

| Feature | File |
|---------|------|
| Home | `compose/home/navigation/HomeNavigation.kt` |
| AppList | `compose/appList/navigation/AppListNavigation.kt` |
| AppDetail | `compose/appDetail/navigation/AppDetailNavigation.kt` |
| Settings | `compose/settings/navigation/SettingsNavigation.kt` |

## Navigation Pattern

Each navigation file contains:

1. **Route definition** - `@Serializable` object/data class
2. **Navigate extension** - `NavController.navigateTo{Feature}()`
3. **Graph builder** - `NavGraphBuilder.{feature}()`

### Example: AppDetail

```kotlin
// 1. Route definition
@Serializable
data class AppDetail(val packageName: String)

// 2. Navigate extension
fun NavController.navigateToAppDetail(packageName: String) {
    this.navigate(AppDetail(packageName), navOptions {
        launchSingleTop = true
    })
}

// 3. Graph builder
fun NavGraphBuilder.appDetail(
    onBackClick: () -> Unit,
) {
    composable<AppDetail> {
        AppDetailScreen(
            onBackClick = onBackClick,
            viewModel = hiltViewModel(),
        )
    }
}
```

## NavHost Setup

In `MainComposeActivity.kt`:

```kotlin
@Composable
fun MainContent() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = AppList,  // Default screen
    ) {
        // Register all routes
        home(
            onNavigateToApps = { navController.navigateToAppList() },
            onNavigateToSettings = { navController.navigateToSettings() },
        )

        appList(
            onAppClick = { packageName ->
                navController.navigateToAppDetail(packageName)
            },
            onNavigateToSettings = { navController.navigateToSettings() },
        )

        appDetail(onBackClick = { navController.popBackStack() })

        settings(onBackClick = { navController.popBackStack() })
    }
}
```

## Navigation Options

Common navigation options:

```kotlin
fun NavController.navigateToAppList() {
    navigate(AppList, navOptions {
        launchSingleTop = true   // Don't create multiple instances
        restoreState = true      // Restore state when navigating back
    })
}
```

### Available Options

| Option | Description |
|--------|-------------|
| `launchSingleTop` | Reuse existing instance if at top |
| `restoreState` | Restore saved state |
| `popUpTo` | Pop back stack to destination |
| `inclusive` | Include target in pop |

## Accessing Route Parameters

Parameters are automatically extracted from the route:

```kotlin
@Serializable
data class AppDetail(val packageName: String)

fun NavGraphBuilder.appDetail(onBackClick: () -> Unit) {
    composable<AppDetail> { backStackEntry ->
        // Option 1: Use hiltViewModel (recommended)
        // SavedStateHandle contains "packageName"
        val viewModel: AppDetailViewModel = hiltViewModel()

        // Option 2: Access directly
        val route = backStackEntry.toRoute<AppDetail>()
        val packageName = route.packageName
    }
}
```

### In ViewModel

```kotlin
@HiltViewModel
class AppDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val appRepository: AppRepository,
) : ViewModel() {

    // Automatically extracted from route
    private val packageName: String = savedStateHandle["packageName"]!!

    val app = appRepository.getApp(PackageName(packageName))
}
```

## Adding a New Route

### Step 1: Define Route

```kotlin
// Simple route
@Serializable
object MyFeature

// Or with parameters
@Serializable
data class MyFeature(val id: Int, val name: String? = null)
```

### Step 2: Create Navigation Functions

```kotlin
fun NavController.navigateToMyFeature(id: Int) {
    navigate(MyFeature(id), navOptions {
        launchSingleTop = true
    })
}

fun NavGraphBuilder.myFeature(
    onBackClick: () -> Unit,
) {
    composable<MyFeature> {
        MyFeatureScreen(
            viewModel = hiltViewModel(),
            onBackClick = onBackClick,
        )
    }
}
```

### Step 3: Add to NavHost

```kotlin
NavHost(navController, startDestination = AppList) {
    // ... existing routes
    myFeature(onBackClick = { navController.popBackStack() })
}
```

### Step 4: Navigate from Other Screens

```kotlin
appList(
    onMyFeatureClick = { id -> navController.navigateToMyFeature(id) },
    // ...
)
```

## Back Navigation

Standard back navigation:

```kotlin
navController.popBackStack()
```

Pop to specific destination:

```kotlin
navController.popBackStack(
    route = AppList,
    inclusive = false,  // Keep AppList in stack
)
```

## Deep Links

To add deep link support:

```kotlin
composable<AppDetail>(
    deepLinks = listOf(
        navDeepLink {
            uriPattern = "droidify://app/{packageName}"
        }
    )
) {
    // ...
}
```

## Testing Navigation

```kotlin
@Test
fun testNavigation() {
    val navController = TestNavHostController(context)

    composeTestRule.setContent {
        NavHost(navController, startDestination = AppList) {
            appList(onAppClick = { navController.navigateToAppDetail(it) })
            appDetail(onBackClick = { navController.popBackStack() })
        }
    }

    // Navigate
    navController.navigateToAppDetail("org.example.app")

    // Assert
    assertEquals(
        "org.example.app",
        navController.currentBackStackEntry?.toRoute<AppDetail>()?.packageName
    )
}
```

## Removed

| Route | Removal Doc |
|-------|-------------|
| Package: `com.looker.droidify` | [package-rename.md](../changes/package-rename.md) |
| `RepoList` | [repository-management.md](../removal/repository-management.md) |
| `RepoDetail` | [repository-management.md](../removal/repository-management.md) |
| `RepoEdit` | [repository-management.md](../removal/repository-management.md) |

## Changes

| Change | Change Doc |
|--------|------------|
| App branding Droid-ify → ATD Store | [app-branding.md](../changes/app-branding.md) |
