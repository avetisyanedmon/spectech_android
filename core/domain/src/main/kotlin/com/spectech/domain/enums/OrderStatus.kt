package com.spectech.domain.enums

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Lenient decoder accepts variant spellings the backend has used at different
 * times: `canceled` vs `cancelled`, `in progress` vs `in_progress`. Mirrors
 * iOS `OrderStatus.init(from:)` in DomainModels.swift.
 */
@Serializable(with = OrderStatusSerializer::class)
enum class OrderStatus(val wire: String, val titleKey: String) {
    OPEN("open", "status_open"),
    PENDING("pending", "status_pending"),
    ACCEPTED("accepted", "status_accepted"),
    IN_PROGRESS("in_progress", "status_in_progress"),
    COMPLETED("completed", "status_completed"),
    CANCELLED("cancelled", "status_cancelled"),
    EXPIRED("expired", "status_expired"),
    CLOSED("closed", "status_closed");
}

object OrderStatusSerializer : KSerializer<OrderStatus> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("OrderStatus", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: OrderStatus) =
        encoder.encodeString(value.wire)

    override fun deserialize(decoder: Decoder): OrderStatus =
        when (decoder.decodeString().trim().lowercase()) {
            "open" -> OrderStatus.OPEN
            "pending" -> OrderStatus.PENDING
            "accepted" -> OrderStatus.ACCEPTED
            "in_progress", "in progress" -> OrderStatus.IN_PROGRESS
            "completed" -> OrderStatus.COMPLETED
            "cancelled", "canceled" -> OrderStatus.CANCELLED
            "expired" -> OrderStatus.EXPIRED
            "closed" -> OrderStatus.CLOSED
            else -> throw SerializationException("Unknown OrderStatus value")
        }
}
