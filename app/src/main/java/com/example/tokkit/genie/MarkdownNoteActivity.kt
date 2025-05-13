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


class MarkdownNoteActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMarkdownNoteBinding
    private lateinit var markwon: Markwon

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
        val markdownContent = intent.getStringExtra(EXTRA_MARKDOWN_CONTENT) ?: ""
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "대화 요약"

        // 로그 추가
        Log.d("MarkdownNote", "수신된 마크다운 내용: $markdownContent")
        Log.d("MarkdownNote", "수신된 제목: $title")

        binding.tvTitle.text = title

        // 마크다운 내용 표시
        markwon.setMarkdown(binding.tvMarkdownContent, markdownContent)

        // 편집 모드로 전환
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
            markwon.setMarkdown(binding.tvMarkdownContent, editedContent)

            binding.tvMarkdownContent.visibility = View.VISIBLE
            binding.markdownEditor.visibility = View.GONE
            binding.btnEdit.visibility = View.VISIBLE
            binding.btnSave.visibility = View.GONE

            Toast.makeText(this, "노트가 저장되었습니다", Toast.LENGTH_SHORT).show()
            // TODO: 노트 저장 로직 구현
        }

        //노트 저장 버튼
        binding.btnSaveNext.setOnClickListener{
            val intent = Intent(this, NoteDetailsActivity::class.java)
            startActivity(intent)

        }
        // 뒤로가기 버튼
        binding.btnBack.setOnClickListener {
            finish()
        }
    }
}