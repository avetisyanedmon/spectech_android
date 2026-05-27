package com.spectech.domain.enums

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class UserRole(val wire: String) {
    @SerialName("customer") CUSTOMER("customer"),
    @SerialName("contractor") CONTRACTOR("contractor"),
    @SerialName("admin") ADMIN("admin");

    companion object {
        fun fromWire(value: String?): UserRole =
            entries.firstOrNull { it.wire == value } ?: CUSTOMER
    }
}
