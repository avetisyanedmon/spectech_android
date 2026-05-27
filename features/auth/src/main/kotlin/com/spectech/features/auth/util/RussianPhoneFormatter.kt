package com.spectech.features.auth.util

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Formats and masks a Russian phone number as "+7 (XXX) XXX-XX-XX". Stores only
 * the 10 subscriber digits internally. Direct port of iOS
 * `RussianPhoneFormatter` (SpecTechIOS/Features/Auth/RussianPhoneFormatter.swift).
 */
class RussianPhoneFormatter {
    private var _rawDigits by mutableStateOf("")
    val rawDigits: String get() = _rawDigits

    val formatted: String
        get() {
            val d = _rawDigits
            if (d.isEmpty()) return "+7"
            return buildString {
                append("+7")
                if (d.isNotEmpty()) append(" (").append(d.take(minOf(3, d.length)))
                if (d.length >= 3) append(")")
                if (d.length >= 4) append(' ').append(d.substring(3, minOf(6, d.length)))
                if (d.length >= 7) append('-').append(d.substring(6, minOf(8, d.length)))
                if (d.length >= 9) append('-').append(d.substring(8, minOf(10, d.length)))
            }
        }

    /** Full E.164 string ("+7XXXXXXXXXX") or null when the user has not finished typing. */
    val e164: String?
        get() = if (_rawDigits.length == 10) "+7$_rawDigits" else null

    val isComplete: Boolean get() = _rawDigits.length == 10

    fun update(newText: String) {
        val previousFormatted = formatted
        val allDigits = newText.filter { it.isDigit() }

        val subscriberDigits: String = when {
            newText.startsWith("+7") || newText.startsWith("+8") -> allDigits.drop(1)
            allDigits.length >= 11 && allDigits.first() == '8' -> allDigits.drop(1)
            allDigits.length >= 11 && allDigits.first() == '7' -> allDigits.drop(1)
            else -> allDigits
        }

        val next = subscriberDigits.take(10)

        // Backspace handling: when the user deletes a formatting char without
        // removing a digit, drop the trailing digit so backspace feels natural.
        _rawDigits = if (next == _rawDigits && newText.length < previousFormatted.length) {
            _rawDigits.dropLast(1)
        } else {
            next
        }
    }

    fun clear() {
        _rawDigits = ""
    }
}
