package dev.miguelehr.truequeropa.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun AuthLoginScreen(onLogin: () -> Unit, onGoRegister: () -> Unit, padding: PaddingValues) {
    Button(onClick = onLogin, modifier = Modifier.padding(padding)) { Text("Entrar (stub)") }
}