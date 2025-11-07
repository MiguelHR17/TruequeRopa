package dev.miguelehr.truequeropa.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import dev.miguelehr.truequeropa.model.Product

/**
 * Pantalla de Perfil
 *
 * @param userId        Si es null => perfil del usuario autenticado
 * @param pinProductId  Si no es null, fija esa publicación arriba (si pertenece al perfil)
 * @param onPublish     Navega a la pantalla de "Publicar"
 * @param onOpenProduct Acción al abrir una publicación (productId) — se usa en perfil propio
 * @param padding       Padding del Scaffold
 */
@Composable
fun ProfileScreen(
    userId: String? = null,
    pinProductId: String? = null,
    onPublish: () -> Unit,
    onOpenProduct: (String) -> Unit,
    padding: PaddingValues
) {
    val me = remember { FakeRepository.currentUser }
    val myId = me.id

    // Estados para flujo de propuesta (solo en perfil ajeno)
    var proposeTarget by remember { mutableStateOf<Product?>(null) }
    var showProposeSheet by remember { mutableStateOf(false) }

    // Usuario del perfil
    val targetUser = remember(userId) {
        FakeRepository.users.find { it.id == userId } ?: me
    }
    val isMe = targetUser.id == me.id

    // Publicaciones del propietario
    val allPosts = remember(targetUser.id) {
        FakeRepository.products.filter { it.ownerId == targetUser.id }
    }

    // Publicación fijada (pin) si pertenece al perfil
    val pinned: Product? = remember(pinProductId, allPosts) {
        if (pinProductId.isNullOrBlank()) null
        else allPosts.find { it.id == pinProductId }
    }

    // Lista sin el pin (para no duplicar)
    val restPosts = remember(pinned, allPosts) {
        if (pinned == null) allPosts else allPosts.filter { it.id != pinned.id }
    }

    // “Paginación” simple en memoria
    var shownCount by remember(targetUser.id) { mutableStateOf(minOf(6, restPosts.size)) }
    val pageSize = 6
    val listState = rememberLazyListState()
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            lastIndex >= shownCount - 2 && shownCount < restPosts.size
        }
    }
    LaunchedEffect(shouldLoadMore, restPosts.size) {
        if (shouldLoadMore) shownCount = minOf(shownCount + pageSize, restPosts.size)
    }
    val posts = remember(shownCount, restPosts) { restPosts.take(shownCount) }

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
                    modifier = Modifier.size(72.dp).clip(CircleShape)
                )
            } else {
                Surface(
                    tonalElevation = 2.dp,
                    modifier = Modifier.size(72.dp).clip(CircleShape)
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

        // CTA Publicar (visible también en perfil ajeno si quieres esconderlo, condiciona con isMe)
        Button(
            onClick = onPublish,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Publicar") }

        Spacer(Modifier.height(12.dp))

        // Título de publicaciones
        Text(
            if (isMe) "Mis publicaciones" else "Publicaciones",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(8.dp))

        // ===== Lista =====
        if (pinned == null && posts.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(if (isMe) "Aún no has publicado prendas" else "Este usuario no tiene publicaciones")
            }
            return
        }

        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Card fijada
            if (pinned != null) {
                item(key = "pin_${pinned.id}") {
                    Text(
                        "Publicación fijada",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(6.dp))
                    ProfilePostCard(
                        product = pinned,
                        onOpen = {
                            if (isMe) {
                                onOpenProduct(pinned.id)
                            } else {
                                proposeTarget = pinned
                                showProposeSheet = true
                            }
                        },
                        pinned = true
                    )
                    Spacer(Modifier.height(6.dp))
                    Divider()
                    Spacer(Modifier.height(6.dp))
                }
            }

            // Resto
            items(posts, key = { it.id }) { product ->
                ProfilePostCard(
                    product = product,
                    onOpen = {
                        if (isMe) {
                            onOpenProduct(product.id)
                        } else {
                            proposeTarget = product
                            showProposeSheet = true
                        }
                    }
                )
            }

            // Footer de carga (mock)
            if (shownCount < restPosts.size) {
                item {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else {
                item { Spacer(Modifier.height(8.dp)) }
            }
        }
    }

    // === Hoja/diálogo de selección multi-ítem para propuesta ===
    if (showProposeSheet && proposeTarget != null) {
        MultiOfferSheet(
            target = proposeTarget!!,
            onDismiss = { showProposeSheet = false },
            onSend = { offeredIds ->
                if (offeredIds.isNotEmpty()) {
                    FakeRepository.sendProposal(
                        fromUserId = myId,
                        toUserId = proposeTarget!!.ownerId,
                        offeredProductIds = offeredIds,
                        requestedProductId = proposeTarget!!.id
                    )
                }
                showProposeSheet = false
            }
        )
    }
}

@Composable
private fun ProfilePostCard(
    product: Product,
    onOpen: () -> Unit,
    pinned: Boolean = false
) {
    val bg = if (pinned)
        MaterialTheme.colorScheme.primaryContainer
    else
        MaterialTheme.colorScheme.surfaceVariant

    Card(
        onClick = onOpen,
        colors = CardDefaults.cardColors(containerColor = bg),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp)) {
            if (product.imageUrl.isNotBlank()) {
                AsyncImage(
                    model = product.imageUrl,
                    contentDescription = product.titulo,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
                Spacer(Modifier.height(8.dp))
            }

            Text(
                product.titulo,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                product.descripcion,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SmallTag(product.talla.name)
                SmallTag(product.estado.name)
                SmallTag(product.categoria.name)
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

/* ========== Sheet de selección multi-ítem para proponer trueque ========== */

@Composable
private fun MultiOfferSheet(
    target: Product,
    onDismiss: () -> Unit,
    onSend: (List<String>) -> Unit
) {
    val myProducts = remember {
        FakeRepository.products.filter { it.ownerId == FakeRepository.currentUser.id }
    }
    val selected = remember { mutableStateListOf<String>() }
    val maxSelect = 5

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = { onSend(selected.toList()) },
                enabled = selected.isNotEmpty()
            ) { Text("Enviar propuesta") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancelar") }
        },
        title = { Text("Proponer trueque") },
        text = {
            Column {
                Text("Objetivo: ${target.titulo}", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                Text("Elige hasta $maxSelect de tus prendas:")

                Spacer(Modifier.height(8.dp))

                myProducts.forEach { p ->
                    val isChecked = p.id in selected
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                if (isChecked) selected.remove(p.id)
                                else if (selected.size < maxSelect) selected.add(p.id)
                            }
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    if (selected.size < maxSelect) selected.add(p.id)
                                } else {
                                    selected.remove(p.id)
                                }
                            }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("${p.titulo} • ${p.categoria} • ${p.talla}")
                    }
                }

                if (selected.size >= maxSelect) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Has alcanzado el máximo de $maxSelect prendas.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    )
}