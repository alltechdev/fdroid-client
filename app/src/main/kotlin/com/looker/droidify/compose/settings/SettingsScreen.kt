package com.looker.droidify.compose.settings

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.looker.droidify.BuildConfig
import com.looker.droidify.R
import com.looker.droidify.compose.components.BackButton
import com.looker.droidify.compose.settings.SettingsViewModel.Companion.localeCodesList
import com.looker.droidify.compose.settings.components.ActionSettingItem
import com.looker.droidify.compose.settings.components.SelectionSettingItem
import com.looker.droidify.compose.settings.components.SettingHeader
import com.looker.droidify.compose.settings.components.SwitchSettingItem
import com.looker.droidify.datastore.model.InstallerType
import com.looker.droidify.datastore.model.LegacyInstallerComponent
import com.looker.droidify.datastore.model.Theme
import com.looker.droidify.utility.common.SdkCheck
import java.util.*

private const val FOXY_DROID_TITLE = "FoxyDroid"
private const val FOXY_DROID_URL = "https://github.com/kitsunyan/foxy-droid"
private const val DROID_IFY_TITLE = "Droid-ify"
private const val DROID_IFY_URL = "https://github.com/Droid-ify/client"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBackClick: () -> Unit,
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.settings)) },
                navigationIcon = { BackButton(onBackClick) },
            )
        },
        snackbarHost = { SnackbarHost(viewModel.snackbarHostState) },
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
        ) {
            item { SettingHeader(title = stringResource(R.string.prefs_personalization)) }

            item {
                LanguageSetting(
                    selectedLanguage = settings.language,
                    onLanguageSelected = viewModel::setLanguage,
                )
            }

            item {
                ThemeSetting(
                    selectedTheme = settings.theme,
                    onThemeSelected = viewModel::setTheme,
                )
            }

            if (SdkCheck.isSnowCake) {
                item {
                    SwitchSettingItem(
                        title = stringResource(R.string.material_you),
                        description = stringResource(R.string.material_you_desc),
                        checked = settings.dynamicTheme,
                        onCheckedChange = viewModel::setDynamicTheme,
                    )
                }
            }

            item {
                SwitchSettingItem(
                    title = stringResource(R.string.home_screen_swiping),
                    description = stringResource(R.string.home_screen_swiping_DESC),
                    checked = settings.homeScreenSwiping,
                    onCheckedChange = viewModel::setHomeScreenSwiping,
                )
            }

            item { SettingHeader(title = stringResource(R.string.updates)) }

            item {
                SwitchSettingItem(
                    title = stringResource(R.string.auto_update),
                    description = stringResource(R.string.auto_update_apps),
                    checked = settings.autoUpdate,
                    onCheckedChange = viewModel::setAutoUpdate,
                )
            }

            item {
                SwitchSettingItem(
                    title = stringResource(R.string.notify_about_updates),
                    description = stringResource(R.string.notify_about_updates_summary),
                    checked = settings.notifyUpdate,
                    onCheckedChange = viewModel::setNotifyUpdates,
                )
            }

            item { SettingHeader(title = stringResource(R.string.install_types)) }

            item {
                InstallerTypeSetting(
                    selectedInstaller = settings.installerType,
                    onInstallerSelected = { viewModel.setInstaller(context, it) },
                )
            }

            if (settings.installerType == InstallerType.LEGACY) {
                item {
                    LegacyInstallerComponentSetting(
                        selectedComponent = settings.legacyInstallerComponent,
                        onComponentSelected = viewModel::setLegacyInstallerComponent,
                    )
                }
            }

            item { SettingHeader(title = stringResource(R.string.credits)) }

            item {
                ActionSettingItem(
                    title = stringResource(R.string.special_credits),
                    description = FOXY_DROID_TITLE,
                    onClick = { uriHandler.openUri(FOXY_DROID_URL) },
                )
            }

            item {
                ActionSettingItem(
                    title = DROID_IFY_TITLE,
                    description = BuildConfig.VERSION_NAME,
                    onClick = { uriHandler.openUri(DROID_IFY_URL) },
                )
            }
        }
    }
}

@Composable
private fun LanguageSetting(
    selectedLanguage: String,
    onLanguageSelected: (String) -> Unit,
) {
    val context = LocalContext.current
    SelectionSettingItem(
        title = stringResource(R.string.prefs_language_title),
        selectedValue = selectedLanguage,
        values = localeCodesList,
        onValueSelected = onLanguageSelected,
        valueToString = { code ->
            context.translateLocale(context.getLocaleOfCode(code))
        },
    )
}

