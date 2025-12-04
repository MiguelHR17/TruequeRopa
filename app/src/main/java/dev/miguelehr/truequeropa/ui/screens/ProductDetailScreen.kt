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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
                    Log.d(
                        "ProductDetailScreen",
                        "Post cargado: ${result.first.titulo}"
                    )
                    Log.d(
                        "ProductDetailScreen",
                        "Imágenes: ${result.first.imageUrls.size}"
                    )
                } else {
                    error = "No se encontró el producto"
                    Log.e(
                        "ProductDetailScreen",
                        "Result es null para postId: $postId"
                    )
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

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    // Galería de imágenes
                    if (p.imageUrls.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(p.imageUrls) { url ->
                                Card(
                                    modifier = Modifier
                                        .size(200.dp),
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
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
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

                    // Botón de proponer trueque
                    Button(
                        onClick = { onProponerTrueque(p.userId,postId) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Proponer Trueque")
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
    }
}