package dev.miguelehr.truequeropa.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import dev.miguelehr.truequeropa.R
import dev.miguelehr.truequeropa.model.Category
import dev.miguelehr.truequeropa.model.Condition
import dev.miguelehr.truequeropa.model.FakeRepository
import dev.miguelehr.truequeropa.model.Product
import dev.miguelehr.truequeropa.model.ProposalStatus
import dev.miguelehr.truequeropa.model.Size
import dev.miguelehr.truequeropa.model.TradeProposal

@Composable
fun TradeHistoryScreen(padding: PaddingValues) {
    val myId = FakeRepository.currentUser.id

    Column(Modifier.padding(padding).padding(12.dp)) {
        Text("Historial de propuestas", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(8.dp)
                ) {
                    Column {
                        Image(
                            painter = painterResource(id = R.drawable.owner1),
                            contentDescription = "Veronica",
                            modifier = Modifier.size(45.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Veronica", fontWeight = FontWeight.Bold)

                    }

                    Column {
                        Text("24/09/2025 Intercambio con Ana", fontWeight = FontWeight.Bold)
                        Text("Jean Azul con Vestido negro")
                    }
                }
            }

        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.padding(8.dp)
            ) {
                Column {
                    Image(
                        painter = painterResource(id = R.drawable.owner2),
                        contentDescription = "Ana",
                        modifier = Modifier.size(45.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Ana", fontWeight = FontWeight.Bold)

                }

                Column {
                    Text("11/10/2025 Intercambio con Fresa21", fontWeight = FontWeight.Bold)
                    Text("Casaca negra con Jean Negro")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))


    }
}