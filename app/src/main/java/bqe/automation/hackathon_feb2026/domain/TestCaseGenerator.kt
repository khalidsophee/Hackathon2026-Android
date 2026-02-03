package bqe.automation.hackathon_feb2026.domain

import bqe.automation.hackathon_feb2026.data.model.GeneratedTestCase

class TestCaseGenerator {
    
    fun generateTestCases(
        storyDescription: String?,
        acceptanceCriteria: String?
    ): List<GeneratedTestCase> {
        val testCases = mutableListOf<GeneratedTestCase>()
        
        // Extract acceptance criteria
        val criteria = parseAcceptanceCriteria(acceptanceCriteria)
        
        // Generate test cases from acceptance criteria
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
        
        // Generate additional test cases from story description
        if (storyDescription != null) {
            val additionalTests = generateFromDescription(storyDescription)
            testCases.addAll(additionalTests)
        }
        
        return testCases
    }
    
    private fun parseAcceptanceCriteria(criteria: String?): List<Criterion> {
        if (criteria.isNullOrBlank()) return emptyList()
        
        val parsedCriteria = mutableListOf<Criterion>()
        
        // Try to parse different formats
        // Format 1: Bullet points with "Given-When-Then" or "As a... I want... So that..."
        val lines = criteria.split("\n").map { it.trim() }
        
        var currentCriterion: Criterion? = null
        
        lines.forEach { line ->
            when {
                line.startsWith("-") || line.startsWith("*") || line.startsWith("•") -> {
                    val content = line.removePrefix("-").removePrefix("*").removePrefix("•").trim()
                    if (content.isNotEmpty()) {
                        currentCriterion = Criterion(
                            summary = extractSummary(content),
                            description = content,
                            steps = extractSteps(content),
                            expectedResult = extractExpectedResult(content)
                        )
                        parsedCriteria.add(currentCriterion)
                    }
                }
                line.matches(Regex("\\d+\\.")) || line.matches(Regex("\\d+\\)")) -> {
                    val content = line.substringAfter(".").substringAfter(")").trim()
                    if (content.isNotEmpty()) {
                        currentCriterion = Criterion(
                            summary = extractSummary(content),
                            description = content,
                            steps = extractSteps(content),
                            expectedResult = extractExpectedResult(content)
                        )
                        parsedCriteria.add(currentCriterion)
                    }
                }
            }
        }
        
        // If no structured format found, create one test case per sentence
        if (parsedCriteria.isEmpty()) {
            val sentences = criteria.split(Regex("[.!?]")).map { it.trim() }.filter { it.isNotEmpty() }
            sentences.forEach { sentence ->
                parsedCriteria.add(
                    Criterion(
                        summary = extractSummary(sentence),
                        description = sentence,
                        steps = listOf("Navigate to the feature", "Perform the action", "Verify the result"),
                        expectedResult = "Expected behavior is achieved"
                    )
                )
            }
        }
        
        return parsedCriteria
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
        val testCases = mutableListOf<GeneratedTestCase>()
        
        // Generate positive test case
        testCases.add(
            GeneratedTestCase(
                title = "TC-Positive: Verify basic functionality",
                description = "Verify that the feature works correctly with valid inputs",
                steps = listOf(
                    "Open the application",
                    "Navigate to the feature",
                    "Perform valid actions",
                    "Verify successful completion"
                ),
                expectedResult = "Feature works as expected",
                priority = "High"
            )
        )
        
        // Generate negative test case
        testCases.add(
            GeneratedTestCase(
                title = "TC-Negative: Verify error handling",
                description = "Verify that the feature handles invalid inputs gracefully",
                steps = listOf(
                    "Open the application",
                    "Navigate to the feature",
                    "Perform invalid actions",
                    "Verify error message is displayed"
                ),
                expectedResult = "Appropriate error message is shown",
                priority = "Medium"
            )
        )
        
        return testCases
    }
    
    private data class Criterion(
        val summary: String,
        val description: String,
        val steps: List<String>,
        val expectedResult: String
    )
}
