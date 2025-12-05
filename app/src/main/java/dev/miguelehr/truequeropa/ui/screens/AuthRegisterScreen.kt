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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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
import dev.miguelehr.truequeropa.auth.FirebaseMockLinker
import dev.miguelehr.truequeropa.data.FirestoreManager

@Composable
fun AuthRegisterScreen(
    onRegistered: () -> Unit,   // ahora ya no lo usaremos, pero lo dejamos por compatibilidad
    onBackToLogin: () -> Unit,
    padding: PaddingValues
) {
    val focus = LocalFocusManager.current

    var nombre by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var pass by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    var passVisible by rememberSaveable { mutableStateOf(false) }
    var loading by rememberSaveable { mutableStateOf(false) }
    var errorMsg by rememberSaveable { mutableStateOf<String?>(null) }

    // ---------- Validaciones ----------
    val nombreOk = nombre.length >= 3
    val emailOk = EMAIL_REGEX.matches(email)
    val passOk = isValidPassword(pass)
    val confirmOk = confirm == pass && pass.isNotEmpty()

    val formOk = nombreOk && emailOk && passOk && confirmOk && !loading

    Column(
        modifier = Modifier
            .padding(padding)
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text("Crear cuenta", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(20.dp))

        // Nombre
        OutlinedTextField(
            value = nombre,
            onValueChange = { nombre = it },
            label = { Text("Nombre completo") },
            singleLine = true,
            isError = nombre.isNotEmpty() && !nombreOk,
            supportingText = {
                if (nombre.isNotEmpty() && !nombreOk) {
                    Text("Mínimo 3 caracteres")
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        // Correo
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Correo") },
            singleLine = true,
            isError = email.isNotEmpty() && !emailOk,
            supportingText = {
                if (email.isNotEmpty() && !emailOk) {
                    Text("Correo inválido")
                }
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
            onValueChange = {
                pass = it
                errorMsg = null
            },
            label = { Text("Contraseña") },
            singleLine = true,
            isError = pass.isNotEmpty() && !passOk,
            supportingText = {
                if (pass.isNotEmpty() && !passOk) {
                    Text("Debe tener al menos 8 caracteres e incluir letras y números.")
                }
            },
            visualTransformation = if (passVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
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

        // Confirmación
        OutlinedTextField(
            value = confirm,
            onValueChange = { confirm = it },
            label = { Text("Confirmar contraseña") },
            singleLine = true,
            isError = confirm.isNotEmpty() && !confirmOk,
            supportingText = {
                if (confirm.isNotEmpty() && !confirmOk) {
                    Text("Las contraseñas no coinciden")
                }
            },
            visualTransformation = if (passVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focus.clearFocus()
                    if (formOk) {
                        doRegister(
                            nombre = nombre,
                            email = email,
                            pass = pass,
                            setLoading = { loading = it },
                            setError = { errorMsg = it },
                            onRegistered = onRegistered // ya no navega
                        )
                    }
                }
            ),
            modifier = Modifier.fillMaxWidth()
        )

        // Mensaje general (error o info)
        errorMsg?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(20.dp))

        // Botón registrarse
        Button(
            onClick = {
                focus.clearFocus()
                doRegister(
                    nombre = nombre,
                    email = email,
                    pass = pass,
                    setLoading = { loading = it },
                    setError = { errorMsg = it },
                    onRegistered = onRegistered
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
                Text("Creando cuenta…")
            } else {
                Text("Registrarme")
            }
        }

        Spacer(Modifier.height(12.dp))

        // Enlace para volver al login
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("¿Ya tienes cuenta?")
            TextButton(onClick = onBackToLogin) {
                Text("Inicia sesión")
            }
        }
    }
}

// ---------- Helpers ----------

private val EMAIL_REGEX =
    Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

private fun isValidPassword(password: String): Boolean {
    val regex = Regex("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,}$")
    return regex.matches(password)
}

private fun doRegister(
    nombre: String,
    email: String,
    pass: String,
    setLoading: (Boolean) -> Unit,
    setError: (String?) -> Unit,
    onRegistered: () -> Unit   // ya no lo usamos para navegar, pero lo dejamos
) {
    setError(null)
    setLoading(true)

    FirebaseAuthManager.register(email, pass) { res: FirebaseAuthManager.Result ->
        when (res) {
            is FirebaseAuthManager.Result.Success -> {
                // ✅ Cuenta creada + correo de verificación enviado
                val uid = FirebaseAuthManager.currentUserId() ?: ""

                if (uid.isNotEmpty()) {
                    // Guardar/actualizar perfil con el nombre correcto
                    FirestoreManager.createUserProfile(uid, nombre, email) { ok, err ->
                        setLoading(false)

                        if (!ok) {
                            setError(err ?: "Error guardando perfil de usuario.")
                        } else {
                            FirebaseMockLinker.syncCurrentUserIntoMock()
                            // Cerramos sesión para que luego solo pueda entrar si verifica el correo
                            FirebaseAuthManager.signOut()
                            // Mostrar mensaje en la misma pantalla
                            setError(
                                "Te hemos enviado un correo de verificación. " +
                                        "Revisa tu bandeja y haz clic en el enlace para activar tu cuenta."
                            )
                            // 🔹 Ya NO llamamos onRegistered() → no navegamos
                        }
                    }
                } else {
                    // No se pudo obtener el uid, pero igual avisamos del correo
                    setLoading(false)
                    FirebaseAuthManager.signOut()
                    setError(
                        "Te hemos enviado un correo de verificación. " +
                                "Revisa tu bandeja y haz clic en el enlace para activar tu cuenta."
                    )
                }
            }

            is FirebaseAuthManager.Result.Error -> {
                setLoading(false)
                setError(res.message ?: "Error al registrarse")
            }
        }
    }
}