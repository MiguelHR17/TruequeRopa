package dev.miguelehr.truequeropa.nav

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import dev.miguelehr.truequeropa.auth.FirebaseAuthManager
import dev.miguelehr.truequeropa.ui.screens.*

/* ========= RUTAS ========= */

sealed class Route(val path: String) {

    // Auth
    data object Login : Route("auth/login")
    data object Register : Route("auth/register")

    // App
    data object Offers : Route("home/offers")
    data object NewProduct : Route("home/new")
    data object Proposals : Route("home/proposals")
    data object History : Route("home/history")

    // Perfil propio
    data object Profile : Route("profile/me")

    // Perfil ajeno + producto fijado (pin)
    data object ProfileById : Route("profile/{userId}?pin={pin}") {
        fun with(userId: String, pin: String? = null): String =
            if (pin.isNullOrBlank()) "profile/$userId?pin="
            else "profile/$userId?pin=$pin"
    }

    // Trueque específico
    data object ProposeTrade :
        Route("trade/propose/{productId}") {
        fun with(id: String) = "trade/propose/$id"
    }

    // Admin
    data object AdminPanel : Route("admin/panel")
}

/* ========= BOTTOM NAV ITEMS ========= */

data class BottomItem(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

/* ========= APP NAV HOST ========= */

@Composable
fun AppNav(navController: NavHostController = rememberNavController()) {

    val bottomItems = listOf(
        BottomItem(Route.Offers.path, "Ofertas", Icons.Default.Store),
        BottomItem(Route.NewProduct.path, "Publicar", Icons.Default.Add),
        BottomItem(Route.Proposals.path, "Propuestas", Icons.Default.Inbox),
        BottomItem(Route.History.path, "Historial", Icons.Default.History),
        BottomItem(Route.Profile.path, "Cuenta", Icons.Default.Person),
    )

    val backstack by navController.currentBackStackEntryAsState()
    val current = backstack?.destination?.route
    var showAccountMenu by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = {
            if (current?.startsWith("auth/") != true) {
                Box {
                    NavigationBar {
                        val selected = current ?: Route.Offers.path
                        bottomItems.forEach { item ->
                            val isProfileButton = item.route == Route.Profile.path

                            NavigationBarItem(
                                selected = selected == item.route,
                                onClick = {
                                    if (isProfileButton) showAccountMenu = true
                                    else navController.navigate(item.route) {
                                        launchSingleTop = true
                                        popUpTo(Route.Offers.path) { inclusive = false }
                                    }
                                },
                                icon = { Icon(item.icon, item.label) },
                                label = { Text(item.label) }
                            )
                        }
                    }

                    /* ===== MENÚ DE CUENTA ===== */

                    DropdownMenu(
                        expanded = showAccountMenu,
                        onDismissRequest = { showAccountMenu = false },
                        offset = androidx.compose.ui.unit.DpOffset(0.dp, (-8).dp)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Mi perfil") },
                            onClick = {
                                showAccountMenu = false
                                navController.navigate(Route.Profile.path) { launchSingleTop = true }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Panel de Administrador") },
                            onClick = {
                                showAccountMenu = false
                                navController.navigate(Route.AdminPanel.path) { launchSingleTop = true }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Cerrar sesión") },
                            onClick = {
                                showAccountMenu = false
                                FirebaseAuthManager.signOut()
                                navController.navigate(Route.Login.path) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        inclusive = true
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

        /* ===== NAVEGACIÓN ===== */

        NavHost(navController = navController, startDestination = Route.Login.path) {

            composable(Route.Login.path) {
                AuthLoginScreen(
                    onLogin = {
                        navController.navigate(Route.Offers.path) {
                            popUpTo(Route.Login.path) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onGoRegister = {
                        navController.navigate(Route.Register.path)
                    },
                    padding = padding
                )
            }

            composable(Route.Register.path) {
                AuthRegisterScreen(
                    onRegistered = {
                        FirebaseAuthManager.signOut()
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
                    padding = padding,
                    onOpenProduct = { productId, ownerUserId ->
                        // entra al perfil del dueño con esa publicación fijada
                        navController.navigate(Route.ProfileById.with(ownerUserId, pin = productId))
                    },
                    onOpenProfile = { uid ->
                        // ir directo al perfil de la persona
                        navController.navigate(Route.ProfileById.with(uid))
                    }
                )
            }

            composable(Route.NewProduct.path) {
                ProductFormScreen(onSaved = { }, padding = padding)
            }

            composable(Route.Proposals.path) {
                ProposalsInboxScreen(padding = padding)
            }

            composable(Route.History.path) {
                TradeHistoryScreen(padding = padding)
            }

            composable(Route.Profile.path) {
                ProfileScreen(
                    userId = null,
                    pinProductId = null,
                    onPublish = { navController.navigate(Route.NewProduct.path) },
                    onOpenProduct = {},
                    padding = padding
                )
            }

            composable(Route.ProfileById.path) { entry ->
                val uid = entry.arguments?.getString("userId") ?: ""
                val pin = entry.arguments?.getString("pin")

                ProfileScreen(
                    userId = uid,
                    pinProductId = pin?.takeIf { it.isNotBlank() },
                    onPublish = { },
                    onOpenProduct = {},
                    padding = padding
                )
            }


            composable(Route.AdminPanel.path) {
                AdminPanelScreen(padding)
            }
        }
    }
}