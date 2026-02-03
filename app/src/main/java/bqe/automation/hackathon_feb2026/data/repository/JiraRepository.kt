package bqe.automation.hackathon_feb2026.data.repository

import bqe.automation.hackathon_feb2026.data.api.JiraApiService
import bqe.automation.hackathon_feb2026.data.model.*
import bqe.automation.hackathon_feb2026.domain.TestCaseGenerator
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException

class JiraRepository(
    private val jiraApiService: JiraApiService,
    private val testCaseGenerator: TestCaseGenerator
) {
    
    suspend fun getStory(issueKey: String): Result<JiraIssue> {
        return try {
            val response = jiraApiService.getIssue(issueKey)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                // Try to parse error response
                val errorBody = response.errorBody()?.string()
                val errorMessage = if (errorBody != null) {
                    try {
                        val gson = Gson()
                        val errorResponse = gson.fromJson(errorBody, JiraErrorResponse::class.java)
                        val messages = mutableListOf<String>()
                        errorResponse.errorMessages?.let { messages.addAll(it) }
                        errorResponse.errors?.forEach { (field, message) ->
                            messages.add("$field: $message")
                        }
                        if (messages.isNotEmpty()) {
                            "Failed to fetch issue: ${messages.joinToString("\n")}"
                        } else {
                            "Failed to fetch issue: ${response.code()} ${response.message()}"
                        }
                    } catch (e: JsonSyntaxException) {
                        "Failed to fetch issue: ${response.code()} ${response.message()}"
                    }
                } else {
                    "Failed to fetch issue: ${response.code()} ${response.message()}"
                }
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Failed to fetch issue: ${e.message}", e))
        }
    }
    
    suspend fun generateTestCases(issue: JiraIssue): List<GeneratedTestCase> {
        return testCaseGenerator.generateTestCases(
            storyDescription = issue.fields.getDescriptionText(),
            acceptanceCriteria = issue.fields.getAcceptanceCriteriaText()
        )
    }
    
    suspend fun createTestCaseInJira(
        projectKey: String,
        testCase: GeneratedTestCase,
        parentIssueKey: String?
    ): Result<CreatedTestCase> {
        return try {
            val descriptionText = formatTestCaseDescription(testCase, parentIssueKey)
            val descriptionADF = ADFConverter.textToADF(descriptionText)
            
            val request = CreateTestCaseRequest(
                fields = TestCaseFields(
                    project = JiraProjectKey(projectKey),
                    summary = testCase.title,
                    description = descriptionADF,
                    issuetype = JiraIssueTypeKey("Test")
                )
            )
            
            val response = jiraApiService.createTestCase(request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                // Try to parse error response
                val errorBody = response.errorBody()?.string()
                val errorMessage = if (errorBody != null) {
                    try {
                        val gson = Gson()
                        val errorResponse = gson.fromJson(errorBody, JiraErrorResponse::class.java)
                        val messages = mutableListOf<String>()
                        errorResponse.errorMessages?.let { messages.addAll(it) }
                        errorResponse.errors?.forEach { (field, message) ->
                            messages.add("$field: $message")
                        }
                        if (messages.isNotEmpty()) {
                            messages.joinToString("\n")
                        } else {
                            "Failed to create test case: ${response.message()}"
                        }
                    } catch (e: JsonSyntaxException) {
                        "Failed to create test case: ${response.message()}"
                    }
                } else {
                    "Failed to create test case: ${response.message()}"
                }
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun formatTestCaseDescription(testCase: GeneratedTestCase, parentIssueKey: String?): String {
        val stepsText = testCase.steps.mapIndexed { index, step ->
            "${index + 1}. $step"
        }.joinToString("\n")
        
        val parentLink = if (parentIssueKey != null) {
            "\n\nRelated Story: $parentIssueKey"
        } else {
            ""
        }
        
        return """
            |${testCase.description}
            |
            |Steps:
            |$stepsText
            |
            |Expected Result:
            |${testCase.expectedResult}
            |
            |Priority: ${testCase.priority}$parentLink
        """.trimMargin()
    }
}
