package dev.miguelehr.truequeropa.model

import com.google.firebase.Timestamp

data class UserRequest(
    val postIdPropietario: String="",
    val postIdSolicitante: String="",
    val fechaAprobacion: Timestamp? = null,
    val estado: String="",
    val createdAt: Timestamp? = null
)
data class UserRequestDetails(
    val request: UserRequest,
    val propietarioProfile: UserProfile,
    val solicitanteProfile: UserProfile,
    val propietarioPost: UserPost,
    val solicitantePost: UserPost
)
