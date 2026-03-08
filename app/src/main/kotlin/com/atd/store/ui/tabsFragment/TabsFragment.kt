package com.atd.store.ui.tabsFragment

import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.SearchView
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import com.atd.store.R
import com.atd.store.database.Database
import com.atd.store.databinding.TabsToolbarBinding
import com.atd.store.service.Connection
import com.atd.store.service.SyncService
import com.atd.store.ui.ScreenFragment
import com.atd.store.ui.appList.AppListFragment
import com.atd.store.utility.common.device.Huawei
import com.atd.store.utility.common.extension.getMutatedIcon
import com.atd.store.utility.common.sdkAbove
import com.atd.store.utility.extension.mainActivity
import com.atd.store.widget.FocusSearchView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import com.atd.store.R.string as stringRes

@AndroidEntryPoint
class TabsFragment : ScreenFragment() {

    enum class BackAction {
        CollapseSearchView,
        None,
    }

    private var _tabsBinding: TabsToolbarBinding? = null
    private val tabsBinding get() = _tabsBinding!!

    private val viewModel: TabsViewModel by viewModels()

    companion object {
        private const val STATE_SEARCH_FOCUSED = "searchFocused"
        private const val STATE_SEARCH_QUERY = "searchQuery"
    }

    private var searchMenuItem: MenuItem? = null
    private var syncRepositoriesMenuItem: MenuItem? = null
    private var viewPager: ViewPager2? = null
    private var onBackPressedCallback: OnBackPressedCallback? = null

    private var searchQuery = ""
    private var pendingSearchQuery: String? = null

    private val syncConnection = Connection(
        serviceClass = SyncService::class.java,
        onBind = { _, _ ->
            viewPager?.let {
                val source = AppListFragment.Source.entries[it.currentItem]
                updateUpdateNotificationBlocker(source)
            }
        },
    )

    private var needSelectUpdates = false

    private val productFragments: Sequence<AppListFragment>
        get() = if (host == null) {
            emptySequence()
        } else {
            childFragmentManager.fragments.asSequence().mapNotNull { it as? AppListFragment }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _tabsBinding = TabsToolbarBinding.inflate(layoutInflater)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        syncConnection.bind(requireContext())

        mainActivity.onToolbarCreated(toolbar)
        toolbar.title = getString(stringRes.application_name)
        // Move focus from SearchView to Toolbar
        toolbar.isFocusable = true

        val searchView = FocusSearchView(toolbar.context).apply {
            maxWidth = Int.MAX_VALUE
            queryHint = getString(stringRes.search)
            setOnQueryTextListener(
                object : SearchView.OnQueryTextListener {
                    override fun onQueryTextSubmit(query: String?): Boolean {
                        clearFocus()
                        return true
                    }

                    override fun onQueryTextChange(newText: String?): Boolean {
                        if (isResumed) {
                            setSearchQuery(newText)
                        }
                        return true
                    }
                },
            )
        }

        toolbar.menu.apply {
            if (!Huawei.isHuaweiEmui) {
                sdkAbove(Build.VERSION_CODES.P) {
                    setGroupDividerEnabled(true)
                }
            }

            searchMenuItem = add(0, R.id.toolbar_search, 0, stringRes.search)
                .setIcon(toolbar.context.getMutatedIcon(R.drawable.ic_search))
                .setActionView(searchView)
                .setShowAsActionFlags(
                    MenuItem.SHOW_AS_ACTION_ALWAYS or MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW,
                )
                .setOnActionExpandListener(
                    object : MenuItem.OnActionExpandListener {
                        override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                            viewModel.isSearchActionItemExpanded.value = true
                            return true
                        }

                        override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                            viewModel.isSearchActionItemExpanded.value = false
                            return true
                        }
                    },
                )

