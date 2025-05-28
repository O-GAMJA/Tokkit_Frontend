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
import com.example.tokkit.MainActivity

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

            if (result.resultCode == Activity.RESULT_OK) {
                val isBookmarkChanged = result.data?.getBooleanExtra("isBookmarkChanged", false) ?: false
                if (isBookmarkChanged) {
                    // MainActivity의 디렉토리 트리 새로고침 요청
                    (requireActivity() as? MainActivity)?.reloadDirectoryTree()
                }
            }

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

        Log.d("CardViewFragment", "=== onViewCreated 호출됨 ===")
        setupRecyclerView()
        observeViewModel()
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

                // registerForActivityResult 사용
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
            Log.d("CardViewFragment", "=== Observer 호출됨 ===")
            Log.d("CardViewFragment", "받은 노트 개수: ${notes.size}")
            Log.d("CardViewFragment", "현재 어댑터 아이템 개수: ${adapter.itemCount}")

            // 노트 ID들 출력
            notes.forEachIndexed { index, note ->
                Log.d("CardViewFragment", "노트 $index: ID=${note.id}, 제목=${note.title}")
            }

            // 중복 ID 체크
            val ids = notes.map { it.id }
            val uniqueIds = ids.toSet()
            if (ids.size != uniqueIds.size) {
                Log.e("CardViewFragment", "❌ 중복된 ID 발견! 전체: ${ids.size}, 유니크: ${uniqueIds.size}")
                Log.e("CardViewFragment", "중복 ID들: ${ids.groupBy { it }.filter { it.value.size > 1 }.keys}")
            } else {
                Log.d("CardViewFragment", "✅ ID 중복 없음")
            }

            adapter.submitList(notes.toList())
        }

        // 로딩 상태 관찰
        noteViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            Log.d("CardViewFragment", "로딩 상태: $isLoading")
            binding.progressBar?.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        // 에러 상태 관찰
        noteViewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Log.d("CardViewFragment", "에러: $error")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        Log.d("CardViewFragment", "=== onResume 호출됨 ===")
        Log.d("CardViewFragment", "현재 ViewModel의 노트 개수: ${noteViewModel.notes.value?.size}")
    }

    // onResume()에서 observeViewModel() 재호출 제거
    // Fragment의 생명주기에서 observeViewModel()은 onViewCreated()에서 한 번만 호출하는 것이 정상


}