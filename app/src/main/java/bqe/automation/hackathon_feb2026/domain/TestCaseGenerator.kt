package bqe.automation.hackathon_feb2026.domain

import bqe.automation.hackathon_feb2026.data.model.GeneratedTestCase

class TestCaseGenerator(
    private val useLLM: Boolean = false,
    private val llmGenerator: LLMTestCaseGenerator? = null
) {
    
    suspend fun generateTestCases(
        storyDescription: String?,
        acceptanceCriteria: String?
    ): List<GeneratedTestCase> {
        println("=== TEST CASE GENERATOR: Received Input ===")
        println("Story Description: ${storyDescription ?: "NULL"}")
        println("Story Description length: ${storyDescription?.length ?: 0}")
        println("Acceptance Criteria: ${acceptanceCriteria ?: "NULL"}")
        println("Acceptance Criteria length: ${acceptanceCriteria?.length ?: 0}")
        println("Use LLM: $useLLM")
        println("LLM Generator available: ${llmGenerator != null}")
        println("============================================")
        
        // Try LLM first if enabled
        if (useLLM && llmGenerator != null) {
            println("✅ LLM is ENABLED - Calling ChatGPT...")
            val llmTestCases = llmGenerator.generateTestCasesWithLLM(storyDescription, acceptanceCriteria)
            // Use LLM results if we got any test cases
            if (llmTestCases.isNotEmpty()) {
                println("✅ LLM generated ${llmTestCases.size} test cases")
                return llmTestCases
            } else {
                println("❌ LLM returned empty, falling back to rule-based")
            }
            // Fallback to rule-based if LLM fails
        } else {
            println("❌ LLM is DISABLED or not available - using rule-based generation")
            println("   useLLM = $useLLM")
            println("   llmGenerator = ${if (llmGenerator != null) "available" else "NULL"}")
        }
        
        // Use rule-based generation
        println("Using rule-based generation...")
        val ruleBasedTestCases = generateTestCasesRuleBased(storyDescription, acceptanceCriteria)
        println("Rule-based generated ${ruleBasedTestCases.size} test cases")
        return ruleBasedTestCases
    }
    
    private fun generateTestCasesRuleBased(
        storyDescription: String?,
        acceptanceCriteria: String?
    ): List<GeneratedTestCase> {
        val testCases = mutableListOf<GeneratedTestCase>()
        
        // Extract acceptance criteria
        val criteria = parseAcceptanceCriteria(acceptanceCriteria)
        
        // Generate test cases from acceptance criteria - ONE PER CRITERION
        criteria.forEachIndexed { index, criterion ->
            testCases.add(
                GeneratedTestCase(
                    title = "TC-${index + 1}: Verify ${criterion.summary}",
                    description = "Test case to verify: ${criterion.description}",
                    steps = criterion.steps,
                    expectedResult = criterion.expectedResult,
                    priority = determinePriority(criterion)
                )
            )
        }
        
        // Don't generate generic test cases - only use acceptance criteria or LLM
        if (testCases.isEmpty()) {
            println("⚠️ No test cases generated from acceptance criteria.")
            println("   ⚠️ Please configure OpenAI API key to generate test cases from description.")
        }
        
        return testCases
    }
    
    private fun parseAcceptanceCriteria(criteria: String?): List<Criterion> {
        if (criteria.isNullOrBlank()) return emptyList()
        
        val parsedCriteria = mutableListOf<Criterion>()
        
        // Try to parse different formats
        val lines = criteria.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
        
        lines.forEach { line ->
            when {
                // Bullet points: -, *, •, or other bullet characters
                line.matches(Regex("^[-*•▪▫◦‣⁃]\\s+.+")) -> {
                    val content = line.replaceFirst(Regex("^[-*•▪▫◦‣⁃]\\s+"), "").trim()
                    if (content.isNotEmpty()) {
                        parsedCriteria.add(createCriterion(content))
                    }
                }
                // Numbered lists: 1., 2., 1), 2), etc.
                line.matches(Regex("^\\d+[.)]\\s+.+")) -> {
                    val content = line.replaceFirst(Regex("^\\d+[.)]\\s+"), "").trim()
                    if (content.isNotEmpty()) {
                        parsedCriteria.add(createCriterion(content))
                    }
                }
                // Lines starting with "AC", "AC:", "Given", "When", "Then"
                line.matches(Regex("^(AC|AC:|Given|When|Then|And|But):?\\s+.+", RegexOption.IGNORE_CASE)) -> {
                    val content = line.replaceFirst(Regex("^(AC|AC:|Given|When|Then|And|But):?\\s+", RegexOption.IGNORE_CASE), "").trim()
                    if (content.isNotEmpty()) {
                        parsedCriteria.add(createCriterion(content))
                    }
                }
                // Lines that look like criteria (contain keywords)
                line.length > 10 && (line.contains("should", ignoreCase = true) || 
                                    line.contains("must", ignoreCase = true) ||
                                    line.contains("verify", ignoreCase = true) ||
                                    line.contains("ensure", ignoreCase = true)) -> {
                    parsedCriteria.add(createCriterion(line))
                }
            }
        }
        
        // If no structured format found, split by sentences or common separators
        if (parsedCriteria.isEmpty()) {
            // Try splitting by multiple newlines first
            val paragraphs = criteria.split(Regex("\n{2,}")).map { it.trim() }.filter { it.isNotEmpty() }
            if (paragraphs.size > 1) {
                paragraphs.forEach { paragraph ->
                    parsedCriteria.add(createCriterion(paragraph))
                }
            } else {
                // Split by sentences
                val sentences = criteria.split(Regex("[.!?]")).map { it.trim() }.filter { it.isNotEmpty() && it.length > 10 }
                sentences.forEach { sentence ->
                    parsedCriteria.add(createCriterion(sentence))
                }
            }
        }
        
        return parsedCriteria
    }
    
    private fun createCriterion(content: String): Criterion {
        return Criterion(
            summary = extractSummary(content),
            description = content,
            steps = extractSteps(content),
            expectedResult = extractExpectedResult(content)
        )
    }
    
    private fun extractSummary(text: String): String {
        return text.take(50).let { if (it.length < text.length) "$it..." else it }
    }
    
    private fun extractSteps(text: String): List<String> {
        val steps = mutableListOf<String>()
        
        // Look for "Given", "When", "Then" patterns
        val givenWhenThen = Regex("(?:Given|When|Then|And|But)\\s+(.+?)(?=(?:Given|When|Then|And|But)|$)", RegexOption.IGNORE_CASE)
        val matches = givenWhenThen.findAll(text)
        
        if (matches.count() > 0) {
            matches.forEach { match ->
                steps.add(match.groupValues[1].trim())
            }
        } else {
            // Default steps
            steps.add("Navigate to the relevant screen")
            steps.add("Perform the required action")
            steps.add("Verify the expected outcome")
        }
        
        return steps
    }
    
    private fun extractExpectedResult(text: String): String {
        // Look for "should", "must", "expected" keywords
        val expectedPattern = Regex("(?:should|must|expected|verify|ensure)\\s+(.+?)(?:\\.|$)", RegexOption.IGNORE_CASE)
        val match = expectedPattern.find(text)
        return match?.groupValues?.get(1)?.trim() ?: "The feature works as expected"
    }
    
    private fun determinePriority(criterion: Criterion): String {
        val text = criterion.description.lowercase()
        return when {
            text.contains("critical") || text.contains("must") || text.contains("required") -> "High"
            text.contains("should") || text.contains("important") -> "Medium"
            else -> "Low"
        }
    }
    
    private fun generateFromDescription(description: String): List<GeneratedTestCase> {
        // Return empty list - we don't want fixed/predefined test cases
        // Only use LLM or acceptance criteria-based generation
        println("⚠️ Rule-based generation called but no acceptance criteria found. Returning empty list.")
        println("   Description: ${description.take(100)}...")
        println("   ⚠️ Please configure OpenAI API key to generate test cases from description.")
        return emptyList()
    }
    
    private data class Criterion(
        val summary: String,
        val description: String,
        val steps: List<String>,
        val expectedResult: String
    )
}
