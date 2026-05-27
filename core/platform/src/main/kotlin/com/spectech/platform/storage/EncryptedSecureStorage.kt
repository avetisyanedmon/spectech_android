package com.spectech.platform.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * EncryptedSharedPreferences-backed [SecureStorage]. Hardware-backed where
 * available; gracefully falls back to software-only on devices without StrongBox.
 *
 * File name `spectech_secure` is excluded from cloud backup / device-transfer
 * via the app's `data_extraction_rules.xml` so the encrypted blob (which would
 * not decrypt on a different device anyway) does not bloat Auto Backup.
 */
@Singleton
class EncryptedSecureStorage @Inject constructor(
    @ApplicationContext private val context: Context,
) : SecureStorage {

    private val prefs: SharedPreferences by lazy { openPrefs() }

    override fun read(key: String): String? = prefs.getString(key, null)

    override fun save(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    override fun delete(key: String) {
        prefs.edit().remove(key).apply()
    }

    private fun openPrefs(): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context,
            FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    companion object {
        const val FILE_NAME = "spectech_secure"
    }
}
