package com.example.tokkit

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.tokkit.databinding.ActivityNoteMarkdownBinding
import io.noties.markwon.Markwon
import io.noties.markwon.editor.MarkwonEditor
import io.noties.markwon.editor.MarkwonEditorTextWatcher
import java.util.concurrent.Executors

class NoteMarkdownActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNoteMarkdownBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNoteMarkdownBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Markwon 기본 설정
        val markwon = Markwon.create(this)
        val editor = MarkwonEditor.create(markwon)

        // 마크다운 TextWatcher 적용 (실시간 하이라이팅)
        val textWatcher = MarkwonEditorTextWatcher.withPreRender(
            editor,
            Executors.newCachedThreadPool(),
            binding.markdownEditor
        )

        // 텍스트 변경 리스너 연결
        binding.markdownEditor.addTextChangedListener(textWatcher)

        // 초기값 마크다운 입력
        binding.markdownEditor.setText(
            """
            ## 운영체제란?
            운영체제는 사용자와 하드웨어 간의 **인터페이스**를 제공하여 시스템 자원을 효율적으로 관리하는 소프트웨어입니다.

            ## 주요 역할
            - **자원 관리:** CPU, 메모리, 저장장치, 입력장치 등의 자원을 할당 및 회수
            - **작업 제어:** 다중 사용자/다중 작업 상황에서 자원을 조율

            ## 예시
            대표적 운영체제는 *Windows, Linux, macOS* 등이 있으며, 각기 다른 기능과 특징이 존재합니다.
            """.trimIndent()
        )

        // 뒤로가기
        binding.btnBack.setOnClickListener { finish() }

        // 저장 버튼
        binding.btnSave.setOnClickListener {
            val noteContent = binding.markdownEditor.text.toString()
            Toast.makeText(this, "노트가 저장되었습니다.", Toast.LENGTH_SHORT).show()

            val userTitle = binding.tvTitle.text.toString()
            Log.d("NoteMarkdown", "NoteDetailsActivity로 제목 전달: $userTitle")

            val intent = Intent(this, NoteDetailsActivity::class.java)
            intent.putExtra("MARKDOWN_CONTENT", noteContent)
            intent.putExtra("NOTE_TITLE", userTitle)
            intent.putExtra("SHOW_IMAGE_UPLOAD_BUTTON", true) // 이미지 업로드 버튼 표시 플래그 추가
            startActivity(intent)
        }
    }
}
