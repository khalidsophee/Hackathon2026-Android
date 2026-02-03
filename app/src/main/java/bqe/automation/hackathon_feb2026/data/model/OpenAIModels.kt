package bqe.automation.hackathon_feb2026.data.model

import com.google.gson.annotations.SerializedName

// OpenAI ChatGPT Models
data class OpenAIRequest(
    val model: String = "gpt-3.5-turbo",
    val messages: List<ChatMessage>,
    val temperature: Double = 0.8,
    val max_tokens: Int = 8000
)

data class ChatMessage(
    val role: String, // "system", "user", "assistant"
    val content: String
)

data class OpenAIResponse(
    val choices: List<Choice>?
)

data class Choice(
    val message: ChatMessage
)

data class OpenAIConfig(
    val apiKey: String,
    val enabled: Boolean = true
)
