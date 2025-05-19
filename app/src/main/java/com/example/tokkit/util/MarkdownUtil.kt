package com.example.tokkit.util

/**
 * 마크다운 관련 유틸리티 클래스
 */
object MarkdownUtil {
    /**
     * 마크다운 텍스트에서 제목을 추출
     * 첫 번째 # 헤더를 제목으로 사용하며, 없을 경우 첫 번째 ** 볼드체 텍스트를 사용
     * 두 가지 모두 없을 경우 기본 제목을 반환
     *
     * @param markdown 마크다운 텍스트
     * @param defaultTitle 기본 제목 (제목이 없을 경우 사용)
     * @return 추출된 제목
     */
    fun extractTitleFromMarkdown(markdown: String, defaultTitle: String = "제목을 입력해주세요"): String {
        val lines = markdown.split("\n")

        // # 헤더 형식 찾기
        for (line in lines) {
            val trimmedLine = line.trim()
            if (trimmedLine.startsWith("# ")) {
                return trimmedLine.substring(2).trim()
            }
        }

        // ** 볼드체 형식 찾기
        for (line in lines) {
            val trimmedLine = line.trim()
            val boldPattern = "\\*\\*(.*?)\\*\\*".toRegex()
            val matcher = boldPattern.find(trimmedLine)
            if (matcher != null) {
                return matcher.groupValues[1].trim()
            }
        }

        return defaultTitle
    }

    /**
     * 마크다운 텍스트에서 제목을 추출하고 해당 제목을 내용에서 제거한 텍스트를 반환
     *
     * @param markdown 마크다운 텍스트
     * @param defaultTitle 기본 제목 (제목이 없을 경우 사용)
     * @return Pair(추출된 제목, 제목이 제거된 내용)
     */
    fun extractTitleAndRemoveFromContent(markdown: String, defaultTitle: String = "제목을 입력해주세요"): Pair<String, String> {
        val lines = markdown.split("\n")
        val contentBuilder = StringBuilder()
        var title = defaultTitle
        var titleLine: String? = null
        var titleFound = false

        // # 헤더 형식 찾기
        for (line in lines) {
            val trimmedLine = line.trim()
            if (!titleFound && trimmedLine.startsWith("# ")) {
                title = trimmedLine.substring(2).trim()
                titleLine = line
                titleFound = true
                continue
            }
            contentBuilder.append(line).append("\n")
        }

        // 헤더를 찾지 못했으면 볼드체 형식 찾기
        if (!titleFound) {
            contentBuilder.clear()
            val boldPattern = "\\*\\*(.*?)\\*\\*".toRegex()

            for (line in lines) {
                if (!titleFound) {
                    val matcher = boldPattern.find(line)
                    if (matcher != null) {
                        title = matcher.groupValues[1].trim()
                        // 볼드체 제목 제거
                        val newLine = line.replace(matcher.value, "").trim()
                        // 라인에 다른 내용이 있으면 추가
                        if (newLine.isNotEmpty()) {
                            contentBuilder.append(newLine).append("\n")
                        }
                        titleFound = true
                        continue
                    }
                }
                contentBuilder.append(line).append("\n")
            }
        }

        return Pair(title, contentBuilder.toString().trim())
    }
}