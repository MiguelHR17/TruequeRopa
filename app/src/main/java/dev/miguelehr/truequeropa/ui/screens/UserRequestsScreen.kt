package dev.miguelehr.truequeropa.ui.screens

import android.text.Selection
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import dev.miguelehr.truequeropa.model.FakeRepository.generateImageUrl
import dev.miguelehr.truequeropa.model.ProposalStatus
import dev.miguelehr.truequeropa.model.UserProfile
import dev.miguelehr.truequeropa.model.UserRequestDetails
import dev.miguelehr.truequeropa.ui.viewmodels.UserRequestsViewModel
import kotlinx.coroutines.launch


@Composable
fun UserRequestsScreen(
    userId: String,
    viewModel: UserRequestsViewModel = viewModel(),
    padding: PaddingValues = PaddingValues(0.dp),
    onUnreviewedCountChange: (Int) -> Unit = {},
    onNavigateToUserPosts: (String, String, String) -> Unit
) {
    LaunchedEffect(userId) {
        viewModel.fetchUserRequests(userId, 0)
    }

    val userRequests by viewModel.userRequests.collectAsState()
    var expandedIds by remember { mutableStateOf(setOf<Int>()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(userRequests) {
       onUnreviewedCountChange(userRequests.count())
    }
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
                "Propuesta de Intercambio",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 6.dp)
            )
        }

        itemsIndexed(userRequests, key = { index, _ -> index }) { index, details ->
            val isExpanded = index in expandedIds
                UserRequestItem(
                    details = details,
                    isExpanded = isExpanded,
                    onToggle = {
                        expandedIds = if (isExpanded) {
                            expandedIds - index
                        } else {
                            expandedIds + index
                        }
                    },

                    onAccept = {

                        scope.launch {

                            viewModel.acceptRequest(
                                details.request.id,
                                details.propietarioProfile.uid
                            )
                            viewModel.fetchUserRequests(userId, 0)
                        }
                        // Colapsa la tarjeta después de la acción
                        expandedIds = expandedIds - index
                    },
                    onReject = {

                            scope.launch {
                                viewModel.updPost(details.propietarioPost.id, "0")
                                viewModel.updPost(details.solicitantePost.id, "0")
                                viewModel.rejectRequest(details.request.id, details.propietarioProfile.uid)
                                viewModel.fetchUserRequests(userId, 0)
                            }



                        // Colapsa la tarjeta después de la acción
                        expandedIds = expandedIds - index
                    },
                    onNavigateToUserPosts = {
                        onNavigateToUserPosts(details.solicitanteProfile.uid,details.solicitantePost.id.toString(),details.request.id.toString())
                    }
                )
        }

    }
}
@Composable
fun UserRequestItem(
    details: UserRequestDetails,
    isExpanded: Boolean,
    onToggle: () -> Unit ,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onNavigateToUserPosts: () -> Unit
) {
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = when (details.request.estado) {
                "0" -> Color(0xFFC5E1A5)    // verde suave
                "1" -> Color(0xFFD4EDDA)    // verde suave
                "2" -> Color(0xFFF8D7DA)   // rojo suave
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize() // ⬅️ expansión sin scroll interno
    ) {
        Column(modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)) {

            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            )

            {
                Text(
                    "${details.solicitanteProfile.nombre} quiere intercambiar contigo",
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )

                // Punto rojo si “Aún no revisado” y la card está colapsada
                if (!details.request.reviewed && !isExpanded) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(Color(0xFFD32F2F), CircleShape)
                    )
                    Spacer(Modifier.width(8.dp))
                }

                IconButton(
                    onClick = onToggle
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null
                    )
                }
            }

            AnimatedVisibility(isExpanded) {
                Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Text("Tu prenda:", fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(4.dp))
                    Image(
                        painter = rememberAsyncImagePainter(generateImageUrl(details.propietarioPost.categoria,2)),
                        contentDescription = details.propietarioPost.descripcion,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.LightGray, RoundedCornerShape(12.dp))
                    )
                    Text(details.propietarioPost.titulo)

                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically, // Alinea el texto y el botón verticalmente
                        horizontalArrangement = Arrangement.SpaceBetween // Empuja el texto a la izquierda y el botón a la derecha
                    ) {
                        Text(
                            text = "Prenda de ${details.solicitanteProfile.nombre}:",
                            fontWeight = FontWeight.Medium
                        )

                        if (details.request.estado == "0") {
                            FilledTonalButton(onClick = onNavigateToUserPosts) {
                                Text("Publicaciones")
                            }
                        }

                    }

                    Spacer(Modifier.height(4.dp))
                    Image(
                        painter = rememberAsyncImagePainter(generateImageUrl(details.solicitantePost.categoria,1)),
                        contentDescription = details.solicitantePost.descripcion,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.LightGray, RoundedCornerShape(12.dp))
                    )
                    Text(details.solicitantePost.titulo)

                    Spacer(Modifier.height(16.dp))

                    // Acciones
                    if (details.request.estado == "0") {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = onAccept,
                                modifier = Modifier.weight(1f)
                            ) { Text("Aceptar") }

                            OutlinedButton(
                                onClick = onReject,
                                modifier = Modifier.weight(1f)
                            ) { Text("Rechazar") }
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (details.request.estado == "1") "✅ Propuesta aceptada"
                        else if(details.request.estado == "2") "❌ Propuesta rechazada"
                        else "! Propuesta pendiente",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

        }
    }
}
