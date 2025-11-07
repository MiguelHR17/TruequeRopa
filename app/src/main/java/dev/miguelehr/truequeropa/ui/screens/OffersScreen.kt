package dev.miguelehr.truequeropa.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import dev.miguelehr.truequeropa.model.Category
import dev.miguelehr.truequeropa.model.FakeRepository
import dev.miguelehr.truequeropa.model.Product
import dev.miguelehr.truequeropa.model.Size

@Composable
fun OffersScreen(onOpenProduct: (String) -> Unit, padding: PaddingValues) {
    var searchText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var selectedSize by remember { mutableStateOf<Size?>(null) }
    var selectedColor by remember { mutableStateOf<String?>(null) }

    var showCategoryMenu by remember { mutableStateOf(false) }
    var showSizeMenu by remember { mutableStateOf(false) }
    var showColorMenu by remember { mutableStateOf(false) }

    val allProducts = FakeRepository.products

    // Obtener colores únicos de los productos
    val availableColors = remember(allProducts) {
        allProducts.mapNotNull { it.color }.distinct().sorted()
    }

    // Filtrar productos por búsqueda y filtros
    val filteredProducts = allProducts.filter { product ->
        val matchesSearch = searchText.isEmpty() ||
                product.titulo.contains(searchText, ignoreCase = true) ||
                product.descripcion.contains(searchText, ignoreCase = true)

        val matchesCategory = selectedCategory == null || product.categoria == selectedCategory
        val matchesSize = selectedSize == null || product.talla == selectedSize
        val matchesColor = selectedColor == null || product.color == selectedColor

        matchesSearch && matchesCategory && matchesSize && matchesColor
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
    ) {
        // Encabezado
        Text(
            text = "Ofertas recientes",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )

        // Buscador
        OutlinedTextField(
            value = searchText,
            onValueChange = { searchText = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("Buscar productos...") },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Buscar")
            },
            trailingIcon = {
                if (searchText.isNotEmpty()) {
                    IconButton(onClick = { searchText = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        // Filtros con dropdowns
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Filtro de Categoría
            Box(modifier = Modifier.weight(1f)) {
                OutlinedButton(
                    onClick = { showCategoryMenu = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = selectedCategory?.name ?: "Categoría",
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }

                DropdownMenu(
                    expanded = showCategoryMenu,
                    onDismissRequest = { showCategoryMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Todas") },
                        onClick = {
                            selectedCategory = null
                            showCategoryMenu = false
                        }
                    )
                    Category.entries.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category.name) },
                            onClick = {
                                selectedCategory = category
                                showCategoryMenu = false
                            }
                        )
                    }
                }
            }

            // Filtro de Talla
            Box(modifier = Modifier.weight(1f)) {
                OutlinedButton(
                    onClick = { showSizeMenu = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = selectedSize?.name ?: "Talla",
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }

                DropdownMenu(
                    expanded = showSizeMenu,
                    onDismissRequest = { showSizeMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Todas") },
                        onClick = {
                            selectedSize = null
                            showSizeMenu = false
                        }
                    )
                    Size.entries.forEach { size ->
                        DropdownMenuItem(
                            text = { Text(size.name) },
                            onClick = {
                                selectedSize = size
                                showSizeMenu = false
                            }
                        )
                    }
                }
            }

            // Filtro de Color
//            Box(modifier = Modifier.weight(1f)) {
//                OutlinedButton(
//                    onClick = { showColorMenu = true },
//                    modifier = Modifier.fillMaxWidth(),
//                    shape = RoundedCornerShape(8.dp),
//                    enabled = availableColors.isNotEmpty()
//                ) {
//                    Text(
//                        text = selectedColor ?: "Color",
//                        modifier = Modifier.weight(1f),
//                        maxLines = 1,
//                        overflow = TextOverflow.Ellipsis
//                    )
//                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
//                }
//
//                DropdownMenu(
//                    expanded = showColorMenu,
//                    onDismissRequest = { showColorMenu = false }
//                ) {
//                    DropdownMenuItem(
//                        text = { Text("Todos") },
//                        onClick = {
//                            selectedColor = null
//                            showColorMenu = false
//                        }
//                    )
//                    availableColors.forEach { color ->
//                        DropdownMenuItem(
//                            text = { Text(color) },
//                            onClick = {
//                                selectedColor = color
//                                showColorMenu = false
//                            }
//                        )
//                    }
//                }
//            }
        }

        // Indicador de filtros activos
        if (selectedCategory != null || selectedSize != null || selectedColor != null) {
            TextButton(
                onClick = {
                    selectedCategory = null
                    selectedSize = null
                    selectedColor = null
                },
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Text("Limpiar filtros")
            }
        }

        // Lista de productos
        if (filteredProducts.isEmpty()) {
            // Estado vacío
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No hay productos disponibles",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (searchText.isNotEmpty() || selectedCategory != null || selectedSize != null || selectedColor != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Intenta ajustar los filtros",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                items(filteredProducts) { product ->
                    EnhancedProductCard(
                        product = product,
                        onClick = { onOpenProduct(product.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EnhancedProductCard(
    product: Product,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp)
        ) {
            // Imagen del producto
            Image(
                painter = rememberAsyncImagePainter(product.imageUrl),
                contentDescription = product.titulo,
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Información del producto
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Título
                Text(
                    text = product.titulo,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Categoría, talla y color
                Text(
                    text = buildString {
                        append(product.categoria.name)
                        append(" • Talla ${product.talla.name}")
                        product.color?.let { append(" • $it") }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Descripción
                Text(
                    text = product.descripcion,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Estado
                Surface(
                    color = if (product.estado.name == "NUEVO")
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        text = product.estado.name,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = if (product.estado.name == "NUEVO")
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}