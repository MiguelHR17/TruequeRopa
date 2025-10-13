package dev.miguelehr.truequeropa.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.miguelehr.truequeropa.model.*

@Composable
fun ProposalsInboxScreen(padding: PaddingValues) {
    val myId = FakeRepository.currentUser.id
    val proposals = remember {
        mutableStateListOf<TradeProposal>().also {
            it.addAll(FakeRepository.proposals.filter { p -> p.toUserId == myId && p.status == ProposalStatus.PENDIENTE })
        }
    }

    Column(Modifier.padding(padding).padding(12.dp)) {
        Text("Propuestas recibidas", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        if (proposals.isEmpty()) {
            Text("No hay propuestas pendientes")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(proposals) { p ->
                    val offered = FakeRepository.products.find { it.id == p.offeredProductId }
                    val requested = FakeRepository.products.find { it.id == p.requestedProductId }

                    Card {
                        Column(Modifier.padding(12.dp)) {
                            Text("Te ofrecen: ${offered?.titulo} por tu ${requested?.titulo}")
                            Spacer(Modifier.height(8.dp))
                            Row {
                                Button(onClick = {
                                    // Aceptar
                                    FakeRepository.trades += TradeHistoryItem(
                                        id = "t${FakeRepository.trades.size + 1}",
                                        prendaOfrecidaId = p.offeredProductId,
                                        prendaRecibidaId = p.requestedProductId,
                                        fecha = "2025-10-10"
                                    )
                                    proposals.remove(p)
                                }) {
                                    Text("Aceptar")
                                }
                                Spacer(Modifier.width(8.dp))
                                OutlinedButton(onClick = { proposals.remove(p) }) {
                                    Text("Rechazar")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}