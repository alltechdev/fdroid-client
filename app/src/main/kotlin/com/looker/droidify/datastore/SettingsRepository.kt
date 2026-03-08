package com.looker.droidify.datastore

import com.looker.droidify.datastore.model.InstallerType
import com.looker.droidify.datastore.model.LegacyInstallerComponent
import com.looker.droidify.datastore.model.SortOrder
import com.looker.droidify.datastore.model.Theme
import java.util.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

interface SettingsRepository {

    val data: Flow<Settings>

    suspend fun getInitial(): Settings

    suspend fun setLanguage(language: String)

    suspend fun enableNotifyUpdates(enable: Boolean)

    suspend fun setTheme(theme: Theme)

    suspend fun setDynamicTheme(enable: Boolean)

    suspend fun setInstallerType(installerType: InstallerType)

    suspend fun setLegacyInstallerComponent(component: LegacyInstallerComponent?)

    suspend fun setAutoUpdate(allow: Boolean)

    suspend fun setSortOrder(sortOrder: SortOrder)

    suspend fun setCleanupInstant()

    suspend fun setRbLogLastModified(date: Date)

    suspend fun updateLastModifiedDownloadStats(date: Date)

    suspend fun setHomeScreenSwiping(value: Boolean)

    suspend fun setRepoEnabled(repoId: Int, enabled: Boolean)

    fun getEnabledRepoIds(): Flow<Set<Int>>

    suspend fun isRepoEnabled(repoId: Int): Boolean
}

inline fun <T> SettingsRepository.get(crossinline block: suspend Settings.() -> T): Flow<T> {
    return data.map(block).distinctUntilChanged()
}
