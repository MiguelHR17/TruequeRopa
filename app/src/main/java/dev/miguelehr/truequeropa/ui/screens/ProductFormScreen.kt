package dev.miguelehr.truequeropa.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun ProductFormScreen(onSaved: () -> Unit, padding: PaddingValues) {
    Text("Publicar prenda (stub)", modifier = Modifier.padding(padding))
}