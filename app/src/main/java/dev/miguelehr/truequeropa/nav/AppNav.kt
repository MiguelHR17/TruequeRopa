package dev.miguelehr.truequeropa.nav

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.miguelehr.truequeropa.auth.FirebaseAuthManager
import dev.miguelehr.truequeropa.ui.screens.AdminPanelScreen
import dev.miguelehr.truequeropa.ui.screens.AuthLoginScreen
import dev.miguelehr.truequeropa.ui.screens.AuthRegisterScreen
import dev.miguelehr.truequeropa.ui.screens.OffersScreen
import dev.miguelehr.truequeropa.ui.screens.ProductFormScreen
import dev.miguelehr.truequeropa.ui.screens.ProfileScreen
import dev.miguelehr.truequeropa.ui.screens.ProposalsInboxScreen
import dev.miguelehr.truequeropa.ui.screens.ProposeTradeScreen
import dev.miguelehr.truequeropa.ui.screens.PublicationPostsScreen
import dev.miguelehr.truequeropa.ui.screens.TradeHistoryScreen
import dev.miguelehr.truequeropa.ui.screens.UserRequestsScreen

/** Rutas de la app */
sealed class Route(val path: String) {
    // Auth
    data object Login : Route("auth/login")
    data object Register : Route("auth/register")

    // Home
    data object Offers : Route("home/offers")
    data object NewProduct : Route("home/new")
    data object Proposals : Route("home/proposals")
    data object UserRequests : Route("home/requests")
    data object UserPostsRequests : Route("publicationPosts/{userId}/{postIdSol}/{requestId}")
    data object History : Route("home/history")

    // Perfil propio
    data object Profile : Route("profile/me")

    // Perfil ajeno con pin opcional de publicación
    data object ProfileById : Route("profile/{userId}?pin={pin}") {
        fun with(userId: String, pin: String? = null): String =
            if (pin.isNullOrBlank()) "profile/$userId?pin="
            else "profile/$userId?pin=$pin"
    }

    // Trueque
    data object ProposeTrade : Route("trade/propose/{productId}") {
        fun with(id: String) = "trade/propose/$id"
    }

    // Admin
    data object AdminPanel : Route("admin/panel")

    // Pestaña de cuenta (sólo para abrir el menú)
    data object Account : Route("home/account")
}

