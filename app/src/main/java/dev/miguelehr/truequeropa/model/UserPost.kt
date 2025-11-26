package dev.miguelehr.truequeropa.model

import com.google.firebase.Timestamp

data class UserPost(
    val id: String = "",            // id del documento en Firestore
    val userId: String = "",        // uid de Firebase del dueño
    val prendaId :String = "",
    val titulo: String = "",
    val descripcion: String = "",
    val categoria: String = "",     // guardamos el name() del enum
    val talla: String = "",         // igual: Size.M -> "M"
    val estado: String = "",        // Condition.USADO -> "USADO"
    val imageUrls: List<String> = emptyList(),
    val estadoTrueque: String = "0",
    val createdAt: Timestamp? = null
)