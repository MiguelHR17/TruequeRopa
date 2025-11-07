package dev.miguelehr.truequeropa.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import dev.miguelehr.truequeropa.model.*

@Composable
fun ProposalsInboxScreen(padding: PaddingValues) {
    val myId = FakeRepository.currentUser.id

    // Solo las que me enviaron a mí y están pendientes
    val proposals = remember {
        mutableStateListOf<TradeProposal>().also { dst ->
            dst += FakeRepository.proposals.filter { it.toUserId == myId && it.status == ProposalStatus.PENDIENTE }
        }
    }

    // Efecto para refrescar si la fuente cambia (mock simple)
    LaunchedEffect(FakeRepository.proposals.size) {
        proposals.clear()
        proposals += FakeRepository.proposals.filter { it.toUserId == myId && it.status == ProposalStatus.PENDIENTE }
    }

    Column(
        Modifier
            .padding(padding)
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Propuestas de trueque", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))

        if (proposals.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No tienes propuestas pendientes")
            }
            return
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(proposals, key = { it.id }) { p ->
                ProposalCard(
                    proposal = p,
                    onAccept = {
                        FakeRepository.acceptProposal(p)
                        proposals.remove(p)
                    },
                    onReject = {
                        FakeRepository.rejectProposal(p)
                        proposals.remove(p)
                    }
                )
            }
        }
    }
}

@Composable
private fun ProposalCard(
    proposal: TradeProposal,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val requested = remember(proposal.requestedProductId) {
        FakeRepository.products.find { it.id == proposal.requestedProductId }
    }
    val fromUser = remember(proposal.fromUserId) {
        FakeRepository.users.find { it.id == proposal.fromUserId }
    }
    val offeredList = remember(proposal.offeredProductIds) {
        proposal.offeredProductIds.mapNotNull { id -> FakeRepository.products.find { it.id == id } }
    }

    ElevatedCard(
        onClick = { expanded = !expanded },
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        Column(Modifier.padding(12.dp)) {

            // Resumen compacto
            Text(
                text = "De: ${fromUser?.nombre ?: proposal.fromUserId}",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Te ofrecen: ${offeredList.joinToString(", ") { it.titulo }}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Por tu: ${requested?.titulo ?: proposal.requestedProductId}",
                style = MaterialTheme.typography.bodyMedium
            )

            if (expanded) {
                Spacer(Modifier.height(12.dp))
                Divider()
                Spacer(Modifier.height(12.dp))

                // Detalle gráfico simple
                Text("Detalle de lo ofrecido", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                offeredList.forEach { prod ->
                    OfferedRow(prod)
                    Spacer(Modifier.height(8.dp))
                }

                Spacer(Modifier.height(10.dp))
                Text("Tu prenda objetivo", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                requested?.let { OfferedRow(it) }

                Spacer(Modifier.height(14.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = onAccept,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Aceptar")
                    }
                    OutlinedButton(
                        onClick = onReject,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Rechazar")
                    }
                }
            } else {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Toca para ver detalle",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun OfferedRow(p: Product) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (p.imageUrl.isNotBlank()) {
            AsyncImage(
                model = p.imageUrl,
                contentDescription = p.titulo,
                modifier = Modifier.size(60.dp)
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(p.titulo, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text("${p.categoria} • ${p.talla} • ${p.estado}", style = MaterialTheme.typography.bodySmall)
        }
    }
}