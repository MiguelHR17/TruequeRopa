package dev.miguelehr.truequeropa.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import dev.miguelehr.truequeropa.model.Category
import dev.miguelehr.truequeropa.model.Condition
import dev.miguelehr.truequeropa.model.Size
import dev.miguelehr.truequeropa.auth.FirebaseAuthManager
import dev.miguelehr.truequeropa.data.FirestoreManager
import java.util.UUID
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import dev.miguelehr.truequeropa.data.FirebaseStorageManager

@Composable
fun ProductFormScreen(
    onSaved: () -> Unit,
    padding: PaddingValues
) {
    var titulo by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var selectedSize by remember { mutableStateOf<Size?>(null) }
    var selectedCondition by remember { mutableStateOf(Condition.USADO) }
    var selectedImages by remember { mutableStateOf(listOf<Uri>()) }

    // estado para guardado
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val canSave = titulo.isNotBlank()
            && descripcion.isNotBlank()
            && selectedCategory != null
            && selectedSize != null
            && selectedImages.isNotEmpty() // ← Agregado: debe tener al menos 1 imagen

    // Launcher para seleccionar imágenes
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            if (selectedImages.size < 5) { // Límite de 5 fotos
                selectedImages = selectedImages + it
            }
        }
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Título principal
        Text(
            text = "REGISTRO DE PRODUCTOS",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Campo: Título
        Text(
            text = "Título del producto",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        OutlinedTextField(
            value = titulo,
            onValueChange = { titulo = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            placeholder = { Text("Ej: Camiseta deportiva Nike") },
            singleLine = true
        )

        // Campo: Descripción
        Text(
            text = "Descripción del producto",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        OutlinedTextField(
            value = descripcion,
            onValueChange = { descripcion = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .padding(bottom = 24.dp),
            placeholder = { Text("Describe tu producto en detalle...") },
            maxLines = 4
        )

        // Selección de categorías
        Text(
            text = "Selecciona la Categoría",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(bottom = 24.dp)
        ) {
            items(Category.entries) { category ->
                CategoryItem(
                    category = category,
                    isSelected = selectedCategory == category,
                    onClick = { selectedCategory = category }
                )
            }
        }

        // Selección de talla
        Text(
            text = "Talla",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 20.dp)
        ) {
            Size.entries.forEach { size ->
                FilterChip(
                    selected = selectedSize == size,
                    onClick = { selectedSize = size },
                    label = { Text(size.name) }
                )
            }
        }

        // Selección de condición
        Text(
            text = "Estado",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 24.dp)
        ) {
            Condition.entries.forEach { condition ->
                FilterChip(
                    selected = selectedCondition == condition,
                    onClick = { selectedCondition = condition },
                    label = { Text(condition.name) }
                )
            }
        }

        // Fotos del producto
        Text(
            text = "Fotos del producto",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "Máximo 5 fotos (mínimo 1)",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(bottom = 32.dp)
        ) {
            // Botón para agregar fotos
            if (selectedImages.size < 5) {
                item {
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                2.dp,
                                Color.Gray.copy(alpha = 0.3f),
                                RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                imagePickerLauncher.launch("image/*")
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Agregar foto",
                                modifier = Modifier.size(40.dp),
                                tint = Color.Gray
                            )
                            Text(
                                text = "Agregar foto",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }

            // Imágenes seleccionadas
            items(selectedImages) { imageUri ->
                Box(
                    modifier = Modifier.size(140.dp)
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(imageUri),
                        contentDescription = "Foto del producto",
                        modifier = Modifier
                            .size(140.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )

                    // Botón para eliminar foto
                    IconButton(
                        onClick = {
                            selectedImages = selectedImages.filter { it != imageUri }
                        },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(32.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.6f)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Eliminar foto",
                                tint = Color.White,
                                modifier = Modifier.padding(4.dp)
                            )
                        }
                    }
                }
            }
        }

        // Mensaje de error (si lo hay)
        error?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // Botón para publicar en el perfil
        Button(
            onClick = {
                val uid = FirebaseAuthManager.currentUserId()
                if (uid == null) {
                    error = "Debes iniciar sesión para publicar."
                    return@Button
                }

                scope.launch {
                    saving = true
                    error = null

                    val prendaId = UUID.randomUUID().toString()

                    try {
                        // 1) Subir imágenes a Storage
                        val imageUrlStrings = FirebaseStorageManager.uploadImagesForPost(
                            context = context,
                            uid = uid,
                            prendaId = prendaId,
                            uris = selectedImages
                        )

                        // 2) Crear el documento en Firestore
                        FirestoreManager.createUserPost(
                            uid = uid,
                            prendaId = prendaId,
                            titulo = titulo.trim(),
                            descripcion = descripcion.trim(),
                            categoria = selectedCategory!!.name,
                            talla = selectedSize!!.name,
                            estado = selectedCondition.name,
                            imageUrls = imageUrlStrings,
                            estadoTrueque = "0"
                        ) { ok, err ->
                            saving = false
                            if (ok) {
                                // Limpiar formulario
                                titulo = ""
                                descripcion = ""
                                selectedCategory = null
                                selectedSize = null
                                selectedCondition = Condition.USADO
                                selectedImages = emptyList()
                                onSaved()
                            } else {
                                error = err ?: "No se pudo guardar la publicación"
                            }
                        }
                    } catch (e: Exception) {
                        saving = false
                        error = e.message ?: "Error al subir imágenes a la nube"
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = canSave && !saving,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            if (saving) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(Modifier.width(8.dp))
                Text("Publicando…")
            } else {
                Text(
                    text = "Publicar en mi perfil",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun CategoryItem(
    category: Category,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .border(
                    width = if (isSelected) 3.dp else 0.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = rememberAsyncImagePainter(getCategoryImage(category)),
                contentDescription = category.name,
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = category.name,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray
        )
    }
}

private fun getCategoryImage(category: Category): String {
    return when (category) {
        Category.CAMISA -> "https://images.unsplash.com/photo-1596755094514-f87e34085b2c?w=300"
        Category.PANTALON -> "https://images.unsplash.com/photo-1473966968600-fa801b869a1a?w=300"
        Category.VESTIDO -> "https://images.unsplash.com/photo-1595777457583-95e059d581b8?w=300"
        Category.CHAQUETA -> "https://images.unsplash.com/photo-1551028719-00167b16eac5?w=300"
        Category.ZAPATOS -> "https://images.unsplash.com/photo-1460353581641-37baddab0fa2?w=300"
        Category.ACCESORIO -> "https://images.unsplash.com/photo-1535632787350-4e68ef0ac584?w=300"
    }
}