package com.looker.droidify.ui.tabsFragment

import androidx.lifecycle.ViewModel
import com.looker.droidify.datastore.SettingsRepository
import com.looker.droidify.datastore.get
import com.looker.droidify.ui.tabsFragment.TabsFragment.BackAction
import com.looker.droidify.utility.common.extension.asStateFlow
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
