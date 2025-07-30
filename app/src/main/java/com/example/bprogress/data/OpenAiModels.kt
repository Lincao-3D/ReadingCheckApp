//  data/OpenAiModels.kt
package com.example.bprogress.data // Or your preferred package

import com.google.gson.annotations.SerializedName

// --- Request Models ---

data class OpenAiChatRequest(
    @SerializedName("model") val model: String = "gpt-3.5-turbo", // Or "gpt-4", etc.
    @SerializedName("messages") val messages: List<ChatMessage>,
    @SerializedName("temperature") val temperature: Double = 0.7, // Controls randomness: Lower is more deterministic
    @SerializedName("max_tokens") val maxTokens: Int = 150 // Adjust as needed for paragraph length
)

data class ChatMessage(
    @SerializedName("role") val role: String, // "system", "user", or "assistant"
    @SerializedName("content") val content: String
)

// --- Response Models ---

data class OpenAiChatResponse(
    @SerializedName("id") val id: String?,
    @SerializedName("object") val objectType: String?,
    @SerializedName("created") val created: Long?,
    @SerializedName("model") val model: String?,
    @SerializedName("choices") val choices: List<Choice>?,
    @SerializedName("usage") val usage: Usage?,
    @SerializedName("error") val error: OpenAiError? // For capturing API errors
)

data class Choice(
    @SerializedName("index") val index: Int?,
    @SerializedName("message") val message: ChatMessage?,
    @SerializedName("finish_reason") val finishReason: String?
)

data class Usage(
    @SerializedName("prompt_tokens") val promptTokens: Int?,
    @SerializedName("completion_tokens") val completionTokens: Int?,
    @SerializedName("total_tokens") val totalTokens: Int?
)

data class OpenAiError(
    @SerializedName("message") val message: String?,
    @SerializedName("type") val type: String?,
    @SerializedName("param") val param: String?,
    @SerializedName("code") val code: String?
)