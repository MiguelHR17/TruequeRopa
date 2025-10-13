package dev.miguelehr.truequeropa.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dev.miguelehr.truequeropa.ui.screens.AdminPanelScreen
import dev.miguelehr.truequeropa.ui.screens.AuthLoginScreen
import dev.miguelehr.truequeropa.ui.screens.AuthRegisterScreen
import dev.miguelehr.truequeropa.ui.screens.OffersScreen
import dev.miguelehr.truequeropa.ui.screens.ProductFormScreen
import dev.miguelehr.truequeropa.ui.screens.ProposalsInboxScreen
import dev.miguelehr.truequeropa.ui.screens.ProposeTradeScreen
import dev.miguelehr.truequeropa.ui.screens.TradeHistoryScreen

sealed class Route(val path: String) {
    data object Login: Route("auth/login")
    data object Register: Route("auth/register")
    data object Offers: Route("home/offers")
    data object NewProduct: Route("home/new")
    data object Proposals: Route("home/proposals")
    data object History: Route("home/history")
    data object ProposeTrade: Route("trade/propose/{productId}") { fun with(id:String) = "trade/propose/$id" }
    data object AdminPanel: Route("admin/panel")
}

data class BottomItem(val route:String, val label:String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

@Composable
fun AppNav(navController: NavHostController = rememberNavController()) {
    val bottomItems = listOf(
        BottomItem(Route.Offers.path, "Ofertas", Icons.Default.Store),
        BottomItem(Route.NewProduct.path, "Publicar", Icons.Default.Add),
        BottomItem(Route.Proposals.path, "Propuestas", Icons.Default.Inbox),
        BottomItem(Route.History.path, "Historial", Icons.Default.History),
    )

    val backstack by navController.currentBackStackEntryAsState()
    val current = backstack?.destination?.route

    Scaffold(
        bottomBar = {
            // Oculta la barra en pantallas de auth
            if (current?.startsWith("auth/") != true) {
                NavigationBar {
                    val sel = current ?: Route.Offers.path
                    bottomItems.forEach {
                        NavigationBarItem(
                            selected = sel == it.route,
                            onClick = {
                                navController.navigate(it.route) {
                                    launchSingleTop = true
                                    popUpTo(Route.Offers.path) { inclusive = false }
                                }
                            },
                            icon = { Icon(it.icon, contentDescription = it.label) },
                            label = { Text(it.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(navController = navController, startDestination = Route.Login.path) {
            composable(Route.Login.path) {
                AuthLoginScreen(
                    onLogin = { navController.navigate(Route.Offers.path) },
                    onGoRegister = { navController.navigate(Route.Register.path) },
                    padding = padding
                )
            }
            composable(Route.Register.path) {
                AuthRegisterScreen(
                    onRegistered = { navController.navigate(Route.Offers.path) },
                    padding = padding
                )
            }
            composable(Route.Offers.path) {
                OffersScreen(
                    onOpenProduct = { id -> navController.navigate(Route.ProposeTrade.with(id)) },
                    padding = padding
                )
            }
            composable(Route.NewProduct.path) {
                ProductFormScreen(
                    onSaved = { /* TODO: snackbar */ },
                    padding = padding
                )
            }
            composable(Route.Proposals.path) { ProposalsInboxScreen(padding) }
            composable(Route.History.path) { TradeHistoryScreen(padding) }
            composable(Route.ProposeTrade.path) { entry ->
                val id = entry.arguments?.getString("productId") ?: ""
                ProposeTradeScreen(productId = id, onSent = { navController.navigate(Route.Proposals.path) })
            }
            composable(Route.AdminPanel.path) { AdminPanelScreen() }
        }
    }
}
