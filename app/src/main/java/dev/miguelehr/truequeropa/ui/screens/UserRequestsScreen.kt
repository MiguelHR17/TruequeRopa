package dev.miguelehr.truequeropa.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import dev.miguelehr.truequeropa.model.UserRequestDetails
import dev.miguelehr.truequeropa.ui.viewmodels.UserRequestsViewModel
import kotlinx.coroutines.launch

@Composable
fun UserRequestsScreen(
    userId: String,
    viewModel: UserRequestsViewModel = viewModel(),
    padding: PaddingValues = PaddingValues(0.dp),
    onUnreviewedCountChange: (Int) -> Unit = {},
    onNavigateToUserPosts: (String, String, String) -> Unit
) {
    LaunchedEffect(userId) {
        viewModel.fetchUserRequests(userId, 0)
    }

    val userRequests by viewModel.userRequests.collectAsState()

    // usamos el id de la request como clave
    var expandedIds by remember { mutableStateOf(setOf<String>()) }
    val scope = rememberCoroutineScope()

    // diálogo para mostrar correo al aceptar
    var contactEmail by remember { mutableStateOf<String?>(null) }

    // Solo propuestas donde yo soy el propietario
    val incomingRequests = remember(userRequests, userId) {
        userRequests.filter { it.propietarioProfile.uid == userId }
    }

    val pendingRequests = incomingRequests.filter { it.request.estado == "0" }
    val attendedRequests = incomingRequests.filter { it.request.estado != "0" }

    // globito rojo = solo pendientes
    LaunchedEffect(pendingRequests) {
        onUnreviewedCountChange(pendingRequests.size)
    }

    val bottomPad = padding.calculateBottomPadding() + 96.dp

    // para colapsar/expandir el bloque de “ya atendidas”
    var attendedExpanded by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .padding(
                start = padding.calculateStartPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                end = padding.calculateEndPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                top = padding.calculateTopPadding(),
                bottom = bottomPad
            )
            .fillMaxSize()
    ) {
        Text(
            "Propuesta de Intercambio",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )

        if (incomingRequests.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Aún no tienes propuestas de trueque.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ======= PENDIENTES =======
                items(pendingRequests, key = { it.request.id }) { details ->
                    val isExpanded = expandedIds.contains(details.request.id)
                    UserRequestItem(
                        details = details,
                        isExpanded = isExpanded,
                        onToggle = {
                            expandedIds =
                                if (isExpanded) expandedIds - details.request.id
                                else expandedIds + details.request.id
                        },
                        onAccept = {
                            scope.launch {
                                // marcar prendas como usadas
                                viewModel.updPost(details.propietarioPost.id, "1")
                                viewModel.updPost(details.solicitantePost.id, "1")

                                viewModel.acceptRequest(
                                    details.request.id,
                                    details.propietarioProfile.uid
                                )
                                viewModel.fetchUserRequests(userId, 0)
                                contactEmail = details.solicitanteProfile.email
                            }
                            expandedIds = expandedIds - details.request.id
                        },
                        onReject = {
                            scope.launch {
                                viewModel.updPost(details.propietarioPost.id, "0")
                                viewModel.updPost(details.solicitantePost.id, "0")
                                viewModel.rejectRequest(
                                    details.request.id,
                                    details.propietarioProfile.uid
                                )
                                viewModel.fetchUserRequests(userId, 0)
                            }
                            expandedIds = expandedIds - details.request.id
                        },
                        onNavigateToUserPosts = {
                            onNavigateToUserPosts(
                                details.solicitanteProfile.uid,
                                details.solicitantePost.id.toString(),
                                details.request.id.toString()
                            )
                        }
                    )
                }

                // ======= YA ATENDIDAS (COLAPSABLE) =======
                if (attendedRequests.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "Propuestas ya atendidas",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    IconButton(
                                        onClick = { attendedExpanded = !attendedExpanded }
                                    ) {
                                        Icon(
                                            imageVector = if (attendedExpanded)
                                                Icons.Default.KeyboardArrowUp
                                            else
                                                Icons.Default.KeyboardArrowDown,
                                            contentDescription = null
                                        )
                                    }
                                }

                                AnimatedVisibility(attendedExpanded) {
                                    Column {
                                        Spacer(Modifier.height(8.dp))
                                        attendedRequests.forEach { details ->
                                            val isExpanded =
                                                expandedIds.contains(details.request.id)
                                            UserRequestItem(
                                                details = details,
                                                isExpanded = isExpanded,
                                                onToggle = {
                                                    expandedIds =
                                                        if (isExpanded) expandedIds - details.request.id
                                                        else expandedIds + details.request.id
                                                },
                                                onAccept = {},   // ya no se usan
                                                onReject = {},
                                                onNavigateToUserPosts = {}
                                            )
                                            Spacer(Modifier.height(8.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ===== diálogo de contacto al aceptar =====
        val email = contactEmail
        if (email != null) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { contactEmail = null },
                title = { Text("Propuesta aceptada") },
                text = {
                    Text(
                        "Ponte en contacto con esta persona para coordinar el intercambio:\n\n$email"
                    )
                },
                confirmButton = {
                    TextButton(onClick = { contactEmail = null }) {
                        Text("Cerrar")
                    }
                }
            )
        }
    }
}

@Composable
fun UserRequestItem(
    details: UserRequestDetails,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onNavigateToUserPosts: () -> Unit
) {
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = when (details.request.estado) {
                "0" -> Color(0xFFC5E1A5)          // pendiente (verde suave)
                "1", "2" -> Color(0xFFF8D7DA)     // atendida
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {

            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${details.solicitanteProfile.nombre} quiere intercambiar contigo",
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )

                if (details.request.estado == "0" && !isExpanded) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(Color(0xFFD32F2F), CircleShape)
                    )
                    Spacer(Modifier.width(8.dp))
                }

                IconButton(onClick = onToggle) {
                    Icon(
                        imageVector = if (isExpanded)
                            Icons.Default.KeyboardArrowUp
                        else
                            Icons.Default.KeyboardArrowDown,
                        contentDescription = null
                    )
                }
            }

            AnimatedVisibility(isExpanded) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {

                    Text(
                        "Tu prenda:",
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                    )
                    Spacer(Modifier.height(4.dp))

                    val ownerImageUrl = details.propietarioPost.imageUrls.firstOrNull()
                    if (ownerImageUrl != null) {
                        Image(
                            painter = rememberAsyncImagePainter(ownerImageUrl),
                            contentDescription = details.propietarioPost.descripcion,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.LightGray, RoundedCornerShape(12.dp))
                        )
                    }
                    Text(details.propietarioPost.titulo)

                    Spacer(Modifier.height(12.dp))

                    // ========= Prenda del solicitante =========
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Prenda de ${details.solicitanteProfile.nombre}:",
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                        )

                        if (details.request.estado == "0") {
                            FilledTonalButton(onClick = onNavigateToUserPosts) {
                                Text("Publicaciones")
                            }
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    val requesterImageUrl = details.solicitantePost.imageUrls.firstOrNull()
                    if (requesterImageUrl != null) {
                        Image(
                            painter = rememberAsyncImagePainter(requesterImageUrl),
                            contentDescription = details.solicitantePost.descripcion,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.LightGray, RoundedCornerShape(12.dp))
                        )
                    }
                    Text(details.solicitantePost.titulo)

                    Spacer(Modifier.height(16.dp))

                    if (details.request.estado == "0") {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = onAccept,
                                modifier = Modifier.weight(1f)
                            ) { Text("Aceptar") }

                            OutlinedButton(
                                onClick = onReject,
                                modifier = Modifier.weight(1f)
                            ) { Text("Rechazar") }
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = when (details.request.estado) {
                            "1" -> "✅ Propuesta aceptada"
                            "2" -> "❌ Propuesta rechazada"
                            else -> "⚠ Propuesta pendiente"
                        },
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                    )
                }
            }
        }
    }
}