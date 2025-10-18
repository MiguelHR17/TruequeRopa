
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
import androidx.compose.material.icons.filled.Person
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination

sealed class Route(val path: String) {
    data object Login: Route("auth/login")
    data object Register: Route("auth/register")
    data object Offers: Route("home/offers")
    data object NewProduct: Route("home/new")
    data object Proposals: Route("home/proposals")
    data object History: Route("home/history")
    data object ProposeTrade: Route("trade/propose/{productId}") { fun with(id:String) = "trade/propose/$id" }
    data object AdminPanel: Route("admin/panel")
    data object Account: Route("home/account")
}

data class BottomItem(val route:String, val label:String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

@Composable
fun AppNav(navController: NavHostController = rememberNavController()) {
    val bottomItems = listOf(
        BottomItem(Route.Offers.path, "Ofertas", Icons.Default.Store),
        BottomItem(Route.NewProduct.path, "Publicar", Icons.Default.Add),
        BottomItem(Route.Proposals.path, "Propuestas", Icons.Default.Inbox),
        BottomItem(Route.History.path, "Historial", Icons.Default.History),
        BottomItem(Route.Account.path, "Cuenta", Icons.Default.Person),
    )

    val backstack by navController.currentBackStackEntryAsState()
    val current = backstack?.destination?.route
    var showAccountMenu by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = {
            if (current?.startsWith("auth/") != true) {
                Box { // 👈 para anclar el menú
                    NavigationBar {
                        val sel = current ?: Route.Offers.path
                        bottomItems.forEach { item ->
                            val isAccount = (item.route == Route.Account.path)
                            NavigationBarItem(
                                selected = sel == item.route,
                                onClick = {
                                    if (isAccount) {
                                        showAccountMenu = true
                                    } else {
                                        navController.navigate(item.route) {
                                            launchSingleTop = true
                                            popUpTo(Route.Offers.path) { inclusive = false }
                                        }
                                    }
                                },
                                icon = { Icon(item.icon, contentDescription = item.label) },
                                label = { Text(item.label) }
                            )
                        }
                    }

                    // 🔽 Menú pequeño que aparece al tocar el icono "Cuenta"
                    DropdownMenu(
                        expanded = showAccountMenu,
                        onDismissRequest = { showAccountMenu = false },
                        // (opcional) desplazar un poco hacia arriba para que no tape la barra
                        offset = androidx.compose.ui.unit.DpOffset(0.dp, (-8).dp)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Cerrar sesión") },
                            onClick = {
                                showAccountMenu = false
                                // 1) salir de Firebase
                                dev.miguelehr.truequeropa.auth.FirebaseAuthManager.signOut()
                                // 2) navegar a Login limpiando el backstack
                                navController.navigate(Route.Login.path) {
                                    // limpia todo hasta el startDestination (Login en tu grafo)
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        inclusive = true // elimina lo anterior
                                    }
                                    launchSingleTop = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(navController = navController, startDestination = Route.Login.path) {
            composable(Route.Login.path) {
                AuthLoginScreen(
                    onLogin = {
                        navController.navigate(Route.Offers.path) {
                            // elimina Login del backstack para que no se pueda volver con "Atrás"
                            popUpTo(Route.Login.path) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onGoRegister = { navController.navigate(Route.Register.path) },
                    padding = padding
                )
            }
            composable(Route.Register.path) {
                AuthRegisterScreen(
                    onRegistered = {
                        dev.miguelehr.truequeropa.auth.FirebaseAuthManager.signOut()
                        navController.navigate(Route.Login.path) {
                            popUpTo(Route.Login.path) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
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
