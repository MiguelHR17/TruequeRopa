package dev.miguelehr.truequeropa.model

import android.R
import com.google.firebase.Timestamp


data class UserRequest(
    val id: String = "",
    val postIdPropietario: String="",
    val postIdSolicitante: String="",
    val fechaAprobacion: Timestamp? = null,
    val estado: String ="0",
    val createdAt: Timestamp? = null,
    val reviewed: Boolean = false
)
data class UserRequestDetails(

    val request: UserRequest,
    val propietarioProfile: UserProfile,
    val solicitanteProfile: UserProfile,
    val propietarioPost: UserPost,
    val solicitantePost: UserPost
)

data class UserPostsDetails(
    val solicitanteProfile: UserProfile,
    val solicitantePost: UserPost
)