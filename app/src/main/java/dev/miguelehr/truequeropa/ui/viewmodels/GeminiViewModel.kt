package dev.miguelehr.truequeropa.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.miguelehr.truequeropa.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

data class GeminiUiState(
    val prompt: String = "",
    val answer: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

class GeminiViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(GeminiUiState())
    val uiState: StateFlow<GeminiUiState> = _uiState.asStateFlow()

    private val client = OkHttpClient()
    private val mediaType = "application/json; charset=utf-8".toMediaType()

    private val apiKey: String = BuildConfig.GEMINI_API_KEY
    // 👇 Nombre correcto del modelo para la REST API
    private val modelName = "gemini-2.5-flash"

    private val baseUrl =
        "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent"

    fun onPromptChange(newPrompt: String) {
        _uiState.value = _uiState.value.copy(
            prompt = newPrompt,
            error = null
        )
    }

    fun ask() {
        val promptText = _uiState.value.prompt.trim()
        if (promptText.isBlank()) return

        viewModelScope.launch(Dispatchers.IO) {

            // 🔹 Validar API key
            if (apiKey.isBlank()) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "API key de Gemini vacía. Revisa local.properties (gemini.api.key)."
                )
                return@launch
            }

            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null,
                answer = ""
            )

            try {
                // ---- Construir JSON de la petición ----
                val partsArray = JSONArray().apply {
                    put(JSONObject().put("text", promptText))
                }
                val contentObj = JSONObject().apply {
                    put("parts", partsArray)
                }
                val contentsArray = JSONArray().apply {
                    put(contentObj)
                }
                val root = JSONObject().apply {
                    put("contents", contentsArray)
                }

                val body = root.toString().toRequestBody(mediaType)

                val url = "$baseUrl?key=$apiKey"

                val request = Request.Builder()
                    .url(url)
                    .post(body)
                    .build()

                client.newCall(request).execute().use { response ->
                    val rawBody = response.body?.string().orEmpty()

                    if (!response.isSuccessful) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Error HTTP ${response.code}: $rawBody"
                        )
                        return@use
                    }

                    // ---- Parsear respuesta de Gemini ----
                    val json = JSONObject(rawBody)
                    val candidates = json.optJSONArray("candidates")

                    if (candidates == null || candidates.length() == 0) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Gemini no devolvió candidatos."
                        )
                        return@use
                    }

                    val text = candidates
                        .getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text")

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        answer = text.trim(),
                        error = null
                    )
                }

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error: ${e.localizedMessage ?: e.toString()}"
                )
            }
        }
    }
}
