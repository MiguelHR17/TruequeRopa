package dev.miguelehr.truequeropa.data

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import dev.miguelehr.truequeropa.model.UserRequest
import dev.miguelehr.truequeropa.model.User
import dev.miguelehr.truequeropa.model.UserPost
import dev.miguelehr.truequeropa.model.UserPostsDetails
import dev.miguelehr.truequeropa.model.UserProfile
import dev.miguelehr.truequeropa.model.UserRequestDetails
import kotlinx.coroutines.tasks.await

//FirestoreManager
object FirestoreManager {
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    // users/{uid}
    fun createUserProfile(
        uid: String,
        nombre: String,
        email: String,
        onComplete: (Boolean, String?) -> Unit
    ) {
        val data = hashMapOf(
            "uid" to uid,
            "nombre" to nombre,
            "email" to email,
            "createdAt" to FieldValue.serverTimestamp()
        )
        db.collection("users").document(uid)
            .set(data)
            .addOnSuccessListener { onComplete(true, null) }
            .addOnFailureListener { onComplete(false, it.localizedMessage) }
    }
    // ---------- CREAR PUBLICACIÓN DE USUARIO ----------
    fun createUserPost(
        uid: String,
        prendaId : String,
        titulo: String,
        descripcion: String,
        categoria: String,
        talla: String,
        estado: String,
        imageUrls: List<String>,
        onComplete: (Boolean, String?) -> Unit
    ) {
        val data = hashMapOf(
            "userId" to uid,
            "prendaId" to prendaId,
            "titulo" to titulo,
            "descripcion" to descripcion,
            "categoria" to categoria,
            "talla" to talla,
            "estado" to estado,
            "imageUrls" to imageUrls,
            "createdAt" to FieldValue.serverTimestamp()
        )

        db.collection("posts")
            .add(data)
            .addOnSuccessListener { onComplete(true, null) }
            .addOnFailureListener { e -> onComplete(false, e.localizedMessage) }
    }

    // ---------- ESCUCHAR PUBLICACIONES DE UN USUARIO ----------
    fun listenPostsForUser(
        uid: String,
        onChange: (List<UserPost>, String?) -> Unit
    ): ListenerRegistration {
        return db.collection("posts")
            .whereEqualTo("userId", uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, e ->
                if (e != null) {
                    onChange(emptyList(), e.localizedMessage)
                    return@addSnapshotListener
                }

                val posts = snap?.documents?.map { doc ->
                    UserPost(
                        id = doc.id,
                        userId = doc.getString("userId") ?: "",
                        prendaId = doc.getString("prendaId") ?: "",
                        titulo = doc.getString("titulo") ?: "",
                        descripcion = doc.getString("descripcion") ?: "",
                        categoria = doc.getString("categoria") ?: "",
                        talla = doc.getString("talla") ?: "",
                        estado = doc.getString("estado") ?: "",
                        imageUrls = (doc.get("imageUrls") as? List<*>)?.filterIsInstance<String>()
                            ?: emptyList(),
                        createdAt = doc.getTimestamp("createdAt")
                    )
                } ?: emptyList()

                onChange(posts, null)
            }
    }
    fun deleteUserPost(
        postId: String,
        onComplete: (Boolean, String?) -> Unit
    ) {
        db.collection("posts")
            .document(postId)
            .delete()
            .addOnSuccessListener { onComplete(true, null) }
            .addOnFailureListener { e -> onComplete(false, e.localizedMessage) }
    }

    fun createUserRequest(
        solicitudId : String,
        userIdPropietario : String,
        userIdSolicitante : String,
        prendaIdPropietario : String,
        prendaIdSolicitante : String,
        fechaAprobacion : Timestamp,
        estado : String,
        onComplete: (Boolean, String?) -> Unit
    )
    {
        val data = hashMapOf(
            "solicitudId" to solicitudId,
            "userIdPropietario" to userIdPropietario,
            "userIdSolicitante" to userIdSolicitante,
            "prendaIdPropietario" to prendaIdPropietario,
            "prendaIdSolicitante" to prendaIdSolicitante,
            "fechaAprobacion" to fechaAprobacion,
            "estado" to estado,
            "createdAt" to FieldValue.serverTimestamp()
        )

        db.collection("request")
            .add(data)
            .addOnSuccessListener { onComplete(true, null) }
            .addOnFailureListener { e -> onComplete(false, e.localizedMessage) }
    }

    suspend fun UpdateEstadoUserRequest(
        requestId : String,
        estado : String
    )
    {

        val requestDoc = db.collection("request").document(requestId)

        // Prepara los datos a actualizar
        val updates = mapOf(
            "estado" to estado,      // 1 para Aprobado
            "fechaAprobacion" to FieldValue.serverTimestamp() // Opcional: guarda la fecha de aprobación
        )

        requestDoc.update(updates).await()
    }

