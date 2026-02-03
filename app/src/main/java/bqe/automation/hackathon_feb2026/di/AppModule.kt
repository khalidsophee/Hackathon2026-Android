package bqe.automation.hackathon_feb2026.di

import bqe.automation.hackathon_feb2026.data.api.JiraApiService
import bqe.automation.hackathon_feb2026.data.network.NetworkModule
import bqe.automation.hackathon_feb2026.data.repository.JiraRepository
import bqe.automation.hackathon_feb2026.domain.TestCaseGenerator
import bqe.automation.hackathon_feb2026.ui.jira.JiraViewModel

object AppModule {
    private val testCaseGenerator = TestCaseGenerator()
    private var _jiraRepository: JiraRepository? = null
    
    private fun getJiraRepository(): JiraRepository {
        if (_jiraRepository == null) {
            try {
                _jiraRepository = JiraRepository(NetworkModule.jiraApiService, testCaseGenerator)
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
    }
}
