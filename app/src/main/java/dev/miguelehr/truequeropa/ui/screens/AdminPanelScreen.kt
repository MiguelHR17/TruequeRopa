package dev.miguelehr.truequeropa.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import dev.miguelehr.truequeropa.data.FirestoreManager
import dev.miguelehr.truequeropa.model.UserPost
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// =======================
//  MODELOS INTERNOS ADMIN
// =======================

private enum class AdminTab { USERS, POSTS, REPORTS }

private data class AdminUser(
    val uid: String,
    val nombre: String,
    val email: String,
    val photoUrl: String? = null,
    val active: Boolean = true
)

private data class UserReportRow(
    val uid: String,
    val nombre: String,
    val email: String,
    val createdAt: Timestamp?,
    val active: Boolean,
    val postsCount: Int
)

// =======================
//  PANTALLA PRINCIPAL
// =======================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(
    onBack: () -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(AdminTab.USERS) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Panel de Administración") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {

            // Tabs
            TabRow(
                selectedTabIndex = selectedTab.ordinal,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTab == AdminTab.USERS,
                    onClick = { selectedTab = AdminTab.USERS },
                    text = { Text("Usuarios") }
                )
                Tab(
                    selected = selectedTab == AdminTab.POSTS,
                    onClick = { selectedTab = AdminTab.POSTS },
                    text = { Text("Publicaciones") }
                )
                Tab(
                    selected = selectedTab == AdminTab.REPORTS,
                    onClick = { selectedTab = AdminTab.REPORTS },
                    text = { Text("Reportes") }
                )
            }

            Spacer(Modifier.height(12.dp))

            when (selectedTab) {
                AdminTab.USERS   -> AdminUsersTab()
                AdminTab.POSTS   -> AdminPostsTab()
                AdminTab.REPORTS -> AdminReportsTab()
            }
        }
    }
}

// =======================
//  TAB: USUARIOS
// =======================

@Composable
private fun AdminUsersTab() {
    val users = remember { mutableStateListOf<AdminUser>() }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        val db = FirebaseFirestore.getInstance()
        val reg: ListenerRegistration = db.collection("users")
            .orderBy("createdAt", Query.Direction.DESCENDING)
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
                        photoUrl = doc.getString("photoUrl"),
                        active = doc.getBoolean("active") ?: true
                    )
                }
                loading = false
            }

        onDispose { reg.remove() }
    }

    Column(Modifier.fillMaxSize()) {
        Text(
            "Usuarios registrados",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

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

        if (users.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No hay usuarios registrados")
            }
            return
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(users, key = { it.uid }) { u ->
                UserRow(u)
            }
        }
    }
}

