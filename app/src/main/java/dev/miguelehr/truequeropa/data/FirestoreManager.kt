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

object FirestoreManager {

    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    // ---------- PERFIL DE USUARIO ----------

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
            "active" to true,
            "restricted" to false
        )
        db.collection("users").document(uid)
            .set(data)
            .addOnSuccessListener { onComplete(true, null) }
            .addOnFailureListener { onComplete(false, it.localizedMessage) }
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
                    onComplete(true)
                } else {
                    val data = hashMapOf(
                        "uid" to uid,
                        "email" to (email ?: ""),
                        "nombre" to (nombre ?: ""),
                        "active" to true,
                        "restricted" to false,
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

    suspend fun getUser(uid: String): UserProfile? {
        val doc = db.collection("users").document(uid).get().await()
        return doc.toObject(UserProfile::class.java)
    }

    suspend fun updateUserPhoto(uid: String, photoUrl: String): Boolean {
        return try {
            db.collection("users")
                .document(uid)
                .update("photoUrl", photoUrl)
                .await()
            true
        } catch (e: Exception) {
            Log.e("FirestoreManager", "Error al actualizar foto de usuario", e)
            false
        }
    }

    // Cambiar solo el estado active (ocultar / permitir login)
    suspend fun setUserActive(uid: String, active: Boolean): Boolean {
        return try {
            db.collection("users")
                .document(uid)
                .update("active", active)
                .await()

            // Si lo desactivo, oculto todos sus posts; si lo activo, los vuelvo a mostrar
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

    // Cambiar flag restricted (para bloquear publicar/proponer trueques)
    suspend fun setUserRestricted(uid: String, restricted: Boolean): Boolean {
        return try {
            db.collection("users")
                .document(uid)
                .update("restricted", restricted)
                .await()
            true
        } catch (e: Exception) {
            Log.e("FirestoreManager", "Error al cambiar restricted de usuario", e)
            false
        }
    }

    // ---------- POSTS (PUBLICACIONES) ----------

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
            "id" to prendaId,                 // opcional, útil tenerlo también en el doc
            "userId" to uid,
            "prendaId" to prendaId,
            "titulo" to titulo,
            "descripcion" to descripcion,
            "categoria" to categoria,
            "talla" to talla,
            "estado" to estado,
            "imageUrls" to imageUrls,
            "estadoTrueque" to estadoTrueque,
            "hidden" to false,
            "createdAt" to FieldValue.serverTimestamp()
        )

        db.collection("posts")
            .document(prendaId)              // 👈 AQUÍ ESTÁ EL CAMBIO CLAVE
            .set(data)
            .addOnSuccessListener { onComplete(true, null) }
            .addOnFailureListener { e -> onComplete(false, e.localizedMessage) }
    }

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
                        estadoTrueque = doc.getString("estadoTrueque") ?: "0",
                        hidden = doc.getBoolean("hidden") ?: false,
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

    suspend fun UpdatePost(
        postId: String,
        postValue: String
    ): Boolean {
        return try {
            db.collection("posts").document(postId)
                .update("estadoTrueque", postValue)
                .await()
            true
        } catch (e: Exception) {
            Log.e("update", "Error al actualizar el estado del post", e)
            false
        }
    }

    suspend fun updatePostDescription(
        postId: String,
        newDescription: String
    ): Boolean {
        return try {
            db.collection("posts")
                .document(postId)
                .update("descripcion", newDescription)
                .await()
            true
        } catch (e: Exception) {
            Log.e("FirestoreManager", "Error al actualizar descripción de post", e)
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

    suspend fun getAllAvailablePosts(): List<UserPostsDetails> {
        val posts = mutableListOf<UserPostsDetails>()
        try {
            val snap = db.collection("posts")
                .whereEqualTo("estadoTrueque", "0")
                .whereEqualTo("hidden", false)
                .get()
                .await()

            for (doc in snap.documents) {
                val post = doc.toObject(UserPost::class.java)?.copy(id = doc.id)
                if (post != null) {
                    posts.add(UserPostsDetails(post))
                }
            }
            posts.sortByDescending { it.solicitantePost.createdAt }
        } catch (e: Exception) {
            Log.e("FirestoreManager", "Error al obtener todos los posts", e)
        }
        return posts
    }

    suspend fun getPostWithUserDetails(postId: String): Pair<UserPost, UserProfile>? {
        return try {
            val postDoc = db.collection("posts").document(postId).get().await()
            if (!postDoc.exists()) return null
            val post = postDoc.toObject(UserPost::class.java)?.copy(id = postDoc.id)
                ?: return null

            var userProfile = getUser(post.userId)
            if (userProfile == null) {
                userProfile = UserProfile(
                    uid = post.userId,
                    nombre = "Usuario desconocido",
                    email = "Sin email"
                )
            }
            Pair(post, userProfile)
        } catch (e: Exception) {
            Log.e("FirestoreManager", "Error al obtener post con detalles", e)
            null
        }
    }

    // ---------- REQUESTS (TRUEQUES) ----------

    fun createUserRequest(
        postIdPropietario: String,
        postIdSolicitante: String,
        estado: String,
        onComplete: (Boolean) -> Unit
    ): Boolean {
        return try {
            val data = hashMapOf(
                "postIdPropietario" to postIdPropietario,
                "postIdSolicitante" to postIdSolicitante,
                "estado" to estado,
                "createdAt" to FieldValue.serverTimestamp()
            )
            db.collection("request")
                .add(data)
                .addOnSuccessListener { onComplete(true) }
                .addOnFailureListener { onComplete(false) }
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun UpdateEstadoUserRequest(
        requestId: String,
        estado: String
    ) {
        val requestDoc = db.collection("request").document(requestId)
        val updates = mapOf(
            "estado" to estado,
            "fechaAprobacion" to FieldValue.serverTimestamp()
        )
        requestDoc.update(updates).await()
    }

    suspend fun UpdatePostSolicitante(
        requestId: String,
        postId: String
    ): Boolean {
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

        val propietarioPostDoc =
            db.collection("posts").document(request.postIdPropietario).get().await()
        val propietarioPost =
            propietarioPostDoc.toObject(UserPost::class.java)?.copy(id = propietarioPostDoc.id)
                ?: return null

        val solicitantePostDoc =
            db.collection("posts").document(request.postIdSolicitante).get().await()
        val solicitantePost =
            solicitantePostDoc.toObject(UserPost::class.java)?.copy(id = solicitantePostDoc.id)
                ?: return null

        val solicitanteProfileDoc =
            db.collection("users").document(solicitantePost.userId).get().await()
        val solicitanteProfile =
            solicitanteProfileDoc.toObject(UserProfile::class.java) ?: return null

        val propietarioProfileDoc =
            db.collection("users").document(propietarioPost.userId).get().await()
        val propietarioProfile =
            propietarioProfileDoc.toObject(UserProfile::class.java) ?: return null

        return UserRequestDetails(
            requestWithId,
            propietarioProfile,
            solicitanteProfile,
            propietarioPost,
            solicitantePost
        )
    }

    suspend fun getAllUserRequestDetailsForUser(userId: String, report: Int): List<UserRequestDetails> {
        val requests = mutableListOf<UserRequestDetails>()
        val processedIds = mutableSetOf<String>()

        try {
            val userPostsSnapshot = db.collection("posts")
                .whereEqualTo("userId", userId)
                .get()
                .await()

            val userPostIds = userPostsSnapshot.documents.map { it.id }
            if (userPostIds.isEmpty()) return emptyList()

            val asOwnerSnap = db.collection("request")
                .whereIn("postIdPropietario", userPostIds)
                .get()
                .await()

            for (doc in asOwnerSnap.documents) {
                if (processedIds.add(doc.id)) {
                    val details = getUserRequestDetails(doc.id)
                    if (details != null) requests.add(details)
                }
            }

            if (report == 1) {
                val asRequesterSnap = db.collection("request")
                    .whereIn("postIdSolicitante", userPostIds)
                    .get()
                    .await()

                for (doc in asRequesterSnap.documents) {
                    if (processedIds.add(doc.id)) {
                        val details = getUserRequestDetails(doc.id)
                        if (details != null) requests.add(details)
                    }
                }
            }
        } catch (_: Exception) { }

        return requests.sortedByDescending { it.request.createdAt }
    }

    // ---------- POSTS PARA ELEGIR EN TRUEQUE ----------

    suspend fun getUserPostDetails(postId: String): UserPostsDetails? {
        val doc = db.collection("posts").document(postId).get().await()
        val post = doc.toObject(UserPost::class.java) ?: return null
        return UserPostsDetails(post.copy(id = doc.id))
    }

    suspend fun getAllUserPostDetailsForUser(userId: String): List<UserPostsDetails> {
        val posts = mutableListOf<UserPostsDetails>()
        val processedIds = mutableSetOf<String>()

        try {
            val snap = db.collection("posts")
                .whereEqualTo("userId", userId)
                .whereEqualTo("estadoTrueque", "0")
                .whereEqualTo("hidden", false)
                .get()
                .await()

            for (doc in snap.documents) {
                if (processedIds.add(doc.id)) {
                    val details = getUserPostDetails(doc.id)
                    if (details != null) posts.add(details)
                }
            }
        } catch (_: Exception) { }

        return posts.sortedByDescending { it.solicitantePost.createdAt }
    }
}