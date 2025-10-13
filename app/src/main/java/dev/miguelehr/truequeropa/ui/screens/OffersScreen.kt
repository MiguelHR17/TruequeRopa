package dev.miguelehr.truequeropa.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import dev.miguelehr.truequeropa.model.Category
import dev.miguelehr.truequeropa.model.FakeRepository
import dev.miguelehr.truequeropa.model.Product

@Composable
fun OffersScreen(onOpenProduct: (String) -> Unit, padding: PaddingValues) {
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    val all = FakeRepository.products
    val filtered = all.filter { p -> selectedCategory?.let { it == p.categoria.name } ?: true }

    Column(Modifier.padding(padding).padding(12.dp)) {
        Text("Ofertas recientes", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        CategoryChips(selectedCategory) { selectedCategory = it }
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(filtered) { product ->
                ProductCard(product) { onOpenProduct(product.id) }
            }
        }
    }
}

@Composable
private fun CategoryChips(selected: String?, onSelect: (String?) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Category.entries.forEach { c ->
            FilterChip(
                selected = selected == c.name,
                onClick = { onSelect(if (selected == c.name) null else c.name) },
                label = { Text(c.name) }
            )
        }
    }
}

@Composable
private fun ProductCard(product: Product, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable { onClick() }) {
        Row(Modifier.padding(12.dp)) {
            Image(
                painter = rememberAsyncImagePainter(product.imageUrl),
                contentDescription = product.titulo,
                modifier = Modifier.size(96.dp),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.fillMaxWidth()) {
                Text(product.titulo, style = MaterialTheme.typography.titleMedium)
                Text("${product.categoria} • Talla ${product.talla}")
                Text(product.descripcion, maxLines = 2)
            }
        }
    }
}