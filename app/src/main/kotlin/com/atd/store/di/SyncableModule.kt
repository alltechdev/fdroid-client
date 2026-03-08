package com.atd.store.di

import android.content.Context
import com.atd.store.sync.LocalSyncable
import com.atd.store.sync.Syncable
import com.atd.store.sync.v2.model.IndexV2
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SyncableModule {

    @Singleton
    @Provides
    fun provideSyncable(
        @ApplicationContext context: Context,
    ): Syncable<IndexV2> = LocalSyncable(context)
}
