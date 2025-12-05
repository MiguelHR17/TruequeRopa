package dev.miguelehr.truequeropa.ui.screens

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import dev.miguelehr.truequeropa.auth.FirebaseAuthManager
import dev.miguelehr.truequeropa.data.FirestoreManager
import dev.miguelehr.truequeropa.model.UserPost
import dev.miguelehr.truequeropa.model.UserProfile
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    postId: String,
    onBack: () -> Unit,
    onProponerTrueque: (String, String) -> Unit,
    onVerPerfil: (String) -> Unit
) {
    var post by remember { mutableStateOf<UserPost?>(null) }
    var owner by remember { mutableStateOf<UserProfile?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    val currentUserId = FirebaseAuthManager.currentUserId()

    // Editar descripción
    var showEditDescDialog by remember { mutableStateOf(false) }
    var editedDescription by remember { mutableStateOf("") }
    var editingDescription by remember { mutableStateOf(false) }

    // Volver a publicar
    var showRepublishDialog by remember { mutableStateOf(false) }
    var republishing by remember { mutableStateOf(false) }

    // Cargar post + info de usuario de Firestore
    LaunchedEffect(postId) {
        scope.launch {
            try {
                Log.d("ProductDetailScreen", "Cargando post: $postId")
                isLoading = true
                error = null

                val result = FirestoreManager.getPostWithUserDetails(postId)

                if (result != null) {
                    post = result.first
                    owner = result.second
                    Log.d("ProductDetailScreen", "Post cargado: ${result.first.titulo}")
                    Log.d("ProductDetailScreen", "Imágenes: ${result.first.imageUrls.size}")
                } else {
                    error = "No se encontró el producto"
                    Log.e("ProductDetailScreen", "Result es null para postId: $postId")
                }
            } catch (e: Exception) {
                error = e.localizedMessage ?: "Error desconocido"
                Log.e("ProductDetailScreen", "Error al cargar", e)
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalle del producto") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                }
            )
        }
    ) { padding ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(error ?: "Ocurrió un error")
                }
            }

            post != null && owner != null -> {
                val p = post!!
                val o = owner!!

                val isMyPost = currentUserId != null && p.userId == currentUserId
                val isUsedInTrade = p.estadoTrueque != "0"
                val canProposeTrade = !isMyPost && !isUsedInTrade

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        // 🔽 margen extra abajo para que el botón no quede tapado
                        .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp)
                ) {
                    // Galería de imágenes
                    if (p.imageUrls.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(p.imageUrls) { url ->
                                Card(
                                    modifier = Modifier.size(200.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Image(
                                        painter = rememberAsyncImagePainter(url),
                                        contentDescription = p.titulo,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(MaterialTheme.shapes.medium),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }
                    } else {
                        // Sin imágenes
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Filled.Image,
                                        contentDescription = null,
                                        modifier = Modifier.size(64.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        "Sin imagen",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Título
                    Text(
                        text = p.titulo,
                        style = MaterialTheme.typography.headlineMedium
                    )

                    Spacer(Modifier.height(8.dp))

                    // Chips de info básica
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (p.categoria.isNotBlank()) {
                            FilterChip(
                                selected = false,
                                onClick = {},
                                label = { Text(p.categoria) }
                            )
                        }
                        if (p.talla.isNotBlank()) {
                            FilterChip(
                                selected = false,
                                onClick = {},
                                label = { Text("Talla ${p.talla}") }
                            )
                        }
                        if (p.estado.isNotBlank()) {
                            FilterChip(
                                selected = false,
                                onClick = {},
                                label = { Text(p.estado) }
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Descripción
                    Text(
                        text = "Descripción",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = p.descripcion,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    // Botón para EDITAR descripción (solo dueño)
                    if (isMyPost) {
                        Spacer(Modifier.height(8.dp))
                        TextButton(
                            onClick = {
                                editedDescription = p.descripcion
                                showEditDescDialog = true
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = "Editar descripción",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Editar descripción")
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    // Info del dueño + botón "Ver perfil"
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                text = "Publicado por",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = o.nombre.ifBlank { o.email.substringBefore("@") },
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = o.email,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme
                                    .onSecondaryContainer
                                    .copy(alpha = 0.7f)
                            )

                            Spacer(Modifier.height(8.dp))

                            // Botón para ver perfil
                            TextButton(
                                onClick = { onVerPerfil(p.userId) }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Person,
                                    contentDescription = "Ver perfil",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.size(4.dp))
                                Text("Ver perfil")
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    // Botón de proponer trueque (dentro del scroll)
                    Button(
                        onClick = {
                            if (canProposeTrade) {
                                onProponerTrueque(p.userId, postId)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = canProposeTrade
                    ) {
                        Text("Proponer Trueque")
                    }

                    // Botón de VOLVER A PUBLICAR (solo dueño y usada)
                    if (isMyPost && isUsedInTrade) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { showRepublishDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !republishing
                        ) {
                            Text("Volver a publicar esta prenda")
                        }
                    }
                }
            }

            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No se encontró el producto.")
                }
            }
        }

        // ===== Diálogo para VOLVER A PUBLICAR desde el detalle =====
        if (showRepublishDialog && post != null) {
            AlertDialog(
                onDismissRequest = {
                    if (!republishing) showRepublishDialog = false
                },
                title = { Text("Volver a publicar") },
                text = {
                    Text(
                        "Esta prenda ya fue utilizada en un trueque aprobado. " +
                                "¿Quieres volver a publicarla para nuevos intercambios?"
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val currentPost = post!!
                            republishing = true
                            scope.launch {
                                val ok = FirestoreManager.UpdatePost(currentPost.id, "0")
                                if (ok) {
                                    post = currentPost.copy(estadoTrueque = "0")
                                } else {
                                    error = "No se pudo volver a publicar la prenda"
                                }
                                republishing = false
                                showRepublishDialog = false
                            }
                        },
                        enabled = !republishing
                    ) {
                        Text("Volver a publicar")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showRepublishDialog = false },
                        enabled = !republishing
                    ) {
                        Text("Cancelar")
                    }
                }
            )
        }

        // ===== Diálogo para EDITAR DESCRIPCIÓN =====
        if (showEditDescDialog && post != null) {
            AlertDialog(
                onDismissRequest = {
                    if (!editingDescription) showEditDescDialog = false
                },
                title = { Text("Editar descripción") },
                text = {
                    Column {
                        Text(
                            "Modifica la descripción de tu prenda:",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = editedDescription,
                            onValueChange = { editedDescription = it },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            maxLines = 5
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val currentPost = post!!
                            val newDesc = editedDescription.trim()
                            if (newDesc.isBlank()) return@TextButton
                            editingDescription = true
                            scope.launch {
                                val ok = FirestoreManager.updatePostDescription(
                                    currentPost.id,
                                    newDesc
                                )
                                if (ok) {
                                    post = currentPost.copy(descripcion = newDesc)
                                } else {
                                    error = "No se pudo actualizar la descripción"
                                }
                                editingDescription = false
                                showEditDescDialog = false
                            }
                        },
                        enabled = !editingDescription && editedDescription.isNotBlank()
                    ) {
                        Text("Guardar")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showEditDescDialog = false },
                        enabled = !editingDescription
                    ) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}