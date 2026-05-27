package com.spectech.data.di

import com.spectech.data.auth.SessionStore
import com.spectech.network.http.SessionProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds the in-process [SessionStore] as the [SessionProvider] the network
 * client depends on. This is the same shape the iOS app uses where
 * `SessionStore: SessionProviding`.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class BindingsModule {
    @Binds
    @Singleton
    abstract fun bindSessionProvider(impl: SessionStore): SessionProvider
}
