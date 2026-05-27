package com.spectech.platform.di

import com.spectech.platform.storage.EncryptedSecureStorage
import com.spectech.platform.storage.SecureStorage
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class StorageModule {
    @Binds
    @Singleton
    abstract fun bindSecureStorage(impl: EncryptedSecureStorage): SecureStorage
}
