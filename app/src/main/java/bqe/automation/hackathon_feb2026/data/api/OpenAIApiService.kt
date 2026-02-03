package bqe.automation.hackathon_feb2026.data.api

import bqe.automation.hackathon_feb2026.data.model.OpenAIRequest
import bqe.automation.hackathon_feb2026.data.model.OpenAIResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface OpenAIApiService {
    
    @POST("chat/completions")
    suspend fun generateChatCompletion(
        @Header("Authorization") authorization: String,
        @Body request: OpenAIRequest
    ): Response<OpenAIResponse>
}
