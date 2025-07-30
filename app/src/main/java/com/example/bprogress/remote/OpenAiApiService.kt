package com.example.bprogress.remote

import com.example.bprogress.data.OpenAiChatRequest
import com.example.bprogress.data.OpenAiChatResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
// @Header import no longer needed if interceptor handles auth

interface OpenAiApiService {
    @POST("v1/chat/completions")
    suspend fun getChatCompletions( // apiKey parameter removed
        @Body requestBody: OpenAiChatRequest
    ): Response<OpenAiChatResponse>
}
