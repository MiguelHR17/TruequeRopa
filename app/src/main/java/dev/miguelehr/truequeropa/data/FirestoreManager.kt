package dev.miguelehr.truequeropa.data

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

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
}