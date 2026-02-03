package bqe.automation.hackathon_feb2026.data.model

// Atlassian Document Format (ADF) structures
data class AtlassianDocument(
    val version: Int = 1,
    val type: String = "doc",
    val content: List<ADFNode>
)

data class ADFNode(
    val type: String,
    val content: List<ADFNode>? = null,
    val text: String? = null
)

object ADFConverter {
    /**
     * Converts plain text to Atlassian Document Format
     */
    fun textToADF(text: String): AtlassianDocument {
        if (text.isBlank()) {
            return AtlassianDocument(
                content = listOf(
                    ADFNode(
                        type = "paragraph",
                        content = emptyList()
                    )
                )
            )
        }
        
        val lines = text.split("\n")
        val paragraphs = mutableListOf<ADFNode>()
        
        lines.forEach { line ->
            if (line.isBlank()) {
                // Empty line - create empty paragraph
                paragraphs.add(
                    ADFNode(
                        type = "paragraph",
                        content = emptyList()
                    )
                )
            } else {
                // Non-empty line - create paragraph with text
                val textNodes = splitIntoTextNodes(line)
                paragraphs.add(
                    ADFNode(
                        type = "paragraph",
                        content = textNodes
                    )
                )
            }
        }
        
        // Ensure at least one paragraph exists
        if (paragraphs.isEmpty()) {
            paragraphs.add(
                ADFNode(
                    type = "paragraph",
                    content = emptyList()
                )
            )
        }
        
        return AtlassianDocument(content = paragraphs)
    }
    
    private fun splitIntoTextNodes(text: String): List<ADFNode> {
        // Create a single text node with the line content
        if (text.isBlank()) {
            return emptyList()
        }
        return listOf(ADFNode(type = "text", text = text))
    }
}
