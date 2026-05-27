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
 * 27 equipment categories. Each carries:
 *   - [wire]: snake_case identifier sent on most endpoints
 *   - [backendCreateValue]: Russian display string sent in POST /orders bodies
 *     (the backend stores categories as Russian display strings when creating
 *     an order — see iOS `backendCreateOrderValue`)
 *   - [titleKey]: stable identifier for `strings.xml` lookup; the UI layer
 *     resolves it to the localized human label.
 *
 * Mirrors iOS `EquipmentCategory` in SpecTechIOS/Shared/Models/DomainModels.swift.
 */
@Serializable(with = EquipmentCategorySerializer::class)
enum class EquipmentCategory(
    val wire: String,
    val backendCreateValue: String,
    val titleKey: String,
) {
    CONCRETE_PUMP("concrete_pump", "Автобетононасос", "cat_concrete_pump"),
    AERIAL_PLATFORM("aerial_platform", "Автовышка", "cat_aerial_platform"),
    TRUCK_CRANE("truck_crane", "Автокран", "cat_truck_crane"),
    SEWAGE_VACUUM("sewage_vacuum", "Ассенизатор-илосос", "cat_sewage_vacuum"),
    FUEL_TANKER("fuel_tanker", "Бензовоз / Автоцистерна", "cat_fuel_tanker"),
    CONCRETE_MIXER("concrete_mixer", "Бетоновоз", "cat_concrete_mixer"),
    BULLDOZER("bulldozer", "Бульдозер", "cat_bulldozer"),
    GRADER("grader", "Грейдер", "cat_grader"),
    GRAB_LOADER("grab_loader", "Грейферный погрузчик", "cat_grab_loader"),
    ROAD_ROLLER("road_roller", "Дорожный каток", "cat_road_roller"),
    SOIL_COMPACTOR("soil_compactor", "Грунтовый каток", "cat_soil_compactor"),
    ASPHALT_PAVER("asphalt_paver", "Асфальтоукладчик", "cat_asphalt_paver"),
    MANIPULATOR("manipulator", "Манипулятор", "cat_manipulator"),
    MINI_LOADER("mini_loader", "Мини-погрузчик", "cat_mini_loader"),
    MINI_EXCAVATOR("mini_excavator", "Мини-экскаватор", "cat_mini_excavator"),
    GARBAGE_TRUCK("garbage_truck", "Мусоровоз / Бункеровоз / Ломовоз", "cat_garbage_truck"),
    LOWBED_TRAILER("lowbed_trailer", "Трал / Низкорамная платформа", "cat_lowbed_trailer"),
    DUMP_TRUCK("dump_truck", "Самосвал", "cat_dump_truck"),
    FRONT_LOADER("front_loader", "Фронтальный погрузчик", "cat_front_loader"),
    TOW_TRUCK("tow_truck", "Эвакуатор / Автовоз", "cat_tow_truck"),
    EXCAVATOR_CRAWLER("excavator_crawler", "Экскаватор гусеничный", "cat_excavator_crawler"),
    EXCAVATOR_WHEELED("excavator_wheeled", "Экскаватор колесный", "cat_excavator_wheeled"),
    AUGER_DRILL("auger_drill", "Ямобур", "cat_auger_drill"),
    BACKHOE_LOADER("backhoe_loader", "Экскаватор-погрузчик", "cat_backhoe_loader"),
    BITUMEN_SPRAYER("bitumen_sprayer", "Гудронатор", "cat_bitumen_sprayer"),
    FORKLIFT("forklift", "Вилочный погрузчик", "cat_forklift"),
    ROAD_MAINTENANCE_VEHICLE("road_maintenance_vehicle", "КДМ", "cat_road_maintenance_vehicle");

    companion object {
        /**
         * Accepts every form the backend has ever shipped:
         *   - canonical snake_case wire codes
         *   - Russian display strings (case-insensitive)
         *   - the 8-category legacy aliases that predate the current set
         */
        fun normalized(raw: String): EquipmentCategory? {
            val trimmed = raw.trim()
            val lowered = trimmed.lowercase().replace('-', '_')
            entries.firstOrNull { it.wire == lowered }?.let { return it }

            val ruLower = trimmed.lowercase()
            entries.firstOrNull { it.backendCreateValue.lowercase() == ruLower }?.let { return it }

            return when (lowered) {
                "excavator", "экскаватор" -> EXCAVATOR_CRAWLER
                "loader", "погрузчик" -> FRONT_LOADER
                "crane", "кран" -> TRUCK_CRANE
                "compactor", "каток" -> ROAD_ROLLER
                else -> null
            }
        }
    }
}

object EquipmentCategorySerializer : KSerializer<EquipmentCategory> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("EquipmentCategory", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: EquipmentCategory) =
        encoder.encodeString(value.wire)

    override fun deserialize(decoder: Decoder): EquipmentCategory {
        val raw = decoder.decodeString()
        return EquipmentCategory.normalized(raw)
            ?: throw SerializationException("Unknown EquipmentCategory value: $raw")
    }
}
