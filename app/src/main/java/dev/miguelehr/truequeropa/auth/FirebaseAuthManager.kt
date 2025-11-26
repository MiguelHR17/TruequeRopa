package dev.miguelehr.truequeropa.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException

object FirebaseAuthManager {

    sealed class Result {
        data object Success : Result()
        data class Error(val message: String?) : Result()
    }

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    // --------- VALIDACIÓN DE CONTRASEÑA ---------
    private fun isValidPassword(password: String): Boolean {
        // Mínimo 8 caracteres, al menos 1 letra y 1 número
        val regex = Regex("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,}$")
        return regex.matches(password)
    }

    // ✅ Registro con envío de correo de verificación
    fun register(
        email: String,
        password: String,
        callback: (Result) -> Unit
    ) {
        // Validar contraseña antes de llamar a Firebase
        if (!isValidPassword(password)) {
            callback(
                Result.Error(
                    "La contraseña debe tener al menos 8 caracteres e incluir letras y números."
                )
            )
            return
        }

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    val ex = task.exception
                    val fbEx = ex as? FirebaseAuthException
                    val message = when (fbEx?.errorCode) {
                        "ERROR_EMAIL_ALREADY_IN_USE" ->
                            "Ya existe una cuenta registrada con este correo."
                        "ERROR_INVALID_EMAIL" ->
                            "El formato del correo no es válido."
                        "ERROR_WEAK_PASSWORD" ->
                            "La contraseña es demasiado débil. Usa al menos 8 caracteres con letras y números."
                        else ->
                            ex?.localizedMessage ?: "No se pudo crear la cuenta. Inténtalo de nuevo."
                    }
                    callback(Result.Error(message))
                    return@addOnCompleteListener
                }

                // Cuenta creada → enviar correo de verificación
                val user = auth.currentUser
                user?.sendEmailVerification()
                    ?.addOnCompleteListener { verTask ->
                        if (verTask.isSuccessful) {
                            // Todo OK, pero el usuario aún debe verificar el correo
                            callback(
                                Result.Error(
                                    "Te hemos enviado un correo de verificación. Revisa tu bandeja y haz clic en el enlace para activar tu cuenta."
                                )
                            )
                        } else {
                            callback(
                                Result.Error(
                                    "Cuenta creada, pero no se pudo enviar el correo de verificación. Intenta más tarde."
                                )
                            )
                        }
                    }
            }
    }

    // ✅ Login: solo permite entrar si el correo está verificado
    fun login(
        email: String,
        password: String,
        callback: (Result) -> Unit
    ) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    val ex = task.exception
                    val fbEx = ex as? FirebaseAuthException

                    val message = when (fbEx?.errorCode) {
                        "ERROR_WRONG_PASSWORD" ->
                            "Contraseña incorrecta."
                        "ERROR_USER_NOT_FOUND" ->
                            "No existe una cuenta con este correo."
                        "ERROR_INVALID_EMAIL" ->
                            "El formato del correo no es válido."
                        else ->
                            ex?.localizedMessage ?: "No se pudo iniciar sesión. Inténtalo de nuevo."
                    }

                    callback(Result.Error(message))
                    return@addOnCompleteListener
                }

                val user = auth.currentUser
                if (user != null && !user.isEmailVerified) {
                    // No dejar entrar si no ha verificado su correo
                    auth.signOut()
                    callback(
                        Result.Error(
                            "Tu correo aún no está verificado. Revisa tu bandeja y haz clic en el enlace de verificación."
                        )
                    )
                } else {
                    callback(Result.Success)
                }
            }
    }

    fun signOut() = auth.signOut()
    fun isLoggedIn(): Boolean = auth.currentUser != null
    fun currentUserEmail(): String? = auth.currentUser?.email
    fun currentUserId(): String? = auth.currentUser?.uid

    fun sendPasswordReset(
        email: String,
        callback: (Result) -> Unit
    ) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    callback(Result.Success)
                } else {
                    val ex = task.exception
                    val fbEx = ex as? FirebaseAuthException
                    val message = when (fbEx?.errorCode) {
                        "ERROR_INVALID_EMAIL" ->
                            "El correo no tiene un formato válido."
                        "ERROR_USER_NOT_FOUND" ->
                            "No existe ninguna cuenta registrada con este correo."
                        else ->
                            ex?.localizedMessage ?: "No se pudo enviar el correo de restablecimiento."
                    }
                    callback(Result.Error(message))
                }
            }
    }
}