package com.spectech.uikit.strings

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.spectech.domain.enums.EquipmentCategory
import com.spectech.domain.enums.OrderStatus
import com.spectech.domain.enums.PaymentType
import com.spectech.domain.enums.PricingUnit
import com.spectech.uikit.R

/**
 * Resolves the `titleKey` carried on each enum to its localized string. Lets
 * feature code call `category.label()` instead of repeating the
 * `when (cat) { … }` block. All keys are defined in `core/ui-kit/res/values`
 * (and `values-ru`).
 */
@Composable fun OrderStatus.label(): String = stringResId().localized()
@Composable fun EquipmentCategory.label(): String = stringResId().localized()
@Composable fun PricingUnit.label(): String = stringResId().localized()
@Composable fun PaymentType.label(): String = stringResId().localized()

fun OrderStatus.label(context: Context): String =
    context.getString(stringResId())

fun EquipmentCategory.label(context: Context): String =
    context.getString(stringResId())

fun PricingUnit.label(context: Context): String =
    context.getString(stringResId())

fun PaymentType.label(context: Context): String =
    context.getString(stringResId())

// MARK: - private resolver -------------------------------------------------

@Composable
private fun Int.localized(): String = LocalContext.current.getString(this)

private fun OrderStatus.stringResId(): Int = when (this) {
    OrderStatus.OPEN -> R.string.status_open
    OrderStatus.PENDING -> R.string.status_pending
    OrderStatus.ACCEPTED -> R.string.status_accepted
    OrderStatus.IN_PROGRESS -> R.string.status_in_progress
    OrderStatus.COMPLETED -> R.string.status_completed
    OrderStatus.CANCELLED -> R.string.status_cancelled
    OrderStatus.EXPIRED -> R.string.status_expired
    OrderStatus.CLOSED -> R.string.status_closed
}

private fun PricingUnit.stringResId(): Int = when (this) {
    PricingUnit.PER_HOUR -> R.string.unit_hour
    PricingUnit.PER_SHIFT -> R.string.unit_shift
    PricingUnit.PER_M3 -> R.string.unit_m3
    PricingUnit.PER_TON -> R.string.unit_ton
    PricingUnit.PER_KM -> R.string.unit_km
    PricingUnit.PER_TON_KM -> R.string.unit_ton_km
    PricingUnit.PER_M3_KM -> R.string.unit_m3_km
    PricingUnit.PER_M2 -> R.string.unit_m2
    PricingUnit.PER_LINEAR_M -> R.string.unit_linear_m
}

private fun PaymentType.stringResId(): Int = when (this) {
    PaymentType.CASH -> R.string.payment_cash
    PaymentType.NDS -> R.string.payment_nds
    PaymentType.USN -> R.string.payment_usn
}

private fun EquipmentCategory.stringResId(): Int = when (this) {
    EquipmentCategory.CONCRETE_PUMP -> R.string.cat_concrete_pump
    EquipmentCategory.AERIAL_PLATFORM -> R.string.cat_aerial_platform
    EquipmentCategory.TRUCK_CRANE -> R.string.cat_truck_crane
    EquipmentCategory.SEWAGE_VACUUM -> R.string.cat_sewage_vacuum
    EquipmentCategory.FUEL_TANKER -> R.string.cat_fuel_tanker
    EquipmentCategory.CONCRETE_MIXER -> R.string.cat_concrete_mixer
    EquipmentCategory.BULLDOZER -> R.string.cat_bulldozer
    EquipmentCategory.GRADER -> R.string.cat_grader
    EquipmentCategory.GRAB_LOADER -> R.string.cat_grab_loader
    EquipmentCategory.ROAD_ROLLER -> R.string.cat_road_roller
    EquipmentCategory.SOIL_COMPACTOR -> R.string.cat_soil_compactor
    EquipmentCategory.ASPHALT_PAVER -> R.string.cat_asphalt_paver
    EquipmentCategory.MANIPULATOR -> R.string.cat_manipulator
    EquipmentCategory.MINI_LOADER -> R.string.cat_mini_loader
    EquipmentCategory.MINI_EXCAVATOR -> R.string.cat_mini_excavator
    EquipmentCategory.GARBAGE_TRUCK -> R.string.cat_garbage_truck
    EquipmentCategory.LOWBED_TRAILER -> R.string.cat_lowbed_trailer
    EquipmentCategory.DUMP_TRUCK -> R.string.cat_dump_truck
    EquipmentCategory.FRONT_LOADER -> R.string.cat_front_loader
    EquipmentCategory.TOW_TRUCK -> R.string.cat_tow_truck
    EquipmentCategory.EXCAVATOR_CRAWLER -> R.string.cat_excavator_crawler
    EquipmentCategory.EXCAVATOR_WHEELED -> R.string.cat_excavator_wheeled
    EquipmentCategory.AUGER_DRILL -> R.string.cat_auger_drill
    EquipmentCategory.BACKHOE_LOADER -> R.string.cat_backhoe_loader
    EquipmentCategory.BITUMEN_SPRAYER -> R.string.cat_bitumen_sprayer
    EquipmentCategory.FORKLIFT -> R.string.cat_forklift
    EquipmentCategory.ROAD_MAINTENANCE_VEHICLE -> R.string.cat_road_maintenance_vehicle
}
