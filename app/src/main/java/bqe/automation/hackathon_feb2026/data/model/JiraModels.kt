package bqe.automation.hackathon_feb2026.data.model

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

// Jira Issue Response
data class JiraIssue(
    val id: String,
    val key: String,
    val fields: JiraFields
)

data class JiraFields(
    val summary: String,
    val description: JsonElement?, // Can be ADF format or string
    @SerializedName("customfield_10026") // Acceptance Criteria field ID (adjust based on your Jira)
    val acceptanceCriteria: JsonElement?,
    val issuetype: JiraIssueType,
    val project: JiraProject
) {
    // Helper function to extract plain text from description
    fun getDescriptionText(): String? {
        val extracted = description?.let { extractTextFromJson(it) }
        println("=== ADF EXTRACTION: Description ===")
        println("Raw description element: ${description?.toString()?.take(200)}")
        println("Extracted text: ${extracted ?: "NULL"}")
        println("Extracted text length: ${extracted?.length ?: 0}")
        println("===================================")
        return extracted
    }
    
    // Helper function to extract plain text from acceptance criteria
    fun getAcceptanceCriteriaText(): String? {
        return acceptanceCriteria?.let { extractTextFromJson(it) }
    }
    
    private fun extractTextFromJson(element: JsonElement): String {
        return when {
            element.isJsonPrimitive && element.asJsonPrimitive.isString -> {
                element.asString
            }
            element.isJsonObject -> {
                val obj = element.asJsonObject
                // Handle ADF format
                when {
                    // Root "doc" type - extract content array
                    obj.has("type") && obj.get("type").asString == "doc" && obj.has("content") -> {
                        obj.getAsJsonArray("content")
                            .mapNotNull { extractTextFromJson(it) }
                            .joinToString("")
                    }
                    // Paragraph node
                    obj.has("type") && obj.get("type").asString == "paragraph" && obj.has("content") -> {
                        obj.getAsJsonArray("content")
                            .mapNotNull { extractTextFromJson(it) }
                            .joinToString("") + "\n"
                    }
                    // Text node
                    obj.has("text") && obj.get("text").isJsonPrimitive -> {
                        obj.get("text").asString
                    }
                    // Media nodes - skip but add note
                    obj.has("type") && (obj.get("type").asString == "mediaSingle" || obj.get("type").asString == "media") -> {
                        "[Media attachment]"
                    }
                    // Content array
                    obj.has("content") && obj.get("content").isJsonArray -> {
                        obj.getAsJsonArray("content")
                            .mapNotNull { extractTextFromJson(it) }
                            .joinToString("")
                    }
                    // Hard break
                    obj.has("type") && obj.get("type").asString == "hardBreak" -> {
                        "\n"
                    }
                    // Other nodes with content
                    else -> {
                        obj.entrySet()
                            .filter { it.key == "content" || it.key == "text" }
                            .mapNotNull { extractTextFromJson(it.value) }
                            .joinToString("")
                    }
                }
            }
            element.isJsonArray -> {
                element.asJsonArray
                    .mapNotNull { extractTextFromJson(it) }
                    .joinToString("")
            }
            else -> ""
        }
    }
}

data class JiraIssueType(
    val name: String,
    val id: String
)

data class JiraProject(
    val key: String,
    val id: String? = null,
    val name: String
)

// Test Case Creation Request
data class CreateTestCaseRequest(
    val fields: TestCaseFields
)

data class TestCaseFields(
    val project: JiraProjectKey,
    val summary: String,
    val description: AtlassianDocument, // Must be in ADF format
    val issuetype: JiraIssueTypeKey
    // Note: Parent link removed - customfield_10027 is not available
    // You can link test cases to stories manually or configure the correct field ID
)

data class JiraProjectKey(
    val key: String? = null,
    val id: String? = null
) {
    init {
        require(key != null || id != null) { "Either project key or project id must be provided" }
    }
}

data class JiraIssueTypeKey(
    val name: String = "Test"
)

// Test Case Response
data class CreatedTestCase(
    val id: String,
    val key: String,
    val self: String
)

// Generated Test Case Model
data class GeneratedTestCase(
    val title: String,
    val description: String,
    val steps: List<String>,
    val expectedResult: String,
    val priority: String = "Medium"
)
