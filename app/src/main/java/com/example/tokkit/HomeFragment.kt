package com.example.tokkit

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.example.tokkit.adapter.HomePagerAdapter
import com.example.tokkit.adapter.TagListAdapter
import com.example.tokkit.data.local.entities.Tag
import com.example.tokkit.data.remote.api.NoteApiService
import com.example.tokkit.databinding.FragmentHomeBinding
import com.example.tokkit.util.RetrofitClient
import kotlinx.coroutines.launch
import androidx.recyclerview.widget.LinearLayoutManager

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val tagViewModel: TagViewModel by viewModels()
    private val noteViewModel: NoteViewModel by activityViewModels()
    private val addedTags = mutableListOf<Tag>()
    private lateinit var tagAdapter: TagListAdapter
    private var selectedTag: String? = null
    private var isSearchByTag = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        makeStatusBarTransparent()

        // ViewPager 구성
        val pagerAdapter = HomePagerAdapter(this)
        binding.viewPager.adapter = pagerAdapter

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateTabs(position)

                // 현재 선택된 탭에 맞게 데이터 갱신
                if (isSearchByTag && selectedTag != null) {
                    // 검색 중이면 검색 결과 다시 적용
                    searchNotesByTag(selectedTag!!)
                } else {
                    // 아니면 기본 노트 목록 로드
                    noteViewModel.resetNotes()
                    noteViewModel.loadNotes(memberId = 1L, page = 0, size = 10)
                }
            }
        })

        binding.tabCard.setOnClickListener { binding.viewPager.currentItem = 0 }
        binding.tabList.setOnClickListener { binding.viewPager.currentItem = 1 }

        // 어댑터 설정
        tagAdapter = TagListAdapter { clickedTag ->
            addTagIfNotExists(clickedTag.name)
            binding.etSearch.text.clear()
            binding.cardRecyclerWrapper.visibility = View.GONE

            // 태그 선택 시 해당 태그로 노트 검색
            selectedTag = clickedTag.name
            isSearchByTag = true
            searchNotesByTag(selectedTag!!)
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = tagAdapter

        // 검색 결과 관찰
        tagViewModel.filteredTags.observe(viewLifecycleOwner) { tags ->
            if (tags.isNotEmpty()) {
                tagAdapter.submitList(tags)
                binding.cardRecyclerWrapper.visibility = View.VISIBLE
            } else {
                tagAdapter.submitList(emptyList())
                binding.cardRecyclerWrapper.visibility = View.GONE
            }
        }

        // 검색 실시간 반영
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim()
                if (query.isNotEmpty()) {
                    tagViewModel.searchTags(query)
                    // 여기서 결과 RecyclerView에 반영하고 싶으면 추가 구현
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.ivClearSearch.setOnClickListener {
            binding.etSearch.text.clear()
            // 검색어를 지울 때 원래 전체 노트 목록으로 복원
            if (isSearchByTag) {
                isSearchByTag = false
                selectedTag = null
                resetNoteSearch()
            }
        }

        binding.etSearch.setOnEditorActionListener { _, _, _ ->
            val tagName = binding.etSearch.text.toString().trim()
            if (tagName.isNotEmpty()) {
                addTagIfNotExists(tagName)
                binding.etSearch.text.clear()

                // 태그 입력 후 엔터 시 해당 태그로 노트 검색
                selectedTag = tagName
                isSearchByTag = true
                searchNotesByTag(selectedTag!!)
            }
            true
        }

        // LiveData 관찰 (원하면 RecyclerView로 보여주기 가능)
        tagViewModel.filteredTags.observe(viewLifecycleOwner) { tags ->
        }
    }

    private fun resetNoteSearch() {
        // 태그 검색 모드 명시적으로 종료
        noteViewModel.exitTagSearchMode()

        // 전체 노트 목록으로 복원
        noteViewModel.resetNotes()
        noteViewModel.loadNotes(memberId = 1L, page = 0, size = 10)

        // 로딩 상태 확인을 위해 로그 추가
        Log.d("HomeFragment", "노트 목록 리셋 실행(태그)")

        // 선택된 태그 칩 초기화
        binding.horizontalTagContainer.removeAllViews()
        addedTags.clear()
    }

    private fun searchNotesByTag(tagName: String) {
        // 기존 직접 API 호출하는 코드 대신 ViewModel 함수 활용
        noteViewModel.searchNotesByTag(tagName, 1L, 0, 10)

        // LiveData 관찰을 통한 처리
        noteViewModel.notes.observe(viewLifecycleOwner) { notes ->
            if (notes.isEmpty()) {
                //Toast.makeText(requireContext(), "'$tagName' 태그가 포함된 노트가 없습니다", Toast.LENGTH_SHORT).show()
            } else {
                // Toast.makeText(requireContext(), "'$tagName' 태그 검색 결과: ${notes.size}개의 노트", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun makeStatusBarTransparent() {
        activity?.window?.apply {
            clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            statusBarColor = android.graphics.Color.TRANSPARENT

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                WindowCompat.setDecorFitsSystemWindows(this, false)
            }
        }
    }

    private fun updateTabs(position: Int) {
        when (position) {
            0 -> {
                binding.tabCard.backgroundTintList = resources.getColorStateList(R.color.main, requireActivity().theme)
                binding.tabList.backgroundTintList = resources.getColorStateList(R.color.gray2, requireActivity().theme)
            }
            1 -> {
                binding.tabCard.backgroundTintList = resources.getColorStateList(R.color.gray2, requireActivity().theme)
                binding.tabList.backgroundTintList = resources.getColorStateList(R.color.main, requireActivity().theme)
            }
        }
    }

    private fun addTagIfNotExists(name: String) {
        if (addedTags.none { it.name.equals(name, ignoreCase = true) }) {
            val newTag = Tag(name = name)
            addedTags.add(newTag)
            addChipForTag(newTag)
        }
    }

    private fun addChipForTag(tag: Tag) {
        val chipView = LayoutInflater.from(requireContext())
            .inflate(R.layout.item_tag_chip, null)

        val tagText = chipView.findViewById<TextView>(R.id.tagName)
        val btnDelete = chipView.findViewById<ImageView>(R.id.btnDelete)

        tagText.text = "# ${tag.name}"

        btnDelete.setOnClickListener {
            addedTags.removeIf { it.name.equals(tag.name, ignoreCase = true) }
            (chipView.parent as? ViewGroup)?.removeView(chipView)

            // 선택된 태그를 삭제한 경우, 원래 노트 목록으로 돌아가기
            if (isSearchByTag && selectedTag == tag.name) {
                isSearchByTag = false
                selectedTag = null
                resetNoteSearch()
            }
        }

        // 태그 클릭 시 해당 태그로 검색
        chipView.setOnClickListener {
            selectedTag = tag.name
            isSearchByTag = true
            searchNotesByTag(selectedTag!!)
        }

        //  여기에 마진을 설정하여 칩 간 여백 주기
        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        layoutParams.setMargins(0, 0, 16, 0) // 오른쪽 마진 16dp
        chipView.layoutParams = layoutParams

        binding.horizontalTagContainer.addView(chipView)
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
