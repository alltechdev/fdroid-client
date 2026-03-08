package com.atd.store.compose.appList

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import com.atd.store.data.AppRepository
import com.atd.store.data.model.AppMinimal
import com.atd.store.datastore.SettingsRepository
import com.atd.store.datastore.get
import com.atd.store.datastore.model.SortOrder
import com.atd.store.sync.v2.model.DefaultName
import com.atd.store.utility.common.extension.asStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce

@HiltViewModel
@OptIn(FlowPreview::class)
class AppListViewModel @Inject constructor(
    private val appRepository: AppRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    val searchQuery = TextFieldState("")
    private val searchQueryStream = snapshotFlow { searchQuery.text.toString() }.debounce(300)

    val categories = appRepository.categories.asStateFlow(emptyList())

    private val _selectedCategories = MutableStateFlow<Set<DefaultName>>(emptySet())
    val selectedCategories: StateFlow<Set<DefaultName>> = _selectedCategories

    val sortOrderFlow = settingsRepository.get { sortOrder }.asStateFlow(SortOrder.UPDATED)

    @OptIn(ExperimentalCoroutinesApi::class)
    val appsState: StateFlow<List<AppMinimal>> = combine(
        searchQueryStream,
        selectedCategories,
        sortOrderFlow,
    ) { searchQuery, categories, sortOrder ->
        appRepository.apps(
            sortOrder = sortOrder,
            searchQuery = searchQuery,
            categoriesToInclude = categories.toList(),
        )
    }.asStateFlow(emptyList())

    fun toggleCategory(category: DefaultName) {
        val currentCategories = _selectedCategories.value
        _selectedCategories.value = if (currentCategories.contains(category)) {
            currentCategories - category
        } else {
            currentCategories + category
        }
    }
}
