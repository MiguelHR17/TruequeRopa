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
import dev.miguelehr.truequeropa.auth.FirebaseAuthManager
import dev.miguelehr.truequeropa.auth.FirebaseAuthManager.Result

@Composable
fun AuthLoginScreen(
    onLogin: () -> Unit,
    onGoRegister: () -> Unit,
    padding: PaddingValues
) {
    val focus = LocalFocusManager.current

    var email by rememberSaveable { mutableStateOf("") }
    var pass by rememberSaveable { mutableStateOf("") }
    var passVisible by rememberSaveable { mutableStateOf(false) }
    var loading by rememberSaveable { mutableStateOf(false) }
    var errorMsg by rememberSaveable { mutableStateOf<String?>(null) }

    // Validaciones simples
    val emailOk = email.isNotBlank() && EMAIL_REGEX.matches(email)
    val passOk = pass.length >= 6
    val formOk = emailOk && passOk && !loading

    Column(
        modifier = Modifier
            .padding(padding)
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Text("Iniciar sesión", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))
        Text(
            "Bienvenido a TruequeRopa. Ingresa tus credenciales para continuar.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(20.dp))

        // Correo
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Correo electrónico") },
            singleLine = true,
            isError = email.isNotEmpty() && !emailOk,
            supportingText = {
                if (email.isNotEmpty() && !emailOk) Text("Ingresa un correo válido")
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
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focus.clearFocus()
                    if (formOk) {
                        doLogin(
                            email = email,
                            pass = pass,
                            setLoading = { loading = it },
                            setError = { errorMsg = it },
                            onSuccess = onLogin
                        )
                    }
                }
            ),
            modifier = Modifier.fillMaxWidth()
        )

        if (errorMsg != null) {
            Spacer(Modifier.height(8.dp))
            Text(errorMsg!!, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(16.dp))

        // Botón Entrar
        Button(
            onClick = {
                doLogin(
                    email = email,
                    pass = pass,
                    setLoading = { loading = it },
                    setError = { errorMsg = it },
                    onSuccess = onLogin
                )
            },
            enabled = formOk,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (loading) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    modifier = Modifier
                        .size(18.dp)
                        .align(Alignment.CenterVertically)
                )
                Spacer(Modifier.width(8.dp))
                Text("Ingresando…")
            } else {
                Text("Entrar")
            }
        }

        Spacer(Modifier.height(12.dp))

        // Ir a registro
        OutlinedButton(
            onClick = onGoRegister,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Crear una cuenta")
        }

        Spacer(Modifier.height(24.dp))
        ProvideTextStyle(MaterialTheme.typography.bodySmall) {
            Text("Al continuar aceptas las políticas de uso y privacidad.")
        }
    }
}

// Regex simple para validar email
private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

// ---- Lógica que llama al manager ----
private fun doLogin(
    email: String,
    pass: String,
    setLoading: (Boolean) -> Unit,
    setError: (String?) -> Unit,
    onSuccess: () -> Unit
) {
    setError(null)
    setLoading(true)
    FirebaseAuthManager.loginWithEmail(email, pass) { res ->
        setLoading(false)
        when (res) {
            is Result.Success -> onSuccess()
            is Result.Error   -> setError(res.message ?: "Error iniciando sesión")
        }
    }
}