package dev.miguelehr.truequeropa.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import dev.miguelehr.truequeropa.data.FirestoreManager
import dev.miguelehr.truequeropa.model.UserPost
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.FileOutputStream
import kotlin.math.min

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

// Tipos de reporte que puede generar el admin
private enum class ReportType(val label: String) {
    ALL_USERS("Lista total de usuarios"),
    USERS_WITH_POSTS("Usuarios y total de sus publicaciones"),
    ALL_POSTS("Lista total de publicaciones"),
    ACTIVE_USERS("Usuarios activos"),
    INACTIVE_USERS("Usuarios restringidos / inactivos")
}

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
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp)
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
            onDismissRequest = { if (!isProcessing) showEditDialog = false },
            title = { Text("Modificar datos del usuario") },
            text = {
                Column {
                    Text("Nombre y apellido (un solo campo por ahora):")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editedName,
                        onValueChange = { editedName = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        supportingText = {
                            Text(
                                text = "Máx. 40 caracteres, sin saltos de línea",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !isProcessing,
                    onClick = {
                        val safeName = sanitizeUserName(editedName)

                        // Si después de sanear queda vacío, no guardamos y cerramos
                        if (safeName.isBlank()) {
                            editedName = u.nombre
                            showEditDialog = false
                            return@TextButton
                        }

                        isProcessing = true
                        scope.launch {
                            try {
                                FirebaseFirestore.getInstance()
                                    .collection("users")
                                    .document(u.uid)
                                    .update("nombre", safeName)
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
                TextButton(
                    enabled = !isProcessing,
                    onClick = { showEditDialog = false }
                ) {
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
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp)
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
        MaterialTheme.colorScheme.error
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
//  TAB: REPORTES (PDF con TABLA)
// =======================

@Composable
private fun AdminReportsTab() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedType by remember { mutableStateOf(ReportType.ALL_USERS) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize()) {
        Text(
            "Reportes",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text("Selecciona el tipo de reporte que quieres generar:")

        Spacer(Modifier.height(8.dp))

        // Opciones de reporte (radio buttons)
        ReportType.values().forEach { type ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 2.dp)
            ) {
                RadioButton(
                    selected = selectedType == type,
                    onClick = { selectedType = type }
                )
                Spacer(Modifier.width(4.dp))
                Text(type.label)
            }
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                loading = true
                error = null
                successMessage = null

                scope.launch {
                    try {
                        val db = FirebaseFirestore.getInstance()

                        // Cargamos usuarios y posts una sola vez
                        val usersSnap = db.collection("users").get().await()
                        val postsSnap = db.collection("posts").get().await()

                        val postsByUser = postsSnap.documents.groupBy {
                            it.getString("userId") ?: ""
                        }

                        val usersList = usersSnap.documents.map { doc ->
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

                        val headers: List<String>
                        val rows: List<List<String>>
                        val title: String
                        val fileName: String

                        when (selectedType) {
                            ReportType.ALL_USERS -> {
                                title = "Lista total de usuarios"
                                fileName = "reporte_usuarios"
                                headers = listOf("Nombre", "Correo")
                                rows = usersList.sortedBy { it.nombre }.map { r ->
                                    listOf(r.nombre, r.email)
                                }
                            }
                            ReportType.USERS_WITH_POSTS -> {
                                title = "Usuarios y publicaciones"
                                fileName = "reporte_usuarios_con_posts"
                                headers = listOf("Nombre", "Correo", "Posts", "Estado")
                                rows = usersList.sortedBy { it.nombre }.map { r ->
                                    listOf(
                                        r.nombre,
                                        r.email,
                                        r.postsCount.toString(),
                                        if (r.active) "Activo" else "Inactivo"
                                    )
                                }
                            }
                            ReportType.ALL_POSTS -> {
                                title = "Lista total de publicaciones"
                                fileName = "reporte_publicaciones"
                                headers = listOf("Título", "Categoría", "Talla", "Usuario")
                                rows = postsSnap.documents.map { doc ->
                                    val titulo = doc.getString("titulo") ?: "(Sin título)"
                                    val cat = doc.getString("categoria") ?: ""
                                    val talla = doc.getString("talla") ?: ""
                                    val userId = doc.getString("userId") ?: ""
                                    listOf(titulo, cat, talla, userId)
                                }
                            }
                            ReportType.ACTIVE_USERS -> {
                                title = "Usuarios activos"
                                fileName = "reporte_usuarios_activos"
                                headers = listOf("Nombre", "Correo", "Posts")
                                rows = usersList.filter { it.active }
                                    .sortedBy { it.nombre }
                                    .map { r ->
                                        listOf(
                                            r.nombre,
                                            r.email,
                                            r.postsCount.toString()
                                        )
                                    }
                            }
                            ReportType.INACTIVE_USERS -> {
                                title = "Usuarios inactivos / restringidos"
                                fileName = "reporte_usuarios_inactivos"
                                headers = listOf("Nombre", "Correo", "Posts")
                                rows = usersList.filter { !it.active }
                                    .sortedBy { it.nombre }
                                    .map { r ->
                                        listOf(
                                            r.nombre,
                                            r.email,
                                            r.postsCount.toString()
                                        )
                                    }
                            }
                        }

                        if (rows.isEmpty()) {
                            throw IllegalStateException("No hay datos para este reporte.")
                        }

                        val file = createPdfTable(
                            context = context,
                            fileName = fileName,
                            title = title,
                            headers = headers,
                            rows = rows
                        )

                        // Abrir el PDF en el emulador
                        openPdfInViewer(context, file)

                        successMessage = "PDF generado correctamente en:\n${file.absolutePath}"
                    } catch (e: Exception) {
                        error = e.localizedMessage
                    } finally {
                        loading = false
                    }
                }
            },
            enabled = !loading
        ) {
            Text(if (loading) "Generando PDF…" else "Generar reporte en PDF")
        }

        Spacer(Modifier.height(12.dp))

        if (error != null) {
            Text(
                text = "Error generando reporte: $error",
                color = MaterialTheme.colorScheme.error
            )
        }

        successMessage?.let { msg ->
            Spacer(Modifier.height(8.dp))
            Text(
                text = msg,
                style = MaterialTheme.typography.bodySmall
            )
        }

        if (!loading && error == null && successMessage == null) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Pulsa el botón para generar el PDF según la opción seleccionada.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

// =======================
//  UTIL: CREAR PDF EN TABLA
// =======================

private fun createPdfTable(
    context: Context,
    fileName: String,
    title: String,
    headers: List<String>,
    rows: List<List<String>>
): File {
    val document = PdfDocument()

    val pageWidth = 595  // A4 aproximado (72 dpi)
    val pageHeight = 842
    val margin = 40f

    val titlePaint = Paint().apply {
        textSize = 18f
        isFakeBoldText = true
    }
    val headerPaint = Paint().apply {
        textSize = 12f
        isFakeBoldText = true
    }
    val cellPaint = Paint().apply {
        textSize = 11f
    }

    val rowHeight = 18f
    val tableWidth = pageWidth - 2 * margin
    val colCount = headers.size
    val colWidth = tableWidth / colCount

    val maxDataRowsPerPage = ((pageHeight - margin * 2 - 50f) / rowHeight).toInt()

    var currentIndex = 0
    var pageNumber = 1

    while (currentIndex < rows.size) {
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        var y = margin

        // Título
        canvas.drawText(title, margin, y, titlePaint)
        y += 30f

        // Encabezados
        var x = margin
        headers.forEach { header ->
            val text = header.take(20)
            canvas.drawText(text, x + 4f, y, headerPaint)
            x += colWidth
        }
        y += rowHeight

        // Filas de datos
        val endIndex = min(currentIndex + maxDataRowsPerPage, rows.size)
        for (i in currentIndex until endIndex) {
            val row = rows[i]
            x = margin
            for (c in 0 until colCount) {
                val cellText = row.getOrNull(c)?.take(30) ?: ""
                canvas.drawText(cellText, x + 4f, y, cellPaint)
                x += colWidth
            }
            y += rowHeight
        }

        document.finishPage(page)
        currentIndex = endIndex
        pageNumber++
    }

    val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        ?: context.filesDir
    if (!dir.exists()) dir.mkdirs()

    val file = File(dir, "$fileName.pdf")
    FileOutputStream(file).use { out ->
        document.writeTo(out)
    }
    document.close()
    return file
}

// =======================
//  UTIL: ABRIR PDF
// =======================

private fun openPdfInViewer(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(
        context,
        context.packageName + ".fileprovider",
        file
    )

    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/pdf")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    val chooser = Intent.createChooser(intent, "Abrir reporte PDF")
    context.startActivity(chooser)
}

// =======================
//  UTIL: SANEAR NOMBRE
// =======================

private fun sanitizeUserName(raw: String): String {
    // Quita saltos de línea, recorta espacios y limita longitud
    val noNewLines = raw
        .replace("\n", " ")
        .replace("\r", " ")
        .trim()

    val maxLen = 40
    return if (noNewLines.length <= maxLen) {
        noNewLines
    } else {
        noNewLines.substring(0, maxLen)
    }
}