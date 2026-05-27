package com.spectech.domain.enums

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = PaymentTypeSerializer::class)
enum class PaymentType(
    val wire: String,
    val backendCreateValue: String,
    val titleKey: String,
) {
    CASH("cash", "наличные", "payment_cash"),
    NDS("nds", "с ндс", "payment_nds"),
    USN("usn", "усн", "payment_usn");

    companion object {
        fun normalized(raw: String): PaymentType? = when (raw.trim().lowercase()) {
            "cash", "наличные", "наличка" -> CASH
            "nds", "с ндс", "ндс" -> NDS
            "usn", "усн" -> USN
            else -> null
        }
    }
}

object PaymentTypeSerializer : KSerializer<PaymentType> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("PaymentType", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: PaymentType) =
        encoder.encodeString(value.wire)

    override fun deserialize(decoder: Decoder): PaymentType {
        val raw = decoder.decodeString()
        return PaymentType.normalized(raw)
            ?: throw SerializationException("Unknown PaymentType value: $raw")
    }
}
