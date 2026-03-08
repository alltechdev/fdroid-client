package com.atd.store.di

import android.content.Context
import com.atd.store.data.PrivacyRepository
import com.atd.store.data.local.AtdDatabase
import com.atd.store.data.local.dao.AppDao
import com.atd.store.data.local.dao.AuthDao
import com.atd.store.data.local.dao.DownloadStatsDao
import com.atd.store.data.local.dao.IndexDao
import com.atd.store.data.local.dao.InstalledDao
import com.atd.store.data.local.dao.RBLogDao
import com.atd.store.data.local.dao.RepoDao
import com.atd.store.data.local.atdDatabase
import com.atd.store.datastore.SettingsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Singleton
    @Provides
    fun provideDatabase(
        @ApplicationContext
        context: Context,
    ): AtdDatabase = atdDatabase(context)

    @Singleton
    @Provides
    fun provideAppDao(
        db: AtdDatabase,
    ): AppDao = db.appDao()

    @Singleton
    @Provides
    fun provideRepoDao(
        db: AtdDatabase,
    ): RepoDao = db.repoDao()

    @Singleton
    @Provides
    fun provideAuthDao(
        db: AtdDatabase,
    ): AuthDao = db.authDao()

    @Singleton
    @Provides
    fun provideInstallDao(
        db: AtdDatabase,
    ): InstalledDao = db.installedDao()

    @Singleton
    @Provides
    fun provideIndexDao(
        db: AtdDatabase,
    ): IndexDao = db.indexDao()

    @Singleton
    @Provides
    fun provideRBLogDao(
        db: AtdDatabase,
    ): RBLogDao = db.rbLogDao()

    @Singleton
    @Provides
    fun provideDownloadStatsDao(
        db: AtdDatabase,
    ): DownloadStatsDao = db.downloadStatsDao()

    @Singleton
    @Provides
    fun providePrivacyRepository(
        rblDao: RBLogDao,
        downloadStatsDao: DownloadStatsDao,
        settingsRepository: SettingsRepository,
    ): PrivacyRepository = PrivacyRepository(
        rbDao = rblDao,
        downloadStatsDao = downloadStatsDao,
        settingsRepo = settingsRepository
    )
}
