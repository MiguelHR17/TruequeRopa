package dev.miguelehr.truequeropa.auth

import com.google.firebase.auth.FirebaseAuth
import dev.miguelehr.truequeropa.model.FakeRepository
import dev.miguelehr.truequeropa.model.User

/**
 * Vincula el usuario autenticado en Firebase con un usuario del mock.
 * Estrategia: match por email. Si no existe en el mock, lo crea.
 */
object FirebaseMockLinker {

    private val auth by lazy { FirebaseAuth.getInstance() }

    /** Llamar justo después de un login/registro exitoso, o al iniciar la app. */
    fun syncCurrentUserIntoMock() {
        val fb = auth.currentUser ?: return

        val email = fb.email?.lowercase()?.trim().orEmpty()
        val name  = fb.displayName?.ifBlank { null } ?: email.substringBefore('@')

        // 1) ¿Ya existe un usuario mock con ese email?
        val existing = FakeRepository.users.find { it.correo.equals(email, ignoreCase = true) }
        if (existing != null) {
            FakeRepository.switchUser(existing.id)
            return
        }

        // 2) Si no existe, lo creamos y lo activamos como current
        val newId = newMockId()   // u4, u5, ...
        val created = User(
            id = newId,
            nombre = name,
            correo = email,
            roles = setOf("USER")
        )
        FakeRepository.users += created
        FakeRepository.switchUser(created.id)
    }

    private fun newMockId(): String {
        // genera uN donde N = tamaño + 1 evitando colisiones sencillas
        var n = FakeRepository.users.size + 1
        var id = "u$n"
        while (FakeRepository.users.any { it.id == id }) {
            n += 1
            id = "u$n"
        }
        return id
    }
}

private fun FakeRepository.switchUser(id: String) {}

