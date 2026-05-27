package com.spectech.platform.storage

/**
 * Encrypted key-value store. Mirrors iOS `KeychainStore`
 * (SpecTechIOS/Infrastructure/Storage/KeychainStore.swift).
 *
 * Implementations must:
 *   - persist values across app launches
 *   - encrypt values with a key bound to the device's hardware-backed KeyStore
 *     where available (StrongBox on supported devices)
 *   - tolerate missing keys (return null instead of throwing)
 *   - not throw on save errors that are recoverable (caller may retry)
 */
interface SecureStorage {
    fun read(key: String): String?
    fun save(key: String, value: String)
    fun delete(key: String)
}
