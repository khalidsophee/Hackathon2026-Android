package bqe.automation.hackathon_feb2026.domain

import bqe.automation.hackathon_feb2026.data.api.OpenAIApiService
import bqe.automation.hackathon_feb2026.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LLMTestCaseGenerator(
    private val openAIApiService: OpenAIApiService?,
    private val openAIApiKey: String
) {
    
    suspend fun generateTestCasesWithLLM(
        storyDescription: String?,
        acceptanceCriteria: String?
    ): List<GeneratedTestCase> {
        if (openAIApiService == null) {
            println("❌ LLMTestCaseGenerator: openAIApiService is null")
            return emptyList()
        }
        
        // Validate API key
        if (openAIApiKey.isBlank()) {
            println("❌ LLMTestCaseGenerator: API key is empty or blank")
            println("   API key length: ${openAIApiKey.length}")
            println("   Please configure your Groq API key in the app settings")
            return emptyList()
        }
        
        println("=== LLM TEST CASE GENERATOR ===")
        println("API Key length: ${openAIApiKey.length}")
        println("API Key (first 10 chars): ${openAIApiKey.take(10)}...")
        println("API Key (last 10 chars): ...${openAIApiKey.takeLast(10)}")
        
        return withContext(Dispatchers.IO) {
            try {
                val messages = buildMessages(storyDescription, acceptanceCriteria)
                
                // Use Groq's Llama 3.1 8B model (fast and reliable, free with API key)
                // Note: llama-3.1-70b-versatile was decommissioned, using 8b-instant instead
                val request = OpenAIRequest(
                    model = "llama-3.1-8b-instant", // Fast and reliable model on Groq
                    messages = messages,
                    temperature = 0.9, // Higher temperature for more creative, description-specific responses
                    max_tokens = 8000 // High token limit to handle detailed test cases
                )
                
                // Use the API key - ensure it's not empty
                val authHeader = "Bearer $openAIApiKey"
                println("Authorization header (first 20 chars): ${authHeader.take(20)}...")
                
                val response = openAIApiService.generateChatCompletion(
                    authorization = authHeader,
                    request = request
                )
                
                println("=== CHATGPT API RESPONSE ===")
                println("Response code: ${response.code()}")
                println("Response isSuccessful: ${response.isSuccessful}")
                
                if (response.isSuccessful && response.body() != null) {
                    val responseBody = response.body()!!
                    val generatedText = responseBody.choices?.firstOrNull()?.message?.content
                    
                    println("Response body exists: true")
                    println("Choices count: ${responseBody.choices?.size ?: 0}")
                    println("Generated text is null: ${generatedText == null}")
                    
                    if (generatedText == null) {
                        println("❌ ERROR: ChatGPT returned null content")
                        println("Full response body: $responseBody")
                        return@withContext emptyList()
                    }
                    
                    println("=== CHATGPT FULL RESPONSE ===")
                    println("Response length: ${generatedText.length} characters")
                    println("Full response text:")
                    println("----------------------------------------")
                    println(generatedText)
                    println("----------------------------------------")
                    
                    val testCases = parseLLMResponse(generatedText)
                    println("=== PARSED TEST CASES ===")
                    println("Number of test cases parsed: ${testCases.size}")
                    testCases.forEachIndexed { index, testCase ->
                        println("TC-${index + 1}: ${testCase.title}")
                        println("   Description: ${testCase.description}")
                        println("   Steps: ${testCase.steps.size} steps")
                        println("   Expected: ${testCase.expectedResult}")
                    }
                    println("============================")
                    
                    return@withContext testCases
                } else {
                    val errorBody = response.errorBody()?.string()
                    println("❌ Groq API Error:")
                    println("   Code: ${response.code()}")
                    println("   Message: ${response.message()}")
                    println("   Error Body: $errorBody")
                    
                    if (response.code() == 401) {
                        println("⚠️ API Key Required!")
                        println("   Groq requires a free API key (completely free, no credit card needed)")
                        println("   Get your free API key at: https://console.groq.com")
                        println("   Then click 'Use Other User' in the app and enter your Groq API key")
                    }
                    
                    return@withContext emptyList()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }
    
    private fun buildMessages(storyDescription: String?, acceptanceCriteria: String?): List<ChatMessage> {
        val messages = mutableListOf<ChatMessage>()
        
        // System message - keep it simple and direct
        messages.add(
            ChatMessage(
                role = "system",
                content = """You are a QA test engineer. When asked to write test cases for a description, you analyze the description and create test cases that are specific to what's described. 
                You do not use generic or template test cases. Each test case must be tailored to the actual features and requirements in the description.
                Always return a valid JSON array."""
            )
        )
        
        // User message with story details
        val userPrompt = buildUserPrompt(storyDescription, acceptanceCriteria)
        messages.add(
            ChatMessage(
                role = "user",
                content = userPrompt
            )
        )
        
        return messages
    }
    
    private fun buildUserPrompt(storyDescription: String?, acceptanceCriteria: String?): String {
        println("=== LLM TEST CASE GENERATOR: Building Prompt ===")
        println("Story Description received: ${storyDescription ?: "NULL"}")
        println("Story Description length: ${storyDescription?.length ?: 0}")
        println("Acceptance Criteria received: ${acceptanceCriteria ?: "NULL"}")
        println("Acceptance Criteria length: ${acceptanceCriteria?.length ?: 0}")
        
        // Build the full description text - use storyDescription as-is since it already contains summary + description
        val fullDescription = buildString {
            if (storyDescription != null && storyDescription.isNotBlank()) {
                append(storyDescription)
            }
            if (acceptanceCriteria != null && acceptanceCriteria.isNotBlank()) {
                if (isNotEmpty()) append("\n\n")
                append("Acceptance Criteria:\n")
                append(acceptanceCriteria)
            }
        }
        
        if (fullDescription.isBlank()) {
            println("WARNING: Full description is blank! Cannot generate test cases.")
            return ""
        }
        
        println("Full Description being sent to ChatGPT:")
        println("----------------------------------------")
        println(fullDescription)
        println("----------------------------------------")
        println("Full Description length: ${fullDescription.length}")
        
        // Direct, simple prompt like a user would ask ChatGPT
        val prompt = """Write 10 test cases for the following description. Make sure each test case is specific to what's described, not generic.

Description:
$fullDescription

Generate 10 test cases that directly test the features and requirements mentioned in the description above. Each test case should be tailored to the specific functionality described.

Return as JSON array:
[
  {
    "title": "TC-01: Specific test case title",
    "description": "Objective of this test",
    "steps": ["Step 1", "Step 2", "Step 3"],
    "expectedResult": "Expected outcome",
    "priority": "High"
  }
]

Return ONLY the JSON array:"""
        
        println("Prompt length: ${prompt.length}")
        println("First 500 chars of prompt:")
        println(prompt.take(500))
        println("=============================================")
        
        return prompt
    }
    
    private fun parseLLMResponse(response: String): List<GeneratedTestCase> {
        val testCases = mutableListOf<GeneratedTestCase>()
        
        println("=== PARSING CHATGPT RESPONSE ===")
        println("Original response length: ${response.length}")
        
        try {
            // Try to extract JSON from response (remove markdown code blocks if present)
            var jsonText = response.trim()
            println("After trim: ${jsonText.length} chars")
            
            // Remove markdown code blocks
            if (jsonText.contains("```json")) {
                println("Found ```json code block")
                val start = jsonText.indexOf("```json") + 7
                val end = jsonText.lastIndexOf("```")
                if (end > start) {
                    jsonText = jsonText.substring(start, end).trim()
                    println("After removing ```json: ${jsonText.length} chars")
                }
            } else if (jsonText.contains("```")) {
                println("Found ``` code block")
                val start = jsonText.indexOf("```") + 3
                val end = jsonText.lastIndexOf("```")
                if (end > start) {
                    jsonText = jsonText.substring(start, end).trim()
                    println("After removing ```: ${jsonText.length} chars")
                }
            } else {
                println("No markdown code blocks found")
            }
            
            // Try to find JSON array in the response
            val jsonStart = jsonText.indexOf('[')
            val jsonEnd = jsonText.lastIndexOf(']')
            println("JSON array start: $jsonStart, end: $jsonEnd")
            
            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                jsonText = jsonText.substring(jsonStart, jsonEnd + 1)
                println("Extracted JSON: ${jsonText.length} chars")
                println("First 200 chars: ${jsonText.take(200)}")
            } else {
                println("❌ No JSON array found in response!")
                println("Looking for '[' at position: $jsonStart")
                println("Looking for ']' at position: $jsonEnd")
            }
            
            // Parse JSON
            println("Attempting to parse JSON...")
            val gson = com.google.gson.Gson()
            val jsonArray = com.google.gson.JsonParser.parseString(jsonText).asJsonArray
            println("✅ JSON parsed successfully! Array size: ${jsonArray.size()}")
            
            jsonArray.forEachIndexed { index, element ->
                try {
                    val obj = element.asJsonObject
                    val title = obj.get("title")?.asString?.trim() ?: "Untitled Test Case"
                    val description = obj.get("description")?.asString?.trim() ?: ""
                    val stepsArray = obj.get("steps")
                    val steps = if (stepsArray != null && stepsArray.isJsonArray) {
                        stepsArray.asJsonArray.mapNotNull { it.asString?.trim() }.filter { it.isNotEmpty() }
                    } else {
                        emptyList()
                    }
                    val expectedResult = obj.get("expectedResult")?.asString?.trim() ?: ""
                    val priority = obj.get("priority")?.asString?.trim() ?: "Medium"
                    
                    println("  Parsed test case ${index + 1}: $title")
                    
                    testCases.add(
                        GeneratedTestCase(
                            title = title,
                            description = description,
                            steps = if (steps.isEmpty()) {
                                listOf("Step 1: Navigate to feature", "Step 2: Perform action", "Step 3: Verify result")
                            } else {
                                steps
                            },
                            expectedResult = expectedResult,
                            priority = priority
                        )
                    )
                } catch (e: Exception) {
                    println("❌ Error parsing test case ${index + 1}: ${e.message}")
                    e.printStackTrace()
                }
            }
            
            if (testCases.isNotEmpty()) {
                println("✅ Successfully parsed ${testCases.size} test cases from JSON")
                return testCases
            } else {
                println("⚠️ JSON parsed but no test cases extracted")
            }
        } catch (e: Exception) {
            println("❌ JSON parsing failed!")
            println("Error: ${e.message}")
            println("Error type: ${e.javaClass.simpleName}")
            e.printStackTrace()
            println("Falling back to text format parsing...")
        }
        
        // Fallback: try to extract test cases from text format
        println("Attempting text format parsing...")
        val textFormatCases = parseTextFormat(response)
        println("Text format parsing returned ${textFormatCases.size} test cases")
        return textFormatCases
    }
    
    private fun parseTextFormat(text: String): List<GeneratedTestCase> {
        // Fallback parser for non-JSON responses
        val testCases = mutableListOf<GeneratedTestCase>()
        
        // Try to find test cases in various formats
        // Pattern 1: TC-1:, TC-2:, etc.
        val tcPattern = Regex("TC-\\d+[:.]\\s*(.+?)(?=TC-\\d+[:.]|$)", RegexOption.DOT_MATCHES_ALL)
        val tcMatches = tcPattern.findAll(text)
        
        if (tcMatches.count() > 0) {
            tcMatches.forEach { match ->
                val content = match.groupValues[1].trim()
                val lines = content.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
                if (lines.isNotEmpty()) {
                    val title = "TC-${testCases.size + 1}: ${lines.first()}"
                    val description = lines.getOrNull(1) ?: lines.first()
                    testCases.add(
                        GeneratedTestCase(
                            title = title,
                            description = description,
                            steps = extractStepsFromText(content),
                            expectedResult = extractExpectedFromText(content),
                            priority = "Medium"
                        )
                    )
                }
            }
        } else {
            // Pattern 2: Numbered test cases
            val numberedPattern = Regex("(?:^|\\n)(\\d+)[.)]\\s*(.+?)(?=(?:^|\\n)\\d+[.)]|$)", setOf(RegexOption.MULTILINE, RegexOption.DOT_MATCHES_ALL))
            val numberedMatches = numberedPattern.findAll(text)
            
            numberedMatches.forEach { match ->
                val content = match.groupValues[2].trim()
                val lines = content.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
                if (lines.isNotEmpty()) {
                    testCases.add(
                        GeneratedTestCase(
                            title = "TC-${testCases.size + 1}: ${lines.first()}",
                            description = lines.first(),
                            steps = extractStepsFromText(content),
                            expectedResult = extractExpectedFromText(content),
                            priority = "Medium"
                        )
                    )
                }
            }
        }
        
        return testCases
    }
    
    private fun extractStepsFromText(text: String): List<String> {
        val steps = mutableListOf<String>()
        
        // Look for "Step 1", "1.", "Given", "When", "Then" patterns
        val stepPatterns = listOf(
            Regex("(?:Step\\s+\\d+[:.]|\\d+[:.])\\s*(.+?)(?=(?:Step\\s+\\d+[:.]|\\d+[:.])|$)", RegexOption.IGNORE_CASE),
            Regex("(?:Given|When|Then|And|But)\\s+(.+?)(?=(?:Given|When|Then|And|But)|$)", RegexOption.IGNORE_CASE)
        )
        
        stepPatterns.forEach { pattern ->
            val matches = pattern.findAll(text)
            if (matches.count() > 0) {
                matches.forEach { match ->
                    val step = match.groupValues[1].trim()
                    if (step.isNotEmpty()) {
                        steps.add(step)
                    }
                }
                return steps
            }
        }
        
        // Default steps if none found
        if (steps.isEmpty()) {
            steps.add("Navigate to the feature")
            steps.add("Perform the required action")
            steps.add("Verify the expected outcome")
        }
        
        return steps
    }
    
    private fun extractExpectedFromText(text: String): String {
        val expectedPatterns = listOf(
            Regex("(?:Expected|Expected Result|Expected Outcome)[:.]?\\s*(.+?)(?:\\.|$)", RegexOption.IGNORE_CASE),
            Regex("(?:should|must|will)\\s+(.+?)(?:\\.|$)", RegexOption.IGNORE_CASE)
        )
        
        expectedPatterns.forEach { pattern ->
            val match = pattern.find(text)
            if (match != null) {
                return match.groupValues[1].trim()
            }
        }
        
        return "Expected behavior is achieved"
    }
}
