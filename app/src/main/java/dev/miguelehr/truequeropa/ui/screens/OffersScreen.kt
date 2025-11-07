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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import dev.miguelehr.truequeropa.R
import dev.miguelehr.truequeropa.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OffersScreen(
    onOpenProduct: (productId: String, ownerUserId: String) -> Unit,
    onOpenUserSearch: () -> Unit = {}, // ya no lo usamos; lo dejamos por compatibilidad
    padding: PaddingValues
) {
    // --- estado de búsqueda & filtros ---
    var query by remember { mutableStateOf("") }
    var catExpanded by remember { mutableStateOf(false) }
    var sizeExpanded by remember { mutableStateOf(false) }
    var colorExpanded by remember { mutableStateOf(false) } // visual; el modelo no tiene color
    var catFilter: Category? by remember { mutableStateOf(null) }
    var sizeFilter: Size? by remember { mutableStateOf(null) }
    var colorFilter: String? by remember { mutableStateOf(null) }

    val allProducts = remember { FakeRepository.products }
    val allUsers = remember { FakeRepository.users }

    // --- búsqueda en vivo: productos + personas ---
    val results = remember(query, catFilter, sizeFilter, colorFilter, allProducts) {
        val q = query.trim()
        val usersMatchedIds: Set<String> =
            if (q.isBlank()) emptySet() else allUsers
                .filter {
                    it.nombre.contains(q, ignoreCase = true) ||
                            it.correo.contains(q, ignoreCase = true)
                }
                .map { it.id }
                .toSet()

        allProducts
            .asSequence()
            // match por texto del producto
            .filter { p ->
                if (q.isBlank()) true
                else p.titulo.contains(q, ignoreCase = true) ||
                        p.descripcion.contains(q, ignoreCase = true) ||
                        p.ownerId in usersMatchedIds   // match por persona
            }
            // filtros
            .filter { p -> catFilter?.let { p.categoria == it } ?: true }
            .filter { p -> sizeFilter?.let { p.talla == it } ?: true }
            // colorFilter solo visual: si quisieras, podrías mapear a una etiqueta en la descripción
            .toList()
    }

    Column(
        modifier = Modifier
            .padding(padding)
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text("Ofertas recientes", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))

        // --- buscador único: productos o personas ---
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
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        // --- filtros (categoría / talla / color visual) ---
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            // Categoría
            ExposedDropdownMenuBox(
                expanded = catExpanded,
                onExpandedChange = { catExpanded = !catExpanded },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = catFilter?.name ?: "Categoría",
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
                    DropdownMenuItem(text = { Text("Todas") }, onClick = {
                        catFilter = null; catExpanded = false
                    })
                    Category.entries.forEach { c ->
                        DropdownMenuItem(text = { Text(c.name) }, onClick = {
                            catFilter = c; catExpanded = false
                        })
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
                    value = sizeFilter?.name ?: "Talla",
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
                    DropdownMenuItem(text = { Text("Todas") }, onClick = {
                        sizeFilter = null; sizeExpanded = false
                    })
                    Size.entries.forEach { s ->
                        DropdownMenuItem(text = { Text(s.name) }, onClick = {
                            sizeFilter = s; sizeExpanded = false
                        })
                    }
                }
            }

            // Color (opcional/visual)
            ExposedDropdownMenuBox(
                expanded = colorExpanded,
                onExpandedChange = { colorExpanded = !colorExpanded },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = colorFilter ?: "Color",
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = colorExpanded,
                    onDismissRequest = { colorExpanded = false }
                ) {
                    listOf("Todos", "Rojo", "Azul", "Negro", "Blanco").forEach { c ->
                        DropdownMenuItem(text = { Text(c) }, onClick = {
                            colorFilter = if (c == "Todos") null else c
                            colorExpanded = false
                        })
                    }
                }
            }
        }

        // limpiar filtros
        TextButton(
            onClick = { catFilter = null; sizeFilter = null; colorFilter = null },
            modifier = Modifier.padding(top = 8.dp)
        ) { Text("Limpiar filtros") }

        Spacer(Modifier.height(8.dp))

        // --- resultados ---
        if (results.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No hay resultados")
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(results, key = { it.id }) { p ->
                    OfferRowCard(
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
private fun OfferRowCard(
    product: Product,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = rememberAsyncImagePainter(product.imageUrl),
                contentDescription = product.titulo,
                modifier = Modifier.size(72.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(product.titulo, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${product.categoria.name} • Talla ${product.talla.name}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    product.descripcion,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                AssistChip(onClick = {}, label = { Text(product.estado.name) })
            }
        }
    }
}