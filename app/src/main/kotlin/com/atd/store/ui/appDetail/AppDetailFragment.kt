package com.atd.store.ui.appDetail

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.net.toUri
import androidx.core.os.BundleCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import coil3.load
import coil3.request.allowHardware
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.atd.store.content.ProductPreferences
import com.atd.store.installer.installers.launchShizuku
import com.atd.store.installer.model.InstallState
import com.atd.store.installer.model.isCancellable
import com.atd.store.model.InstalledItem
import com.atd.store.model.Product
import com.atd.store.model.ProductPreference
import com.atd.store.model.Release
import com.atd.store.model.Repository
import com.atd.store.model.findSuggested
import com.atd.store.service.Connection
import com.atd.store.service.DownloadService
import com.atd.store.ui.Message
import com.atd.store.ui.MessageDialog
import com.atd.store.ui.ScreenFragment
import com.atd.store.ui.appDetail.AppDetailViewModel.Companion.ARG_PACKAGE_NAME
import com.atd.store.ui.appDetail.AppDetailViewModel.Companion.ARG_REPO_ADDRESS
import com.atd.store.utility.common.cache.Cache
import com.atd.store.utility.common.extension.getLauncherActivities
import com.atd.store.utility.common.extension.getMutatedIcon
import com.atd.store.utility.common.extension.isFirstItemVisible
import com.atd.store.utility.common.extension.isSystemApplication
import com.atd.store.utility.common.extension.systemBarsPadding
import com.atd.store.utility.common.extension.updateAsMutable
import com.atd.store.utility.extension.mainActivity
import com.atd.store.utility.extension.startUpdate
import com.stfalcon.imageviewer.StfalconImageViewer
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.atd.store.R.string as stringRes

@AndroidEntryPoint
class AppDetailFragment() : ScreenFragment(), AppDetailAdapter.Callbacks {
    companion object {
        private const val STATE_LAYOUT_MANAGER = "layoutManager"
        private const val STATE_ADAPTER = "adapter"
    }

    constructor(packageName: String, repoAddress: String? = null) : this() {
        arguments = bundleOf(
            ARG_PACKAGE_NAME to packageName,
            ARG_REPO_ADDRESS to repoAddress,
        )
    }

    private enum class Action(
        val id: Int,
        val adapterAction: AppDetailAdapter.Action,
    ) {
        INSTALL(1, AppDetailAdapter.Action.INSTALL),
        UPDATE(2, AppDetailAdapter.Action.UPDATE),
        LAUNCH(3, AppDetailAdapter.Action.LAUNCH),
    }

    private class Installed(
        val installedItem: InstalledItem,
        val isSystem: Boolean,
        val launcherActivities: List<Pair<String, String>>,
    )

    private val viewModel: AppDetailViewModel by viewModels()

    @SuppressLint("RestrictedApi")
    private var layoutManagerState: LinearLayoutManager.SavedState? = null

    private var actions = Pair(emptySet<Action>(), null as Action?)
    private var products = emptyList<Pair<Product, Repository>>()
    private var installed: Installed? = null
    private var downloading = false
    private var installing: InstallState? = null

    private var recyclerView: RecyclerView? = null
    private var detailAdapter: AppDetailAdapter? = null
    private var imageViewer: StfalconImageViewer.Builder<Product.Screenshot>? = null

    private val downloadConnection = Connection(
        serviceClass = DownloadService::class.java,
        onBind = { _, binder ->
            lifecycleScope.launch {
                binder.downloadState.collect(::updateDownloadState)
            }
        },
    )

