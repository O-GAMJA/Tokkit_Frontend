package com.example.tokkit.genie

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.tokkit.NoteDetailsActivity
import com.example.tokkit.databinding.ActivityMarkdownNoteBinding
import io.noties.markwon.Markwon
import android.content.Intent
import com.example.tokkit.util.MarkdownUtil


class MarkdownNoteActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMarkdownNoteBinding
    private lateinit var markwon: Markwon
    private var markdownContent: String = ""

    companion object {
        const val EXTRA_MARKDOWN_CONTENT = "markdown_content"
        const val EXTRA_TITLE = "title"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMarkdownNoteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Markwon 초기화
        markwon = Markwon.create(this)

        // 인텐트에서 데이터 가져오기
        markdownContent = intent.getStringExtra(EXTRA_MARKDOWN_CONTENT) ?: ""
        var title = intent.getStringExtra(EXTRA_TITLE)

        // 제목이 null이거나, 비어있거나, 하드코딩된 기본값인 경우 마크다운에서 제목 추출
        if (title.isNullOrBlank() || title == "대화 요약<일단 하드코딩1>" || title == "대화 요약 하드코딩2") {
            title = MarkdownUtil.extractTitleFromMarkdown(markdownContent)
        }

        // 로그 추가
        Log.d("MarkdownNote", "수신된 마크다운 내용: $markdownContent")
        Log.d("MarkdownNote", "사용할 제목: $title")

        binding.tvTitle.setText(title)

        // 마크다운 내용 표시
        markwon.setMarkdown(binding.tvMarkdownContent, markdownContent)

        // 편집 모드로 전환f
        binding.btnEdit.setOnClickListener {
            binding.tvMarkdownContent.visibility = View.GONE
            binding.markdownEditor.visibility = View.VISIBLE
            binding.markdownEditor.setText(markdownContent)
            binding.btnEdit.visibility = View.GONE
            binding.btnSave.visibility = View.VISIBLE
        }

        // 저장 버튼
        binding.btnSave.setOnClickListener {
            val editedContent = binding.markdownEditor.text.toString()
            markdownContent = editedContent // 업데이트된 내용 저장
            markwon.setMarkdown(binding.tvMarkdownContent, editedContent)

            // 사용자가 직접 입력한 제목 유지
            val userTitle = binding.tvTitle.text.toString()

            // 제목이 비어있을 경우만 마크다운에서 제목 추출
            if (userTitle.isBlank()) {
                val extractedTitle = MarkdownUtil.extractTitleFromMarkdown(editedContent)
                binding.tvTitle.setText(extractedTitle)
            }
            // 그렇지 않으면 사용자가 입력한 제목 유지

            binding.tvMarkdownContent.visibility = View.VISIBLE
            binding.markdownEditor.visibility = View.GONE
            binding.btnEdit.visibility = View.VISIBLE
            binding.btnSave.visibility = View.GONE

            Toast.makeText(this, "노트가 저장되었습니다", Toast.LENGTH_SHORT).show()
        }
        //노트 저장 버튼
        binding.btnSaveNext.setOnClickListener {

            val intent = Intent(this, NoteDetailsActivity::class.java)

            // 현재 마크다운 내용을 가져옴 (편집 모드인 경우 에디터 내용, 아닌 경우 원본 내용)
            val currentMarkdownContent = if (binding.markdownEditor.visibility == View.VISIBLE) {
                binding.markdownEditor.text.toString()
            } else {
                // 인텐트에서 가져오려 하지 말고, 직접 markdownContent 변수 사용
                markdownContent
            }

            // 현재 제목 가져오기
            val currentTitle = binding.tvTitle.text.toString()

            Log.d("NoteDetail", "MarkDown-> NoteDetails로 전달할 마크다운 내용: $currentMarkdownContent")
            Log.d("NoteDetail", "MarkDown-> NoteDetails로 전달할 제목: $currentTitle")

            // 대화 내용과 마크다운 내용을 Intent에 추가
            val conversationText = ConversationManager.getConversationText()
            intent.putExtra("MARKDOWN_CONTENT", currentMarkdownContent)
            intent.putExtra("CONVERSATION_TEXT", conversationText)
            intent.putExtra("NOTE_TITLE", currentTitle)

            // NoteDetailActivity로 이동
            startActivity(intent)
        }

        // 뒤로가기 버튼
        binding.btnBack.setOnClickListener {
            finish()
        }
    }
}