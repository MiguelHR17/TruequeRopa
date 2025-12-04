package dev.miguelehr.truequeropa.ui.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import dev.miguelehr.truequeropa.model.UserRequestDetails
import dev.miguelehr.truequeropa.ui.viewmodels.UserRequestsViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun TradeHistoryScreen(
    userId: String,
    viewModel: UserRequestsViewModel = viewModel(),
    padding: PaddingValues = PaddingValues(0.dp),
    onUnreviewedCountChange: (Int) -> Unit = {},
) {
    LaunchedEffect(userId) {
        viewModel.fetchUserRequests(userId, 1)
    }

    val userRequests by viewModel.userRequests.collectAsState()
    var selectedDetails by remember { mutableStateOf<UserRequestDetails?>(null) }

    // En historial no queremos globito rojo: lo ponemos en 0
    LaunchedEffect(userRequests) {
        onUnreviewedCountChange(0)
    }

    val bottomPad = padding.calculateBottomPadding() + 96.dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                start = padding.calculateStartPadding(LayoutDirection.Ltr),
                end = padding.calculateEndPadding(LayoutDirection.Ltr),
                top = padding.calculateTopPadding(),
                bottom = bottomPad
            )
    ) {
        if (userRequests.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Aún no tienes historial de trueques.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text(
                        "Historial de propuestas",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                }

                items(userRequests) { details ->
                    HistoryUserRequestItem(
                        currentUserId = userId,
                        details = details,
                        onClick = { selectedDetails = details }
                    )
                }
            }
        }

        val dialogDetails = selectedDetails
        if (dialogDetails != null) {
            HistoryDetailsDialog(
                currentUserId = userId,
                details = dialogDetails,
                onDismiss = { selectedDetails = null }
            )
        }
    }
}

@SuppressLint("NewApi")
@Composable
fun HistoryUserRequestItem(
    currentUserId: String,
    details: UserRequestDetails,
    onClick: () -> Unit
) {
    val isUserPropietario = details.propietarioProfile.uid == currentUserId
    val otherName = if (isUserPropietario) {
        details.solicitanteProfile.nombre.ifBlank { details.solicitanteProfile.email }
    } else {
        details.propietarioProfile.nombre.ifBlank { details.propietarioProfile.email }
    }

    val myPost = if (isUserPropietario) details.propietarioPost else details.solicitantePost
    val otherPost = if (isUserPropietario) details.solicitantePost else details.propietarioPost

    val (statusText, statusColor) = when (details.request.estado) {
        "1" -> "Aceptado" to Color(0xFF388E3C)
        "2" -> "Rechazado" to Color(0xFFD32F2F)
        else -> "Pendiente" to Color(0xFFF9A825)
    }

    val formatter = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm") }
    val timestamp = details.request.fechaAprobacion ?: details.request.createdAt
    val dateTimeText = timestamp?.let { ts ->
        val instant = Instant.ofEpochSecond(ts.seconds, ts.nanoseconds.toLong())
        instant.atZone(ZoneId.systemDefault()).format(formatter)
    } ?: "Fecha no disponible"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = dateTimeText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.size(4.dp))

            Text(
                text = "Intercambio con $otherName",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.size(4.dp))

            Text(
                text = "${myPost.titulo}  ↔  ${otherPost.titulo}",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2
            )

            Spacer(modifier = Modifier.size(8.dp))

            Text(
                text = statusText,
                color = statusColor,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun HistoryDetailsDialog(
    currentUserId: String,
    details: UserRequestDetails,
    onDismiss: () -> Unit
) {
    val isUserPropietario = details.propietarioProfile.uid == currentUserId
    val myProfile = if (isUserPropietario) details.propietarioProfile else details.solicitanteProfile
    val otherProfile = if (isUserPropietario) details.solicitanteProfile else details.propietarioProfile
    val myPost = if (isUserPropietario) details.propietarioPost else details.solicitantePost
    val otherPost = if (isUserPropietario) details.solicitantePost else details.propietarioPost

    val (statusText, statusColor) = when (details.request.estado) {
        "1" -> "Aceptado" to Color(0xFF388E3C)
        "2" -> "Rechazado" to Color(0xFFD32F2F)
        else -> "Pendiente" to Color(0xFFF9A825)
    }

    val formatter = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm") }
    val timestamp = details.request.fechaAprobacion ?: details.request.createdAt
    val dateTimeText = timestamp?.let { ts ->
        val instant = Instant.ofEpochSecond(ts.seconds, ts.nanoseconds.toLong())
        instant.atZone(ZoneId.systemDefault()).format(formatter)
    } ?: "Fecha no disponible"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Detalle del trueque")
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Cerrar"
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .padding(top = 4.dp)
            ) {
                Text(
                    text = statusText,
                    color = statusColor,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = dateTimeText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.size(12.dp))

                // ===== Tu prenda =====
                Text("Tu prenda:", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.size(4.dp))

                if (myPost.imageUrls.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(myPost.imageUrls) { url ->
                            Card(
                                modifier = Modifier.size(160.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Image(
                                    painter = rememberAsyncImagePainter(url),
                                    contentDescription = myPost.titulo,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(MaterialTheme.shapes.medium),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.size(4.dp))
                Text(myPost.titulo, style = MaterialTheme.typography.bodyMedium)

                Spacer(modifier = Modifier.size(16.dp))

                // ===== Prenda del otro =====
                Text(
                    "Prenda de ${otherProfile.nombre.ifBlank { otherProfile.email }}:",
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.size(4.dp))

                if (otherPost.imageUrls.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(otherPost.imageUrls) { url ->
                            Card(
                                modifier = Modifier.size(160.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Image(
                                    painter = rememberAsyncImagePainter(url),
                                    contentDescription = otherPost.titulo,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(MaterialTheme.shapes.medium),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.size(4.dp))
                Text(otherPost.titulo, style = MaterialTheme.typography.bodyMedium)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )
}