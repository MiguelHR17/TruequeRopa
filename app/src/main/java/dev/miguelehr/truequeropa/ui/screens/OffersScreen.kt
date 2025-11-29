package dev.miguelehr.truequeropa.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import dev.miguelehr.truequeropa.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OffersScreen(
    onOpenProduct: (postId: String, ownerUserId: String) -> Unit,
    padding: PaddingValues,
    viewModel: OffersViewModel = viewModel()
) {
    val offersState by viewModel.offersState.collectAsState()

    // Estado de búsqueda & filtros
    var query by remember { mutableStateOf("") }
    var catExpanded by remember { mutableStateOf(false) }
    var sizeExpanded by remember { mutableStateOf(false) }
    var catFilter: String? by remember { mutableStateOf(null) }
    var sizeFilter: String? by remember { mutableStateOf(null) }

    // Cargar posts al iniciar
    LaunchedEffect(Unit) {
        viewModel.loadAllAvailablePosts()
    }

    // Aplicar filtros cuando cambian
    LaunchedEffect(query, catFilter, sizeFilter) {
        viewModel.filterPosts(query, catFilter, sizeFilter)
    }

    Column(
        modifier = Modifier
            .padding(padding)
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text("Ofertas recientes", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))

        // Buscador
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Buscar productos o personas…") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Search
            ),
            modifier = Modifier.fillMaxWidth(),
            enabled = !offersState.isLoading
        )

        Spacer(Modifier.height(12.dp))

        // Filtros
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            // Categoría
            ExposedDropdownMenuBox(
                expanded = catExpanded,
                onExpandedChange = { catExpanded = !catExpanded },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = catFilter ?: "Categoría",
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = catExpanded,
                    onDismissRequest = { catExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Todas") },
                        onClick = { catFilter = null; catExpanded = false }
                    )
                    listOf("CAMISA", "PANTALON", "VESTIDO", "CHAQUETA", "ZAPATOS", "ACCESORIO").forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat) },
                            onClick = { catFilter = cat; catExpanded = false }
                        )
                    }
                }
            }

            // Talla
            ExposedDropdownMenuBox(
                expanded = sizeExpanded,
                onExpandedChange = { sizeExpanded = !sizeExpanded },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = sizeFilter ?: "Talla",
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = sizeExpanded,
                    onDismissRequest = { sizeExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Todas") },
                        onClick = { sizeFilter = null; sizeExpanded = false }
                    )
                    listOf("XS", "S", "M", "L", "XL").forEach { size ->
                        DropdownMenuItem(
                            text = { Text(size) },
                            onClick = { sizeFilter = size; sizeExpanded = false }
                        )
                    }
                }
            }
        }

        // Limpiar filtros
        TextButton(
            onClick = {
                query = ""
                catFilter = null
                sizeFilter = null
            },
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text("Limpiar filtros")
        }

        Spacer(Modifier.height(8.dp))

        // Mostrar loading, error o resultados
        when {
            offersState.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            offersState.error != null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = offersState.error ?: "Error desconocido",
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { viewModel.loadAllAvailablePosts() }) {
                            Text("Reintentar")
                        }
                    }
                }
            }
            offersState.filteredPosts.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No hay ofertas disponibles")
                }
            }
            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        offersState.filteredPosts,
                        key = { it.solicitantePost.id }
                    ) { postDetail ->
                        OfferRowCard(
                            post = postDetail.solicitantePost,
                            onClick = {
                                onOpenProduct(
                                    postDetail.solicitantePost.id,
                                    postDetail.solicitantePost.userId
                                )
                            }
                        )
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                }
            }
        }
    }
}

@Composable
private fun OfferRowCard(
    post: dev.miguelehr.truequeropa.model.UserPost,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mostrar primera imagen si existe
            if (post.imageUrls.isNotEmpty()) {
                Image(
                    painter = rememberAsyncImagePainter(post.imageUrls.first()),
                    contentDescription = post.titulo,
                    modifier = Modifier.size(72.dp)
                )
            } else {
                // Placeholder si no hay imagen
                Box(
                    modifier = Modifier.size(72.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(post.titulo, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${post.categoria} • Talla ${post.talla}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    post.descripcion,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
                Spacer(Modifier.height(4.dp))
                AssistChip(
                    onClick = {},
                    label = { Text(post.estado) }
                )
            }
        }
    }
}