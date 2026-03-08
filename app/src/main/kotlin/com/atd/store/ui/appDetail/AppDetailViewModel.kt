package com.atd.store.ui.appDetail

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.atd.store.BuildConfig
import com.atd.store.data.PrivacyRepository
import com.atd.store.data.local.model.RBLogEntity
import com.atd.store.data.model.toPackageName
import com.atd.store.database.Database
import com.atd.store.datastore.SettingsRepository
import com.atd.store.datastore.model.InstallerType
import com.atd.store.installer.InstallManager
import com.atd.store.installer.installers.isShizukuAlive
import com.atd.store.installer.installers.isShizukuGranted
import com.atd.store.installer.installers.isShizukuInstalled
import com.atd.store.installer.installers.isSuiAvailable
import com.atd.store.installer.installers.requestPermissionListener
import com.atd.store.installer.model.InstallState
import com.atd.store.installer.model.installFrom
import com.atd.store.model.InstalledItem
import com.atd.store.model.Product
import com.atd.store.model.Repository
import com.atd.store.utility.common.extension.asStateFlow
import com.atd.store.utility.extension.combine
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@HiltViewModel
class AppDetailViewModel @Inject constructor(
    private val installer: InstallManager,
    private val settingsRepository: SettingsRepository,
    privacyRepository: PrivacyRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val packageName: String = requireNotNull(savedStateHandle[ARG_PACKAGE_NAME])

    private val repoAddress: StateFlow<String?> =
        savedStateHandle.getStateFlow(ARG_REPO_ADDRESS, null)

    val installerState: StateFlow<InstallState?> =
        installer.state.mapNotNull { stateMap ->
            stateMap[packageName.toPackageName()]
        }.asStateFlow(null)

    val state =
        combine(
            Database.ProductAdapter.getStream(packageName),
            Database.RepositoryAdapter.getAllStream(),
            Database.InstalledAdapter.getStream(packageName),
            privacyRepository.getRBLogs(packageName),
            privacyRepository.getLatestDownloadStats(packageName),
            repoAddress,
            settingsRepository.data
        ) { products, repositories, installedItem, rblogs, downloads, suggestedAddress, settings ->
            val idAndRepos = repositories.associateBy { it.id }
            val filteredProducts = products.filter { product ->
                idAndRepos[product.repositoryId] != null
            }
            AppDetailUiState(
                products = filteredProducts,
                repos = repositories,
                rblogs = rblogs,
                downloads = downloads,
                installedItem = installedItem,
                isSelf = packageName == BuildConfig.APPLICATION_ID,
                addressIfUnavailable = suggestedAddress,
            )
        }.asStateFlow(AppDetailUiState())

    fun shizukuState(context: Context): ShizukuState? {
        val isSelected =
            runBlocking { settingsRepository.getInitial().installerType == InstallerType.SHIZUKU }
        if (!isSelected) return null
        val isAlive = isShizukuAlive()
        val isSuiAvailable = isSuiAvailable()
        if (isSuiAvailable) return null

        val isGranted = if (isAlive) {
            if (isShizukuGranted()) {
                true
            } else {
                runBlocking { requestPermissionListener() }
            }
        } else false
        return ShizukuState(
            isNotInstalled = !isShizukuInstalled(context),
            isNotGranted = !isGranted,
            isNotAlive = !isAlive,
        )
    }

    fun setDefaultInstaller() {
        viewModelScope.launch {
            settingsRepository.setInstallerType(InstallerType.Default)
        }
    }

    fun installPackage(packageName: String, fileName: String) {
        viewModelScope.launch {
            installer install (packageName installFrom fileName)
        }
    }

    fun removeQueue() {
        viewModelScope.launch {
            installer remove packageName.toPackageName()
        }
    }

    companion object {
        const val ARG_PACKAGE_NAME = "package_name"
        const val ARG_REPO_ADDRESS = "repo_address"
    }
}

data class ShizukuState(
    val isNotInstalled: Boolean,
    val isNotGranted: Boolean,
    val isNotAlive: Boolean,
) {
    val check: Boolean
        get() = isNotInstalled || isNotAlive || isNotGranted
}

data class AppDetailUiState(
    val products: List<Product> = emptyList(),
    val repos: List<Repository> = emptyList(),
    val rblogs: List<RBLogEntity> = emptyList(),
    val downloads: Long = -1,
    val installedItem: InstalledItem? = null,
    val isSelf: Boolean = false,
    val addressIfUnavailable: String? = null,
)
