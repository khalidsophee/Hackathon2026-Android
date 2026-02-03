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
        val summary = issue.fields.summary
        val description = issue.fields.getDescriptionText()
        val acceptanceCriteria = issue.fields.getAcceptanceCriteriaText()
        
        // Combine summary and description - summary is often the main issue description
        val fullDescription = buildString {
            if (summary.isNotBlank()) {
                append(summary)
            }
            if (description != null && description.isNotBlank()) {
                // Only append if description is different from summary (to avoid duplication)
                val descText = description.trim()
                val summaryText = summary.trim()
                if (descText != summaryText && !descText.contains(summaryText)) {
                    if (isNotEmpty()) append("\n\n")
                    append(descText)
                }
            }
        }
        
        println("=== JIRA REPOSITORY: Extracted Information ===")
        println("Summary: $summary")
        println("Description: ${description ?: "NULL"}")
        println("Full Description (combined): $fullDescription")
        println("Full Description length: ${fullDescription.length}")
        println("Acceptance Criteria: ${acceptanceCriteria ?: "NULL"}")
        println("Acceptance Criteria length: ${acceptanceCriteria?.length ?: 0}")
        println("===============================================")
        
        return testCaseGenerator.generateTestCases(
            storyDescription = if (fullDescription.isNotBlank()) fullDescription else null,
            acceptanceCriteria = acceptanceCriteria
        )
    }
    
    suspend fun createTestCaseInJira(
        projectKey: String,
        testCase: GeneratedTestCase,
        parentIssueKey: String?,
        fallbackProjectId: String? = null
    ): Result<CreatedTestCase> {
        return try {
            val descriptionText = formatTestCaseDescription(testCase, parentIssueKey)
            val descriptionADF = ADFConverter.textToADF(descriptionText)
            
            // Try with project key first, fallback to project ID if provided
            val project = if (fallbackProjectId != null) {
                JiraProjectKey(id = fallbackProjectId)
            } else {
                JiraProjectKey(key = projectKey)
            }
            
            val request = CreateTestCaseRequest(
                fields = TestCaseFields(
                    project = project,
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
                            if (field == "project" && message.contains("valid project")) {
                                messages.add("Project '$projectKey' not found or you don't have permission to create issues in it.\n" +
                                        "Please verify:\n" +
                                        "1. The project key is correct (check in Jira)\n" +
                                        "2. You have permission to create issues in this project\n" +
                                        "3. The project exists and is accessible")
                            } else {
                                messages.add("$field: $message")
                            }
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
        // Format steps with clear numbering
        val stepsText = testCase.steps.mapIndexed { index, step ->
            "Step ${index + 1}: $step"
        }.joinToString("\n")
        
        val parentLink = if (parentIssueKey != null) {
            "\n\nRelated Story: $parentIssueKey"
        } else {
            ""
        }
        
        // Create a well-structured description with steps and execution details
        return """
            |${testCase.description}
            |
            |=== TEST STEPS ===
            |$stepsText
            |
            |=== EXPECTED RESULT ===
            |${testCase.expectedResult}
            |
            |=== TEST EXECUTION ===
            |Status: Not Executed
            |Priority: ${testCase.priority}
            |$parentLink
        """.trimMargin()
    }
}
