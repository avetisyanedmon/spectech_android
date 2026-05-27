package com.spectech.domain.serializers

import com.spectech.domain.enums.EquipmentCategory
import com.spectech.domain.enums.PaymentType
import com.spectech.domain.enums.PricingUnit
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Lenient deserializer that maps unknown enum values to `null` instead of
 * failing the whole payload decode. Used on optional fields that the iOS app
 * also tolerates being absent — e.g. `Order.equipmentCategory`, where a
 * single stale category string shouldn't blow up the marketplace list.
 *
 * Mirrors iOS `try? c.decodeIfPresent(EquipmentCategory.self, …)` behaviour.
 */
object NullableEquipmentCategorySerializer : KSerializer<EquipmentCategory?> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("EquipmentCategoryOrNull", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: EquipmentCategory?) {
        if (value == null) encoder.encodeNull()
        else encoder.encodeString(value.wire)
    }

    override fun deserialize(decoder: Decoder): EquipmentCategory? =
        runCatching { EquipmentCategory.normalized(decoder.decodeString()) }.getOrNull()
}

object NullablePricingUnitSerializer : KSerializer<PricingUnit?> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("PricingUnitOrNull", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: PricingUnit?) {
        if (value == null) encoder.encodeNull()
        else encoder.encodeString(value.wire)
    }

    override fun deserialize(decoder: Decoder): PricingUnit? =
        runCatching { PricingUnit.normalized(decoder.decodeString()) }.getOrNull()
}

object NullablePaymentTypeSerializer : KSerializer<PaymentType?> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("PaymentTypeOrNull", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: PaymentType?) {
        if (value == null) encoder.encodeNull()
        else encoder.encodeString(value.wire)
    }

    override fun deserialize(decoder: Decoder): PaymentType? =
        runCatching { PaymentType.normalized(decoder.decodeString()) }.getOrNull()
}
