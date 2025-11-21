package dev.miguelehr.truequeropa.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FieldValue
import dev.miguelehr.truequeropa.data.FirestoreManager
import dev.miguelehr.truequeropa.model.UserRequestDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class UserRequestsViewModel : ViewModel() {

    private val _userRequests = MutableStateFlow<List<UserRequestDetails>>(emptyList())
    val userRequests: StateFlow<List<UserRequestDetails>> = _userRequests

    fun fetchUserRequests(userId: String) {
        viewModelScope.launch {
            val requests = FirestoreManager.getAllUserRequestDetailsForUser(userId)
            _userRequests.value = requests
        }
    }

    fun acceptRequest(requestId: String,userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val requestUpd = FirestoreManager.UpdateEstadoUserRequest(requestId,"1")
            val requests = FirestoreManager.getAllUserRequestDetailsForUser(userId)
            _userRequests.value = requests
        }
    }

    // (Opcional) Haz lo mismo para rechazar
    fun rejectRequest(requestId: String,userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val requestUpd = FirestoreManager.UpdateEstadoUserRequest(requestId,"2")
            val requests = FirestoreManager.getAllUserRequestDetailsForUser(userId)
            _userRequests.value = requests
        }
    }
}