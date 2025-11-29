package dev.miguelehr.truequeropa.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.miguelehr.truequeropa.data.FirestoreManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class OffersState(
    val allPosts: List<UserPostsDetails> = emptyList(),
    val filteredPosts: List<UserPostsDetails> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class OffersViewModel : ViewModel() {

    private val _offersState = MutableStateFlow(OffersState())
    val offersState: StateFlow<OffersState> = _offersState.asStateFlow()

    fun loadAllAvailablePosts() {
        viewModelScope.launch {
            _offersState.value = _offersState.value.copy(isLoading = true, error = null)

            try {
                // Aquí cargamos TODOS los posts disponibles (no solo de un usuario)
                // Necesitamos modificar FirestoreManager para esto
                val posts = FirestoreManager.getAllAvailablePosts()

                _offersState.value = OffersState(
                    allPosts = posts,
                    filteredPosts = posts,
                    isLoading = false
                )
            } catch (e: Exception) {
                _offersState.value = _offersState.value.copy(
                    isLoading = false,
                    error = e.localizedMessage ?: "Error al cargar ofertas"
                )
            }
        }
    }

    fun filterPosts(
        query: String,
        categoria: String? = null,
        talla: String? = null
    ) {
        val filtered = _offersState.value.allPosts.filter { postDetail ->
            val post = postDetail.solicitantePost

            // Filtro de texto (busca en título y descripción)
            val matchesQuery = if (query.isBlank()) true else {
                post.titulo.contains(query, ignoreCase = true) ||
                        post.descripcion.contains(query, ignoreCase = true)
            }

            // Filtro de categoría
            val matchesCategoria = categoria?.let {
                post.categoria.equals(it, ignoreCase = true)
            } ?: true

            // Filtro de talla
            val matchesTalla = talla?.let {
                post.talla.equals(it, ignoreCase = true)
            } ?: true

            matchesQuery && matchesCategoria && matchesTalla
        }

        _offersState.value = _offersState.value.copy(filteredPosts = filtered)
    }

    fun clearError() {
        _offersState.value = _offersState.value.copy(error = null)
    }
}