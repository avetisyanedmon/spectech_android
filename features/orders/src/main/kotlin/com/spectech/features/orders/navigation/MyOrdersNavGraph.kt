package com.spectech.features.orders.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.spectech.features.orders.ui.MyOrderDetailScreen
import com.spectech.features.orders.ui.MyOrdersListScreen
import com.spectech.features.orders.viewmodel.MyOrdersViewModel

/**
 * MyOrders tab — graph-level VM so the list cache survives navigating into
 * the order detail and back. Mirrors how the marketplace tab does it.
 */
@Composable
fun MyOrdersNavGraph(paddingValues: PaddingValues = PaddingValues()) {
    val nav = rememberNavController()
    val vm: MyOrdersViewModel = hiltViewModel()

    NavHost(nav, startDestination = MyOrdersRoute.List) {
        composable<MyOrdersRoute.List> {
            MyOrdersListScreen(
                onOrderClick = { id -> nav.navigate(MyOrdersRoute.Detail(id)) },
                paddingValues = paddingValues,
                viewModel = vm,
            )
        }
        composable<MyOrdersRoute.Detail> { entry ->
            val args = entry.toRoute<MyOrdersRoute.Detail>()
            MyOrderDetailScreen(
                orderId = args.orderId,
                paddingValues = paddingValues,
                viewModel = vm,
            )
        }
    }
}
