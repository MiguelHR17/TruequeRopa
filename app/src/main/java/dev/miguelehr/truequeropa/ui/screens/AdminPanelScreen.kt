package dev.miguelehr.truequeropa.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner

// ====== Modelo simple del usuario que guardas en Firestore ======
data class AdminUser(
    val uid: String,
    val nombre: String,
    val email: String,
    val photoUrl: String? = null
)

// ====== Pestañas del panel ======
private enum class AdminTab(val label: String) {
    USERS("Usuarios"),
    POSTS("Publicaciones"),
    REPORTS("Reportes"),
    SETTINGS("Ajustes")
}

// ====== Pantalla principal del panel ======
@Composable
fun AdminPanelScreen(padding: PaddingValues) {
    var selectedTab by remember { mutableStateOf(AdminTab.USERS) }

    Column(
        Modifier
            .padding(padding)
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            "Panel de Administrador",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold)
        )
        Spacer(Modifier.height(16.dp))

        // Tabs
        TabRow(
            selectedTabIndex = selectedTab.ordinal,
            divider = {},
            modifier = Modifier.fillMaxWidth()
        ) {
            AdminTab.values().forEach { tab ->
                Tab(
                    selected = selectedTab == tab,
                    onClick = { selectedTab = tab },
                    text = { Text(tab.label) }
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        when (selectedTab) {
            AdminTab.USERS   -> AdminUsersTab()
            AdminTab.POSTS   -> PlaceholderCard("Publicaciones")
            AdminTab.REPORTS -> PlaceholderCard("Reportes")
            AdminTab.SETTINGS-> PlaceholderCard("Ajustes")
        }
    }
}

// ====== Pestaña de USUARIOS (funcional) ======
@Composable
private fun AdminUsersTab() {
    var query by remember { mutableStateOf("") }
    val users = remember { mutableStateListOf<AdminUser>() }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    // Suscripción en vivo a Firestore (users)
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(Unit) {
        val db = FirebaseFirestore.getInstance()
        var reg: ListenerRegistration? = null

        val start = {
            loading = true
            error = null
            reg = db.collection("users")
                .addSnapshotListener { snap, e ->
                    if (e != null) {
                        error = e.localizedMessage
                        loading = false
                        return@addSnapshotListener
                    }
                    users.clear()
                    snap?.documents?.forEach { doc ->
                        users += AdminUser(
                            uid = doc.getString("uid") ?: doc.id,
                            nombre = doc.getString("nombre") ?: "(Sin nombre)",
                            email = doc.getString("email") ?: "",
                            photoUrl = doc.getString("photoUrl")
                        )
                    }
                    loading = false
                }
        }

        val stop = { reg?.remove(); reg = null }

        // observar lifecycle para pausar/resumir listener
        val obs = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> start()
                Lifecycle.Event.ON_STOP  -> stop()
                else -> {}
            }
        }
        lifecycle.addObserver(obs)
        // arrancar de una
        start()

        onDispose {
            lifecycle.removeObserver(obs)
            stop()
        }
    }

    Column(Modifier.fillMaxSize()) {
        // Buscador
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Buscar usuario") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))

        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return
        }

        if (error != null) {
            Text(
                text = "Error cargando usuarios: $error",
                color = MaterialTheme.colorScheme.error
            )
            return
        }

        val filtered = remember(query, users.toList()) {
            if (query.isBlank()) users.toList()
            else users.filter {
                it.nombre.contains(query, ignoreCase = true) ||
                        it.email.contains(query, ignoreCase = true)
            }
        }

        if (filtered.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No hay usuarios que coincidan")
            }
            return
        }

        // Lista
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Text(
                    "Resultados de búsqueda",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            items(filtered, key = { it.uid }) { u ->
                UserRow(u)
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

// ====== Fila de usuario con menú contextual ======
@Composable
private fun UserRow(u: AdminUser) {
    var menuOpen by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!u.photoUrl.isNullOrBlank()) {
                AsyncImage(
                    model = u.photoUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                )
            } else {
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(u.nombre.take(1).uppercase())
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    u.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    u.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Botón de acciones (3 puntos) + menú
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Acciones")
                }
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Opción 2") },
                        onClick = { menuOpen = false }
                    )
                    DropdownMenuItem(
                        text = { Text("Opción 3") },
                        onClick = { menuOpen = false }
                    )
                    DropdownMenuItem(
                        text = { Text("Opción 4") },
                        onClick = { menuOpen = false }
                    )
                    Divider()
                    DropdownMenuItem(
                        text = { Text("Eliminar usuario", color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            menuOpen = false
                            showDeleteDialog = true
                        }
                    )
                }
            }
        }
    }

    // Diálogo de confirmación (solo visual)
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Eliminar usuario") },
            text = { Text("¿Seguro que deseas eliminar a \"${u.nombre}\"? (solo visual)") },
            confirmButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

// ====== Placeholders para las otras pestañas ======
@Composable
private fun PlaceholderCard(title: String) {
    Box(
        Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("$title (en construcción)")
    }
}
