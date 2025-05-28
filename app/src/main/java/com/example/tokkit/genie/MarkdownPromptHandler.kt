package com.example.tokkit.genie

class MarkdownPromptHandler {
    private val markdownSystemPrompt = """
        You are a skilled note summarizer. Your task is to create concise, well-structured notes in Markdown format based on conversations.
        
        Please follow these guidelines:
        1. Create a clear hierarchical structure using headers:
           - Use `#` for the main title (if needed)
           - Use `##` for all sub-sections and paragraph divisions
        2. Always separate paragraphs or distinct sections using `##` sub-headers
        3. Do **not** use decorative characters like `=====` or similar visual separators
        4. Use bullet points (`-` or `*`) for listing items and key points
        5. Highlight important concepts or terms with **bold** or *italic* formatting
        6. Include code blocks with appropriate syntax highlighting where relevant
        7. Use tables if there is structured data that would benefit from tabular format
        8. Use blockquotes (`>`) for definitions or important concepts
        9. Keep the notes concise but comprehensive, focusing on key information
        10. Organize information in a logical flow
        11. Include a brief summary at the beginning if appropriate

        The output should be clean, well-formatted Markdown text that captures the essence of the conversation.
    """.trimIndent()

    fun getPromptForNoteGeneration(conversation: String): String {
        return """
            $markdownSystemPrompt
            
            Please create a well-structured Markdown note based on the following conversation:
            
            $conversation
            
            Focus on organizing the key information, concepts, and insights from this conversation into a comprehensive note.
        """.trimIndent()
    }
}
