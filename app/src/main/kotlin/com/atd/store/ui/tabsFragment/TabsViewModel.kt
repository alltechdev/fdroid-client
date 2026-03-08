package com.atd.store.ui.tabsFragment

import androidx.lifecycle.ViewModel
import com.atd.store.datastore.SettingsRepository
import com.atd.store.datastore.get
import com.atd.store.ui.tabsFragment.TabsFragment.BackAction
import com.atd.store.utility.common.extension.asStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@HiltViewModel
class TabsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val allowHomeScreenSwiping = settingsRepository
        .get { homeScreenSwiping }
        .asStateFlow(false)

    val isSearchActionItemExpanded = MutableStateFlow(false)

    val backAction = isSearchActionItemExpanded
        .map { expanded ->
            if (expanded) BackAction.CollapseSearchView else BackAction.None
        }
        .asStateFlow(BackAction.None)
}
