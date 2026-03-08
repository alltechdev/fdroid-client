package com.looker.droidify.ui.appList

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.looker.droidify.database.CursorOwner.Request.Available
import com.looker.droidify.database.CursorOwner.Request.Installed
import com.looker.droidify.database.CursorOwner.Request.Updates
import com.looker.droidify.database.Database
import com.looker.droidify.datastore.SettingsRepository
import com.looker.droidify.datastore.get
import com.looker.droidify.datastore.model.SortOrder
import com.looker.droidify.service.Connection
import com.looker.droidify.service.SyncService
import com.looker.droidify.utility.common.extension.asStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@HiltViewModel
class AppListViewModel
@Inject constructor(
    settingsRepository: SettingsRepository,
) : ViewModel() {

    val state = settingsRepository
        .get { sortOrder }
        .map { AppListState(sortOrder = it) }
        .asStateFlow(AppListState())

    val reposStream = Database.RepositoryAdapter
        .getAllStream()
        .asStateFlow(emptyList())

    val showUpdateAllButton = Database.ProductAdapter
        .getUpdatesStream(false)
        .map { it.isNotEmpty() }
        .asStateFlow(false)

    val syncConnection = Connection(SyncService::class.java)

    fun updateAll() {
        viewModelScope.launch {
            syncConnection.binder?.updateAllApps()
        }
    }
}

data class AppListState(
    val sortOrder: SortOrder = SortOrder.UPDATED,
) {
    fun toRequest(source: AppListFragment.Source, searchQuery: String) = when (source) {
        AppListFragment.Source.AVAILABLE -> Available(
            searchQuery = searchQuery,
            order = sortOrder,
            skipSignatureCheck = false,
        )

        AppListFragment.Source.INSTALLED -> Installed(
            searchQuery = searchQuery,
            order = sortOrder,
            skipSignatureCheck = false,
        )

        AppListFragment.Source.UPDATES -> Updates(
            searchQuery = searchQuery,
            order = sortOrder,
            skipSignatureCheck = false,
        )
    }
}
