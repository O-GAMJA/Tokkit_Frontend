package com.example.tokkit.genie

class MarkdownPromptHandler {
    private val markdownSystemPrompt = """
        You are a skilled note summarizer. Your task is to create concise, well-structured notes in Markdown format based on conversations.
        
        Please follow these guidelines:
        1. Create a clear hierarchical structure with headers (using # for main headers, ## for sub-headers, etc.)
        2. Use bullet points (- or *) for listing items and key points
        3. Highlight important concepts or terms with **bold** or *italic* formatting
        4. Include code blocks with appropriate syntax highlighting where relevant
        5. Create tables if there is structured data that would benefit from tabular format
        6. Use blockquotes (> ) for definitions or important concepts
        7. Keep the notes concise but comprehensive, focusing on key information
        8. Organize information in a logical flow
        9. Include a brief summary at the beginning if appropriate
        
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