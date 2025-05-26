package com.example.tokkit

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tokkit.adapter.NoteAdapter
import com.example.tokkit.data.remote.model.Note
import com.example.tokkit.databinding.FragmentReviewBinding
import io.noties.markwon.Markwon

class ReviewFragment : Fragment() {

    private var _binding: FragmentReviewBinding? = null
    private val binding get() = _binding!!

    private val stageButtons = mutableListOf<TextView>()
    private var selectedStage: Int = 0 // 0은 전체, 1-5는 각 단계

    private lateinit var viewModel: ReviewViewModel
    private lateinit var noteAdapter: NoteAdapter
    private lateinit var markwon: Markwon
    private val allNotes = mutableListOf<Note>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ViewModel 초기화
        viewModel = ViewModelProvider(this)[ReviewViewModel::class.java]

        // Markwon 초기화
        markwon = Markwon.create(requireContext())

        // 어댑터 초기화
        noteAdapter = NoteAdapter(
            onItemClick = { note ->
                val intent = Intent(requireContext(), ForgettingCurveActivity::class.java)
                intent.putExtra("NOTE_ID", note.id)
                intent.putExtra("NOTE_CONTENT", note.content)
                intent.putExtra("ARTICLE_TITLE", note.title)
                intent.putExtra("ARTICLE_STAGE", note.tags?.find { it.startsWith("단계") }?.removePrefix("단계")?.toIntOrNull() ?: 0)
                startActivity(intent)
            },
            useCardLayout = true,  // 카드 레이아웃 사용
            markwon = markwon
        )

        // 단계 버튼 초기화
        initStageButtons()

        // 리사이클러뷰 설정
        setupRecyclerView()

        // 노트 + 단계 정보 불러오기
        viewModel.loadNotesWithStages(memberId = 1L) // 실제 사용자 ID로 대체

        // LiveData 관찰
        viewModel.notes.observe(viewLifecycleOwner) { notes ->
            allNotes.clear()
            allNotes.addAll(notes)
            updateNoteList()
        }

    }

    private fun initStageButtons() {
        // 각 버튼을 리스트에 저장
        stageButtons.add(binding.btnAll)
        stageButtons.add(binding.btnStep1)
        stageButtons.add(binding.btnStep2)
        stageButtons.add(binding.btnStep3)
        stageButtons.add(binding.btnStep4)
        stageButtons.add(binding.btnStep5)

        // 버튼 클릭 이벤트 설정
        for (i in stageButtons.indices) {
            stageButtons[i].setOnClickListener {
                updateSelectedStage(i)
                updateNoteList()
            }
        }
    }

    private fun updateSelectedStage(stage: Int) {
        selectedStage = stage

        // 선택된 단계에 따라 버튼 스타일 변경
        for (i in stageButtons.indices) {
            if (i == stage) {
                // 선택된 버튼 스타일
                stageButtons[i].setBackgroundResource(R.drawable.rounded_stage_bg)
                stageButtons[i].setTextColor(ContextCompat.getColor(requireContext(), R.color.stageBtn))
            } else {
                // 선택되지 않은 버튼 스타일
                stageButtons[i].setBackgroundResource(R.drawable.rounded_stage_bg2)
                stageButtons[i].setTextColor(ContextCompat.getColor(requireContext(), R.color.gray))
            }
        }
    }

    private fun setupRecyclerView() {
        binding.recyclerReviewArticles.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerReviewArticles.adapter = noteAdapter
    }

    private fun updateNoteList() {
        val filtered = if (selectedStage == 0) {
            allNotes
        } else {
            allNotes.filter { note ->
                note.tags?.any { it == "단계${selectedStage - 1}" } ?: false
            }
        }

        noteAdapter.submitList(filtered) {
            binding.recyclerReviewArticles.scrollToPosition(0)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        // 복습 완료 후 Fragment로 돌아왔을 때도 데이터 새로고침
        if (::viewModel.isInitialized) {
            viewModel.loadNotesWithStages(memberId = 1L)
        }
    }
}