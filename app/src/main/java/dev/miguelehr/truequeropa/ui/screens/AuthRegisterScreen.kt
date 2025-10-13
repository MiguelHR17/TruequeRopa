package dev.miguelehr.truequeropa.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun AuthRegisterScreen(onRegistered: () -> Unit, padding: PaddingValues) {
    Button(onClick = onRegistered, modifier = Modifier.padding(padding)) { Text("Registrarme (stub)") }
}