/** Item para la barra inferior */
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
        BottomItem(Route.UserRequests.path, "Propuestas", Icons.Default.Inbox),
        BottomItem(Route.History.path, "Historial", Icons.Default.History),
        BottomItem(Route.Profile.path, "Cuenta", Icons.Default.Person),
    )

    val backstack by navController.currentBackStackEntryAsState()
    val currentRoute = backstack?.destination?.route
    var showAccountMenu by remember { mutableStateOf(false) }
    var proposalsBadge by remember { mutableStateOf(0) }
    var requestsBadge by remember { mutableStateOf(0) }

    Scaffold(
        bottomBar = {
            // Oculta la barra en pantallas de auth
            if (currentRoute?.startsWith("auth/") != true) {
                Box {
                    NavigationBar {
                        val selected = currentRoute ?: Route.Offers.path
                        bottomItems.forEach { item ->
                            val isProfileButton = item.route == Route.Profile.path

                            NavigationBarItem(
                                selected = selected == item.route,
                                onClick = {
                                    if (isProfileButton) {
                                        showAccountMenu = true
                                    } else {
                                        navController.navigate(item.route) {
                                            launchSingleTop = true
                                            popUpTo(Route.Offers.path) { inclusive = false }
                                        }
                                    }
                                },
                                icon = {
                                    if (item.route == Route.Proposals.path) {
                                        BadgedBox(
                                            badge = {
                                                if (proposalsBadge > 0) {
                                                    Badge { Text(proposalsBadge.toString()) }
                                                }
                                            }
                                        ) {
                                            Icon(item.icon, contentDescription = item.label)
                                        }
                                    } else if (item.route == Route.UserRequests.path) {
                                        BadgedBox(
                                            badge = {
                                                if (requestsBadge > 0) {
                                                    Badge { Text(requestsBadge.toString()) }
                                                }
                                            }
                                        ) {
                                            Icon(item.icon, contentDescription = item.label)
                                        }
                                    } else {
                                        Icon(item.icon, contentDescription = item.label)
                                    }
                                },
                                label = { Text(item.label) }
                            )
                        }
                    }
                    // Menú de cuenta
                    DropdownMenu(
                        expanded = showAccountMenu,
                        onDismissRequest = { showAccountMenu = false },
                        offset = DpOffset(0.dp, (-8).dp)
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

        NavHost(
            navController = navController,
            startDestination = Route.Login.path
        ) {
            // ===== AUTH =====
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
                        // Después de registrarse, lo mandas al login
                        FirebaseAuthManager.signOut()
                        navController.navigate(Route.Login.path) {
                            popUpTo(Route.Login.path) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onBackToLogin = {
                        // Aquí vuelves al login cuando toque "¿Ya tienes cuenta? Inicia sesión"
                        navController.popBackStack()
                    },
                    padding = padding
                )
            }

            // ===== HOME =====
            composable(Route.Offers.path) {
                OffersScreen(
                    onOpenProduct = { productId, ownerUserId ->
                        // Ir al perfil del dueño y fijar esa publicación arriba
                        navController.navigate(Route.ProfileById.with(ownerUserId, pin = productId))
                    },
                    onOpenUserSearch = { /* callback por compatibilidad si lo necesitas */ },
                    padding = padding
                )
            }

            // Bandeja de propuestas (si la usas)
            composable(Route.Proposals.path) {
                ProposalsInboxScreen(
                    padding = padding,
                    onUnreviewedCountChange = { proposalsBadge = it }
                )
            }

            // Solicitudes que le han hecho al usuario actual
            composable(Route.UserRequests.path) {
                val currentUserId = FirebaseAuthManager.currentUserId()
                if (currentUserId != null) {
                    UserRequestsScreen(
                        userId = currentUserId,
                        padding = padding,
                        onUnreviewedCountChange = { requestsBadge = it },
                        onNavigateToUserPosts = { solicitanteId, postIdSol, requestId ->
                            val route = Route.UserPostsRequests.path
                                .replace("{userId}", solicitanteId)
                                .replace("{postIdSol}", postIdSol)
                                .replace("{requestId}", requestId)
                            navController.navigate(route)
                        }
                    )
                }
            }

            // Pantalla que muestra publicaciones del solicitante para responder a una solicitud
            composable(Route.UserPostsRequests.path) { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId") ?: ""
                val postIdSol = backStackEntry.arguments?.getString("postIdSol") ?: ""
                val requestId = backStackEntry.arguments?.getString("requestId") ?: ""
                val currentUserId = FirebaseAuthManager.currentUserId()

                if (currentUserId != null) {
                    PublicationPostsScreen(
                        userId = userId,
                        postIdSol = postIdSol,
                        requestId = requestId,
                        onNavigateToRequestDetails = {
                            navController.navigate(Route.UserRequests.path)
                        },
                        onBack = {
                            navController.popBackStack()
                        }
                    )
                }
            }

            composable(Route.History.path) {
                val currentUserId = FirebaseAuthManager.currentUserId()
                if (currentUserId != null) {
                    TradeHistoryScreen(userId = currentUserId)
                }
            }

            // ===== PERFIL =====
            // Perfil propio
            composable(Route.Profile.path) {
                ProfileScreen(
                    userId = null,              // null => actual
                    pinProductId = null,        // sin publicación fijada
                    onPublish = { navController.navigate(Route.NewProduct.path) },
                    onOpenProduct = { /* Detalle interno si quisieras */ },
                    padding = padding
                )
            }

            // Perfil ajeno con pin opcional
            composable(
                route = Route.ProfileById.path,
                arguments = listOf(
                    navArgument("userId") { nullable = false },
                    navArgument("pin") { nullable = true; defaultValue = "" }
                )
            ) { entry ->
                val uid = entry.arguments?.getString("userId") ?: ""
                val pin = entry.arguments?.getString("pin")
                ProfileScreen(
                    userId = uid,
                    pinProductId = pin?.takeIf { it.isNotBlank() },
                    onPublish = { /* en perfil ajeno normalmente no se muestra */ },
                    onOpenProduct = { /* expandir dentro del propio perfil */ },
                    padding = padding
                )
            }

            // ===== TRUEQUE =====
            composable(Route.ProposeTrade.path) { entry ->
                val id = entry.arguments?.getString("productId") ?: ""
                ProposeTradeScreen(
                    productId = id,
                    onSent = { navController.navigate(Route.Proposals.path) }
                )
            }

            // ===== ADMIN =====
            composable(Route.AdminPanel.path) {
                AdminPanelScreen(padding)
            }

            // === PUBLICAR ===
            composable(Route.NewProduct.path) {
                ProductFormScreen(
                    onSaved = {
                        // Después de publicar, puedes volver al perfil o a ofertas.
                        navController.popBackStack()
                    },
                    padding = padding
                )
            }
        }
    }
}