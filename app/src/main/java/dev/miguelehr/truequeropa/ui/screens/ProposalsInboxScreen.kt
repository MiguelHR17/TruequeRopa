package dev.miguelehr.truequeropa.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.animation.animateContentSize

private enum class ProposalStatus { PENDIENTE, ACEPTADA, RECHAZADA }

private data class UiProposal(
    val id: String,
    val userName: String,
    val myProductImage: String,
    val theirProductImage: String,
    val theirItemName: String,
    val myItemName: String,
    val status: ProposalStatus = ProposalStatus.PENDIENTE,
    val reviewed: Boolean = false // false => “Aún no revisado”
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProposalsInboxScreen(
    padding: PaddingValues,
    onUnreviewedCountChange: (Int) -> Unit = {}
) {
    var proposals by remember {
        mutableStateOf(
            listOf(
                UiProposal(
                    id = "p1",
                    userName = "Verónica",
                    myProductImage = "https://picsum.photos/seed/my1/600/800",
                    theirProductImage = "https://picsum.photos/seed/her1/600/800",
                    theirItemName = "Jean azul talla 28",
                    myItemName = "Polo blanco M",
                    reviewed = false
                ),
                UiProposal(
                    id = "p2",
                    userName = "Marco",
                    myProductImage = "https://picsum.photos/seed/my2/600/800",
                    theirProductImage = "https://picsum.photos/seed/his2/600/800",
                    theirItemName = "Casaca negra L",
                    myItemName = "Polo gris L",
                    reviewed = false
                ),
                UiProposal(
                    id = "p3",
                    userName = "Andrea",
                    myProductImage = "https://picsum.photos/seed/my3/600/800",
                    theirProductImage = "https://picsum.photos/seed/her3/600/800",
                    theirItemName = "Vestido rojo S",
                    myItemName = "Blusa blanca S",
                    reviewed = true
                ),
                UiProposal(
                    id = "p4",
                    userName = "Luis",
                    myProductImage = "https://picsum.photos/seed/my4/600/800",
                    theirProductImage = "https://picsum.photos/seed/his4/600/800",
                    theirItemName = "Zapatillas 41",
                    myItemName = "Pantalón beige 32",
                    reviewed = false
                )
            )
        )
    }

    // ids de tarjetas expandidas
    var expandedIds by remember { mutableStateOf(setOf<String>()) }

    // contador de “no revisados” para el menú inferior
    val unreviewed = proposals.count { !it.reviewed }
    LaunchedEffect(unreviewed) { onUnreviewedCountChange(unreviewed) }

    // padding inferior generoso para que la BottomBar no tape los botones
    val bottomPad = padding.calculateBottomPadding() + 96.dp

    LazyColumn(
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .padding(top = padding.calculateTopPadding())
            .fillMaxSize(),
        contentPadding = PaddingValues(bottom = bottomPad),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "Propuestas",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        items(proposals, key = { it.id }) { p ->
            val isExpanded = p.id in expandedIds

            ElevatedCard(
                colors = CardDefaults.elevatedCardColors(
                    containerColor = when (p.status) {
                        ProposalStatus.ACEPTADA -> Color(0xFFD4EDDA)    // verde suave
                        ProposalStatus.RECHAZADA -> Color(0xFFF8D7DA)   // rojo suave
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize() // ⬅️ expansión sin scroll interno
            ) {
                Column(Modifier.fillMaxWidth().padding(12.dp)) {

                    // ---- Cabecera (colapsada) ----
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${p.userName} quiere intercambiar contigo",
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )

                        // Punto rojo si “Aún no revisado” y la card está colapsada
                        if (!p.reviewed && !isExpanded) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(Color(0xFFD32F2F), CircleShape)
                            )
                            Spacer(Modifier.width(8.dp))
                        }

                        IconButton(
                            onClick = {
                                expandedIds = if (isExpanded) expandedIds - p.id else expandedIds + p.id
                            }
                        ) {
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null
                            )
                        }
                    }

                    // ---- Contenido expandido ----
                    AnimatedVisibility(isExpanded) {
                        Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                            Text("Tu prenda:", fontWeight = FontWeight.Medium)
                            Spacer(Modifier.height(4.dp))
                            Image(
                                painter = rememberAsyncImagePainter(p.myProductImage),
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .background(Color.LightGray, RoundedCornerShape(12.dp))
                            )
                            Text(p.myItemName)

                            Spacer(Modifier.height(12.dp))

                            Text("Prenda de ${p.userName}:", fontWeight = FontWeight.Medium)
                            Spacer(Modifier.height(4.dp))
                            Image(
                                painter = rememberAsyncImagePainter(p.theirProductImage),
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .background(Color.LightGray, RoundedCornerShape(12.dp))
                            )
                            Text(p.theirItemName)

                            Spacer(Modifier.height(16.dp))

                            // Acciones
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Button(
                                    onClick = {
                                        proposals = proposals.map {
                                            if (it.id == p.id)
                                                it.copy(status = ProposalStatus.ACEPTADA, reviewed = true)
                                            else it
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                ) { Text("Aceptar ✅") }

                                OutlinedButton(
                                    onClick = {
                                        proposals = proposals.map {
                                            if (it.id == p.id)
                                                it.copy(status = ProposalStatus.RECHAZADA, reviewed = true)
                                            else it
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                ) { Text("Rechazar ❌") }
                            }

                            Spacer(Modifier.height(10.dp))

                            // Toggle Revisado / Aún no revisado
                            val isReviewed = proposals.find { it.id == p.id }?.reviewed == true
                            FilledTonalButton(
                                onClick = {
                                    proposals = proposals.map {
                                        if (it.id == p.id) it.copy(reviewed = !it.reviewed)
                                        else it
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    if (isReviewed) "Marcar como AÚN NO REVISADO"
                                    else "Marcar como REVISADO"
                                )
                            }

                            // Mensaje de estado
                            if (p.status != ProposalStatus.PENDIENTE) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    if (p.status == ProposalStatus.ACEPTADA) "✅ Propuesta aceptada"
                                    else "❌ Propuesta rechazada",
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(8.dp)) }
    }
}