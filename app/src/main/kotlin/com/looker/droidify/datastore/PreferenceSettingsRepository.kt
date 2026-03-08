package com.looker.droidify.datastore

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.looker.droidify.datastore.model.InstallerType
import com.looker.droidify.datastore.model.LegacyInstallerComponent
import com.looker.droidify.datastore.model.SortOrder
import com.looker.droidify.datastore.model.Theme
import com.looker.droidify.utility.common.extension.updateAsMutable
import java.util.*
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalTime::class)
class PreferenceSettingsRepository(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    override val data: Flow<Settings> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Log.e("PreferencesSettingsRepository", "Error reading preferences.", exception)
            } else {
                throw exception
            }
        }.map(::mapSettings)

    override suspend fun getInitial(): Settings {
        return data.first()
    }

    override suspend fun setLanguage(language: String) =
        LANGUAGE.update(language)

    override suspend fun enableNotifyUpdates(enable: Boolean) =
        NOTIFY_UPDATES.update(enable)

    override suspend fun setTheme(theme: Theme) =
        THEME.update(theme.name)

    override suspend fun setDynamicTheme(enable: Boolean) =
        DYNAMIC_THEME.update(enable)

    override suspend fun setInstallerType(installerType: InstallerType) =
        INSTALLER_TYPE.update(installerType.name)

    override suspend fun setLegacyInstallerComponent(component: LegacyInstallerComponent?) {
        when (component) {
            null -> {
                LEGACY_INSTALLER_COMPONENT_TYPE.update("")
                LEGACY_INSTALLER_COMPONENT_CLASS.update("")
                LEGACY_INSTALLER_COMPONENT_ACTIVITY.update("")
            }

            is LegacyInstallerComponent.Component -> {
                LEGACY_INSTALLER_COMPONENT_TYPE.update("component")
                LEGACY_INSTALLER_COMPONENT_CLASS.update(component.clazz)
                LEGACY_INSTALLER_COMPONENT_ACTIVITY.update(component.activity)
            }

            LegacyInstallerComponent.Unspecified -> {
                LEGACY_INSTALLER_COMPONENT_TYPE.update("unspecified")
                LEGACY_INSTALLER_COMPONENT_CLASS.update("")
                LEGACY_INSTALLER_COMPONENT_ACTIVITY.update("")
            }

            LegacyInstallerComponent.AlwaysChoose -> {
                LEGACY_INSTALLER_COMPONENT_TYPE.update("always_choose")
                LEGACY_INSTALLER_COMPONENT_CLASS.update("")
                LEGACY_INSTALLER_COMPONENT_ACTIVITY.update("")
            }
        }
    }

    override suspend fun setAutoUpdate(allow: Boolean) =
        AUTO_UPDATE.update(allow)

    override suspend fun setSortOrder(sortOrder: SortOrder) =
        SORT_ORDER.update(sortOrder.name)

    override suspend fun setCleanupInstant() =
        LAST_CLEAN_UP.update(Clock.System.now().toEpochMilliseconds())

    override suspend fun setRbLogLastModified(date: Date) =
        LAST_RB_FETCH.update(date.time)

    override suspend fun updateLastModifiedDownloadStats(date: Date) {
        dataStore.edit { pref ->
            val currentValue = pref[LAST_MODIFIED_DS] ?: 0
            if (date.time > currentValue) pref[LAST_MODIFIED_DS] = date.time
        }
    }

    override suspend fun setHomeScreenSwiping(value: Boolean) =
        HOME_SCREEN_SWIPING.update(value)

    override suspend fun setRepoEnabled(repoId: Int, enabled: Boolean) {
        dataStore.edit { preference ->
            val currentSet = preference[ENABLED_REPO_IDS] ?: emptySet()
            val newSet = currentSet.updateAsMutable {
                if (enabled) add(repoId.toString()) else remove(repoId.toString())
            }
            preference[ENABLED_REPO_IDS] = newSet
        }
    }

    override fun getEnabledRepoIds(): Flow<Set<Int>> {
        return data.map { it.enabledRepoIds }
    }

    override suspend fun isRepoEnabled(repoId: Int): Boolean {
        return repoId in data.first().enabledRepoIds
    }

    private fun mapSettings(preferences: Preferences): Settings {
        val installerType =
            InstallerType.valueOf(preferences[INSTALLER_TYPE] ?: InstallerType.Default.name)
        val legacyInstallerComponent = when (preferences[LEGACY_INSTALLER_COMPONENT_TYPE]) {
            "component" -> {
                preferences[LEGACY_INSTALLER_COMPONENT_CLASS]?.takeIf { it.isNotBlank() }
                    ?.let { cls ->
                        preferences[LEGACY_INSTALLER_COMPONENT_ACTIVITY]?.takeIf { it.isNotBlank() }
                            ?.let { act ->
                                LegacyInstallerComponent.Component(cls, act)
                            }
                    }
            }

            "unspecified" -> LegacyInstallerComponent.Unspecified
            "always_choose" -> LegacyInstallerComponent.AlwaysChoose
            else -> null
        }

        val language = preferences[LANGUAGE] ?: "system"
        val notifyUpdate = preferences[NOTIFY_UPDATES] ?: true
        val theme = Theme.valueOf(preferences[THEME] ?: Theme.SYSTEM.name)
        val dynamicTheme = preferences[DYNAMIC_THEME] ?: false
        val autoUpdate = preferences[AUTO_UPDATE] ?: false
        val sortOrder = SortOrder.valueOf(preferences[SORT_ORDER] ?: SortOrder.UPDATED.name)
        val lastCleanup = preferences[LAST_CLEAN_UP]?.let { Instant.fromEpochMilliseconds(it) }
        val lastRbLogFetch = preferences[LAST_RB_FETCH]
        val lastModifiedDownloadStats = preferences[LAST_MODIFIED_DS]?.takeIf { it > 0L }
        val homeScreenSwiping = preferences[HOME_SCREEN_SWIPING] ?: true
        val enabledRepoIds =
            preferences[ENABLED_REPO_IDS]?.mapNotNull { it.toIntOrNull() }?.toSet() ?: emptySet()

        return Settings(
            language = language,
            notifyUpdate = notifyUpdate,
            theme = theme,
            dynamicTheme = dynamicTheme,
            installerType = installerType,
            legacyInstallerComponent = legacyInstallerComponent,
            autoUpdate = autoUpdate,
            sortOrder = sortOrder,
            lastCleanup = lastCleanup,
            lastRbLogFetch = lastRbLogFetch,
            lastModifiedDownloadStats = lastModifiedDownloadStats,
            homeScreenSwiping = homeScreenSwiping,
            enabledRepoIds = enabledRepoIds,
        )
    }

    private suspend inline fun <T> Preferences.Key<T>.update(newValue: T) {
        dataStore.edit { preferences ->
            preferences[this] = newValue
        }
    }

    companion object PreferencesKeys {
        val LANGUAGE = stringPreferencesKey("key_language")
        val NOTIFY_UPDATES = booleanPreferencesKey("key_notify_updates")
        val DYNAMIC_THEME = booleanPreferencesKey("key_dynamic_theme")
        val AUTO_UPDATE = booleanPreferencesKey("key_auto_updates")
        val LAST_CLEAN_UP = longPreferencesKey("key_last_clean_up_time")
        val LAST_RB_FETCH = longPreferencesKey("key_last_rb_logs_fetch_time")
        val LAST_MODIFIED_DS = longPreferencesKey("key_last_modified_download_stats")
        val HOME_SCREEN_SWIPING = booleanPreferencesKey("key_home_swiping")
        val LEGACY_INSTALLER_COMPONENT_CLASS =
            stringPreferencesKey("key_legacy_installer_component_class")
        val LEGACY_INSTALLER_COMPONENT_ACTIVITY =
            stringPreferencesKey("key_legacy_installer_component_activity")
        val LEGACY_INSTALLER_COMPONENT_TYPE =
            stringPreferencesKey("key_legacy_installer_component_type")
        val ENABLED_REPO_IDS = stringSetPreferencesKey("key_enabled_repo_ids")

        // Enums
        val THEME = stringPreferencesKey("key_theme")
        val INSTALLER_TYPE = stringPreferencesKey("key_installer_type")
        val SORT_ORDER = stringPreferencesKey("key_sort_order")

        fun MutablePreferences.setting(settings: Settings): Preferences {
            set(LANGUAGE, settings.language)
            set(NOTIFY_UPDATES, settings.notifyUpdate)
            set(THEME, settings.theme.name)
            set(DYNAMIC_THEME, settings.dynamicTheme)
            when (settings.legacyInstallerComponent) {
                is LegacyInstallerComponent.Component -> {
                    set(LEGACY_INSTALLER_COMPONENT_TYPE, "component")
                    set(LEGACY_INSTALLER_COMPONENT_CLASS, settings.legacyInstallerComponent.clazz)
                    set(
                        LEGACY_INSTALLER_COMPONENT_ACTIVITY,
                        settings.legacyInstallerComponent.activity,
                    )
                }

                LegacyInstallerComponent.Unspecified -> {
                    set(LEGACY_INSTALLER_COMPONENT_TYPE, "unspecified")
                    set(LEGACY_INSTALLER_COMPONENT_CLASS, "")
                    set(LEGACY_INSTALLER_COMPONENT_ACTIVITY, "")
                }

                LegacyInstallerComponent.AlwaysChoose -> {
                    set(LEGACY_INSTALLER_COMPONENT_TYPE, "always_choose")
                    set(LEGACY_INSTALLER_COMPONENT_CLASS, "")
                    set(LEGACY_INSTALLER_COMPONENT_ACTIVITY, "")
                }

                null -> {}
            }
            set(INSTALLER_TYPE, settings.installerType.name)
            set(AUTO_UPDATE, settings.autoUpdate)
            set(SORT_ORDER, settings.sortOrder.name)
            settings.lastCleanup?.toEpochMilliseconds()?.let { set(LAST_CLEAN_UP, it) }
            settings.lastRbLogFetch?.let { set(LAST_RB_FETCH, it) }
            settings.lastModifiedDownloadStats?.let { set(LAST_MODIFIED_DS, it) }
            set(HOME_SCREEN_SWIPING, settings.homeScreenSwiping)
            set(ENABLED_REPO_IDS, settings.enabledRepoIds.map { it.toString() }.toSet())
            return this.toPreferences()
        }
    }
}