            syncRepositoriesMenuItem = add(0, R.id.toolbar_sync, 0, stringRes.sync)
                .setIcon(toolbar.context.getMutatedIcon(R.drawable.ic_sync))
                .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS)
                .setOnMenuItemClickListener {
                    syncConnection.binder?.sync(SyncService.SyncRequest.MANUAL)
                    true
                }

            add(1, 0, 0, stringRes.settings)
                .setOnMenuItemClickListener {
                    view.post { mainActivity.navigatePreferences() }
                    true
                }
        }

        toolbar.post {
            toolbar.findViewById<View>(R.id.toolbar_sync)?.setOnLongClickListener {
                Database.RepositoryAdapter.getAll().forEach {
                    if (it.lastModified.isNotEmpty() || it.entityTag.isNotEmpty()) {
                        Database.RepositoryAdapter.put(it.copy(lastModified = "", entityTag = ""))
                    }
                }
                syncConnection.binder?.sync(SyncService.SyncRequest.FORCE)
                true
            }
        }

        searchQuery = savedInstanceState?.getString(STATE_SEARCH_QUERY).orEmpty()
        setSearchQuery(searchQuery)

        val toolbarExtra = fragmentBinding.toolbarExtra
        toolbarExtra.addView(tabsBinding.root)

        val content = fragmentBinding.fragmentContent

        viewPager = ViewPager2(content.context).apply {
            id = R.id.fragment_pager
            adapter = object : FragmentStateAdapter(this@TabsFragment) {
                override fun getItemCount(): Int = AppListFragment.Source.entries.size
                override fun createFragment(position: Int): Fragment = AppListFragment(
                    source = AppListFragment.Source.entries[position],
                ).also { it.setSearchQuery(searchQuery) }
            }
            content.addView(this)
            registerOnPageChangeCallback(pageChangeCallback)
            offscreenPageLimit = 1
        }

        viewPager?.let {
            TabLayoutMediator(tabsBinding.tabs, it) { tab, position ->
                tab.text = getString(AppListFragment.Source.entries[position].titleResId)
            }.attach()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                launch {
                    viewModel.allowHomeScreenSwiping.collect {
                        viewPager?.isUserInputEnabled = it
                    }
                }
                launch {
                    viewModel.backAction.collect {
                        onBackPressedCallback?.isEnabled = it != BackAction.None
                    }
                }
                launch {
                    SyncService.syncState.collect {
                        when (it) {
                            is SyncService.State.Connecting -> {
                                tabsBinding.syncState.isVisible = true
                                tabsBinding.syncState.isIndeterminate = true
                            }

                            SyncService.State.Finish -> {
                                tabsBinding.syncState.isGone = true
                            }

                            is SyncService.State.Syncing -> {
                                tabsBinding.syncState.isVisible = true
                                tabsBinding.syncState.setProgressCompat(it.progress, true)
                            }
                        }
                    }
                }
            }
        }

        onBackPressedCallback = object : OnBackPressedCallback(enabled = false) {
            override fun handleOnBackPressed() {
                performOnBackPressed()
            }
        }
        onBackPressedCallback?.let {
            mainActivity.onBackPressedDispatcher.addCallback(
                viewLifecycleOwner,
                it,
            )
        }
    }

    override fun onResume() {
        super.onResume()

        val psq = pendingSearchQuery
        if (psq != null) {
            activateSearch(psq)
            pendingSearchQuery = null
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        searchMenuItem = null
        syncRepositoriesMenuItem = null
        viewPager = null

        syncConnection.unbind(requireContext())

        _tabsBinding = null
        onBackPressedCallback = null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putBoolean(STATE_SEARCH_FOCUSED, searchMenuItem?.actionView?.hasFocus() == true)
        outState.putString(STATE_SEARCH_QUERY, searchQuery)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)

        (searchMenuItem?.actionView as FocusSearchView).allowFocus = true
        if (needSelectUpdates) {
            selectUpdatesInternal(false)
        }
    }

    private fun performOnBackPressed() {
        when (viewModel.backAction.value) {
            BackAction.CollapseSearchView -> {
                searchMenuItem?.collapseActionView()
            }

            BackAction.None -> {
                // should never be called
            }
        }
    }

    internal fun selectUpdates() = selectUpdatesInternal(true)

    private fun updateUpdateNotificationBlocker(activeSource: AppListFragment.Source) {
        val blockerFragment = if (activeSource == AppListFragment.Source.UPDATES) {
            productFragments.find { it.source == activeSource }
        } else {
            null
        }
        syncConnection.binder?.setUpdateNotificationBlocker(blockerFragment)
    }

    private fun selectUpdatesInternal(allowSmooth: Boolean) {
        if (view != null) {
            val viewPager = viewPager
            viewPager?.setCurrentItem(
                AppListFragment.Source.UPDATES.ordinal,
                allowSmooth && viewPager.isLaidOut,
            )
        } else {
            needSelectUpdates = true
        }
    }

    fun activateSearch(query: String?) {
        if (query.isNullOrBlank()) return

        if (isResumed) {
            val searchMenuItem = searchMenuItem!!
            searchMenuItem.expandActionView()

            val searchView = searchMenuItem.actionView as FocusSearchView
            searchView.setQuery(query, true)
        } else {
            pendingSearchQuery = query
        }
    }

    private fun setSearchQuery(query: String?) {
        val newSearchQuery = query.orEmpty()
        searchQuery = newSearchQuery
        productFragments.forEach { it.setSearchQuery(newSearchQuery) }
    }

    private val pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            val source = AppListFragment.Source.entries[position]
            updateUpdateNotificationBlocker(source)
            syncRepositoriesMenuItem!!.setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS)
        }

        override fun onPageScrollStateChanged(state: Int) {
            if (state == ViewPager2.SCROLL_STATE_IDLE) {
                val source = AppListFragment.Source.entries[viewPager!!.currentItem]
                updateUpdateNotificationBlocker(source)
            }
        }
    }
}
