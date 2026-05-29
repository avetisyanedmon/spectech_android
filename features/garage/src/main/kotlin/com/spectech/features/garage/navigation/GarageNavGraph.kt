package com.spectech.features.garage.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.spectech.data.events.TabReselection
import com.spectech.data.events.TabReselectionBus
import com.spectech.features.garage.ui.EquipmentDetailScreen
import com.spectech.features.garage.ui.GarageListScreen
import com.spectech.features.garage.viewmodel.GarageViewModel

/**
 * Garage tab graph. VM hoisted to graph level so the list cache survives
 * navigating into the equipment detail and back.
 *
 * Listens to [TabReselectionBus] so re-tapping the active Garage tab pops the
 * nested back stack to its list root (matches iOS TabView's default
 * tap-active-tab behaviour).
 */
@Composable
fun GarageNavGraph(
    paddingValues: PaddingValues = PaddingValues(),
    onSignInRequested: () -> Unit = {},
) {
    val accessor: GarageNavGraphAccessor = hiltViewModel()
    val nav = rememberNavController()
    val vm: GarageViewModel = hiltViewModel()

    LaunchedEffect(Unit) {
        accessor.tabReselectionBus.tappedTab.collect { id ->
            if (id == TabReselection.GARAGE) {
                nav.popBackStack(nav.graph.findStartDestination().id, inclusive = false)
            }
        }
    }

    NavHost(nav, startDestination = GarageRoute.List) {
        composable<GarageRoute.List> {
            GarageListScreen(
                onEquipmentClick = { id -> nav.navigate(GarageRoute.Detail(id)) },
                onSignIn = onSignInRequested,
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

@dagger.hilt.android.lifecycle.HiltViewModel
class GarageNavGraphAccessor @javax.inject.Inject constructor(
    val tabReselectionBus: TabReselectionBus,
) : androidx.lifecycle.ViewModel()