    suspend fun UpdatePostSolicitante(
        requestId : String,
        postId: String
    ): Boolean
    {
        return try {
            val requestDoc = db.collection("request").document(requestId)
            val updates = mapOf(
                "postIdSolicitante" to postId,
                "createdAt" to FieldValue.serverTimestamp()
            )
            requestDoc.update(updates).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getUserRequestDetails(requestId: String): UserRequestDetails? {
        val requestDoc = db.collection("request").document(requestId).get().await()
        val request = requestDoc.toObject(UserRequest::class.java) ?: return null

        val requestWithId = request.copy(id = requestDoc.id)

        val propietarioPostDoc = db.collection("posts").document(request.postIdPropietario).get().await()
        val propietarioPost = propietarioPostDoc.toObject(UserPost::class.java) ?: return null

        val solicitantePostDoc = db.collection("posts").document(request.postIdSolicitante).get().await()
        val solicitantePost = solicitantePostDoc.toObject(UserPost::class.java) ?: return null

        val propietarioProfileDoc = db.collection("users").document(propietarioPost.userId).get().await()
        val propietarioProfile = propietarioProfileDoc.toObject(UserProfile::class.java) ?: return null

        val solicitanteProfileDoc = db.collection("users").document(solicitantePost.userId).get().await()
        val solicitanteProfile = solicitanteProfileDoc.toObject(UserProfile::class.java) ?: return null

        return UserRequestDetails(
            requestWithId,
            propietarioProfile,
            solicitanteProfile,
            propietarioPost,
            solicitantePost
        )
    }

    suspend fun getAllUserRequestDetailsForUser(userId: String): List<UserRequestDetails> {
        val TAG = "FirestoreManager"
        Log.d(TAG, "Buscando solicitudes para el userId: $userId")

        val requests = mutableListOf<UserRequestDetails>()
        val processedRequestIds = mutableSetOf<String>() // Para evitar duplicados

        try {
            // Primero, obtenemos todos los posts del usuario.
            val userPostsSnapshot = db.collection("posts")
                .whereEqualTo("userId", userId)
                .get()
                .await()

            val userPostIds = userPostsSnapshot.documents.map { it.id }

            if (userPostIds.isEmpty()) {
                Log.d(TAG, "El usuario $userId no tiene posts, no se pueden encontrar solicitudes.")
                return emptyList()
            }

            // --- Consulta 1: Solicitudes donde el usuario es el PROPIETARIO de la prenda ---
            val requestsAsOwnerSnapshot = db.collection("request")
                .whereIn("postIdPropietario", userPostIds)
                .get()
                .await()

            Log.d(TAG, "Encontradas ${requestsAsOwnerSnapshot.size()} solicitudes como propietario.")

            for (document in requestsAsOwnerSnapshot.documents) {
                if (processedRequestIds.add(document.id)) { // Añade si no existe
                    val details = getUserRequestDetails(document.id)
                    if (details != null) {
                        requests.add(details)
                    } else {
                        Log.w(TAG, "No se pudieron obtener detalles para la solicitud (como propietario): ${document.id}")
                    }
                }
            }

            // --- Consulta 2: Solicitudes donde el usuario es el SOLICITANTE de la prenda ---
            val requestsAsRequesterSnapshot = db.collection("request")
                .whereIn("postIdSolicitante", userPostIds)
                .get()
                .await()

            Log.d(TAG, "Encontradas ${requestsAsRequesterSnapshot.size()} solicitudes como solicitante.")

            for (document in requestsAsRequesterSnapshot.documents) {
                if (processedRequestIds.add(document.id)) { // Añade si no existe
                    val details = getUserRequestDetails(document.id)
                    if (details != null) {
                        requests.add(details)
                    } else {
                        Log.w(TAG, "No se pudieron obtener detalles para la solicitud (como solicitante): ${document.id}")
                    }
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error al obtener las solicitudes de usuario", e)
        }

        Log.d(TAG, "Retornando ${requests.size} solicitudes detalladas en total.")
        return requests.sortedByDescending { it.request.createdAt } // Opcional: ordenar por fecha
    }

    suspend fun getUserPostDetails(PostsId: String): UserPostsDetails? {

        val solicitantePostDoc = db.collection("posts").document(PostsId).get().await()
        val solicitantePost = solicitantePostDoc.toObject(UserPost::class.java) ?: return null

        val postsWithId = solicitantePost.copy(id = solicitantePostDoc.id)


        val solicitanteProfileDoc = db.collection("users").document(solicitantePost.userId).get().await()
        val solicitanteProfile = solicitanteProfileDoc.toObject(UserProfile::class.java) ?: return null

        return UserPostsDetails(
            solicitanteProfile,
            postsWithId
        )
    }

    suspend fun getAllUserPostDetailsForUser(userId: String): List<UserPostsDetails> {
        val TAG = "FirestoreManager"
        Log.d(TAG, "Buscando prendas para el userId: $userId")

        val posts = mutableListOf<UserPostsDetails>()
        val processedPostsIds = mutableSetOf<String>() // Para evitar duplicados

        try {
            // Primero, obtenemos todos los posts del usuario.
            val userPostsSnapshot = db.collection("posts")
                .whereEqualTo("userId", userId)
                .get()
                .await()

            val userPostIds = userPostsSnapshot.documents.map { it.id }

            if (userPostIds.isEmpty()) {
                Log.d(TAG, "El usuario $userId no tiene posts, no se pueden encontrar solicitudes.")
                return emptyList()
            }

            for (document in userPostsSnapshot.documents) {
                if (processedPostsIds.add(document.id)) { // Añade si no existe
                    val details = getUserPostDetails(document.id)
                    if (details != null) {
                        posts.add(details)
                    } else {
                        Log.w(TAG, "No se pudieron obtener detalles para la solicitud (como propietario): ${document.id}")
                    }
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error al obtener las solicitudes de usuario", e)
        }

        return posts.sortedByDescending { it.solicitantePost.createdAt } // .sortedByDescending { it.request.createdAt }
    }
}