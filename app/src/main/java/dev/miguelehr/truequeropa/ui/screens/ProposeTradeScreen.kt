package dev.miguelehr.truequeropa.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.miguelehr.truequeropa.model.FakeRepository
import dev.miguelehr.truequeropa.model.TradeProposal

@Composable
fun ProposeTradeScreen(productId: String, onSent: () -> Unit) {
    val target = remember(productId) { FakeRepository.products.find { it.id == productId } }
    val myProducts = remember { FakeRepository.products.filter { it.ownerId == FakeRepository.currentUser.id } }
    var selected by remember { mutableStateOf(myProducts.firstOrNull()?.id) }

    Column(Modifier.padding(20.dp)) {
        Text("Propuesta de trueque", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text("Prenda objetivo: ${target?.titulo ?: "Desconocida"}")
        Spacer(Modifier.height(12.dp))
        Text("Selecciona qué ofrecerás:")
        Spacer(Modifier.height(8.dp))
        myProducts.forEach {
            Row {
                RadioButton(selected = selected == it.id, onClick = { selected = it.id })
                Spacer(Modifier.width(8.dp))
                Text("${it.titulo} • ${it.categoria} • ${it.talla}")
            }
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                if (selected != null && target != null) {
                    FakeRepository.proposals += TradeProposal(
                        id = "pr${FakeRepository.proposals.size+1}",
                        fromUserId = FakeRepository.currentUser.id,
                        toUserId = target.ownerId,
                        offeredProductId = selected!!,
                        requestedProductId = target.id
                    )
                    onSent()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Enviar propuesta") }
    }
}