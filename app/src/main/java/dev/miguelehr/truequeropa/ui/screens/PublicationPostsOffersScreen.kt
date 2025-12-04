package dev.miguelehr.truequeropa.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import dev.miguelehr.truequeropa.auth.FirebaseAuthManager
import dev.miguelehr.truequeropa.data.FirestoreManager
import dev.miguelehr.truequeropa.model.UserPostsDetails
import kotlinx.coroutines.launch

/**
 * Pantalla donde el USUARIO ACTUAL elige una de SUS publicaciones
 * para ofrecer en trueque por la prenda de otra persona.
 *
 * @param userId       Id del dueño de la publicación destino (receptor del trueque)
 * @param postIdProp   Id de la publicación del receptor (la que viste en el detalle)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublicationPostsOffersScreen(
    userId: String,        // dueño de la prenda objetivo (receptor)
    postIdProp: String,    // post del receptor
    onNavigateToOffers: () -> Unit,
    onBack: () -> Unit
) {
    val currentUserId = FirebaseAuthManager.currentUserId()

    // Si por algún motivo no hay usuario logueado, salimos
    if (currentUserId == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Debes iniciar sesión para proponer un trueque.")
        }
        return
    }

    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var myPosts by remember { mutableStateOf<List<UserPostsDetails>>(emptyList()) }
    var selectedPost by remember { mutableStateOf<UserPostsDetails?>(null) }
    var showResultDialog by remember { mutableStateOf(false) }
    var resultMessage by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()

    // Cargar SOLO publicaciones del usuario actual
    LaunchedEffect(currentUserId) {
        loading = true
        error = null
        try {
            // Solo prendas disponibles (estadoTrueque == "0")
            myPosts = FirestoreManager.getAllUserPostDetailsForUser(currentUserId)
        } catch (e: Exception) {
            error = e.localizedMessage ?: "Error al cargar tus publicaciones."
        } finally {
            loading = false
        }
    }

    Scaffold(
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { Text("Elige una prenda") },
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            when {
                loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                error != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(error ?: "Ocurrió un error.")
                    }
                }

                myPosts.isEmpty() -> {
                    // No tiene publicaciones disponibles
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No tienes publicaciones disponibles para ofrecer.\n" +
                                    "Primero crea una publicación en tu perfil.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                else -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = "Selecciona cuál de tus prendas quieres ofrecer en trueque.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(12.dp))

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(myPosts) { details ->
                                val post = details.solicitantePost  // así se llama en tu modelo
                                PublicationOfferCard(
                                    title = post.titulo,
                                    description = post.descripcion,
                                    imageUrls = post.imageUrls,
                                    onClick = { selectedPost = details }
                                )
                            }
                        }
                    }
                }
            }

            // Diálogo de confirmación de envío de propuesta
            val postToOffer = selectedPost
            if (postToOffer != null) {
                val myPost = postToOffer.solicitantePost

                AlertDialog(
                    onDismissRequest = { selectedPost = null },
                    title = { Text("Confirmar trueque") },
                    text = {
                        Text(
                            "¿Quieres ofrecer \"${myPost.titulo}\" a cambio de la prenda seleccionada?\n\n" +
                                    "Esta propuesta será enviada al dueño de la publicación."
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                // Enviamos la propuesta a Firestore
                                scope.launch {
                                    try {
                                        FirestoreManager.createUserRequest(
                                            postIdPropietario = postIdProp,           // prenda del receptor
                                            postIdSolicitante = myPost.id,            // tu prenda
                                            estado = "0"                              // pendiente
                                        ) { ok ->
                                            if (ok) {
                                                resultMessage =
                                                    "Tu propuesta de trueque fue enviada correctamente."
                                            } else {
                                                resultMessage =
                                                    "Ocurrió un problema al enviar la propuesta."
                                            }
                                            showResultDialog = true
                                        }
                                    } catch (e: Exception) {
                                        resultMessage =
                                            "Error al enviar la propuesta: ${e.localizedMessage}"
                                        showResultDialog = true
                                    } finally {
                                        selectedPost = null
                                    }
                                }
                            }
                        ) {
                            Text("Confirmar")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { selectedPost = null }) {
                            Text("Cancelar")
                        }
                    }
                )
            }

            // Diálogo final informando resultado y volviendo a Ofertas
            if (showResultDialog) {
                AlertDialog(
                    onDismissRequest = {
                        showResultDialog = false
                        onNavigateToOffers()
                    },
                    title = { Text("Propuesta de trueque") },
                    text = { Text(resultMessage) },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showResultDialog = false
                                onNavigateToOffers()
                            }
                        ) {
                            Text("Aceptar")
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun PublicationOfferCard(
    title: String,
    description: String,
    imageUrls: List<String>,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(Modifier.padding(12.dp)) {
            val firstImage = imageUrls.firstOrNull()
            if (firstImage != null) {
                Image(
                    painter = rememberAsyncImagePainter(firstImage),
                    contentDescription = title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.LightGray, RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.height(8.dp))
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3
            )
        }
    }
}