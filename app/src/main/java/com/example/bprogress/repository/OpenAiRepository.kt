package com.example.bprogress.repository

import android.util.Log
import com.example.bprogress.data.ChatMessage
import com.example.bprogress.data.OpenAiChatRequest
import com.example.bprogress.remote.OpenAiApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

// Sealed class to represent the result of an OpenAI API call
sealed class OpenAiResult {
    data class Success(val message: String) : OpenAiResult()
    data class Error(val errorMessage: String, val errorCode: Int? = null) : OpenAiResult()
}

@Singleton
class OpenAiRepository @Inject constructor(
    private val openAiApiService: OpenAiApiService // Hilt will provide this
) {
    companion object {
        private const val TAG = "OpenAiRepository"
    }

    suspend fun generateMotivationalMessage(
        userPrompt: String,
        systemInstructions: String = "You are a helpful assistant that provides short, encouraging motivational messages.",
        model: String = "gpt-3.5-turbo", // Or your preferred model
        maxTokens: Int = 70 // Keep it concise for notifications
    ): OpenAiResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Attempting to fetch tip from OpenAI. Prompt: \"$userPrompt\"")
            val request = OpenAiChatRequest(
                model = model,
                messages = listOf(
                    ChatMessage(role = "system", content = systemInstructions),
                    ChatMessage(role = "user", content = userPrompt)
                ),
                maxTokens = maxTokens,
                temperature = 0.7 // Adjust as needed
            )

            // The API key is handled by the AuthInterceptor from NetworkModule
            val response = openAiApiService.getChatCompletions(request)

            if (response.isSuccessful) {
                val messageContent = response.body()?.choices?.firstOrNull()?.message?.content
                if (!messageContent.isNullOrBlank()) {
                    Log.i(TAG, "Successfully fetched message from OpenAI: $messageContent")
                    OpenAiResult.Success(messageContent.trim())
                } else {
                    Log.w(TAG, "OpenAI response was successful but content was empty/null.")
                    OpenAiResult.Error("OpenAI response was empty or malformed.")
                }
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown API error"
                Log.e(TAG, "OpenAI API error: ${response.code()} - $errorBody")
                OpenAiResult.Error("OpenAI API error: $errorBody", response.code())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during OpenAI API call: ${e.message}", e)
            OpenAiResult.Error("Network or unexpected error: ${e.localizedMessage}")
        }
    }
}
