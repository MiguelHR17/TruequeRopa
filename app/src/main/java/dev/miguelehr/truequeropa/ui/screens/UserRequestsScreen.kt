package dev.miguelehr.truequeropa.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.miguelehr.truequeropa.model.UserRequestDetails
import dev.miguelehr.truequeropa.ui.viewmodels.UserRequestsViewModel

@Composable
fun UserRequestsScreen(
    userId: String,
    viewModel: UserRequestsViewModel = viewModel(),
    padding: PaddingValues = PaddingValues(0.dp),
    onUnreviewedCountChange: (Int) -> Unit = {}
) {
    viewModel.fetchUserRequests(userId)
    val userRequests by viewModel.userRequests.collectAsState()

    LaunchedEffect(userRequests) {
       onUnreviewedCountChange(userRequests.count())
    }

    LazyColumn(
        modifier = Modifier.padding(padding),
        contentPadding = PaddingValues(16.dp)
    ) {
        items(userRequests) {
            UserRequestItem(it)
        }
    }
}

@Composable
fun UserRequestItem(details: UserRequestDetails) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Solicitud de: ${details.solicitanteProfile.nombre}", style = MaterialTheme.typography.titleMedium)
            Text(text = "Para: ${details.propietarioProfile.nombre}", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Estado: ${details.request.estado}")
            Spacer(modifier = Modifier.height(16.dp))
            Row {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Ofrece:", style = MaterialTheme.typography.titleSmall)
                    Text(details.solicitantePost.titulo)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Pide:", style = MaterialTheme.typography.titleSmall)
                    Text(details.propietarioPost.titulo)
                }
            }
        }
    }
}
