package com.spectech.domain.enums

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = PricingUnitSerializer::class)
enum class PricingUnit(
    val wire: String,
    val backendCreateValue: String,
    val titleKey: String,
) {
    PER_HOUR("per_hour", "за час", "unit_hour"),
    PER_SHIFT("per_shift", "за смену", "unit_shift"),
    PER_M3("per_m3", "за м3", "unit_m3"),
    PER_TON("per_ton", "за тонну", "unit_ton"),
    PER_KM("per_km", "за км", "unit_km"),
    PER_TON_KM("per_ton_km", "за т*км", "unit_ton_km"),
    PER_M3_KM("per_m3_km", "за м3*км", "unit_m3_km"),
    PER_M2("per_m2", "за м2", "unit_m2"),
    PER_LINEAR_M("per_linear_m", "за погонный метр", "unit_linear_m");

    companion object {
        fun normalized(raw: String): PricingUnit? = when (raw.trim().lowercase()) {
            "per_hour", "за час" -> PER_HOUR
            "per_shift", "за смену" -> PER_SHIFT
            "per_m3", "за м3", "за м^3" -> PER_M3
            "per_ton", "за тонну" -> PER_TON
            "per_ton_km", "за т*км", "за т-км" -> PER_TON_KM
            "per_m3_km", "за м3*км", "за м3-км" -> PER_M3_KM
            "per_m2", "за м2" -> PER_M2
            "per_linear_m", "за погонный метр" -> PER_LINEAR_M
            "per_km", "за км" -> PER_KM
            else -> null
        }
    }
}

object PricingUnitSerializer : KSerializer<PricingUnit> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("PricingUnit", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: PricingUnit) =
        encoder.encodeString(value.wire)

    override fun deserialize(decoder: Decoder): PricingUnit {
        val raw = decoder.decodeString()
        return PricingUnit.normalized(raw)
            ?: throw SerializationException("Unknown PricingUnit value: $raw")
    }
}
