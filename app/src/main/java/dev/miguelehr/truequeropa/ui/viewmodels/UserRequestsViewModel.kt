package dev.miguelehr.truequeropa.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FieldValue
import dev.miguelehr.truequeropa.auth.FirebaseMockLinker
import dev.miguelehr.truequeropa.data.FirestoreManager
import dev.miguelehr.truequeropa.model.UserPostsDetails
import dev.miguelehr.truequeropa.model.UserProfile
import dev.miguelehr.truequeropa.model.UserRequestDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UserRequestsViewModel : ViewModel() {

    private val _userRequests = MutableStateFlow<List<UserRequestDetails>>(emptyList())
    private val _userPosts = MutableStateFlow<List<UserPostsDetails>>(emptyList())
    val userRequests: StateFlow<List<UserRequestDetails>> = _userRequests
    val userPosts: StateFlow<List<UserPostsDetails>> = _userPosts

    suspend fun createRequest(
        postIdPropietario: String,
        postIdSolicitante: String,
        estado: String
    ): Boolean {
        // withContext(Dispatchers.IO) asegura que esta operación de red se ejecute en un hilo de fondo.
        return withContext(Dispatchers.IO) {
            FirestoreManager.createUserRequest(postIdPropietario, postIdSolicitante, estado){ ok ->

            }
        }
    }

    fun launchCreateRequest(
        postIdPropietario: String,
        postIdSolicitante: String,
        estado: String,
        onComplete: (Boolean) -> Unit // Callback para notificar el resultado a la UI
    ) {
        viewModelScope.launch {
            // Llama a TU PROPIA función suspend 'createRequest'
            val success = createRequest(postIdPropietario, postIdSolicitante, estado)
            if (success) {
            // Usa el callback 'onComplete' para devolver el resultado
            onComplete(success)
            } else {
            onComplete(false)
            }
        }
    }

    fun fetchUserRequests(userId: String,report: Int ) {
        viewModelScope.launch {
            val requests = FirestoreManager.getAllUserRequestDetailsForUser(userId,report)
            _userRequests.value = requests
        }
    }

    fun fetchUserPosts(userId: String) {
        viewModelScope.launch {
            val posts = FirestoreManager.getAllUserPostDetailsForUser(userId)
            _userPosts.value = posts
        }
    }

    fun acceptRequest(requestId: String,userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val requestUpd = FirestoreManager.UpdateEstadoUserRequest(requestId,"1")
            val requests = FirestoreManager.getAllUserRequestDetailsForUser(userId,0)
            _userRequests.value = requests
        }
    }

    fun rejectRequest(requestId: String,userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val requestUpd = FirestoreManager.UpdateEstadoUserRequest(requestId,"2")
            val requests = FirestoreManager.getAllUserRequestDetailsForUser(userId,0)
            _userRequests.value = requests
        }
    }

    suspend fun updPostRequestSolicitante(requestId: String,postId: String): Int {
            val success = FirestoreManager.UpdatePostSolicitante(requestId,postId)
            return if (success) 1 else 0
    }

    suspend fun updPost(postId: String,postValue: String): Int {
        val success = FirestoreManager.UpdatePost (postId,postValue)
        return if (success) 1 else 0
    }

    suspend fun selUser(uid: String): UserProfile? {
        val successUser = FirestoreManager.getUser(uid)
        return successUser
    }
}