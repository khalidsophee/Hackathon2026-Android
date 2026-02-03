package bqe.automation.hackathon_feb2026.ui.jira

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bqe.automation.hackathon_feb2026.data.model.CreatedTestCase
import bqe.automation.hackathon_feb2026.data.model.GeneratedTestCase
import bqe.automation.hackathon_feb2026.data.model.JiraIssue
import bqe.automation.hackathon_feb2026.data.repository.JiraRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

data class JiraUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val story: JiraIssue? = null,
    val generatedTestCases: List<GeneratedTestCase> = emptyList(),
    val createdTestCases: List<CreatedTestCase> = emptyList(),
    val jiraConfigured: Boolean = false,
    val usingAI: Boolean = false,
    val availableProjects: List<bqe.automation.hackathon_feb2026.data.model.JiraProjectInfo> = emptyList()
)

class JiraViewModel(
    private val repositoryProvider: () -> JiraRepository
) : ViewModel() {
    
    private val repository: JiraRepository by lazy { repositoryProvider() }
    
    private val _uiState = MutableStateFlow(JiraUiState())
    val uiState: StateFlow<JiraUiState> = _uiState.asStateFlow()
    
    init {
        // Note: Jira and Groq credentials must be configured by the user
        // Users can configure via the "Configure" button in the UI
        // Or by calling configureJira() and configureOpenAI() programmatically
        
        // Automatically fetch and list all projects when ViewModel is created (if Jira is configured)
        viewModelScope.launch {
            delay(1000) // Wait 1 second for potential configuration
            if (_uiState.value.jiraConfigured) {
                fetchAllProjects()
            }
        }
    }
    
    fun configureJira(baseUrl: String, email: String, apiToken: String) {
        try {
            bqe.automation.hackathon_feb2026.data.network.NetworkModule.initialize(baseUrl, email, apiToken)
            bqe.automation.hackathon_feb2026.di.AppModule.resetRepository()
            _uiState.value = _uiState.value.copy(jiraConfigured = true, error = null)
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                jiraConfigured = false,
                error = "Failed to configure Jira: ${e.message}"
            )
        }
    }
    
    fun configureOpenAI(apiKey: String) {
        try {
            bqe.automation.hackathon_feb2026.data.network.NetworkModule.initializeOpenAI(apiKey)
            bqe.automation.hackathon_feb2026.di.AppModule.resetRepository()
            _uiState.value = _uiState.value.copy(error = null)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to configure Groq API: ${e.message}"
                )
            }
    }
    
    fun fetchStory(issueKey: String) {
        if (!_uiState.value.jiraConfigured) {
            _uiState.value = _uiState.value.copy(error = "Please configure Jira first")
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            repository.getStory(issueKey)
                .onSuccess { issue ->
                    _uiState.value = _uiState.value.copy(
                        story = issue,
                        isLoading = false
                    )
                    generateTestCases(issue)
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to fetch story"
                    )
                }
        }
    }
    
    private fun generateTestCases(issue: JiraIssue) {
        viewModelScope.launch {
            val isUsingAI = bqe.automation.hackathon_feb2026.data.network.NetworkModule.openAIApiKey.isNotEmpty()
            val testCases = repository.generateTestCases(issue)
            _uiState.value = _uiState.value.copy(
                generatedTestCases = testCases,
                usingAI = isUsingAI
            )
        }
    }
    
    fun fetchAllProjects() {
        if (!_uiState.value.jiraConfigured) {
            _uiState.value = _uiState.value.copy(error = "Please configure Jira first")
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            repository.getAllProjects()
                .onSuccess { projects ->
                    println("✅ Successfully fetched ${projects.size} projects")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = null,
                        availableProjects = projects
                    )
                }
                .onFailure { error ->
                    println("❌ Failed to fetch projects: ${error.message}")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to fetch projects: ${error.message}"
                    )
                }
        }
    }
    
    fun createTestCasesInJira(targetProjectKey: String = "XRM") {
        if (!_uiState.value.jiraConfigured) {
            _uiState.value = _uiState.value.copy(error = "Please configure Jira first")
            return
        }
        
        val story = _uiState.value.story ?: return
        val testCases = _uiState.value.generatedTestCases
        
        // Validate that the issue is a Story, not a Bug or Epic
        val issueType = story.fields.issuetype.name.lowercase()
        if (issueType != "story") {
            _uiState.value = _uiState.value.copy(
                error = "Test cases can only be added to Stories. Current issue type: '${story.fields.issuetype.name}'. Please fetch a Story instead."
            )
            return
        }
        
        if (testCases.isEmpty()) {
            _uiState.value = _uiState.value.copy(error = "No test cases to create. Please generate test cases first.")
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            val created = mutableListOf<CreatedTestCase>()
            val errors = mutableListOf<String>()
            
            println("=== CREATING TEST CASES IN JIRA ===")
            println("Target Project: $targetProjectKey")
            println("Number of test cases: ${testCases.size}")
            
            // Get the story's project ID as fallback
            val fallbackProjectId = story.fields.project.id
            
            testCases.forEachIndexed { index, testCase ->
                println("Creating test case ${index + 1}/${testCases.size}: ${testCase.title}")
                repository.createTestCaseInJira(
                    projectKey = targetProjectKey, // Use target project (e.g., TCM-XRAY (TC) -> will find by name)
                    testCase = testCase,
                    parentIssueKey = story.key,
                    fallbackProjectId = if (targetProjectKey == story.fields.project.key) fallbackProjectId else null,
                    availableProjects = _uiState.value.availableProjects, // Pass available projects for lookup
                    linkToParent = true, // Automatically link to parent issue (Xray "Tests" relationship)
                    setResolutionPending = true // Set resolution to "Pending" (Xray test execution status)
                ).onSuccess { createdTestCase ->
                    created.add(createdTestCase)
                    println("✅ Created and linked: ${createdTestCase.key}")
                }.onFailure { error ->
                    val errorMsg = "Failed to create ${testCase.title}: ${error.message}"
                    errors.add(errorMsg)
                    println("❌ $errorMsg")
                    
                    // If project key failed, suggest using the story's project
                    if (error.message?.contains("Project") == true && targetProjectKey != story.fields.project.key) {
                        println("💡 Suggestion: Try using project '${story.fields.project.key}' (from the story) instead of '$targetProjectKey'")
                        println("💡 Note: Project key will be cleaned (e.g., 'TCM-XRAY (TC)' -> 'TCM')")
                    }
                }
            }
            
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                createdTestCases = created,
                error = if (errors.isNotEmpty()) errors.joinToString("\n") else null
            )
        }
    }
    
    fun generateAndroidTestFiles(): String {
        val testCases = _uiState.value.generatedTestCases
        if (testCases.isEmpty()) return ""
        
        val packageName = "bqe.automation.hackathon_feb2026"
        val className = _uiState.value.story?.key?.replace("-", "")?.let { 
            "Jira${it}TestCases" 
        } ?: "JiraTestCases"
        
        val testMethods = testCases.mapIndexed { index, testCase ->
            generateTestMethod(testCase, index + 1)
        }.joinToString("\n\n")
        
        return """
package $packageName

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class $className {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
$testMethods
}
        """.trimIndent()
    }
    
    private fun generateTestMethod(testCase: GeneratedTestCase, testNumber: Int): String {
        val methodName = "test${testCase.title.replace(Regex("[^A-Za-z0-9]"), "")}"
        val steps = testCase.steps.joinToString("\n        ") { step ->
            "// TODO: Implement step: $step"
        }
        
        return """
    @Test
    fun $methodName() {
        // Test: ${testCase.title}
        // Description: ${testCase.description}
        // Priority: ${testCase.priority}
        
        composeTestRule.setContent {
            // TODO: Set up your composable content
        }
        
        $steps
        
        // Expected Result: ${testCase.expectedResult}
        // TODO: Add assertions
    }
        """.trimIndent()
    }
}
