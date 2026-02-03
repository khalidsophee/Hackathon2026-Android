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
    
    suspend fun getAllProjects(): Result<List<JiraProjectInfo>> {
        return try {
            val response = jiraApiService.getAllProjects()
            if (response.isSuccessful && response.body() != null) {
                val projects = response.body()!!
                println("=== AVAILABLE JIRA PROJECTS ===")
                println("Total projects: ${projects.size}")
                projects.forEachIndexed { index, project ->
                    println("${index + 1}. Key: '${project.key}' | Name: '${project.name}' | ID: ${project.id} | Type: ${project.projectTypeKey ?: "N/A"}")
                }
                println("=================================")
                Result.success(projects)
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMessage = "Failed to fetch projects: ${response.message()}. Error: $errorBody"
                println("❌ $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            println("❌ Exception while fetching projects: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
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
    
    /**
     * Clean project key by removing spaces, parentheses, and extracting the actual key
     * Examples:
     * - "TCM-XRAY (TC)" -> "TCM" (extract before dash)
     * - "XRM" -> "XRM"
     * - "AND" -> "AND"
     */
    private fun cleanProjectKey(projectKey: String): String {
        // Remove spaces and parentheses first
        var cleaned = projectKey.trim().replace(" ", "").replace("(", "").replace(")", "")
        
        // If it contains a dash, try to extract the part before the dash (common pattern: "TCM-XRAY" -> "TCM")
        if (cleaned.contains("-")) {
            val parts = cleaned.split("-")
            if (parts.isNotEmpty()) {
                // Return the first part (usually the project key)
                return parts[0].uppercase()
            }
        }
        
        // Otherwise return the cleaned version
        return cleaned.uppercase()
    }
    
    /**
     * Link a test case to its parent issue using Xray's "Tests" relationship
     * Note: In Xray, the "Tests" link means: parent issue "is tested by" test case
     * So: outwardIssue = parent, inwardIssue = test case
     */
    private suspend fun linkTestCaseToParent(testCaseKey: String, parentIssueKey: String) {
        // Try different link types that might work for Xray
        val linkTypes = listOf("Tests", "Relates", "is tested by")
        
        for (linkTypeName in linkTypes) {
            try {
                val linkRequest = IssueLinkRequest(
                    type = IssueLinkType(linkTypeName),
                    inwardIssue = IssueKey(testCaseKey), // Test case
                    outwardIssue = IssueKey(parentIssueKey) // Parent story
                )
                
                val response = jiraApiService.linkIssue(linkRequest)
                if (response.isSuccessful) {
                    println("✅ Linked test case $testCaseKey to parent $parentIssueKey using '$linkTypeName' relationship")
                    return
                }
                
                // Try reverse direction
                val reverseLinkRequest = IssueLinkRequest(
                    type = IssueLinkType(linkTypeName),
                    inwardIssue = IssueKey(parentIssueKey),
                    outwardIssue = IssueKey(testCaseKey)
                )
                val reverseResponse = jiraApiService.linkIssue(reverseLinkRequest)
                if (reverseResponse.isSuccessful) {
                    println("✅ Linked test case $testCaseKey to parent $parentIssueKey using '$linkTypeName' (reverse direction)")
                    return
                }
            } catch (e: Exception) {
                println("⚠️ Failed to link with '$linkTypeName': ${e.message}")
                continue
            }
        }
        
        // If all link types failed, log but don't throw - test case was created successfully
        println("⚠️ Warning: Could not automatically link test case $testCaseKey to parent $parentIssueKey. Please link manually in Jira.")
    }
    
    /**
     * Set test case resolution to "Pending" (Xray test execution status)
     */
    private suspend fun setTestCaseResolution(testCaseKey: String, resolution: String) {
        val updateRequest = UpdateIssueRequest(
            fields = UpdateIssueFields(
                resolution = Resolution(resolution)
            )
        )
        
        val response = jiraApiService.updateIssue(testCaseKey, updateRequest)
        if (response.isSuccessful) {
            println("✅ Set resolution to '$resolution' for test case $testCaseKey")
        } else {
            val errorBody = response.errorBody()?.string()
            throw Exception("Failed to set resolution: ${response.message()}. Error: $errorBody")
        }
    }
    
    suspend fun createTestCaseInJira(
        projectKey: String,
        testCase: GeneratedTestCase,
        parentIssueKey: String?,
        fallbackProjectId: String? = null,
        availableProjects: List<bqe.automation.hackathon_feb2026.data.model.JiraProjectInfo> = emptyList(),
        linkToParent: Boolean = true,
        setResolutionPending: Boolean = true
    ): Result<CreatedTestCase> {
        return try {
            val descriptionText = formatTestCaseDescription(testCase, parentIssueKey)
            val descriptionADF = ADFConverter.textToADF(descriptionText)
            
            // Try to find project by name first (e.g., "TCM-XRAY (TC)" -> find project with that name)
            val foundProject = if (availableProjects.isNotEmpty()) {
                availableProjects.firstOrNull { 
                    it.name.equals(projectKey, ignoreCase = true) || 
                    it.key.equals(projectKey, ignoreCase = true) ||
                    projectKey.contains(it.name, ignoreCase = true) ||
                    projectKey.contains(it.key, ignoreCase = true)
                }
            } else null

            val project: JiraProjectKey = when {
                // If project found by name, use its key or ID
                foundProject != null -> {
                    println("✅ Found project by name/key: '${foundProject.name}' -> Key: '${foundProject.key}', ID: ${foundProject.id}")
                    // Prefer ID if available, otherwise use key
                    if (foundProject.id.isNotEmpty()) {
                        JiraProjectKey(id = foundProject.id)
                    } else {
                        JiraProjectKey(key = foundProject.key)
                    }
                }
                // If fallback project ID provided, use it
                fallbackProjectId != null -> {
                    println("Using fallback project ID: $fallbackProjectId")
                    JiraProjectKey(id = fallbackProjectId)
                }
                // Otherwise, try to clean and use the provided key
                else -> {
                    // Clean the project key (e.g., "TCM-XRAY (TC)" -> "TCM")
                    val cleanedProjectKey = projectKey.split(" ", "-", "(").firstOrNull()?.trim() ?: projectKey
                    println("Cleaned Project Key: $cleanedProjectKey")
                    JiraProjectKey(key = cleanedProjectKey)
                }
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
                val createdTestCase = response.body()!!
                
                // Link to parent issue if requested
                if (linkToParent && parentIssueKey != null) {
                    try {
                        linkTestCaseToParent(createdTestCase.key, parentIssueKey)
                    } catch (e: Exception) {
                        println("⚠️ Warning: Failed to link test case ${createdTestCase.key} to parent $parentIssueKey: ${e.message}")
                        // Continue even if linking fails
                    }
                }
                
                // Set resolution to Pending if requested
                if (setResolutionPending) {
                    try {
                        setTestCaseResolution(createdTestCase.key, "Pending")
                    } catch (e: Exception) {
                        println("⚠️ Warning: Failed to set resolution for ${createdTestCase.key}: ${e.message}")
                        // Continue even if setting resolution fails
                    }
                }
                
                Result.success(createdTestCase)
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
