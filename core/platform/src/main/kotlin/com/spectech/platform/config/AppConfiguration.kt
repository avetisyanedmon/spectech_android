package com.spectech.platform.config

import com.spectech.platform.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralized read of build-time configuration. Mirrors the iOS
 * `AppConfiguration` struct (see SpecTechIOS/Infrastructure/Support/AppConfiguration.swift).
 *
 * All values are sourced from [BuildConfig] so build variants and CI can override
 * without changing source code.
 */
@Singleton
class AppConfiguration @Inject constructor() {
    val apiBaseUrl: String = BuildConfig.API_BASE_URL
    val clientId: String = BuildConfig.API_CLIENT_ID
    val clientSecret: String = BuildConfig.API_CLIENT_SECRET
    val bypassAuthFlow: Boolean = BuildConfig.BYPASS_AUTH_FLOW
    val bypassPhone: String = BuildConfig.BYPASS_PHONE
    val bypassCode: String = BuildConfig.BYPASS_CODE
    val pinCertificates: Boolean = BuildConfig.PIN_CERTIFICATES
}
