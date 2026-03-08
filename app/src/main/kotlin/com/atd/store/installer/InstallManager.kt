package com.atd.store.installer

import android.content.Context
import com.atd.store.data.model.PackageName
import com.atd.store.database.Database
import com.atd.store.datastore.SettingsRepository
import com.atd.store.datastore.get
import com.atd.store.datastore.model.InstallerType
import com.atd.store.installer.installers.Installer
import com.atd.store.installer.installers.LegacyInstaller
import com.atd.store.installer.installers.root.RootInstaller
import com.atd.store.installer.installers.session.SessionInstaller
import com.atd.store.installer.installers.shizuku.ShizukuInstaller
import com.atd.store.installer.model.InstallItem
import com.atd.store.installer.model.InstallState
import com.atd.store.service.SyncService
import com.atd.store.utility.common.Constants
import com.atd.store.utility.common.cache.Cache
import com.atd.store.utility.common.extension.addAndCompute
import com.atd.store.utility.common.extension.filter
import com.atd.store.utility.common.extension.notificationManager
import com.atd.store.utility.common.extension.updateAsMutable
import com.atd.store.utility.notifications.createInstallNotification
import com.atd.store.utility.notifications.installNotification
import com.atd.store.utility.notifications.removeInstallNotification
import com.atd.store.utility.notifications.updatesAvailableNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class InstallManager(
    private val context: Context,
    private val settingsRepository: SettingsRepository
) {

    private val installItems = Channel<InstallItem>()
    private val uninstallItems = Channel<PackageName>()

    val state = MutableStateFlow<Map<PackageName, InstallState>>(emptyMap())

    private var _installer: Installer? = null
        set(value) {
            field?.close()
            field = value
        }
    private val installer: Installer get() = _installer!!

    private val lock = Mutex()
    private val installerPreference = settingsRepository.get { installerType }
    private val notificationManager by lazy { context.notificationManager }

    suspend operator fun invoke() = coroutineScope {
        setupInstaller()
        installer()
        uninstaller()
    }

    fun close() {
        _installer = null
        uninstallItems.close()
        installItems.close()
    }

    suspend infix fun install(installItem: InstallItem) {
        installItems.send(installItem)
    }

    suspend infix fun uninstall(packageName: PackageName) {
        uninstallItems.send(packageName)
    }

    infix fun remove(packageName: PackageName) {
        updateState { remove(packageName) }
    }

    infix fun setFailed(packageName: PackageName) {
        updateState { put(packageName, InstallState.Failed) }
    }

    private fun CoroutineScope.setupInstaller() = launch {
        installerPreference.collectLatest(::setInstaller)
    }

    private fun CoroutineScope.installer() = launch {
        val currentQueue = mutableSetOf<String>()
        installItems.filter { item ->
            currentQueue.addAndCompute(item.packageName.name) { isAdded ->
                if (isAdded) {
                    updateState { put(item.packageName, InstallState.Pending) }
                }
            }
        }.consumeEach { item ->
            if (state.value.containsKey(item.packageName)) {
                updateState { put(item.packageName, InstallState.Installing) }
                notificationManager?.installNotification(
                    packageName = item.packageName.name,
                    notification = context.createInstallNotification(
                        appName = item.packageName.name,
                        state = InstallState.Installing,
                    )
                )
                val result = installer.use { it.install(item) }
                if (result == InstallState.Installed) {
                    val apkFile = Cache.getReleaseFile(context, item.installFileName)
                    apkFile.delete()
                }
                if (result == InstallState.Installed && SyncService.autoUpdating) {
                    val updates = Database.ProductAdapter.getUpdates(false)
                    when {
                        updates.isEmpty() -> {
                            SyncService.autoUpdating = false
                            notificationManager?.cancel(Constants.NOTIFICATION_ID_UPDATES)
                        }
                        updates.map { it.packageName } != SyncService.autoUpdateStartedFor -> {
                            notificationManager?.notify(
                                Constants.NOTIFICATION_ID_UPDATES,
                                updatesAvailableNotification(context, updates),
                            )
                        }
                    }
                }
                notificationManager?.removeInstallNotification(item.packageName.name)
                updateState { put(item.packageName, result) }
                currentQueue.remove(item.packageName.name)
            }
        }
    }

    private fun CoroutineScope.uninstaller() = launch {
        uninstallItems.consumeEach {
            installer.uninstall(it)
        }
    }

    private suspend fun setInstaller(installerType: InstallerType) {
        lock.withLock {
            _installer = when (installerType) {
                InstallerType.LEGACY -> LegacyInstaller(context, settingsRepository)
                InstallerType.SESSION -> SessionInstaller(context)
                InstallerType.SHIZUKU -> ShizukuInstaller(context)
                InstallerType.ROOT -> RootInstaller(context)
            }
        }
    }

    private inline fun updateState(block: MutableMap<PackageName, InstallState>.() -> Unit) {
        state.update { it.updateAsMutable(block) }
    }
}
