package com.spectech.domain.enums

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class AppLanguage(val wire: String, val locale: String) {
    @SerialName("english") ENGLISH("english", "en"),
    @SerialName("russian") RUSSIAN("russian", "ru");
}
