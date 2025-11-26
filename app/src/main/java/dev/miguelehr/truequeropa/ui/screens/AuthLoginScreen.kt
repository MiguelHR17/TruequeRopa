package dev.miguelehr.truequeropa.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
    var infoMsg by rememberSaveable { mutableStateOf<String?>(null) } // 👈 mensajes informativos (reset ok)

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
            "Ingresa tus credenciales para continuar.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(20.dp))

        // Correo
        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                errorMsg = null
                infoMsg = null
            },
            label = { Text("Correo electrónico") },
            singleLine = true,
            isError = email.isNotEmpty() && !emailOk,
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
            onValueChange = {
                pass = it
                errorMsg = null
                infoMsg = null
            },
            label = { Text("Contraseña") },
            singleLine = true,
            isError = pass.isNotEmpty() && !passOk,
            visualTransformation = if (passVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            trailingIcon = {
                IconButton(onClick = { passVisible = !passVisible }) {
                    Icon(
                        imageVector = if (passVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = null
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
                        login(
                            email = email,
                            pass = pass,
                            setLoading = { loading = it },
                            setError = { errorMsg = it; infoMsg = null },
                            onLogin = onLogin
                        )
                    }
                }
            ),
            modifier = Modifier.fillMaxWidth()
        )

        // 👇 Enlace "¿Olvidaste tu contraseña?"
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.CenterEnd
        ) {
            TextButton(
                onClick = {
                    resetPassword(
                        email = email,
                        setLoading = { loading = it },
                        setError = { errorMsg = it; infoMsg = null },
                        setInfo = { infoMsg = it; errorMsg = null }
                    )
                }
            ) {
                Text("¿Olvidaste tu contraseña?")
            }
        }

        // Mensaje de error (login o reset)
        errorMsg?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        // Mensaje informativo (reset exitoso)
        infoMsg?.let {
            Spacer(Modifier.height(4.dp))
            Text(it, color = MaterialTheme.colorScheme.primary)
        }

        Spacer(Modifier.height(20.dp))

        // Botón Entrar
        Button(
            onClick = {
                login(
                    email = email,
                    pass = pass,
                    setLoading = { loading = it },
                    setError = { errorMsg = it; infoMsg = null },
                    onLogin = onLogin
                )
            },
            enabled = formOk,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (loading) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Ingresando…")
            } else {
                Text("Entrar")
            }
        }

        Spacer(Modifier.height(12.dp))

        // Botón para ir al registro
        OutlinedButton(
            onClick = onGoRegister,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Crear una cuenta")
        }
    }
}

private fun login(
    email: String,
    pass: String,
    setLoading: (Boolean) -> Unit,
    setError: (String?) -> Unit,
    onLogin: () -> Unit
) {
    setError(null)
    setLoading(true)
    FirebaseAuthManager.login(email, pass) { res: Result ->
        when (res) {
            is Result.Success -> {
                setLoading(false)
                onLogin()
            }
            is Result.Error -> {
                setLoading(false)
                setError(res.message ?: "Error iniciando sesión")
            }
        }
    }
}

// 👇 Nuevo helper para restablecer contraseña
private fun resetPassword(
    email: String,
    setLoading: (Boolean) -> Unit,
    setError: (String?) -> Unit,
    setInfo: (String?) -> Unit
) {
    setError(null)
    setInfo(null)

    if (email.isBlank() || !EMAIL_REGEX.matches(email)) {
        setError("Ingresa un correo válido para restablecer tu contraseña.")
        return
    }

    setLoading(true)
    FirebaseAuthManager.sendPasswordReset(email) { res ->
        when (res) {
            is Result.Success -> {
                setLoading(false)
                setInfo("Te hemos enviado un enlace para restablecer tu contraseña a $email.")
            }
            is Result.Error -> {
                setLoading(false)
                setError(res.message ?: "No se pudo enviar el correo de restablecimiento.")
            }
        }
    }
}

private fun FirebaseAuthManager.sendPasswordReset(email: String, function: Any) {}

// Regex de correo (ya lo tenías)
private val EMAIL_REGEX =
    Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")