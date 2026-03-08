package com.atd.store.di

import android.content.Context
import com.atd.store.datastore.SettingsRepository
import com.atd.store.installer.InstallManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object InstallModule {

    @Singleton
    @Provides
    fun providesInstaller(
        @ApplicationContext context: Context,
        settingsRepository: SettingsRepository
    ): InstallManager = InstallManager(context, settingsRepository)
}
