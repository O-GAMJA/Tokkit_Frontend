package com.example.tokkit.util

/**
 * 마크다운 관련 유틸리티 클래스
 */
object MarkdownUtil {
    /**
     * 마크다운 텍스트에서 제목을 추출
     * 첫 번째 # 헤더를 제목으로 사용하며, 없을 경우 기본 제목을 반환
     *
     * @param markdown 마크다운 텍스트
     * @param defaultTitle 기본 제목 (제목이 없을 경우 사용)
     * @return 추출된 제목
     */
    fun extractTitleFromMarkdown(markdown: String, defaultTitle: String = "제목을 입력해주세요"): String {
        val lines = markdown.split("\n")
        for (line in lines) {
            val trimmedLine = line.trim()
            // # 으로 시작하는 헤더를 찾아 제목으로 사용
            if (trimmedLine.startsWith("# ")) {
                return trimmedLine.substring(2).trim()
            }
        }
        return defaultTitle
    }
}