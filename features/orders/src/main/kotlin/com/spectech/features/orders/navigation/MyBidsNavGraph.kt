package com.spectech.features.orders.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.spectech.features.orders.ui.MyBidDetailScreen
import com.spectech.features.orders.ui.MyBidsListScreen
import com.spectech.features.orders.viewmodel.MyBidsViewModel

/** MyBids tab — same nested-NavHost pattern as MyOrders. */
@Composable
fun MyBidsNavGraph(paddingValues: PaddingValues = PaddingValues()) {
    val nav = rememberNavController()
    val vm: MyBidsViewModel = hiltViewModel()

    NavHost(nav, startDestination = MyBidsRoute.List) {
        composable<MyBidsRoute.List> {
            MyBidsListScreen(
                onOrderClick = { id -> nav.navigate(MyBidsRoute.Detail(id)) },
                paddingValues = paddingValues,
                viewModel = vm,
            )
        }
        composable<MyBidsRoute.Detail> { entry ->
            val args = entry.toRoute<MyBidsRoute.Detail>()
            MyBidDetailScreen(
                orderId = args.orderId,
                paddingValues = paddingValues,
                viewModel = vm,
            )
        }
    }
}
