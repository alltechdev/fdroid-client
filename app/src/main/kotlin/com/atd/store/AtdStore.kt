package com.atd.store

import android.annotation.SuppressLint
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.appcompat.app.AppCompatDelegate
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.NetworkType
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.asImage
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.intercept.Interceptor
import coil3.memory.MemoryCache
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.ImageResult
import coil3.request.SuccessResult
import coil3.request.crossfade
import com.atd.store.content.ProductPreferences
import com.atd.store.database.Database
import com.atd.store.datastore.SettingsRepository
import com.atd.store.index.RepositoryUpdater
import com.atd.store.installer.InstallManager
import com.atd.store.network.Downloader
import com.atd.store.receivers.InstalledAppReceiver
import com.atd.store.service.Connection
import com.atd.store.service.SyncService
import com.atd.store.sync.SyncPreference
import com.atd.store.sync.toJobNetworkType
import com.atd.store.utility.common.Constants
import com.atd.store.utility.common.cache.Cache
import com.atd.store.utility.common.extension.getDrawableCompat
import com.atd.store.utility.common.extension.getInstalledPackagesCompat
import com.atd.store.utility.common.extension.jobScheduler
import com.atd.store.utility.extension.toInstalledItem
import com.atd.store.work.CleanUpWorker
import dagger.hilt.android.HiltAndroidApp
import io.ktor.client.HttpClient
import javax.inject.Inject
import kotlin.time.Duration.Companion.hours
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

@HiltAndroidApp
class AtdStore : Application(), SingletonImageLoader.Factory, Configuration.Provider {

    private val parentJob = SupervisorJob()
    private val appScope = CoroutineScope(Dispatchers.Default + parentJob)

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var installer: InstallManager

    @Inject
    lateinit var downloader: Downloader

    @Inject
    lateinit var httpClient: HttpClient

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()

        val databaseUpdated = Database.init(this)
        ProductPreferences.init(this, appScope)
        RepositoryUpdater.init(appScope, downloader)
        listenApplications()
        checkLanguage()
        updatePreference()
        appScope.launch { installer() }

        if (databaseUpdated) forceSyncAll()
    }

    override fun onTerminate() {
        super.onTerminate()
        appScope.cancel("Application Terminated")
        installer.close()
    }

    private fun listenApplications() {
        val installedItems = packageManager
            .getInstalledPackagesCompat()
            ?.map { it.toInstalledItem() }
        if (installedItems != null) {
            Database.InstalledAdapter.putAll(installedItems)
        }
        appScope.launch {
            registerReceiver(
                InstalledAppReceiver(packageManager),
                IntentFilter().apply {
                    addAction(Intent.ACTION_PACKAGE_ADDED)
                    addAction(Intent.ACTION_PACKAGE_REMOVED)
                    addDataScheme("package")
                },
            )
        }
    }

    private fun checkLanguage() {
        appScope.launch {
            val lastSetLanguage = settingsRepository.getInitial().language
            val systemSetLanguage = AppCompatDelegate.getApplicationLocales().toLanguageTags()
            if (systemSetLanguage != lastSetLanguage && lastSetLanguage != "system") {
                settingsRepository.setLanguage(systemSetLanguage)
            }
        }
    }

    private fun updatePreference() {
        CleanUpWorker.scheduleCleanup(applicationContext, 6.hours)
        scheduleSyncJob()
    }

    private fun scheduleSyncJob() {
        val jobScheduler = jobScheduler ?: return
        val isCompleted = jobScheduler.allPendingJobs.none { it.id == Constants.JOB_ID_SYNC }
        if (isCompleted) {
            val syncConditions = SyncPreference(NetworkType.CONNECTED)
            val period = 12.hours.inWholeMilliseconds
            val job = SyncService.Job.create(
                context = this,
                periodMillis = period,
                networkType = syncConditions.toJobNetworkType(),
                isCharging = syncConditions.pluggedIn,
                isBatteryLow = syncConditions.batteryNotLow,
            )
            jobScheduler.schedule(job)
        }
    }

    private fun forceSyncAll() {
        Database.RepositoryAdapter.getAll().forEach {
            if (it.lastModified.isNotEmpty() || it.entityTag.isNotEmpty()) {
                Database.RepositoryAdapter.put(it.copy(lastModified = "", entityTag = ""))
            }
        }
        Connection(
            SyncService::class.java,
            onBind = { connection, binder ->
                binder.sync(SyncService.SyncRequest.FORCE)
                connection.unbind(this)
            },
        ).bind(this)
    }

    class BootReceiver : BroadcastReceiver() {
        @SuppressLint("UnsafeProtectedBroadcastReceiver")
        override fun onReceive(context: Context, intent: Intent) = Unit
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        val memoryCache = MemoryCache.Builder()
            .maxSizePercent(context, 0.25)
            .build()

        val diskCache = DiskCache.Builder()
            .directory(Cache.getImagesDir(this))
            .maxSizePercent(0.05)
            .build()

        return ImageLoader.Builder(this)
            .memoryCache(memoryCache)
            .diskCache(diskCache)
            .error(getDrawableCompat(R.drawable.ic_cannot_load).asImage())
            .crossfade(350)
            .components {
                add(KtorNetworkFetcherFactory(httpClient = { httpClient }))
                add(FallbackIconInterceptor())
            }
            .build()
    }
}

private class FallbackIconInterceptor : Interceptor {
    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val request = chain.request
        val result = chain.proceed()

        if (result is SuccessResult) return result

        val fallbackIconUrl = request.newBuilder()
            .data((request.data as String).replaceAfterLast('/', "icon.png"))
            .build()
        return chain.withRequest(fallbackIconUrl).proceed()
    }
}
