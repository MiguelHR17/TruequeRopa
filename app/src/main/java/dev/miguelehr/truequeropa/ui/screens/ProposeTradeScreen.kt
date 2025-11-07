package dev.miguelehr.truequeropa.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import dev.miguelehr.truequeropa.model.FakeRepository
import dev.miguelehr.truequeropa.model.Product

/**
 * Pantalla para proponer un trueque por la prenda [productId].
 * El usuario puede seleccionar de 1 a 5 prendas propias para ofrecer.
 */
@Composable
fun ProposeTradeScreen(
    productId: String,
    onSent: () -> Unit
) {
    val target: Product? = remember(productId) {
        FakeRepository.products.find { it.id == productId }
    }
    val myId = FakeRepository.currentUser.id
    val myProducts = remember {
        FakeRepository.products.filter { it.ownerId == myId }
    }

    // selección múltiple (máx 5)
    val selected = remember { mutableStateListOf<String>() }
    val limit = 5

    // Si el usuario no tiene prendas, no tiene sentido esta pantalla
    if (target == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Publicación no encontrada")
        }
        return
    }
    if (myProducts.isEmpty()) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Aún no tienes prendas publicadas para ofrecer.")
            Spacer(Modifier.height(12.dp))
            Text(
                "Publica una prenda desde la pestaña “Publicar” y vuelve a intentarlo.",
                style = MaterialTheme.typography.bodySmall
            )
        }
        return
    }

    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Text("Propuesta de trueque", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))

        // Resumen de la prenda objetivo
        TargetCard(target)

        Spacer(Modifier.height(16.dp))
        Text(
            "Selecciona qué ofrecerás (máx. $limit):",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            "${selected.size} seleccionada(s)",
            style = MaterialTheme.typography.bodySmall,
            color = if (selected.size in 1..limit) MaterialTheme.colorScheme.onSurfaceVariant
            else MaterialTheme.colorScheme.error
        )

        Spacer(Modifier.height(8.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(myProducts, key = { it.id }) { p ->
                SelectableProductRow(
                    product = p,
                    checked = selected.contains(p.id),
                    onToggle = {
                        if (selected.contains(p.id)) {
                            selected.remove(p.id)
                        } else if (selected.size < limit) {
                            selected.add(p.id)
                        }
                    }
                )
            }
            item { Spacer(Modifier.height(6.dp)) }
        }

        Spacer(Modifier.height(10.dp))

        Button(
            onClick = {
                // Envía la propuesta usando el helper del repositorio
                FakeRepository.sendProposal(
                    fromUserId = myId,
                    toUserId = target.ownerId,
                    offeredProductIds = selected.toList(),
                    requestedProductId = target.id
                )
                onSent()
            },
            enabled = selected.size in 1..limit,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Enviar propuesta")
        }
    }
}

@Composable
private fun TargetCard(p: Product) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp)) {
            Text("Prenda objetivo", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (p.imageUrl.isNotBlank()) {
                    AsyncImage(
                        model = p.imageUrl,
                        contentDescription = p.titulo,
                        modifier = Modifier.size(72.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(p.titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("${p.categoria} • ${p.talla} • ${p.estado}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun SelectableProductRow(
    product: Product,
    checked: Boolean,
    onToggle: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        onClick = onToggle,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(12.dp)
        ) {
            Checkbox(checked = checked, onCheckedChange = { onToggle() })
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(product.titulo, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${product.categoria} • ${product.talla} • ${product.estado}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}