    @SuppressLint("RestrictedApi")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        detailAdapter = AppDetailAdapter(this@AppDetailFragment)
        mainActivity.onToolbarCreated(toolbar)
        toolbar.menu.apply {
            Action.entries.forEach { action ->
                add(0, action.id, 0, action.adapterAction.titleResId)
                    .setIcon(toolbar.context.getMutatedIcon(action.adapterAction.iconResId))
                    .setVisible(false)
                    .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS)
                    .setOnMenuItemClickListener {
                        onActionClick(action.adapterAction)
                        true
                    }
            }
        }

        val content = fragmentBinding.fragmentContent
        content.addView(
            RecyclerView(content.context).apply {
                id = android.R.id.list
                this.layoutManager = LinearLayoutManager(
                    context,
                    LinearLayoutManager.VERTICAL,
                    false,
                )
                isMotionEventSplittingEnabled = false
                isVerticalScrollBarEnabled = false
                adapter = detailAdapter
                (itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
                if (detailAdapter != null && savedInstanceState != null) {
                    BundleCompat.getParcelable(savedInstanceState, STATE_ADAPTER, AppDetailAdapter.SavedState::class.java)
                        ?.let(detailAdapter!!::restoreState)
                }
                layoutManagerState = savedInstanceState?.let { BundleCompat.getParcelable(it, STATE_LAYOUT_MANAGER, LinearLayoutManager.SavedState::class.java) }
                recyclerView = this
                systemBarsPadding(includeFab = false)
            },
        )
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                launch {
                    viewModel.state.collectLatest { state ->
                        products = state.products.mapNotNull { product ->
                            val requiredRepo = state.repos.find { it.id == product.repositoryId }
                            requiredRepo?.let { product to it }
                        }
                        layoutManagerState?.let {
                            recyclerView?.layoutManager!!.onRestoreInstanceState(it)
                        }
                        layoutManagerState = null
                        installed = state.installedItem?.let {
                            with(requireContext().packageManager) {
                                val isSystem = isSystemApplication(viewModel.packageName)
                                val launcherActivities = if (state.isSelf) {
                                    emptyList()
                                } else {
                                    getLauncherActivities(viewModel.packageName)
                                }
                                Installed(it, isSystem, launcherActivities)
                            }
                        }
                        val adapter = recyclerView?.adapter as? AppDetailAdapter

                        // `delay` is cancellable hence it waits for 50 milliseconds to show empty page
                        if (products.isEmpty()) delay(50)

                        adapter?.setProducts(
                            context = requireContext(),
                            packageName = viewModel.packageName,
                            suggestedRepo = state.addressIfUnavailable,
                            products = products,
                            rblogs = state.rblogs,
                            downloads = state.downloads,
                            installedItem = state.installedItem,
                        )
                        updateButtons()
                    }
                }
                launch {
                    viewModel.installerState.collect(::updateInstallState)
                }
                launch {
                    recyclerView?.isFirstItemVisible?.collect(::updateToolbarButtons)
                }
            }
        }

        downloadConnection.bind(requireContext())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        recyclerView = null
        detailAdapter = null
        imageViewer = null

        downloadConnection.unbind(requireContext())
    }

    @SuppressLint("RestrictedApi")
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        val layoutManagerState =
            layoutManagerState ?: recyclerView?.layoutManager?.onSaveInstanceState()
        layoutManagerState?.let { outState.putParcelable(STATE_LAYOUT_MANAGER, it) }
        val adapterState = (recyclerView?.adapter as? AppDetailAdapter)?.saveState()
        adapterState?.let { outState.putParcelable(STATE_ADAPTER, it) }
    }

    private fun updateButtons(
        preference: ProductPreference = ProductPreferences[viewModel.packageName],
    ) {
        val installed = installed
        val product = products.findSuggested(installed?.installedItem)?.first
        val compatible = product != null && product.selectedReleases.firstOrNull()
            .let { it != null && it.incompatibilities.isEmpty() }
        val canInstall = product != null && installed == null && compatible
        val canUpdate =
            product != null && compatible && product.canUpdate(installed?.installedItem) &&
                    !preference.shouldIgnoreUpdate(product.versionCode)
        val canLaunch =
            product != null && installed != null && installed.launcherActivities.isNotEmpty()

        val actions = buildSet {
            if (canInstall) add(Action.INSTALL)
            if (canUpdate) add(Action.UPDATE)
            if (canLaunch) add(Action.LAUNCH)
        }

        val primaryAction = when {
            canUpdate -> Action.UPDATE
            canLaunch -> Action.LAUNCH
            canInstall -> Action.INSTALL
            else -> Action.INSTALL
        }

        val adapterAction = when {
            installing == InstallState.Installing -> null
            installing == InstallState.Pending -> AppDetailAdapter.Action.CANCEL
            downloading -> AppDetailAdapter.Action.CANCEL
            else -> primaryAction.adapterAction
        }

        (recyclerView?.adapter as? AppDetailAdapter)?.action = adapterAction

        for (action in sequenceOf(
            Action.INSTALL,
            Action.UPDATE,
        )) {
            toolbar.menu.findItem(action.id).isEnabled = !downloading
        }
        this.actions = Pair(actions, primaryAction)
        updateToolbarButtons()
    }

    private fun updateToolbarButtons(
        isActionVisible: Boolean = (recyclerView?.layoutManager as LinearLayoutManager)
            .findFirstVisibleItemPosition() == 0,
    ) {
        toolbar.title = if (isActionVisible) {
            getString(stringRes.application)
        } else {
            products.firstOrNull()?.first?.name ?: getString(stringRes.application)
        }
        val (actions, primaryAction) = actions
        val displayActions = actions.updateAsMutable {
            if (isActionVisible && primaryAction != null) {
                remove(primaryAction)
            }
        }
        Action.entries.forEach { action ->
            toolbar.menu.findItem(action.id).isVisible = action in displayActions
        }
    }

    private fun updateInstallState(installerState: InstallState?) {
        val status = when (installerState) {
            InstallState.Pending -> AppDetailAdapter.Status.PendingInstall
            InstallState.Installing -> AppDetailAdapter.Status.Installing
            else -> AppDetailAdapter.Status.Idle
        }
        (recyclerView?.adapter as? AppDetailAdapter)?.status = status
        installing = installerState
        updateButtons()
    }

    private fun updateDownloadState(state: DownloadService.DownloadState) {
        val packageName = viewModel.packageName
        val isPending = packageName in state.queue
        val isDownloading = state isDownloading packageName
        val isCompleted = state isComplete packageName
        val isActive = isPending || isDownloading
        if (isPending) {
            detailAdapter?.status = AppDetailAdapter.Status.Pending
        }
        if (isDownloading) {
            detailAdapter?.status = when (state.currentItem) {
                is DownloadService.State.Connecting -> AppDetailAdapter.Status.Connecting
                is DownloadService.State.Downloading -> AppDetailAdapter.Status.Downloading(
                    state.currentItem.read,
                    state.currentItem.total,
                )

                else -> AppDetailAdapter.Status.Idle
            }
        }
        if (isCompleted) {
            detailAdapter?.status = AppDetailAdapter.Status.Idle
        }
        if (this.downloading != isActive) {
            this.downloading = isActive
            updateButtons()
        }
        if (state.currentItem is DownloadService.State.Success && isResumed) {
            viewModel.installPackage(
                state.currentItem.packageName,
                state.currentItem.release.cacheFileName,
            )
        }
    }

    override fun onActionClick(action: AppDetailAdapter.Action) {
        when (action) {
            AppDetailAdapter.Action.INSTALL,
            AppDetailAdapter.Action.UPDATE,
                -> {
                if (Cache.getEmptySpace(requireContext()) < products.first().first.releases.first().size) {
                    MessageDialog(Message.InsufficientStorage).show(childFragmentManager)
                    return
                }
                val shizukuState = viewModel.shizukuState(requireContext())
                if (shizukuState != null && shizukuState.check) {
                    shizukuDialog(
                        context = requireContext(),
                        shizukuState = shizukuState,
                        openShizuku = { launchShizuku(requireContext()) },
                        switchInstaller = { viewModel.setDefaultInstaller() },
                    ).show()
                    return
                }
                downloadConnection.startUpdate(
                    packageName = viewModel.packageName,
                    installedItem = installed?.installedItem,
                    products = products,
                )
            }

            AppDetailAdapter.Action.LAUNCH -> {
                val launcherActivities = installed?.launcherActivities.orEmpty()
                if (launcherActivities.size >= 2) {
                    LaunchDialog(launcherActivities).show(
                        childFragmentManager,
                        LaunchDialog::class.java.name,
                    )
                } else {
                    launcherActivities.firstOrNull()?.let { startLauncherActivity(it.first) }
                }
            }

            AppDetailAdapter.Action.CANCEL -> {
                val binder = downloadConnection.binder
                if (installing?.isCancellable == true) {
                    viewModel.removeQueue()
                } else if (downloading && binder != null) {
                    binder.cancel(viewModel.packageName)
                }
            }
        }
    }

    private fun startLauncherActivity(name: String) {
        try {
            startActivity(
                Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_LAUNCHER)
                    .setComponent(ComponentName(viewModel.packageName, name))
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onPreferenceChanged(preference: ProductPreference) {
        updateButtons(preference)
    }

    override fun onScreenshotClick(position: Int) {
        if (imageViewer == null) {
            val productRepository = products.findSuggested(installed?.installedItem) ?: return
            val screenshots = productRepository.first.screenshots.mapNotNull {
                if (it.type == Product.Screenshot.Type.VIDEO) null
                else it
            }
            imageViewer = StfalconImageViewer
                .Builder(context, screenshots) { view, current ->
                    val screenshotUrl = current.url(
                        context = requireContext(),
                        repository = productRepository.second,
                        packageName = viewModel.packageName,
                    )
                    view.load(screenshotUrl) {
                        allowHardware(false)
                    }
                }
        }
        imageViewer?.withStartPosition(position)
        imageViewer?.show()
    }

    override fun onRequestAddRepository(address: String) {
    }

    override fun onUriClick(uri: Uri, shouldConfirm: Boolean): Boolean {
        return if (shouldConfirm && (uri.scheme == "http" || uri.scheme == "https")) {
            MessageDialog(Message.Link(uri)).show(childFragmentManager)
            true
        } else {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, uri))
                true
            } catch (e: ActivityNotFoundException) {
                e.printStackTrace()
                false
            }
        }
    }

    class LaunchDialog() : DialogFragment() {
        companion object {
            private const val EXTRA_NAMES = "names"
            private const val EXTRA_LABELS = "labels"
        }

        constructor(launcherActivities: List<Pair<String, String>>) : this() {
            arguments = Bundle().apply {
                putStringArrayList(EXTRA_NAMES, ArrayList(launcherActivities.map { it.first }))
                putStringArrayList(EXTRA_LABELS, ArrayList(launcherActivities.map { it.second }))
            }
        }

        override fun onCreateDialog(savedInstanceState: Bundle?): AlertDialog {
            val names = requireArguments().getStringArrayList(EXTRA_NAMES)!!
            val labels = requireArguments().getStringArrayList(EXTRA_LABELS)!!
            return MaterialAlertDialogBuilder(requireContext())
                .setTitle(stringRes.launch)
                .setItems(labels.toTypedArray()) { _, position ->
                    (parentFragment as AppDetailFragment)
                        .startLauncherActivity(names[position])
                }
                .setNegativeButton(stringRes.cancel, null)
                .create()
        }
    }
}
