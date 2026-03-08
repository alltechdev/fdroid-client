package com.looker.droidify.compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.looker.droidify.compose.appDetail.navigation.appDetail
import com.looker.droidify.compose.appDetail.navigation.navigateToAppDetail
import com.looker.droidify.compose.appList.navigation.AppList
import com.looker.droidify.compose.appList.navigation.appList
import com.looker.droidify.compose.home.navigation.home
import com.looker.droidify.compose.settings.navigation.navigateToSettings
import com.looker.droidify.compose.settings.navigation.settings
import com.looker.droidify.compose.theme.DroidifyTheme
import com.looker.droidify.data.RepoRepository
import com.looker.droidify.datastore.SettingsRepository
import com.looker.droidify.model.Repository
import com.looker.droidify.utility.common.requestNotificationPermission
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainComposeActivity : ComponentActivity() {

    @Inject
    lateinit var repository: RepoRepository

    @Inject
    lateinit var settingsRepository: SettingsRepository

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            val repos = repository.repos.first()
            if (repos.isEmpty()) {
                val defaultRepo = Repository.defaultRepositories.first()
                repository.insertRepo(
                    defaultRepo.address,
                    defaultRepo.fingerprint,
                    null,
                    null,
                    defaultRepo.name,
                    defaultRepo.description
                )
                // Enable the repo
                val insertedRepos = repository.repos.first()
                insertedRepos.firstOrNull()?.let {
                    settingsRepository.setRepoEnabled(it.id, true)
                }
            }
        }
        enableEdgeToEdge()
        requestNotificationPermission(request = notificationPermission::launch)
        setContent {
            DroidifyTheme {
                val navController = rememberNavController()
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NavHost(
                        modifier = Modifier.padding(innerPadding),
                        navController = navController,
                        startDestination = AppList,
                    ) {
                        home(
                            onNavigateToApps = { navController.navigate(AppList) },
                            onNavigateToSettings = { navController.navigateToSettings() },
                        )
                        appList(
                            onAppClick = { packageName ->
                                navController.navigateToAppDetail(packageName)
                            },
                            onNavigateToSettings = { navController.navigateToSettings() },
                        )

                        appDetail(
                            onBackClick = { navController.popBackStack() },
                        )

                        settings(onBackClick = { navController.popBackStack() })
                    }
                }
            }
        }
    }
}
