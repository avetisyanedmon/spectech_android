package com.spectech.domain.util

import com.spectech.domain.error.ApiError

/**
 * Normalises Russian phone-number variants to canonical E.164 form (+7XXXXXXXXXX).
 *
 * Mirrors iOS `PhoneNormalizer.normalizeRussianPhone(_:)` in
 * SpecTechIOS/Features/Auth/AuthService.swift. Accepts:
 *   - 10 subscriber digits         → "+7" + digits
 *   - 11 digits starting with '8'  → drop the 8, prepend "+7"
 *   - 11 digits starting with '7'  → prepend "+"
 *
 * Throws [ApiError.InvalidPhone] for anything else, including empty input.
 */
object PhoneNormalizer {
    fun normalizeRussian(input: String): String {
        val digits = input.filter { it.isDigit() }
        if (digits.isEmpty()) throw ApiError.InvalidPhone

        val normalized: String = when {
            digits.length == 10 -> "+7$digits"
            digits.length == 11 && digits.first() == '8' -> "+7" + digits.drop(1)
            digits.length == 11 && digits.first() == '7' -> "+$digits"
            else -> throw ApiError.InvalidPhone
        }
        if (normalized.length != 12) throw ApiError.InvalidPhone
        return normalized
    }
}
