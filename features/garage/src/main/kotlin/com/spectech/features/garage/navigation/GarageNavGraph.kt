package com.spectech.features.garage.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.spectech.features.garage.ui.EquipmentDetailScreen
import com.spectech.features.garage.ui.GarageListScreen
import com.spectech.features.garage.viewmodel.GarageViewModel

/**
 * Garage tab graph. VM hoisted to graph level so the list cache survives
 * navigating into the equipment detail and back.
 */
@Composable
fun GarageNavGraph(paddingValues: PaddingValues = PaddingValues()) {
    val nav = rememberNavController()
    val vm: GarageViewModel = hiltViewModel()

    NavHost(nav, startDestination = GarageRoute.List) {
        composable<GarageRoute.List> {
            GarageListScreen(
                onEquipmentClick = { id -> nav.navigate(GarageRoute.Detail(id)) },
                paddingValues = paddingValues,
                viewModel = vm,
            )
        }
        composable<GarageRoute.Detail> { entry ->
            val args = entry.toRoute<GarageRoute.Detail>()
            EquipmentDetailScreen(
                equipmentId = args.equipmentId,
                onClose = { nav.popBackStack() },
                paddingValues = paddingValues,
                viewModel = vm,
            )
        }
    }
}
