package dev.miguelehr.truequeropa.ui.screens

import androidx.activity.result.launch
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import dev.miguelehr.truequeropa.model.UserPostsDetails
import dev.miguelehr.truequeropa.model.UserProfile
import dev.miguelehr.truequeropa.ui.viewmodels.UserRequestsViewModel
import kotlinx.coroutines.launch

@Composable
fun PublicationPostsScreen(
    userId: String,
    postIdSol: String,
    requestId:String,
    onNavigateToRequestDetails: (String) -> Unit,
    onBack: () -> Unit,
    vm: UserRequestsViewModel = viewModel()
) {

    val userPosts by vm.userPosts.collectAsState()
    var selectedPostId by remember { mutableStateOf<String?>(null) }
    var showDialogForPost by remember { mutableStateOf<String?>(null) }
    var userSolicitante by remember { mutableStateOf<UserProfile?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(userId) {
        userSolicitante = vm.selUser(userId)
    }

    showDialogForPost?.let { postId ->
        AlertDialog(
            onDismissRequest = {
                showDialogForPost = null
            },
            title = { Text("Confirmar selección") },
            text = { Text("¿Deseas seleccionar esta prenda para el trueque?") },
            confirmButton = {
                TextButton(
                    onClick = {

                        scope.launch {
                            vm.updPost(postIdSol, "0")
                            vm.updPostRequestSolicitante (requestId, postId)
                            vm.updPost(postId, "1")
                             //onNavigateToRequestDetails(userId)
                            onBack()
                        }

                        // Cerramos el diálogo después de navegar.
                        showDialogForPost = null
                    }
                ) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        // Si el usuario cancela, simplemente cerramos el diálogo.
                        showDialogForPost = null
                        onBack()
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    LaunchedEffect(userId) {
        vm.fetchUserPosts(userId)
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(24.dp))
        Text(
            text = "Prendas de ${userSolicitante?.nombre?.uppercase()}:",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(16.dp))
        LazyColumn(
            modifier = Modifier.weight(1f, fill = false),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            items(userPosts) { postDetails ->

                PostItem(
                    postDetails = postDetails,
                    isSelected = postDetails.solicitantePost.id == selectedPostId,
                    onItemClick = {

                        selectedPostId = if (selectedPostId == postDetails.solicitantePost.id) {
                            null
                        } else {
                            postDetails.solicitantePost.id
                        }
                        showDialogForPost = postDetails.solicitantePost.id
                        //onNavigateToRequestDetails(userId )
                    }
                )
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
        Spacer(Modifier.height(16.dp))
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
            .clickable {
                onItemClick()
            } ,
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
        Row(
            modifier = Modifier
                .padding(12.dp)
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        )
        {
            Image(
                painter = rememberAsyncImagePainter(generateImageUrl(postDetails.solicitantePost.categoria,1)),
                contentDescription = postDetails.solicitantePost.descripcion,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxHeight()
                    .width(90.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.LightGray)
            )
            Spacer(Modifier.width(16 .dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = postDetails.solicitantePost.titulo.uppercase(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "${postDetails.solicitantePost.categoria} • Talla ${postDetails.solicitantePost.talla}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "Descripción: ${postDetails.solicitantePost.descripcion} ",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2
            )

            AssistChip(onClick = {}, label = { Text("Estado: ${postDetails.solicitantePost.estado}") })

        }
        }
    }
}
