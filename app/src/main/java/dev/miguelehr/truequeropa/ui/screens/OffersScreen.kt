package dev.miguelehr.truequeropa.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
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

@Composable
fun OffersScreen(onOpenProduct: (String) -> Unit, padding: PaddingValues) {
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    val allProducts = FakeRepository.products

    // Filtrar por categoría seleccionada
    val filteredProducts = if (selectedCategory != null) {
        allProducts.filter { it.categoria == selectedCategory }
    } else {
        allProducts
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

        // Filtros de categoría
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            items(Category.entries.toList()) { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = {
                        selectedCategory = if (selectedCategory == category) null else category
                    },
                    label = {
                        Text(
                            text = category.name,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                )
            }
        }

        // Lista de productos
        if (filteredProducts.isEmpty()) {
            // Estado vacío
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Text(
                    text = "No hay productos disponibles",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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

                // Categoría y talla
                Text(
                    text = "${product.categoria.name} • Talla ${product.talla.name}",
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