@Composable
private fun UserRow(u: AdminUser) {
    val scope = rememberCoroutineScope()
    var menuOpen by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editedName by remember { mutableStateOf(u.nombre) }
    var isProcessing by remember { mutableStateOf(false) }
    var localActive by remember { mutableStateOf(u.active) }

    val cardBorderColor = if (!localActive)
        MaterialTheme.colorScheme.error
    else
        MaterialTheme.colorScheme.outlineVariant ?: MaterialTheme.colorScheme.outline

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        border = BorderStroke(1.dp, cardBorderColor)
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
                if (!localActive) {
                    Text(
                        "Cuenta desactivada",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            Box {
                IconButton(
                    onClick = { if (!isProcessing) menuOpen = true },
                    enabled = !isProcessing
                ) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Acciones")
                }
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false }
                ) {
                    // Activar / desactivar
                    val textEstado = if (localActive) "Desactivar cuenta" else "Activar cuenta"
                    DropdownMenuItem(
                        text = { Text(textEstado) },
                        onClick = {
                            menuOpen = false
                            isProcessing = true
                            scope.launch {
                                val ok = FirestoreManager.setUserActive(
                                    u.uid,
                                    !localActive
                                )
                                if (ok) {
                                    localActive = !localActive
                                }
                                isProcessing = false
                            }
                        }
                    )

                    // Modificar nombre
                    DropdownMenuItem(
                        text = { Text("Modificar nombre") },
                        onClick = {
                            menuOpen = false
                            editedName = u.nombre
                            showEditDialog = true
                        }
                    )

                    Divider()

                    DropdownMenuItem(
                        text = {
                            Text(
                                "Eliminar usuario",
                                color = MaterialTheme.colorScheme.error
                            )
                        },
                        onClick = {
                            menuOpen = false
                            showDeleteDialog = true
                        }
                    )
                }
            }
        }
    }

    // Diálogo editar nombre
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Modificar datos del usuario") },
            text = {
                Column {
                    Text("Nombre y apellido (un solo campo por ahora):")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editedName,
                        onValueChange = { editedName = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !isProcessing,
                    onClick = {
                        isProcessing = true
                        scope.launch {
                            try {
                                FirebaseFirestore.getInstance()
                                    .collection("users")
                                    .document(u.uid)
                                    .update("nombre", editedName)
                                    .await()
                            } finally {
                                isProcessing = false
                                showEditDialog = false
                            }
                        }
                    }
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Diálogo eliminar usuario
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Eliminar usuario") },
            text = {
                Text("¿Seguro que deseas eliminar a \"${u.nombre}\"? Esto no se puede deshacer.")
            },
            confirmButton = {
                TextButton(
                    enabled = !isProcessing,
                    onClick = {
                        isProcessing = true
                        scope.launch {
                            try {
                                FirebaseFirestore.getInstance()
                                    .collection("users")
                                    .document(u.uid)
                                    .delete()
                                    .await()
                                // Si quisieras también borrar sus posts, aquí podrías hacerlo.
                            } finally {
                                isProcessing = false
                                showDeleteDialog = false
                            }
                        }
                    }
                ) {
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

// =======================
//  TAB: PUBLICACIONES
// =======================

@Composable
private fun AdminPostsTab() {
    val posts = remember { mutableStateListOf<UserPost>() }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        val db = FirebaseFirestore.getInstance()
        val reg = db.collection("posts")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, e ->
                if (e != null) {
                    error = e.localizedMessage
                    loading = false
                    return@addSnapshotListener
                }
                posts.clear()
                snap?.documents?.forEach { doc ->
                    posts += UserPost(
                        id = doc.id,
                        userId = doc.getString("userId") ?: "",
                        prendaId = doc.getString("prendaId") ?: "",
                        titulo = doc.getString("titulo") ?: "",
                        descripcion = doc.getString("descripcion") ?: "",
                        categoria = doc.getString("categoria") ?: "",
                        talla = doc.getString("talla") ?: "",
                        estado = doc.getString("estado") ?: "",
                        imageUrls = (doc.get("imageUrls") as? List<*>)?.filterIsInstance<String>()
                            ?: emptyList(),
                        estadoTrueque = doc.getString("estadoTrueque") ?: "0",
                        hidden = doc.getBoolean("hidden") ?: false,
                        createdAt = doc.getTimestamp("createdAt")
                    )
                }
                loading = false
            }

        onDispose { reg.remove() }
    }

    Column(Modifier.fillMaxSize()) {
        Text(
            "Publicaciones",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return
        }
        if (error != null) {
            Text(
                text = "Error cargando publicaciones: $error",
                color = MaterialTheme.colorScheme.error
            )
            return
        }
        if (posts.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No hay publicaciones")
            }
            return
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(posts, key = { it.id }) { p ->
                AdminPostRow(p)
            }
        }
    }
}

@Composable
private fun AdminPostRow(post: UserPost) {
    val scope = rememberCoroutineScope()
    var localHidden by remember { mutableStateOf(post.hidden) }
    var processing by remember { mutableStateOf(false) }

    val borderColor = if (localHidden)
        MaterialTheme.colorScheme.error      // 🔴 borde rojo cuando está oculta
    else
        MaterialTheme.colorScheme.outlineVariant ?: MaterialTheme.colorScheme.outline

    Card(
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(2.dp, borderColor),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(12.dp)) {
            val firstImg = post.imageUrls.firstOrNull()
            if (firstImg != null) {
                AsyncImage(
                    model = firstImg,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
                Spacer(Modifier.height(8.dp))
            }

            Text(
                post.titulo,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                post.descripcion,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SmallTag(post.talla)
                SmallTag(post.estado)
                SmallTag(post.categoria)
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (localHidden) {
                    Text(
                        "OCULTA",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelMedium
                    )
                } else {
                    Text(
                        "Visible",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                TextButton(
                    enabled = !processing,
                    onClick = {
                        processing = true
                        scope.launch {
                            val ok = FirestoreManager.setPostHidden(post.id, !localHidden)
                            if (ok) {
                                localHidden = !localHidden
                            }
                            processing = false
                        }
                    }
                ) {
                    Text(if (localHidden) "Volver a mostrar" else "Ocultar")
                }
            }
        }
    }
}

@Composable
private fun SmallTag(text: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 1.dp
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

// =======================
//  TAB: REPORTES
// =======================

@Composable
private fun AdminReportsTab() {
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var rows by remember { mutableStateOf<List<UserReportRow>>(emptyList()) }
    var generatedCsv by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize()) {
        Text(
            "Reportes",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Button(
            onClick = {
                loading = true
                error = null
                generatedCsv = null
                scope.launch {
                    try {
                        val db = FirebaseFirestore.getInstance()
                        val usersSnap = db.collection("users").get().await()
                        val postsSnap = db.collection("posts").get().await()

                        val postsByUser = postsSnap.documents.groupBy {
                            it.getString("userId") ?: ""
                        }

                        val list = usersSnap.documents.map { doc ->
                            val uid = doc.getString("uid") ?: doc.id
                            val name = doc.getString("nombre") ?: "(Sin nombre)"
                            val email = doc.getString("email") ?: ""
                            val createdAt = doc.getTimestamp("createdAt")
                            val active = doc.getBoolean("active") ?: true
                            val countPosts = postsByUser[uid]?.size ?: 0

                            UserReportRow(
                                uid = uid,
                                nombre = name,
                                email = email,
                                createdAt = createdAt,
                                active = active,
                                postsCount = countPosts
                            )
                        }

                        rows = list
                        generatedCsv = buildCsvForUsers(list)
                    } catch (e: Exception) {
                        error = e.localizedMessage
                    } finally {
                        loading = false
                    }
                }
            },
            enabled = !loading
        ) {
            Text(if (loading) "Generando…" else "Generar reporte de usuarios")
        }

        Spacer(Modifier.height(12.dp))

        if (error != null) {
            Text(
                text = "Error generando reporte: $error",
                color = MaterialTheme.colorScheme.error
            )
        }

        if (rows.isNotEmpty()) {
            Text(
                "Usuarios encontrados: ${rows.size}",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(8.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f, fill = true)
            ) {
                items(rows, key = { it.uid }) { r ->
                    Text(
                        "- ${r.nombre} (${r.email}) · Posts: ${r.postsCount} · " +
                                (if (r.active) "Activo" else "Inactivo")
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            if (generatedCsv != null) {
                Text(
                    "CSV generado (puedes copiarlo y pegarlo en un .csv y abrirlo en Excel):",
                    style = MaterialTheme.typography.labelMedium
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    generatedCsv!!,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        } else if (!loading) {
            Text("No hay datos aún. Pulsa el botón para generar el reporte.")
        }
    }
}

private fun buildCsvForUsers(list: List<UserReportRow>): String {
    val header = "uid;nombre;email;createdAt;activo;cantidadPosts"
    val rows = list.map { r ->
        val created = r.createdAt?.toDate()?.toString() ?: ""
        val activeStr = if (r.active) "1" else "0"
        "${r.uid};\"${r.nombre}\";${r.email};${created};${activeStr};${r.postsCount}"
    }
    return (listOf(header) + rows).joinToString("\n")
}