package com.looker.droidify.di

import com.looker.droidify.BuildConfig.BUILD_TYPE
import com.looker.droidify.BuildConfig.VERSION_NAME
import com.looker.droidify.network.Downloader
import com.looker.droidify.network.KtorDownloader
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.UserAgent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Singleton
    @Provides
    fun provideHttpClient(): HttpClient {
        return HttpClient(OkHttp) {
            install(UserAgent) {
                agent = "Droid-ify/${VERSION_NAME}-${BUILD_TYPE}"
            }
            install(HttpTimeout) {
                connectTimeoutMillis = 30_000L
                socketTimeoutMillis = 15_000L
            }
        }
    }

    @Singleton
    @Provides
    fun provideDownloader(
        httpClient: HttpClient,
        @IoDispatcher
        dispatcher: CoroutineDispatcher,
    ): Downloader = KtorDownloader(
        client = httpClient,
        dispatcher = dispatcher,
    )
}
