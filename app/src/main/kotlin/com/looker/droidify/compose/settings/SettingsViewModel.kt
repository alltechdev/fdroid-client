package com.looker.droidify.compose.settings

import android.content.Context
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.material3.SnackbarHostState
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.looker.droidify.BuildConfig
import com.looker.droidify.R
import com.looker.droidify.data.StringHandler
import com.looker.droidify.datastore.Settings
import com.looker.droidify.datastore.SettingsRepository
import com.looker.droidify.datastore.model.InstallerType
import com.looker.droidify.datastore.model.LegacyInstallerComponent
import com.looker.droidify.datastore.model.Theme
import com.looker.droidify.installer.installers.initSui
import com.looker.droidify.installer.installers.isMagiskGranted
import com.looker.droidify.installer.installers.isShizukuAlive
import com.looker.droidify.installer.installers.isShizukuGranted
import com.looker.droidify.installer.installers.isShizukuInstalled
import com.looker.droidify.installer.installers.requestPermissionListener
import com.looker.droidify.utility.common.extension.asStateFlow
import com.looker.droidify.utility.common.extension.updateAsMutable
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.*
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val handler: StringHandler,
) : ViewModel() {

    val snackbarHostState = SnackbarHostState()

    val settings = settingsRepository.data.asStateFlow(Settings())

    fun showSnackbar(@StringRes messageRes: Int) {
        viewModelScope.launch {
            snackbarHostState.showSnackbar(handler.getString(messageRes))
        }
    }

    fun setLanguage(language: String) {
        viewModelScope.launch {
            val appLocale = LocaleListCompat.create(language.toLocale())
            AppCompatDelegate.setApplicationLocales(appLocale)
            settingsRepository.setLanguage(language)
        }
    }

    fun setTheme(theme: Theme) {
        viewModelScope.launch {
            settingsRepository.setTheme(theme)
        }
    }

    fun setDynamicTheme(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setDynamicTheme(enabled)
        }
    }

    fun setHomeScreenSwiping(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setHomeScreenSwiping(enabled)
        }
    }

    fun setAutoUpdate(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAutoUpdate(enabled)
        }
    }

    fun setNotifyUpdates(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.enableNotifyUpdates(enabled)
        }
    }

    fun setInstaller(context: Context, installerType: InstallerType) {
        viewModelScope.launch {
            when (installerType) {
                InstallerType.SHIZUKU -> handleShizukuInstaller(context, installerType)
                InstallerType.ROOT -> handleRootInstaller(installerType)
                else -> settingsRepository.setInstallerType(installerType)
            }
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

    private suspend fun handleRootInstaller(installerType: InstallerType) {
        if (isMagiskGranted()) {
            settingsRepository.setInstallerType(installerType)
        }
    }

    fun setLegacyInstallerComponent(component: LegacyInstallerComponent?) {
        viewModelScope.launch {
            settingsRepository.setLegacyInstallerComponent(component)
        }
    }

    companion object {
        val localeCodesList: List<String> = BuildConfig.DETECTED_LOCALES
            .toList()
            .updateAsMutable { add(0, "system") }
    }
}

private fun String.toLocale(): Locale = when {
    contains("-r") -> Locale.Builder()
        .setLanguage(substring(0, 2))
        .setRegion(substring(4))
        .build()
    contains("_") -> Locale.Builder()
        .setLanguage(substring(0, 2))
        .setRegion(substring(3))
        .build()
    else -> Locale.Builder()
        .setLanguage(this)
        .build()
}
