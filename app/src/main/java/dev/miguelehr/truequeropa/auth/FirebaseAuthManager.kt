package dev.miguelehr.truequeropa.auth

import com.google.firebase.auth.FirebaseAuth

object FirebaseAuthManager {

    sealed class Result {
        data object Success : Result()
        data class Error(val message: String?) : Result()
    }

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    // ✅ Registro
    fun register(
        email: String,
        password: String,
        callback: (Result) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) callback(Result.Success)
                else callback(Result.Error(task.exception?.localizedMessage))
            }
    }

    // ✅ Login
    fun login(
        email: String,
        password: String,
        callback: (Result) -> Unit
    ) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) callback(Result.Success)
                else callback(Result.Error(task.exception?.localizedMessage))
            }
    }

    fun signOut() = auth.signOut()
    fun isLoggedIn(): Boolean = auth.currentUser != null
    fun currentUserEmail(): String? = auth.currentUser?.email
    fun currentUserId(): String? = auth.currentUser?.uid
}