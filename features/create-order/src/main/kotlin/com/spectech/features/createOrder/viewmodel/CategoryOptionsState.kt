package com.spectech.features.createOrder.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import com.spectech.domain.enums.EquipmentCategory

/**
 * Per-category optional fields for the Create Order form. Mirrors the
 * `optX` state cluster on iOS `CreateOrderViewModel`
 * (SpecTechIOS/Features/CreateOrder/CreateOrderView.swift:76-102).
 *
 * Owned by [CreateOrderViewModel] and exposed to the UI as a stateful object
 * so the screen can mutate fields directly without round-tripping every
 * change through the VM. [buildSummary] then turns the populated fields into
 * the "Параметры техники: …" sentence that gets prepended to the order
 * description before submit.
 *
 * Fields default to empty strings / nulls / empty sets — only the fields that
 * apply to the chosen [EquipmentCategory] are rendered by
 * `CategoryOptionsSection`. Switching categories does **not** clear the
 * fields: iOS keeps them around too, so a user can flip back and forth
 * without losing what they typed.
 */
class CategoryOptionsState {

    // Numeric / free-text (decimal-style strings the user typed). String not
    // Double so the field accepts partial input ("12.", ",5") without losing
    // the value mid-edit. Trimmed + comma-normalised when summarised.
    var boomLength by mutableStateOf("")          // concrete pump, aerial platform (reach), truck crane, manipulator
    var liftCapacity by mutableStateOf("")        // truck crane, manipulator, forklift
    var tonnage by mutableStateOf("")             // road roller, mini excavator
    var volumeCubic by mutableStateOf("")         // bitumen sprayer
    var drillingDepth by mutableStateOf("")       // auger drill
    var drillingDiameter by mutableStateOf("")    // auger drill
    var inertMaterial by mutableStateOf("")       // dump truck — free-text material name

    // Single-select (null = no selection)
    var sewageType by mutableStateOf<String?>(null)
    var fuelTankerType by mutableStateOf<String?>(null)
    var graderSize by mutableStateOf<String?>(null)
    var asphaltPaverSize by mutableStateOf<String?>(null)
    var roadRollerType by mutableStateOf<String?>(null)
    var soilCompactorType by mutableStateOf<String?>(null)
    var miniLoaderBase by mutableStateOf<String?>(null)
    var garbageTruckType by mutableStateOf<String?>(null)
    var lowbedType by mutableStateOf<String?>(null)
    var dumpTruckAxles by mutableStateOf<String?>(null)
    var frontLoaderBucket by mutableStateOf<String?>(null)
    var excavatorBucket by mutableStateOf<String?>(null)
    var towType by mutableStateOf<String?>(null)
    var backhoeBucketWidth by mutableStateOf<String?>(null)

    // Multi-select attachments — observable lists so Compose recomposes on toggle.
    val miniLoaderAttachments: SnapshotStateList<String> = mutableListOf<String>().toMutableStateList()
    val backhoeAttachments: SnapshotStateList<String> = mutableListOf<String>().toMutableStateList()

    fun toggleMiniLoaderAttachment(option: String) {
        if (miniLoaderAttachments.contains(option)) miniLoaderAttachments.remove(option)
        else miniLoaderAttachments.add(option)
    }

    fun toggleBackhoeAttachment(option: String) {
        if (backhoeAttachments.contains(option)) backhoeAttachments.remove(option)
        else backhoeAttachments.add(option)
    }

