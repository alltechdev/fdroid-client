package com.atd.store.di

import android.content.Context
import com.atd.store.data.AppRepository
import com.atd.store.data.InstalledRepository
import com.atd.store.data.RepoRepository
import com.atd.store.data.encryption.EncryptionStorage
import com.atd.store.data.local.dao.AppDao
import com.atd.store.data.local.dao.AuthDao
import com.atd.store.data.local.dao.IndexDao
import com.atd.store.data.local.dao.InstalledDao
import com.atd.store.data.local.dao.RepoDao
import com.atd.store.datastore.SettingsRepository
import com.atd.store.network.Downloader
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher

@Module
@InstallIn(SingletonComponent::class)
object RepoModule {

    @Provides
    fun provideRepoRepository(
        repoDao: RepoDao,
        appDao: AppDao,
        authDao: AuthDao,
        indexDao: IndexDao,
        settingsRepository: SettingsRepository,
        encryptionStorage: EncryptionStorage,
        downloader: Downloader,
        @ApplicationContext context: Context,
        @IoDispatcher syncDispatcher: CoroutineDispatcher,
    ): RepoRepository = RepoRepository(
        encryptionStorage = encryptionStorage,
        downloader = downloader,
        context = context,
        syncDispatcher = syncDispatcher,
        repoDao = repoDao,
        appDao = appDao,
        authDao = authDao,
        indexDao = indexDao,
        settingsRepository = settingsRepository,
    )

    @Provides
    fun provideAppRepository(
        appDao: AppDao,
        repoDao: RepoDao,
        settingsRepository: SettingsRepository,
    ): AppRepository = AppRepository(
        appDao = appDao,
        repoDao = repoDao,
        settingsRepository = settingsRepository,
    )

    @Provides
    fun provideInstalledRepository(
        installedDao: InstalledDao,
    ): InstalledRepository = InstalledRepository(
        installedDao = installedDao,
    )

}