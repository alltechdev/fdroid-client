package com.atd.store.ui.appList

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.atd.store.database.CursorOwner.Request.Available
import com.atd.store.database.CursorOwner.Request.Installed
import com.atd.store.database.CursorOwner.Request.Updates
import com.atd.store.database.Database
import com.atd.store.datastore.SettingsRepository
import com.atd.store.datastore.get
import com.atd.store.datastore.model.SortOrder
import com.atd.store.service.Connection
import com.atd.store.service.SyncService
import com.atd.store.utility.common.extension.asStateFlow
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