    /**
     * Builds the human-readable Russian summary of the category-specific
     * options the user filled in. Returns `null` if nothing applies / is set.
     * Caller prepends this to the order description so contractors see it
     * inline with the rest of the request.
     *
     * Mirrors iOS `CreateOrderViewModel.buildOptionsSummary()`
     * (SpecTechIOS/Features/CreateOrder/CreateOrderView.swift:130-201) — same
     * Russian field labels and same "Параметры техники: A; B; C." sentence
     * structure so the marketplace card's parser ([OrderCardView] from
     * Section 5) keeps recognising the prefix.
     */
    fun buildSummary(category: EquipmentCategory): String? {
        val parts = mutableListOf<String>()

        fun trimmedOrNull(value: String): String? =
            value.trim().replace(',', '.').takeIf { it.isNotEmpty() }

        when (category) {
            EquipmentCategory.CONCRETE_PUMP ->
                trimmedOrNull(boomLength)?.let { parts += "длина стрелы: $it м" }
            EquipmentCategory.AERIAL_PLATFORM ->
                trimmedOrNull(boomLength)?.let { parts += "вылет стрелы: $it м" }
            EquipmentCategory.TRUCK_CRANE, EquipmentCategory.MANIPULATOR -> {
                trimmedOrNull(liftCapacity)?.let { parts += "грузоподъёмность: $it т" }
                trimmedOrNull(boomLength)?.let { parts += "длина стрелы: $it м" }
            }
            EquipmentCategory.SEWAGE_VACUUM ->
                sewageType?.let { parts += "тип: $it" }
            EquipmentCategory.FUEL_TANKER ->
                fuelTankerType?.let { parts += "тип: $it" }
            EquipmentCategory.GRADER ->
                graderSize?.let { parts += "размер: $it" }
            EquipmentCategory.ASPHALT_PAVER ->
                asphaltPaverSize?.let { parts += "размер: $it" }
            EquipmentCategory.ROAD_ROLLER -> {
                roadRollerType?.let { parts += "тип: $it" }
                trimmedOrNull(tonnage)?.let { parts += "тоннаж: $it т" }
            }
            EquipmentCategory.SOIL_COMPACTOR ->
                soilCompactorType?.let { parts += "тип: $it" }
            EquipmentCategory.MINI_LOADER -> {
                miniLoaderBase?.let { parts += "ходовая: $it" }
                if (miniLoaderAttachments.isNotEmpty()) {
                    parts += "доп. оборудование: ${miniLoaderAttachments.sorted().joinToString(", ")}"
                }
            }
            EquipmentCategory.MINI_EXCAVATOR ->
                trimmedOrNull(tonnage)?.let { parts += "тоннаж: $it т" }
            EquipmentCategory.GARBAGE_TRUCK ->
                garbageTruckType?.let { parts += "тип: $it" }
            EquipmentCategory.LOWBED_TRAILER ->
                lowbedType?.let { parts += "тип: $it" }
            EquipmentCategory.DUMP_TRUCK -> {
                dumpTruckAxles?.let { parts += "конфигурация: $it" }
                trimmedOrNull(inertMaterial)?.let { parts += "инертный материал: $it" }
            }
            EquipmentCategory.FRONT_LOADER ->
                frontLoaderBucket?.let { parts += "объём ковша: $it м³" }
            EquipmentCategory.TOW_TRUCK ->
                towType?.let { parts += "тип: $it" }
            EquipmentCategory.EXCAVATOR_CRAWLER, EquipmentCategory.EXCAVATOR_WHEELED ->
                excavatorBucket?.let { parts += "объём ковша: $it м³" }
            EquipmentCategory.AUGER_DRILL -> {
                trimmedOrNull(drillingDepth)?.let { parts += "глубина бурения: $it м" }
                trimmedOrNull(drillingDiameter)?.let { parts += "диаметр: $it мм" }
            }
            EquipmentCategory.BACKHOE_LOADER -> {
                if (backhoeAttachments.isNotEmpty()) {
                    parts += "доп. оборудование: ${backhoeAttachments.sorted().joinToString(", ")}"
                }
                backhoeBucketWidth?.let { parts += "ширина ковша: $it см" }
            }
            EquipmentCategory.BITUMEN_SPRAYER ->
                trimmedOrNull(volumeCubic)?.let { parts += "объём: $it м³" }
            EquipmentCategory.FORKLIFT ->
                trimmedOrNull(liftCapacity)?.let { parts += "грузоподъёмность: $it т" }
            // Categories without any category-specific options.
            EquipmentCategory.CONCRETE_MIXER,
            EquipmentCategory.BULLDOZER,
            EquipmentCategory.GRAB_LOADER,
            EquipmentCategory.ROAD_MAINTENANCE_VEHICLE -> Unit
        }

        return if (parts.isEmpty()) null
        else "Параметры техники: " + parts.joinToString("; ") + "."
    }

    companion object {
        /**
         * Categories that have no category-specific options. The UI renders
         * the section card conditionally via this set so the form doesn't
         * show an empty parameter box. Mirrors iOS `hasAnyOptions`
         * (SpecTechIOS/Features/CreateOrder/CreateOrderView.swift:605-612).
         */
        val CATEGORIES_WITHOUT_OPTIONS: Set<EquipmentCategory> = setOf(
            EquipmentCategory.CONCRETE_MIXER,
            EquipmentCategory.BULLDOZER,
            EquipmentCategory.GRAB_LOADER,
            EquipmentCategory.ROAD_MAINTENANCE_VEHICLE,
        )
    }
}
