package dev.miguelehr.truequeropa.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
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
import dev.miguelehr.truequeropa.model.FakeRepository
import dev.miguelehr.truequeropa.auth.FirebaseAuthManager
import dev.miguelehr.truequeropa.data.FirestoreManager
import dev.miguelehr.truequeropa.model.UserPost
import androidx.compose.material.icons.filled.Delete
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale

/**
 * Pantalla de Perfil
 *
 * @param userId        Si es null => perfil del usuario autenticado
 * @param pinProductId  (por ahora sin uso real, se usaba con FakeRepository)
 * @param onPublish     Navega a la pantalla de "Publicar"
 * @param onOpenProduct Acción al abrir una publicación (por ahora no se usa con UserPost)
 * @param padding       Padding del Scaffold
 */
@Composable
fun ProfileScreen(
    userId: String? = null,
    pinProductId: String? = null, // reservado para futura lógica de "pin"
    onPublish: () -> Unit,
    onOpenProduct: (String) -> Unit,
    padding: PaddingValues
) {
    val me = remember { FakeRepository.currentUser }
    val myId = me.id

    // Usuario del perfil (si userId es null, muestra mi propio perfil)
    val targetUser = remember(userId) {
        FakeRepository.users.find { it.id == userId } ?: me
    }
    val isMe = targetUser.id == me.id

    Column(
        modifier = Modifier
            .padding(padding)
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // ===== Header =====
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!targetUser.photoUrl.isNullOrBlank()) {
                AsyncImage(
                    model = targetUser.photoUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                )
            } else {
                Surface(
                    tonalElevation = 2.dp,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(targetUser.nombre.take(1).uppercase())
                    }
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    targetUser.nombre,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    targetUser.correo,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isMe) {
                ElevatedButton(onClick = { /* TODO: abrir picker y actualizar photoUrl */ }) {
                    Icon(Icons.Filled.Edit, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Editar foto")
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // CTA Publicar
        Button(
            onClick = onPublish,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Publicar")
        }

        Spacer(Modifier.height(12.dp))

        // Título de publicaciones
        Text(
            if (isMe) "Mis publicaciones" else "Publicaciones",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(8.dp))

        // ===== Listado REAL de publicaciones desde Firestore =====
        // userId == null  => uso el usuario autenticado (mi perfil)
        // userId != null   => perfil de otro usuario
        UserPostsSection(
            userId = if (isMe) null else targetUser.id,
            isOwner = isMe,                    // 👈 solo el dueño puede eliminar
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Sección que escucha en tiempo real las publicaciones de un usuario
 * desde Firestore y las muestra en un LazyColumn.
 *
 * @param userId  Si es null => usa el uid del usuario autenticado
 */
@Composable
fun UserPostsSection(
    userId: String?,
    isOwner: Boolean,
    modifier: Modifier = Modifier
) {
    val uidForProfile = userId ?: FirebaseAuthManager.currentUserId()
    val posts = remember { mutableStateListOf<UserPost>() }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var deletingId by remember { mutableStateOf<String?>(null) }

    if (uidForProfile == null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Debes iniciar sesión para ver tus publicaciones")
        }
        return
    }

    // Escuchar los posts del usuario en tiempo real
    DisposableEffect(uidForProfile) {
        val reg = FirestoreManager.listenPostsForUser(uidForProfile) { list, err ->
            if (err != null) {
                error = err
                loading = false
            } else {
                posts.clear()
                posts.addAll(list)
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
                            onDelete = {
                                deletingId = post.id
                                FirestoreManager.deleteUserPost(post.id) { ok, err ->
                                    if (!ok) {
                                        error = err ?: "No se pudo eliminar la publicación"
                                    }
                                    deletingId = null
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UserPostCard(
    post: UserPost,
    canDelete: Boolean,
    deleting: Boolean,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(Modifier.padding(12.dp)) {

            // 👇 Mostrar la primera imagen (si hay)
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

            // Título y descripción
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
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(8.dp))

            // Tags de info básica
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SmallTag(post.talla)
                SmallTag(post.estado)
                SmallTag(post.categoria)
            }

            // Botón eliminar solo si es el dueño del perfil
            if (canDelete) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDelete,
                        enabled = !deleting
                    ) {
                        if (deleting) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Eliminando…")
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "Eliminar publicación"
                            )
                            Spacer(Modifier.width(4.dp))
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
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(100))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium, color = Color.Unspecified)
    }
}