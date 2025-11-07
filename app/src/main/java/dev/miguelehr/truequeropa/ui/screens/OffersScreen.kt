package dev.miguelehr.truequeropa.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import dev.miguelehr.truequeropa.model.*

@Composable
fun OffersScreen(
    padding: PaddingValues,
    onOpenProduct: (productId: String, ownerUserId: String) -> Unit,
    onOpenProfile: (userId: String) -> Unit
) {
    // --- estado de búsqueda de personas
    var personQuery by remember { mutableStateOf("") }

    // --- filtros de categoría para ofertas
    var selectedCategory: Category? by remember { mutableStateOf(null) }

    val allUsers = remember { FakeRepository.users }
    val allProducts = remember { FakeRepository.products }

    // Sugerencias de personas (muestra máx. 5)
    val userResults = remember(personQuery, allUsers) {
        val q = personQuery.trim()
        if (q.length < 2) emptyList()
        else allUsers.filter { u ->
            u.nombre.contains(q, ignoreCase = true) ||
                    u.correo.contains(q, ignoreCase = true)
        }.take(5)
    }

    // Ofertas filtradas por categoría
    val offers = remember(selectedCategory, allProducts) {
        if (selectedCategory == null) allProducts
        else allProducts.filter { it.categoria == selectedCategory }
    }

    Column(
        modifier = Modifier
            .padding(padding)
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Título
        Text(
            "Explorar",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(12.dp))

        // --- Búsqueda de personas (en la misma pantalla)
        OutlinedTextField(
            value = personQuery,
            onValueChange = { personQuery = it },
            singleLine = true,
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            placeholder = { Text("Buscar personas por nombre o correo…") },
            modifier = Modifier.fillMaxWidth()
        )

        // Sugerencias (si hay query)
        if (userResults.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Surface(
                tonalElevation = 1.dp,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(vertical = 6.dp)) {
                    userResults.forEach { u ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenProfile(u.id) }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            if (!u.photoUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = u.photoUrl,
                                    contentDescription = null,
                                    modifier = Modifier.size(36.dp).clip(CircleShape)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(u.nombre.take(1).uppercase())
                                }
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(u.nombre, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    u.correo,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // --- Filtros de categorías
        Text("Categorías", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { selectedCategory = null },
                    label = { Text("Todas") }
                )
            }
            items(Category.entries.size) { idx ->
                val cat = Category.entries[idx]
                FilterChip(
                    selected = selectedCategory == cat,
                    onClick = { selectedCategory = cat },
                    label = { Text(cat.name) }
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // --- Ofertas disponibles (debajo del buscador, como pediste)
        Text("Ofertas disponibles", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        if (offers.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No hay ofertas para esta categoría")
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(offers, key = { it.id }) { p ->
                    OfferCard(
                        product = p,
                        onClick = { onOpenProduct(p.id, p.ownerId) }
                    )
                }
                item { Spacer(Modifier.height(8.dp)) }
            }
        }
    }
}

@Composable
private fun OfferCard(
    product: Product,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp)
        ) {
            if (product.imageUrl.isNotBlank()) {
                AsyncImage(
                    model = product.imageUrl,
                    contentDescription = product.titulo,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
            } else {
                Surface(
                    tonalElevation = 1.dp,
                    modifier = Modifier.size(72.dp).clip(RoundedCornerShape(10.dp))
                ) {}
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(product.titulo, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${product.categoria} • Talla ${product.talla}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    product.descripcion,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}