package bqe.automation.hackathon_feb2026.di

import bqe.automation.hackathon_feb2026.data.api.JiraApiService
import bqe.automation.hackathon_feb2026.data.network.NetworkModule
import bqe.automation.hackathon_feb2026.data.repository.JiraRepository
import bqe.automation.hackathon_feb2026.domain.LLMTestCaseGenerator
import bqe.automation.hackathon_feb2026.domain.TestCaseGenerator
import bqe.automation.hackathon_feb2026.ui.jira.JiraViewModel

object AppModule {
    private var testCaseGenerator: TestCaseGenerator? = null
    private var _jiraRepository: JiraRepository? = null
    
    private fun getTestCaseGenerator(): TestCaseGenerator {
        if (testCaseGenerator == null) {
            val openAIService = NetworkModule.openAIApiService
            val openAIKey = NetworkModule.openAIApiKey
            // Always use LLM if service is available (Groq works with or without key for free tier)
            val useLLM = openAIService != null
            
            println("=== APP MODULE: Creating TestCaseGenerator ===")
            println("OpenAI Service available: ${openAIService != null}")
            println("OpenAI Key length: ${openAIKey.length}")
            println("OpenAI Key (first 10 chars): ${openAIKey.take(10)}...")
            println("Use LLM: $useLLM")
            
            val llmGenerator = if (useLLM) {
                println("✅ Creating LLMTestCaseGenerator")
                LLMTestCaseGenerator(openAIService, openAIKey)
            } else {
                println("❌ NOT creating LLMTestCaseGenerator - OpenAI not configured")
                null
            }
            testCaseGenerator = TestCaseGenerator(useLLM, llmGenerator)
            println("TestCaseGenerator created with useLLM=$useLLM")
            println("=============================================")
        }
        return testCaseGenerator!!
    }
    
    private fun getJiraRepository(): JiraRepository {
        if (_jiraRepository == null) {
            try {
                _jiraRepository = JiraRepository(NetworkModule.jiraApiService, getTestCaseGenerator())
            } catch (e: IllegalStateException) {
                // NetworkModule not initialized yet - create a dummy repository that will be replaced
                // This should not happen in normal flow, but handles the initialization race condition
                throw IllegalStateException("Jira must be configured before creating ViewModel", e)
            }
        }
        return _jiraRepository!!
    }
    
    fun createJiraViewModel(): JiraViewModel {
        // Don't create repository here - let ViewModel handle it lazily
        // We'll pass a factory function instead
        return JiraViewModel { getJiraRepository() }
    }
    
    fun resetRepository() {
        _jiraRepository = null
        testCaseGenerator = null
    }
}
