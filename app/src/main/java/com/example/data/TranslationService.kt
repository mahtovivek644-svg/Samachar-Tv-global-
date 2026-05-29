package com.example.data

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object TranslationService {
    private const val TAG = "TranslationService"
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val mediaType = "application/json; charset=utf-8".toMediaType()

    /**
     * Translates a news item title, subtitle, and description to the target language using Gemini 3.5 Flash.
     */
    suspend fun translateNews(
        title: String,
        subtitle: String,
        description: String,
        targetLanguage: String
    ): TranslatedNews? = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "Gemini API Key is not set or is a placeholder")
            return@withContext null
        }

        // Prepare the system-directed instruction prompt
        val systemPrompt = "You are a professional broadcast news translator for Samachar TV Global. " +
                "Translate the following JSON object containing news details perfectly to $targetLanguage. " +
                "Keep proper nouns, names of states/cities like 'Jharkhand', 'Bihar', 'Ranchi', 'India' recognizable, but accurately localized. " +
                "Maintain the news flavor (e.g. breaking news style). " +
                "Ensure that the translation is completely natural and accurate. " +
                "You must output ONLY a valid raw JSON object matching the exact input keys: 'title', 'subtitle', and 'description'. " +
                "Do NOT include any markdown code blocks, do NOT write ```json, do NOT write any introductory or trailing explanations. Return only the raw JSON string content."

        val inputJsonObject = JSONObject().apply {
            put("title", title)
            put("subtitle", subtitle)
            put("description", description)
        }

        val requestUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

        val requestBodyJson = JSONObject().apply {
            val contentsArray = JSONArray().apply {
                val contentObj = JSONObject().apply {
                    val partsArray = JSONArray().apply {
                        val partObj = JSONObject().apply {
                            put("text", "Input news to translate:\n${inputJsonObject.toString()}")
                        }
                        put(partObj)
                    }
                    put("parts", partsArray)
                }
                put(contentObj)
            }
            put("contents", contentsArray)

            val systemInstructionObj = JSONObject().apply {
                val partsArray = JSONArray().apply {
                    val partObj = JSONObject().apply {
                        put("text", systemPrompt)
                    }
                    put(partObj)
                }
                put("parts", partsArray)
            }
            put("systemInstruction", systemInstructionObj)

            // Dynamic format response schema
            val configObj = JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", 0.3)
            }
            put("generationConfig", configObj)
        }

        try {
            val request = Request.Builder()
                .url(requestUrl)
                .post(requestBodyJson.toString().toRequestBody(mediaType))
                .header("Content-Type", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Failed request to Gemini API: ${response.code} - ${response.message}")
                    return@withContext null
                }

                val bodyString = response.body?.string() ?: return@withContext null
                val rootJson = JSONObject(bodyString)
                val candidatesArray = rootJson.optJSONArray("candidates")
                val firstCandidateObj = candidatesArray?.optJSONObject(0)
                val contentObj = firstCandidateObj?.optJSONObject("content")
                val partsArray = contentObj?.optJSONArray("parts")
                val firstPartObj = partsArray?.optJSONObject(0)
                val textResponse = firstPartObj?.optString("text")?.trim()

                if (!textResponse.isNullOrEmpty()) {
                    val cleanedJson = cleanJsonResponse(textResponse)
                    val resultJson = JSONObject(cleanedJson)
                    return@withContext TranslatedNews(
                        title = resultJson.optString("title", title),
                        subtitle = resultJson.optString("subtitle", subtitle),
                        description = resultJson.optString("description", description)
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error performing dynamic translation via Gemini", e)
        }
        return@withContext null
    }

    private fun cleanJsonResponse(rawResponse: String): String {
        var clean = rawResponse
        if (clean.startsWith("```json")) {
            clean = clean.removePrefix("```json")
        } else if (clean.startsWith("```")) {
            clean = clean.removePrefix("```")
        }
        if (clean.endsWith("```")) {
            clean = clean.removeSuffix("```")
        }
        return clean.trim()
    }
}

data class TranslatedNews(
    val title: String,
    val subtitle: String,
    val description: String
)
