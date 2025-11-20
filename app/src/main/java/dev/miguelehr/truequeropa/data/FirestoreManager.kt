package dev.miguelehr.truequeropa.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import dev.miguelehr.truequeropa.model.UserPost

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

    fun CreateUserRequest(
        solicitudId: String,
        userIdPropietario: String,
        userIdSolicitante: String,
        prendaIdSolicitante: String,
        prendaIdPropietario: String,
        estado: String,
        fechaAprobacion: Timestamp,
        onComplete: (Boolean, String?) -> Unit
    ){
        val data = hashMapOf(
            "solicitudId" to solicitudId,
            "userIdPropietario" to userIdPropietario,
            "userIdSolicitante" to userIdSolicitante,
            "prendaIdSolicitante" to prendaIdSolicitante,
            "prendaIdPropietario" to prendaIdPropietario,
            "estado" to estado,
            "fechaAprobacion" to fechaAprobacion,
            "createdAt" to FieldValue.serverTimestamp()
        )
        db.collection("request")
            .add(data)
            .addOnSuccessListener { onComplete(true, null) }
            .addOnFailureListener { e -> onComplete(false, e.localizedMessage) }
    }

}