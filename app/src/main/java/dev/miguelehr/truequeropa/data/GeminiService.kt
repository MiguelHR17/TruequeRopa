package dev.miguelehr.truequeropa.data

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONArray
import org.json.JSONObject

object GeminiService {

    // ⚠️ PON AQUÍ TU API KEY DE GEMINI
    // Lo ideal es leerlo de BuildConfig o local.properties,
    // pero para hacerlo simple lo pondremos directo:
    private const val GEMINI_API_KEY = "TU_API_KEY_AQUI"

    private val client = OkHttpClient()

    /**
     * Llama a Gemini con un prompt de texto y devuelve el texto de respuesta.
     */
    fun askGemini(prompt: String): String {
        if (GEMINI_API_KEY.isBlank()) {
            return "Error: falta configurar GEMINI_API_KEY en GeminiService."
        }

        // Endpoint de Gemini (modelo rápido y barato)
        val url =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$GEMINI_API_KEY"

        // Construimos el JSON del body:
        // {
        //   "contents": [{
        //      "parts": [{"text": "tu prompt"}]
        //   }]
        // }
        val jsonBody = JSONObject().apply {
            val partsArray = JSONArray().apply {
                put(JSONObject().put("text", prompt))
            }
            val contentsArray = JSONArray().apply {
                put(JSONObject().put("parts", partsArray))
            }
            put("contents", contentsArray)
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = RequestBody.create(mediaType, jsonBody.toString())

        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                return "Error de red: código HTTP ${response.code}"
            }

            val bodyStr = response.body?.string() ?: return "Respuesta vacía de la API."

            // Parseamos:
            // candidates[0].content.parts[0].text
            return try {
                val root = JSONObject(bodyStr)
                val candidates = root.optJSONArray("candidates") ?: return "Sin candidatos en la respuesta."
                if (candidates.length() == 0) return "Sin candidatos en la respuesta."

                val firstCandidate = candidates.getJSONObject(0)
                val content = firstCandidate.getJSONObject("content")
                val parts = content.getJSONArray("parts")
                if (parts.length() == 0) return "Sin partes en el contenido."

                parts.getJSONObject(0).getString("text")
            } catch (e: Exception) {
                "Error al leer la respuesta de la API: ${e.localizedMessage}"
            }
        }
    }
}