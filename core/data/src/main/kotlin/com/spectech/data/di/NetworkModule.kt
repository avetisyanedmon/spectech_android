package com.spectech.data.di

import com.spectech.network.http.ApiClient
import com.spectech.network.http.SessionProvider
import com.spectech.network.http.SpecTechJson
import com.spectech.network.http.buildHttpClient
import com.spectech.platform.config.AppConfiguration
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = SpecTechJson

    @Provides
    @Singleton
    fun provideHttpClient(
        config: AppConfiguration,
        sessionProvider: SessionProvider,
    ): HttpClient = buildHttpClient(
        clientId = config.clientId,
        clientSecret = config.clientSecret,
        sessionProvider = sessionProvider,
        pinCertificates = config.pinCertificates,
    )

    @Provides
    @Singleton
    fun provideApiClient(
        client: HttpClient,
        config: AppConfiguration,
    ): ApiClient = ApiClient(client = client, baseUrl = config.apiBaseUrl)
}
