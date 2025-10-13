package dev.miguelehr.truequeropa.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.miguelehr.truequeropa.model.FakeRepository

@Composable
fun TradeHistoryScreen(padding: PaddingValues) {
    val items = remember { FakeRepository.trades.toList() }

    Column(Modifier.padding(padding).padding(12.dp)) {
        Text("Historial de trueques", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        if (items.isEmpty()) {
            Text("Aún no has realizado trueques")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(items) { t ->
                    val of = FakeRepository.products.find { it.id == t.prendaOfrecidaId }
                    val rec = FakeRepository.products.find { it.id == t.prendaRecibidaId }
                    ListItem(
                        headlineContent = { Text("${of?.titulo} ⇄ ${rec?.titulo}") },
                        supportingContent = { Text("Fecha: ${t.fecha}") }
                    )
                    Divider()
                }
            }
        }
    }
}