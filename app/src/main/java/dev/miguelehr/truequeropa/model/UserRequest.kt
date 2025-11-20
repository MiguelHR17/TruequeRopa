package dev.miguelehr.truequeropa.model

import com.google.firebase.Timestamp

data class UserRequest(
    val solicitudId: String="",
    val userIdPropietario: String="",
    val userIdSolicitante: String="",
    val prendaIdPropietario: String="",
    val prendaIdSolicitante: String="",
    val fechaSolicitud: Timestamp? = null,
    val estado: String="",
    val createdAt: Timestamp? = null
)
