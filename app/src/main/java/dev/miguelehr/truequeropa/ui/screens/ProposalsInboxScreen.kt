package dev.miguelehr.truequeropa.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import dev.miguelehr.truequeropa.model.*
import dev.miguelehr.truequeropa.R
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProposalsInboxScreen(onSent: () -> Unit,padding: PaddingValues) {
    val myId = FakeRepository.currentUser.id
    val proposals = remember {
        mutableStateListOf<TradeProposal>().also {
            it.addAll(FakeRepository.proposals.filter { p -> p.toUserId == myId && p.status == ProposalStatus.PENDIENTE })
        }
    }

    var expanded by remember { mutableStateOf(false) }
    var selectedGenero by remember { mutableStateOf("") }
    val generos = listOf("Masculino", "Femenino")
    var selectedTipo by remember { mutableStateOf<String?>(null) }

    val productos = listOf(
        Product(id="01", ownerId = "Veronica",talla = Size.M,estado = Condition.NUEVO, categoria = Category.PANTALON,  titulo="JEAN AZUL", descripcion=  "Talla: Veronica mide 1.72m y está usando talla 28", imageUrl = "https://picsum.photos/seed/p2/600/400"),
        Product(id="02", ownerId = "Veronica",talla = Size.M,estado = Condition.USADO, categoria = Category.CAMISA,titulo="POLO BLANCO",  descripcion= "Polo de algodón orgánico.", imageUrl = "https://picsum.photos/seed/p1/600/400"),
        Product(id="03", ownerId = "Veronica",talla = Size.M,estado = Condition.USADO, categoria = Category.VESTIDO,titulo="VESTIDO ROJO", descripcion= "Vestido corto elegante.", imageUrl = "https://picsum.photos/seed/p3/600/400")
    )

    val tipoImagenMap = mapOf(
        Category.PANTALON to R.drawable.jeans_tipo,
        Category.CAMISA to R.drawable.polo_tipo,
        Category.VESTIDO to R.drawable.vestido_tipo,
        Category.CHAQUETA to R.drawable.vestido_tipo,
        Category.ACCESORIO to R.drawable.vestido_tipo
    )
    var productoSeleccionado by remember { mutableStateOf<Product?>(null) }


    Column(Modifier.padding(padding).padding(12.dp)) {
        Text("Propuestas Trueque", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))

        Text(text = "Género", style = MaterialTheme.typography.bodyLarge,fontWeight = FontWeight.Bold)
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }  // 👈 click funciona perfectamente
        ) {
            OutlinedTextField(
                value = selectedGenero,
                onValueChange = {},
                readOnly = true,
                label = { Text("Selecciona un género") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                generos.forEach { genero ->
                    DropdownMenuItem(
                        text = { Text(genero) },
                        onClick = {
                            selectedGenero = genero
                            expanded = false
                        }
                    )
                }
            }
        }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Tipo", fontWeight = FontWeight.Bold)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                items(Category.entries.toTypedArray()) { tipo ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { selectedTipo = tipo.name }
                            .border(
                                width = if (selectedTipo == tipo.name) 2.dp else 1.dp,
                                color = if(selectedTipo == tipo.name) Color.Blue else Color.Gray,
                                shape = RoundedCornerShape(8.dp  )
                            )
                            .padding(4.dp)
                    )

                    {
                        Image(
                            painter = painterResource(id = tipoImagenMap[tipo] ?: R.drawable.jeans_tipo),
                            contentDescription = tipo.name,
                            modifier = Modifier.size(60.dp)
                        )
                        Text(tipo.name)
                    }
                }
            }

        // 2️⃣ Detalle del producto
        productos.forEach { producto ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    .clickable {
                        productoSeleccionado = producto

                    },
                colors = CardDefaults.cardColors(
                    containerColor = if (productoSeleccionado==producto) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    modifier = Modifier.padding(8.dp)
                ) {
                    Column {
                        Image(
                            painter = painterResource(id = R.drawable.owner1),
                            contentDescription = producto.ownerId,
                            modifier = Modifier.size(45.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(producto.ownerId, fontWeight = FontWeight.Bold)

                    }

                    Image(
                        painter = rememberAsyncImagePainter(producto.imageUrl),
                        contentDescription = producto.titulo,
                        modifier = Modifier.size(70.dp).clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(producto.titulo, fontWeight = FontWeight.Bold)
                        Text(producto.descripcion)
                    }
                }
            }

        }
        Spacer(modifier = Modifier.height(16.dp))

        productoSeleccionado?.let {
            Spacer(modifier = Modifier.height(12.dp))
            Text("Seleccionaste: ${it.titulo}", fontWeight = FontWeight.Bold)
        }
        // Filtros
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {onSent()},
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .fillMaxWidth()
            ) {
                Text("Solicitar Intercambio")
            }
        }



        if (proposals.isEmpty()) {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(proposals) { p ->
                    val offered = FakeRepository.products.find { it.id == p.offeredProductId }
                    val requested = FakeRepository.products.find { it.id == p.requestedProductId }

                    Card {
                        Column(Modifier.padding(12.dp)) {
                            Text("Te ofrecen: ${offered?.titulo} por tu ${requested?.titulo}")
                            Spacer(Modifier.height(8.dp))
                            Row {
                                Button(onClick = {
                                    // Aceptar
                                    FakeRepository.trades += TradeHistoryItem(
                                        id = "t${FakeRepository.trades.size + 1}",
                                        prendaOfrecidaId = p.offeredProductId,
                                        prendaRecibidaId = p.requestedProductId,
                                        fecha = "2025-10-10"
                                    )
                                    proposals.remove(p)
                                }) {
                                    Text("Aceptar")
                                }
                                Spacer(Modifier.width(8.dp))
                                OutlinedButton(onClick = { proposals.remove(p) }) {
                                    Text("Rechazar")
                                }
                            }
                        }
                    }
                }
            }

        } else
        {
            Text("")
        }
    }
}