@Composable
private fun ThemeSetting(
    selectedTheme: Theme,
    onThemeSelected: (Theme) -> Unit,
) {
    SelectionSettingItem(
        title = stringResource(R.string.theme),
        dialogTitle = stringResource(R.string.themes),
        selectedValue = selectedTheme,
        values = Theme.entries,
        onValueSelected = onThemeSelected,
        valueToString = { theme ->
            when (theme) {
                Theme.SYSTEM -> stringResource(R.string.system)
                Theme.SYSTEM_BLACK -> "${stringResource(R.string.system)} ${stringResource(R.string.amoled)}"
                Theme.LIGHT -> stringResource(R.string.light)
                Theme.DARK -> stringResource(R.string.dark)
                Theme.AMOLED -> stringResource(R.string.amoled)
            }
        },
    )
}

@Composable
private fun InstallerTypeSetting(
    selectedInstaller: InstallerType,
    onInstallerSelected: (InstallerType) -> Unit,
) {
    SelectionSettingItem(
        title = stringResource(R.string.installer),
        selectedValue = selectedInstaller,
        values = InstallerType.entries,
        onValueSelected = onInstallerSelected,
        valueToString = { installer ->
            when (installer) {
                InstallerType.LEGACY -> stringResource(R.string.legacy_installer)
                InstallerType.SESSION -> stringResource(R.string.session_installer)
                InstallerType.SHIZUKU -> stringResource(R.string.shizuku_installer)
                InstallerType.ROOT -> stringResource(R.string.root_installer)
            }
        },
    )
}

@Composable
private fun LegacyInstallerComponentSetting(
    selectedComponent: LegacyInstallerComponent?,
    onComponentSelected: (LegacyInstallerComponent?) -> Unit,
) {
    val context = LocalContext.current
    val installerOptions = remember { context.getInstallerOptions() }

    SelectionSettingItem(
        title = stringResource(R.string.legacyInstallerComponent),
        selectedValue = selectedComponent ?: LegacyInstallerComponent.Unspecified,
        values = installerOptions,
        onValueSelected = onComponentSelected,
        valueToString = { component ->
            when (component) {
                is LegacyInstallerComponent.Component -> {
                    val appLabel = runCatching {
                        val info = context.packageManager.getApplicationInfo(component.clazz, 0)
                        context.packageManager.getApplicationLabel(info).toString()
                    }.getOrElse { component.clazz }
                    "$appLabel (${component.activity})"
                }

                LegacyInstallerComponent.Unspecified -> stringResource(R.string.unspecified)
                LegacyInstallerComponent.AlwaysChoose -> stringResource(R.string.always_choose)
            }
        },
    )
}

@Suppress("DEPRECATION")
private fun Context.getInstallerOptions(): List<LegacyInstallerComponent> {
    val intent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
        setDataAndType("content://".toUri(), "application/vnd.android.package-archive")
    }
    val activities = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
    return listOf(
        LegacyInstallerComponent.Unspecified,
        LegacyInstallerComponent.AlwaysChoose,
    ) + activities.map {
        LegacyInstallerComponent.Component(
            clazz = it.activityInfo.packageName,
            activity = it.activityInfo.name,
        )
    }
}

@Suppress("DEPRECATION")
private fun Context.getLocaleOfCode(localeCode: String): Locale? = when {
    localeCode.isEmpty() -> if (SdkCheck.isNougat) {
        resources.configuration.locales[0]
    } else {
        resources.configuration.locale
    }

    localeCode.contains("-r") -> Locale(localeCode.substring(0, 2), localeCode.substring(4))
    localeCode.contains("_") -> Locale(localeCode.substring(0, 2), localeCode.substring(3))
    localeCode == "system" -> null
    else -> Locale(localeCode)
}

private fun Context.translateLocale(locale: Locale?): String {
    val country = locale?.getDisplayCountry(locale)
    val language = locale?.getDisplayLanguage(locale)
    return if (locale != null) {
        val capitalizedLanguage = language?.replaceFirstChar { it.uppercase(Locale.getDefault()) }
        val countrySuffix = if (country?.isNotEmpty() == true && country.compareTo(
                language.toString(),
                ignoreCase = true
            ) != 0
        ) {
            "($country)"
        } else {
            ""
        }
        "$capitalizedLanguage$countrySuffix"
    } else {
        getString(R.string.system)
    }
}
