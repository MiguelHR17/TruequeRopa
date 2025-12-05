package dev.miguelehr.truequeropa.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import dev.miguelehr.truequeropa.auth.FirebaseAuthManager
import dev.miguelehr.truequeropa.data.FirestoreManager
import dev.miguelehr.truequeropa.data.FirebaseStorageManager
import dev.miguelehr.truequeropa.model.UserPost
import dev.miguelehr.truequeropa.model.UserProfile
import kotlinx.coroutines.launch

/**
 * Pantalla de Perfil
 */
@Composable
fun ProfileScreen(
    userId: String? = null,
    pinProductId: String? = null,
    onPublish: () -> Unit,
    onOpenProduct: (String) -> Unit,
    padding: PaddingValues
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val currentUid = FirebaseAuthManager.currentUserId()
    val uidForProfile = userId ?: currentUid

    var profile by remember { mutableStateOf<UserProfile?>(null) }
    var loadingProfile by remember { mutableStateOf(true) }
    var errorProfile by remember { mutableStateOf<String?>(null) }

    // URL que se muestra en el circulito (puede ser content:// o https://)
    var localPhotoUrl by remember { mutableStateOf<String?>(null) }
    var uploadingPhoto by remember { mutableStateOf(false) }

    // Para mostrar la foto en grande
    var showPhotoPreview by remember { mutableStateOf(false) }

    // Cargar perfil desde Firestore
    LaunchedEffect(uidForProfile) {
        if (uidForProfile == null) {
            errorProfile = "Debes iniciar sesión para ver tu perfil."
            loadingProfile = false
            return@LaunchedEffect
        }
        loadingProfile = true
        errorProfile = null
        try {
            val fetched = FirestoreManager.getUser(uidForProfile)
            profile = fetched
            localPhotoUrl = fetched?.photoUrl    // <- SE TOMA DE FIRESTORE
        } catch (e: Exception) {
            errorProfile = e.localizedMessage ?: "Error al cargar el perfil."
        } finally {
            loadingProfile = false
        }
    }

    val isMe = uidForProfile != null && uidForProfile == currentUid

    // Picker de imagen para la foto de perfil
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null && uidForProfile != null) {
            // 1) Preview inmediato
            localPhotoUrl = uri.toString()

            // 2) Subir a Storage y guardar URL real en Firestore
            scope.launch {
                uploadingPhoto = true
                try {
                    val url = FirebaseStorageManager.uploadProfilePhoto(
                        context = context,
                        uid = uidForProfile,
                        uri = uri
                    )
                    if (url != null) {
                        val ok = FirestoreManager.updateUserPhoto(uidForProfile, url)
                        if (ok) {
                            localPhotoUrl = url
                            profile = profile?.copy(photoUrl = url)
                        }
                    }
                } finally {
                    uploadingPhoto = false
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .padding(padding)
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        when {
            loadingProfile -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
                return@Column
            }

            errorProfile != null || uidForProfile == null || profile == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(errorProfile ?: "No se pudo cargar el perfil.")
                }
                return@Column
            }
        }

        val user = profile!!

        val fullName = user.nombre.ifBlank { user.email.substringBefore("@") }
        val firstName = fullName.trim().split(" ").firstOrNull().orEmpty()

        // ⚠️ Nuevo: considerar usuario restringido
        val isRestricted = (!user.active) || user.restricted

        // ===== Header =====
        Row(verticalAlignment = Alignment.CenterVertically) {

            // Avatar clickeable para ver la foto en grande
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .clickable(
                        enabled = !localPhotoUrl.isNullOrBlank()
                    ) { showPhotoPreview = true }
            ) {
                if (!localPhotoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = localPhotoUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Surface(
                        tonalElevation = 2.dp,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(
                            Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(firstName.take(1).uppercase())
                        }
                    }
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = fullName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (isMe) {
                    Text(
                        text = "Correo: ${user.email}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // 🔴 Texto de usuario restringido (solo en su propio perfil)
                    if (isRestricted) {
                        Text(
                            text = "Usuario restringido",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        if (isMe) {
            Button(
                onClick = onPublish,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isRestricted    // 👈 botón visible pero desactivado si está restringido
            ) {
                Text("Publicar")
            }

            if (isRestricted) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "No puedes publicar mientras tu cuenta esté restringida.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(Modifier.height(12.dp))
        }

        Text(
            text = if (isMe) "Mis publicaciones" else "Publicaciones",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(8.dp))

        UserPostsSection(
            userId = if (isMe) null else uidForProfile,
            isOwner = isMe,
            onOpenProduct = onOpenProduct,
            modifier = Modifier.weight(1f)
        )
    }

    // ===== Preview en grande de la foto de perfil =====
    if (showPhotoPreview && !localPhotoUrl.isNullOrBlank()) {
        AlertDialog(
            onDismissRequest = { showPhotoPreview = false },
            confirmButton = {
                TextButton(onClick = { showPhotoPreview = false }) {
                    Text("Cerrar")
                }
            },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = localPhotoUrl,
                        contentDescription = "Foto de perfil",
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        )
    }
}

/* ==== UserPostsSection, UserPostCard y SmallTag ==== */

@Composable
fun UserPostsSection(
    userId: String?,
    isOwner: Boolean,
    onOpenProduct: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uidForProfile = userId ?: FirebaseAuthManager.currentUserId()
    val posts = remember { mutableStateListOf<UserPost>() }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var deletingId by remember { mutableStateOf<String?>(null) }
    var postToDelete by remember { mutableStateOf<UserPost?>(null) }

    // 🔁 nuevo: volver a publicar
    var republishingId by remember { mutableStateOf<String?>(null) }
    var postToRepublish by remember { mutableStateOf<UserPost?>(null) }
    val scope = rememberCoroutineScope()

    if (uidForProfile == null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Debes iniciar sesión para ver tus publicaciones")
        }
        return
    }

    DisposableEffect(uidForProfile) {
        val reg = FirestoreManager.listenPostsForUser(uidForProfile) { list, err ->
            if (err != null) {
                error = err
                loading = false
            } else {
                posts.clear()
                // ✅ El dueño ve todos sus posts (incluyendo usados)
                //   Otros sólo ven posts disponibles (estadoTrueque == "0")
                val visible = if (isOwner) {
                    list
                } else {
                    list.filter { it.estadoTrueque == "0" }
                }
                posts.addAll(visible)
                loading = false
            }
        }
        onDispose { reg.remove() }
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        when {
            loading -> {
                Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            error != null -> {
                Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Error cargando publicaciones: $error")
                }
            }

            posts.isEmpty() -> {
                Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Aún no hay publicaciones.")
                }
            }

            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(posts, key = { it.id }) { post ->
                        UserPostCard(
                            post = post,
                            canDelete = isOwner,
                            deleting = deletingId == post.id,
                            republishing = republishingId == post.id,
                            onDelete = { postToDelete = post },
                            onRepublish = { postToRepublish = post },
                            onClick = { onOpenProduct(post.id) }
                        )
                    }
                }
            }
        }

        // Diálogo de ELIMINAR
        val toDelete = postToDelete
        if (toDelete != null) {
            AlertDialog(
                onDismissRequest = {
                    if (deletingId == null) postToDelete = null
                },
                title = { Text("Eliminar publicación") },
                text = {
                    Text("¿Estás seguro de eliminar esta publicación? Esta acción no se puede deshacer.")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            deletingId = toDelete.id
                            FirestoreManager.deleteUserPost(toDelete.id) { ok, err ->
                                if (!ok) {
                                    error = err ?: "No se pudo eliminar la publicación"
                                }
                                deletingId = null
                                postToDelete = null
                            }
                        },
                        enabled = deletingId == null
                    ) {
                        Text("Eliminar", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { postToDelete = null },
                        enabled = deletingId == null
                    ) {
                        Text("Cancelar")
                    }
                }
            )
        }

        // Diálogo de VOLVER A PUBLICAR
        val toRepublish = postToRepublish
        if (toRepublish != null) {
            AlertDialog(
                onDismissRequest = {
                    if (republishingId == null) postToRepublish = null
                },
                title = { Text("Volver a publicar") },
                text = {
                    Text(
                        "Esta prenda ya fue utilizada en un trueque aprobado. " +
                                "¿Deseas volver a publicarla para que aparezca nuevamente en las ofertas?"
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            republishingId = toRepublish.id
                            scope.launch {
                                val ok = FirestoreManager.UpdatePost(
                                    toRepublish.id,
                                    "0" // vuelve a estar disponible
                                )
                                if (!ok) {
                                    error = "No se pudo volver a publicar la prenda"
                                }
                                republishingId = null
                                postToRepublish = null
                            }
                        },
                        enabled = republishingId == null
                    ) {
                        Text("Volver a publicar")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { postToRepublish = null },
                        enabled = republishingId == null
                    ) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}

@Composable
private fun UserPostCard(
    post: UserPost,
    canDelete: Boolean,
    deleting: Boolean,
    republishing: Boolean,
    onDelete: () -> Unit,
    onRepublish: () -> Unit,
    onClick: () -> Unit
) {
    val isUsedInTrade = post.estadoTrueque != "0"
    val cardColor = if (isUsedInTrade) {
        // 🔴 Tarjeta roja SUAVE para posts usados en trueque (sólo dueño los ve)
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val isBusy = deleting || republishing

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = cardColor
        )
    ) {
        Column(Modifier.padding(12.dp)) {
            val firstImage = post.imageUrls.firstOrNull()
            if (firstImage != null) {
                AsyncImage(
                    model = firstImage,
                    contentDescription = post.titulo,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.height(8.dp))
            }

            Text(
                text = post.titulo,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))

            Text(
                text = post.descripcion,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SmallTag(post.talla)
                SmallTag(post.estado)
                SmallTag(post.categoria)
            }

            if (isUsedInTrade) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "✅ Utilizada en un trueque aprobado",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            if (canDelete) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // A la izquierda: Volver a publicar (solo si está usada)
                    if (isUsedInTrade) {
                        TextButton(
                            onClick = onRepublish,
                            enabled = !isBusy
                        ) {
                            Text("Volver a publicar")
                        }
                        Spacer(Modifier.width(8.dp))
                    }

                    // A la derecha: Eliminar
                    TextButton(
                        onClick = onDelete,
                        enabled = !isBusy
                    ) {
                        if (deleting) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Eliminando…")
                        } else {
                            androidx.compose.material3.Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "Eliminar"
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Eliminar")
                        }
                    }
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