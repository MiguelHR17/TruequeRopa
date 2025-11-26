package dev.miguelehr.truequeropa.ui.screens

import android.R.attr.padding
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import dev.miguelehr.truequeropa.R
import dev.miguelehr.truequeropa.model.Category
import dev.miguelehr.truequeropa.model.Condition
import dev.miguelehr.truequeropa.model.FakeRepository
import dev.miguelehr.truequeropa.model.FakeRepository.generateImageUrl
import dev.miguelehr.truequeropa.model.Product
import dev.miguelehr.truequeropa.model.ProposalStatus
import dev.miguelehr.truequeropa.model.Size
import dev.miguelehr.truequeropa.model.TradeProposal
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
    viewModel.fetchUserRequests(userId,1)
    val userRequests by viewModel.userRequests.collectAsState()
    var expandedIds by remember { mutableStateOf(setOf<Int>()) }

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
            Text("Historial de propuestas", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
        }

        itemsIndexed(userRequests, key = { index, _ -> index }) { index, details ->
            val isExpanded = index in expandedIds
            HistoryUserRequestItem(
                details = details,
                isExpanded = isExpanded
            )
        }

    }
}

@Composable
fun HistoryUserRequestItem(details: UserRequestDetails, isExpanded: Boolean) {

    val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.padding(8.dp)
            ) {
                Column {
                    Image(
                        painter = rememberAsyncImagePainter(generateImageUrl("USUARIO",1)),
                        contentDescription = "Veronica",
                        modifier = Modifier.size(45.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(details.propietarioProfile.nombre, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(16 .dp))
                Column {
                    val formattedDate = details.request.fechaAprobacion?.let { timestamp ->
                        // Convierte el Timestamp de Firestore a un objeto de fecha/hora local
                        val instant = Instant.ofEpochSecond(timestamp.seconds, timestamp.nanoseconds.toLong())
                        // Formatea esa fecha usando el formateador que creamos
                        instant.atZone(ZoneId.systemDefault()).toLocalDate().format(dateFormatter)
                    } ?: "Fecha no disponible" // Texto por si la fecha es nula
                    Text( "$formattedDate Intercambio con ${details.solicitanteProfile.nombre} ", fontWeight = FontWeight.Bold)
                    Text("${details.propietarioPost.descripcion} con ${details.solicitantePost.descripcion}")
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }

