package dev.miguelehr.truequeropa.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun AuthRegisterScreen(
    onRegistered: () -> Unit,
    padding: PaddingValues
) {
    val focus = LocalFocusManager.current

    var nombre by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var pass by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    var passVisible by rememberSaveable { mutableStateOf(false) }
    var loading by rememberSaveable { mutableStateOf(false) }

    // Validaciones básicas
    val emailOk = EMAIL_REGEX.matches(email)
    val passOk = pass.length >= 6
    val confirmOk = confirm == pass
    val nombreOk = nombre.length >= 3
    val formOk = emailOk && passOk && confirmOk && nombreOk && !loading

    Column(
        modifier = Modifier
            .padding(padding)
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Text("Crear cuenta", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))
        Text(
            "Regístrate para comenzar a intercambiar prendas en TruequeRopa.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(20.dp))

        // Nombre
        OutlinedTextField(
            value = nombre,
            onValueChange = { nombre = it },
            label = { Text("Nombre completo") },
            singleLine = true,
            isError = nombre.isNotEmpty() && !nombreOk,
            supportingText = {
                if (nombre.isNotEmpty() && !nombreOk) Text("Debe tener al menos 3 caracteres")
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        // Correo
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Correo electrónico") },
            singleLine = true,
            isError = email.isNotEmpty() && !emailOk,
            supportingText = {
                if (email.isNotEmpty() && !emailOk) Text("Correo inválido")
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        // Contraseña
        OutlinedTextField(
            value = pass,
            onValueChange = { pass = it },
            label = { Text("Contraseña") },
            singleLine = true,
            isError = pass.isNotEmpty() && !passOk,
            supportingText = {
                if (pass.isNotEmpty() && !passOk) Text("Mínimo 6 caracteres")
            },
            visualTransformation = if (passVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { passVisible = !passVisible }) {
                    Icon(
                        imageVector = if (passVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (passVisible) "Ocultar" else "Mostrar"
                    )
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        // Confirmar contraseña
        OutlinedTextField(
            value = confirm,
            onValueChange = { confirm = it },
            label = { Text("Confirmar contraseña") },
            singleLine = true,
            isError = confirm.isNotEmpty() && !confirmOk,
            supportingText = {
                if (confirm.isNotEmpty() && !confirmOk) Text("Las contraseñas no coinciden")
            },
            visualTransformation = if (passVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focus.clearFocus()
                    if (formOk) simulateRegister(onRegistered, setLoading = { loading = it })
                }
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(20.dp))

        // Botón Registrar
        Button(
            onClick = {
                simulateRegister(onRegistered, setLoading = { loading = it })
            },
            enabled = formOk,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (loading) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    modifier = Modifier
                        .size(18.dp)
                        .align(Alignment.CenterVertically)
                )
                Spacer(Modifier.width(8.dp))
                Text("Creando cuenta…")
            } else {
                Text("Registrarme")
            }
        }

        Spacer(Modifier.height(24.dp))
        ProvideTextStyle(MaterialTheme.typography.bodySmall) {
            Text("Al registrarte aceptas los términos de uso y privacidad (mock).")
        }
    }
}

// Usa la misma regex y simulación que el login
private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

private fun simulateRegister(onRegistered: () -> Unit, setLoading: (Boolean) -> Unit) {
    setLoading(true)
    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
        setLoading(false)
        onRegistered()
    }, 700)
}