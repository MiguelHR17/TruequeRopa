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
            "createdAt" to FieldValue.serverTimestamp(),
            "active" to true                // ✅ NUEVO: usuario activo por defecto
        )
        db.collection("users").document(uid)
            .set(data)
            .addOnSuccessListener { onComplete(true, null) }
            .addOnFailureListener { onComplete(false, it.localizedMessage) }
    }
    // ---------- CREAR PUBLICACIÓN DE USUARIO ----------
    fun createUserPost(
        uid: String,
        prendaId: String,
        titulo: String,
        descripcion: String,
        categoria: String,
        talla: String,
        estado: String,
        imageUrls: List<String>,
        estadoTrueque: String,
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
            "estadoTrueque" to estadoTrueque,
            "hidden" to false,                    // ✅ NUEVO: visible por defecto
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
            .whereEqualTo("hidden", false)  // ✅ sólo posts visibles
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
                        estadoTrueque = doc.getString("estadoTrueque") ?: "0",
                        hidden = doc.getBoolean("hidden") ?: false,   // ✅
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

    suspend fun UpdatePost(
        postId: String,
        postValue: String
    ): Boolean
    {
        return try {
            val postDoc = db.collection("posts").document(postId)

            val updates = mapOf(
                "estadoTrueque" to postValue,
            )
            postDoc.update(updates).await()
            true
        } catch (e: Exception) {
            Log.e("update", "Error al obtener las solicitudes de usuario", e)
            false
        }
    }

    suspend fun getUser(uid: String): UserProfile? {

        val propietarioProfileDoc = db.collection("users").document(uid).get().await()
        val propietarioProfile = propietarioProfileDoc.toObject(UserProfile::class.java) ?: return null

        return propietarioProfile
    }

    suspend fun getUserRequestDetails(requestId: String): UserRequestDetails? {
        val requestDoc = db.collection("request").document(requestId).get().await()
        val request = requestDoc.toObject(UserRequest::class.java) ?: return null

        val requestWithId = request.copy(id = requestDoc.id)

        val propietarioPostDoc = db.collection("posts").document(request.postIdPropietario).get().await()
        val propietarioPost = propietarioPostDoc.toObject(UserPost::class.java) ?: return null
        val propietarioPostWithId = propietarioPost.copy(id = propietarioPostDoc.id)

        val solicitantePostDoc = db.collection("posts").document(request.postIdSolicitante).get().await()
        val solicitantePost = solicitantePostDoc.toObject(UserPost::class.java) ?: return null
        val solicitantePostWithId = solicitantePost.copy(id = solicitantePostDoc.id)

        val solicitanteProfileDoc = db.collection("users").document(solicitantePost.userId).get().await()
        val solicitanteProfile = solicitanteProfileDoc.toObject(UserProfile::class.java) ?: return null

        val propietarioProfileDoc = db.collection("users").document(propietarioPost.userId).get().await()
        val propietarioProfile = propietarioProfileDoc.toObject(UserProfile::class.java) ?: return null


        return UserRequestDetails(
            requestWithId,
            propietarioProfile,
            solicitanteProfile,
            propietarioPostWithId,
            solicitantePostWithId
        )
    }

    suspend fun getAllUserRequestDetailsForUser(userId: String,report: Int): List<UserRequestDetails> {

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
                return emptyList()
            }

            // --- Consulta 1: Solicitudes donde el usuario es el PROPIETARIO de la prenda ---
            val requestsAsOwnerSnapshot = db.collection("request")
                .whereIn("postIdPropietario", userPostIds)
                .get()
                .await()

            for (document in requestsAsOwnerSnapshot.documents) {
                if (processedRequestIds.add(document.id)) { // Añade si no existe
                    val details = getUserRequestDetails(document.id)
                    if (details != null) {
                        requests.add(details)
                    }
                }
            }

            // --- Consulta 2: Solicitudes donde el usuario es el SOLICITANTE de la prenda ---
            if(report == 1) {
                val requestsAsRequesterSnapshot = db.collection("request")
                    .whereIn("postIdSolicitante", userPostIds)
                    .get()
                    .await()

                for (document in requestsAsRequesterSnapshot.documents) {
                    if (processedRequestIds.add(document.id)) { // Añade si no existe
                        val details = getUserRequestDetails(document.id)
                        if (details != null) {
                            requests.add(details)
                        }
                    }
                }
            }

        } catch (e: Exception) {
            //Log.e(TAG, "Error al obtener las solicitudes de usuario", e)
        }

        return requests.sortedByDescending { it.request.createdAt } // Opcional: ordenar por fecha
    }

    suspend fun getUserPostDetails(PostsId: String): UserPostsDetails? {

        val solicitantePostDoc = db.collection("posts").document(PostsId).get().await()
        val solicitantePost = solicitantePostDoc.toObject(UserPost::class.java) ?: return null

        val postsWithId = solicitantePost.copy(id = solicitantePostDoc.id)

        return UserPostsDetails(
            postsWithId
        )
    }

    suspend fun getAllUserPostDetailsForUser(userId: String): List<UserPostsDetails> {

        val posts = mutableListOf<UserPostsDetails>()
        val processedPostsIds = mutableSetOf<String>() // Para evitar duplicados

        try {
            // Primero, obtenemos todos los posts del usuario.
            val userPostsSnapshot = db.collection("posts")
                .whereEqualTo("userId", userId)
                .whereEqualTo("estadoTrueque", "0")
                .get()
                .await()

            val userPostIds = userPostsSnapshot.documents.map { it.id }

            if (userPostIds.isEmpty()) {
                return emptyList()
            }

            for (document in userPostsSnapshot.documents) {
                if (processedPostsIds.add(document.id)) { // Añade si no existe
                    val details = getUserPostDetails(document.id)
                    if (details != null) {
                        posts.add(details)
                    }
                }
            }

        } catch (e: Exception) {
           // Log.e(TAG, "Error al obtener las solicitudes de usuario", e)
        }

        return posts.sortedByDescending { it.solicitantePost.createdAt } // .sortedByDescending { it.request.createdAt }
    }

    suspend fun setUserActive(uid: String, active: Boolean): Boolean {
        return try {
            // 1) Actualizar el estado del usuario
            db.collection("users")
                .document(uid)
                .update("active", active)
                .await()

            // 2) Si se desactiva, ocultar todos sus posts.
            //    Si se activa, volver a mostrarlos.
            val postsSnap = db.collection("posts")
                .whereEqualTo("userId", uid)
                .get()
                .await()

            for (doc in postsSnap.documents) {
                doc.reference.update("hidden", !active).await()
            }
            true
        } catch (e: Exception) {
            Log.e("FirestoreManager", "Error al cambiar estado de usuario", e)
            false
        }
    }

    suspend fun setPostHidden(postId: String, hidden: Boolean): Boolean {
        return try {
            db.collection("posts")
                .document(postId)
                .update("hidden", hidden)
                .await()
            true
        } catch (e: Exception) {
            Log.e("FirestoreManager", "Error al cambiar visibilidad del post", e)
            false
        }
    }
    fun ensureUserProfile(
        uid: String,
        email: String?,
        nombre: String? = null,
        onComplete: (Boolean) -> Unit
    ) {
        val docRef = db.collection("users").document(uid)

        docRef.get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    // Ya tiene perfil
                    onComplete(true)
                } else {
                    // Crear perfil mínimo
                    val data = hashMapOf(
                        "uid" to uid,
                        "email" to (email ?: ""),
                        "nombre" to (nombre ?: ""),
                        "active" to true,
                        "createdAt" to FieldValue.serverTimestamp()
                    )

                    docRef.set(data)
                        .addOnSuccessListener { onComplete(true) }
                        .addOnFailureListener { onComplete(false) }
                }
            }
            .addOnFailureListener {
                onComplete(false)
            }
    }
}