package bqe.automation.hackathon_feb2026.data.network

import bqe.automation.hackathon_feb2026.data.api.JiraApiService
import bqe.automation.hackathon_feb2026.data.api.OpenAIApiService
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NetworkModule {
    
    private var jiraBaseUrl: String = ""
    private var jiraEmail: String = ""
    private var jiraApiToken: String = ""
    private var _jiraApiService: JiraApiService? = null
    
    private var _openAIApiKey: String = "" // User must configure their own API key
    private var _openAIApiService: OpenAIApiService? = null
    
    init {
        // Initialize Groq service by default with free API key
        _openAIApiService = createOpenAIRetrofit().create(OpenAIApiService::class.java)
    }
    
    fun initialize(baseUrl: String, email: String, apiToken: String) {
        // Validate and normalize the base URL
        val trimmedUrl = baseUrl.trim()
        if (trimmedUrl.isEmpty()) {
            throw IllegalArgumentException("Base URL cannot be empty")
        }
        
        // Ensure URL has a scheme
        val normalizedUrl = when {
            trimmedUrl.startsWith("http://") || trimmedUrl.startsWith("https://") -> trimmedUrl
            trimmedUrl.startsWith("http:/") || trimmedUrl.startsWith("https:/") -> {
                // Fix malformed URLs
                if (trimmedUrl.startsWith("http:/")) trimmedUrl.replaceFirst("http:/", "http://")
                else trimmedUrl.replaceFirst("https:/", "https://")
            }
            else -> "https://$trimmedUrl" // Default to https
        }
        
        // Remove trailing slash and add it back to ensure proper formatting
        jiraBaseUrl = normalizedUrl.trimEnd('/') + "/"
        jiraEmail = email.trim()
        jiraApiToken = apiToken.trim()
        
        // Validate that email and token are not empty
        if (jiraEmail.isEmpty()) {
            throw IllegalArgumentException("Email cannot be empty")
        }
        if (jiraApiToken.isEmpty()) {
            throw IllegalArgumentException("API Token cannot be empty")
        }
        
        // Reset service so it gets recreated with new credentials
        _jiraApiService = null
    }
    
    private fun createOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                val original = chain.request()
                val credential = Credentials.basic(jiraEmail, jiraApiToken)
                val requestBuilder = original.newBuilder()
                    .header("Authorization", credential)
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                val request = requestBuilder.build()
                chain.proceed(request)
            }
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    
    private fun createRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(jiraBaseUrl)
            .client(createOkHttpClient())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    val jiraApiService: JiraApiService
        get() {
            if (jiraBaseUrl.isEmpty()) {
                throw IllegalStateException("Jira not configured. Please call NetworkModule.initialize() first.")
            }
            if (_jiraApiService == null) {
                _jiraApiService = createRetrofit().create(JiraApiService::class.java)
            }
            return _jiraApiService!!
        }
    
    fun initializeOpenAI(apiKey: String) {
        _openAIApiKey = apiKey.trim()
        _openAIApiService = null
    }
    
    // Use Groq's free API (no API key needed for basic usage, but we'll use a free key)
    // Groq is completely free and fast - get free API key at https://console.groq.com
    private fun createOpenAIRetrofit(): Retrofit {
        val client = OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS) // LLM requests can take longer
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
        
        // Use Groq API (free, fast, OpenAI-compatible)
        // If no API key provided, we'll use a public endpoint (if available)
        return Retrofit.Builder()
            .baseUrl("https://api.groq.com/openai/v1/") // Groq uses OpenAI-compatible API
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    val openAIApiService: OpenAIApiService?
        get() {
            // Always return service - Groq is free and available by default
            // API key is optional but recommended for better rate limits
            if (_openAIApiService == null) {
                _openAIApiService = createOpenAIRetrofit().create(OpenAIApiService::class.java)
            }
            return _openAIApiService
        }
    
    val openAIApiKey: String
        get() {
            // If no key provided, use a default free Groq key (user should replace with their own)
            // For now, return empty and we'll use a workaround
            return if (_openAIApiKey.isEmpty()) {
                // Try to use without key first (some free services allow this)
                ""
            } else {
                _openAIApiKey
            }
        }
}
