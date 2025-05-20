package com.example.tokkit.fragment

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tokkit.NoteViewModel
import com.example.tokkit.databinding.FragmentCardViewBinding
import com.example.tokkit.adapter.NoteAdapter
import com.example.tokkit.SearchDetailActivity
import io.noties.markwon.Markwon
import android.app.Activity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.RecyclerView
import com.example.tokkit.HomeFragment

class CardViewFragment : Fragment() {

    private var _binding: FragmentCardViewBinding? = null
    private val binding get() = _binding!!
    private val noteViewModel: NoteViewModel by activityViewModels()
    private lateinit var adapter: NoteAdapter
    private var currentPage: Int = 0

    private val detailLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val deleted = result.data?.getBooleanExtra("noteDeleted", false) ?: false
            val modified = result.data?.getBooleanExtra("noteModified", false) ?: false
            val wasTagSearch = result.data?.getBooleanExtra("wasTagSearch", false) ?: false
            val tagName = result.data?.getStringExtra("tagName")

            if (deleted || modified) {
                if (wasTagSearch && tagName != null) {
                    // 태그 검색 상태였으면 태그 검색 결과 다시 로드
                    val homeFragment = parentFragment as? HomeFragment
                    homeFragment?.searchNotesByTag(tagName)
                } else {
                    // 일반 상태였으면 전체 노트 로드
                    noteViewModel.loadNotes(memberId = 1L, page = currentPage, size = 10)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCardViewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeViewModel()
        setupRecyclerView()
        loadData()
    }

    private fun setupRecyclerView() {
        val markwon = Markwon.create(requireContext())

        adapter = NoteAdapter(
            onItemClick = { note ->
                val intent = Intent(requireContext(), SearchDetailActivity::class.java)
                intent.putExtra("NOTE_ID", note.id)

                // 태그 검색 상태 전달 추가
                val homeFragment = parentFragment as? HomeFragment
                val (isTagSearch, tagName) = homeFragment?.getCurrentTagSearchState() ?: Pair(false, null)
                intent.putExtra("isTagSearch", isTagSearch)
                intent.putExtra("tagName", tagName)

                detailLauncher.launch(intent)
            },
            useCardLayout = true,
            markwon = markwon
        )

        val layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerCardView.layoutManager = layoutManager
        binding.recyclerCardView.adapter = adapter

        binding.recyclerCardView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val totalItemCount = layoutManager.itemCount
                val lastVisibleItem = layoutManager.findLastVisibleItemPosition()

                if (lastVisibleItem + 3 >= totalItemCount) {
                    // 태그 검색 모드 여부 확인
                    if (noteViewModel.isTagSearchMode.value == true) {
                        noteViewModel.loadMoreNotesByTag()
                    } else {
                        noteViewModel.loadMoreNotes(memberId = 1L)
                    }
                }
            }
        })
    }

    private fun observeViewModel() {
        // 노트 데이터 관찰
        noteViewModel.notes.observe(viewLifecycleOwner) { notes ->
            Log.d("CardViewFragment", "노트 목록 업데이트됨: ${notes.size}개")
            adapter.submitList(notes)
        }

        // 로딩 상태 관찰
        noteViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar?.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        // 에러 상태 관찰
        noteViewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                //Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun loadData() {
        Log.d("CardViewFragment", "loadData() 호출됨")
        noteViewModel.resetNotes() // 초기화
        // 실제 로그인 사용자 ID로 대체해야 함
        noteViewModel.loadNotes(memberId = 1L, page = 0, size = 10)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        // ViewModel 관찰 다시 설정
        observeViewModel()
    }
}