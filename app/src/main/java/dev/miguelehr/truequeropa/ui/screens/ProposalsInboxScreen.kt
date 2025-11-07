package dev.miguelehr.truequeropa.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.miguelehr.truequeropa.model.FakeRepository
import dev.miguelehr.truequeropa.model.TradeHistoryItem
import dev.miguelehr.truequeropa.model.TradeProposal
import dev.miguelehr.truequeropa.model.ProposalStatus
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import kotlinx.coroutines.launch

@Composable
fun ProposalsInboxScreen(padding: PaddingValues) {
    val myId = FakeRepository.currentUser.id

    // Trae solo propuestas que debo decidir (dirigidas a mí y pendientes si existe el estado)
    val initial = remember {
        FakeRepository.proposals.filter { p ->
            p.toUserId == myId && (runCatching { p.status == ProposalStatus.PENDIENTE }.getOrDefault(true))
        }
    }
    val proposals = remember { mutableStateListOf<TradeProposal>().also { it.addAll(initial) } }

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .padding(padding)
            .fillMaxSize()
    ) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Text("Propuestas", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))

            if (proposals.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No tienes propuestas por revisar")
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(proposals, key = { it.id }) { p ->
                        ProposalCard(
                            proposal = p,
                            onAccept = {
                                // ✅ Aceptar (mock): movemos a historial y sacamos de la lista
                                FakeRepository.trades += TradeHistoryItem(
                                    id = "t${FakeRepository.trades.size + 1}",
                                    prendaOfrecidaId = p.offeredProductId,
                                    prendaRecibidaId = p.requestedProductId,
                                    fecha = "2025-11-06"
                                )
                                proposals.remove(p)
                                scope.launch { snackbar.showSnackbar("Propuesta aceptada") }
                            },
                            onReject = {
                                proposals.remove(p)
                                scope.launch { snackbar.showSnackbar("Propuesta rechazada") }
                            }
                        )
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                }
            }
        }

        // Host para mensajes
        SnackbarHost(snackbar, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun ProposalCard(
    proposal: TradeProposal,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    val offered = remember(proposal.offeredProductId) {
        FakeRepository.products.find { it.id == proposal.offeredProductId }
    }
    val requested = remember(proposal.requestedProductId) {
        FakeRepository.products.find { it.id == proposal.requestedProductId }
    }
    val fromUser = remember(proposal.fromUserId) {
        FakeRepository.users.find { it.id == proposal.fromUserId }
    }

    var expanded by remember { mutableStateOf(false) }

    ElevatedCard(
        onClick = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp)) {
            // Resumen compacto
            Text(
                text = "De: ${fromUser?.nombre ?: proposal.fromUserId}",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "Te ofrecen: ${offered?.titulo ?: proposal.offeredProductId}",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Por tu: ${requested?.titulo ?: proposal.requestedProductId}",
                style = MaterialTheme.typography.bodyMedium
            )

            if (expanded) {
                Spacer(Modifier.height(10.dp))
                // Detalle básico (puedes enriquecer con fotos y specs)
                Text(
                    text = "Detalle del trueque:",
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "• Ofrecen: ${offered?.categoria} • ${offered?.talla} • ${offered?.estado}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "• Tu prenda: ${requested?.categoria} • ${requested?.talla} • ${requested?.estado}",
                    style = MaterialTheme.typography.bodySmall
                )

                Spacer(Modifier.height(12.dp))
                Row {
                    Button(onClick = onAccept) { Text("Aceptar") }
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(onClick = onReject) { Text("Rechazar") }
                }
            } else {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Toca para ver detalles",
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}