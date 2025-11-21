package dev.miguelehr.truequeropa.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.miguelehr.truequeropa.model.UserPostsDetails
import dev.miguelehr.truequeropa.ui.viewmodels.UserRequestsViewModel

@Composable
fun PublicationPostsScreen(
    userId: String,
    nombre:String,
    requestId:String,
    vm: UserRequestsViewModel = viewModel()
) {
    // 1. Observar el StateFlow del ViewModel
    // `collectAsState` convierte el Flow en un State que recompone la UI al cambiar.
    val userPosts by vm.userPosts.collectAsState()
    var selectedPostId by remember { mutableStateOf<String?>(null) }
    // 2. Llamar a la función para obtener los datos
    // `LaunchedEffect` ejecuta la corrutina solo una vez cuando la pantalla se muestra por primera vez.
    // Si `userId` cambiara, se volvería a ejecutar.
    LaunchedEffect(userId) {
        vm.fetchUserPosts(userId)
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Prendas de $nombre:",
            fontWeight = FontWeight.Medium
        )
        // 3. Usar LazyColumn para mostrar la lista de posts
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(vertical =16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp) // Espacio entre cada item
        ) {

            items(userPosts) { postDetails ->
                // `PostItem` es un Composable que tú creas para mostrar un solo post
                PostItem(
                    postDetails = postDetails,
                    isSelected = postDetails.solicitantePost.id == selectedPostId,
                    onItemClick = {
                        // Al hacer clic, actualizamos el ID seleccionado.
                        // Si se vuelve a clicar el mismo, se deselecciona.
                        selectedPostId = if (selectedPostId == postDetails.solicitantePost.id) {
                            null
                        } else {
                            postDetails.solicitantePost.id
                        }
                    }
                )
            }
            item {
                Button(
                    onClick = {

                    },

                    enabled = selectedPostId != null,
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    Text("Confirmar Trueque")
                }
            }
        }



    }
}

/**
 * Un Composable para mostrar la información de un solo post.
 * Puedes personalizarlo como quieras.
 */
@Composable
fun PostItem(
    postDetails: UserPostsDetails,
    isSelected: Boolean, // <-- Recibe si está seleccionado
    onItemClick: () -> Unit // <-- Recibe la acción de clic
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onItemClick), // <-- Hacemos toda la tarjeta clicable
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = if (isSelected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            null
        },
        colors = if (isSelected) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Asumo que tu data class `UserPostsDetails` tiene un campo `post`
            // y que la clase `UserPost` tiene un campo `titulo`.
            Text(
                text = postDetails.solicitantePost.titulo,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Estado: ${postDetails.solicitantePost.estado}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Descripción: ${postDetails.solicitantePost.descripcion}",
                style = MaterialTheme.typography.bodyMedium
            )
            // Puedes añadir más detalles, como imágenes, botones, etc.
        }
    }
}