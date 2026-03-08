package com.atd.store.compose

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
import com.atd.store.compose.appDetail.navigation.appDetail
import com.atd.store.compose.appDetail.navigation.navigateToAppDetail
import com.atd.store.compose.appList.navigation.AppList
import com.atd.store.compose.appList.navigation.appList
import com.atd.store.compose.home.navigation.home
import com.atd.store.compose.settings.navigation.navigateToSettings
import com.atd.store.compose.settings.navigation.settings
import com.atd.store.compose.theme.AtdTheme
import com.atd.store.data.RepoRepository
import com.atd.store.datastore.SettingsRepository
import com.atd.store.model.Repository
import com.atd.store.utility.common.requestNotificationPermission
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
            AtdTheme {
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
