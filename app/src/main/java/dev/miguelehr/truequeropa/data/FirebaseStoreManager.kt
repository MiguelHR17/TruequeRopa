package dev.miguelehr.truequeropa.data

import android.content.Context
import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

object FirebaseStorageManager {

    private val storage by lazy { FirebaseStorage.getInstance() }

    /**
     * Sube una lista de imágenes (uris locales) a Storage y devuelve sus download URLs.
     *
     * Ruta: users/{uid}/posts/{prendaId}/img_{index}.jpg
     */
    suspend fun uploadImagesForPost(
        context: Context,
        uid: String,
        prendaId: String,
        uris: List<Uri>
    ): List<String> {
        if (uris.isEmpty()) return emptyList()

        val result = mutableListOf<String>()

        for ((index, uri) in uris.withIndex()) {
            val ref = storage.reference
                .child("users")
                .child(uid)
                .child("posts")
                .child(prendaId)
                .child("img_$index.jpg")

            // Leer bytes de la Uri (galería)
            val bytes = withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    input.readBytes()
                }
            } ?: continue // si falla esa imagen, la saltamos

            // Subir a Storage
            val uploadTaskSnapshot = ref.putBytes(bytes).await()

            // Obtener URL de descarga
            val downloadUrl = uploadTaskSnapshot.storage.downloadUrl.await().toString()
            result += downloadUrl
        }

        return result
    }
}