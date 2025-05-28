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

    // 외부에서 태그 검색 모드로 진입했는지 확인하는 플래그
    private var isExternalTagSearch = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        makeStatusBarTransparent()

        // Arguments에서 태그 검색 정보 확인
        arguments?.let { args ->
            val searchTag = args.getString("search_tag")
            val isTagSearch = args.getBoolean("is_tag_search", false)

            if (isTagSearch && !searchTag.isNullOrEmpty()) {
                isExternalTagSearch = true
                selectedTag = searchTag
                isSearchByTag = true

                Log.d("HomeFragment", "Arguments에서 태그 검색 정보 확인: $searchTag")

                // 즉시 태그 칩 추가
                addTagIfNotExists(searchTag)
            }
        }

        // ViewPager 구성
        val pagerAdapter = HomePagerAdapter(this)
        binding.viewPager.adapter = pagerAdapter

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateTabs(position)

                // 현재 선택된 탭에 맞게 데이터 갱신
                if (isSearchByTag && selectedTag != null) {
                    // 검색 중이면 검색 결과 다시 적용
                    Log.d("HomeFragment", "ViewPager 페이지 변경 - 태그 검색 유지: $selectedTag")
                    searchNotesByTag(selectedTag!!)
                } else if (!isSearchByTag && !isExternalTagSearch) {
                    // 태그 검색도 아니고 외부 태그 검색 모드도 아닐 때만 기본 노트 목록 로드
                    Log.d("HomeFragment", "ViewPager 페이지 변경 - 전체 노트 로드")
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

            // 태그 선택 시 해당 태그로 노트 검색
            selectedTag = clickedTag.name
            isSearchByTag = true
            searchNotesByTag(selectedTag!!)
        }

        // 검색 실시간 반영
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim()
                if (query.isNotEmpty()) {
                    tagViewModel.searchTags(query)
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.ivClearSearch.setOnClickListener {
            binding.etSearch.text.clear()
            // 검색어를 지울 때만 검색창 내용 클리어, 태그 칩은 유지
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

        // LiveData 관찰
        tagViewModel.filteredTags.observe(viewLifecycleOwner) { tags ->
            // 필요시 자동완성 기능 구현
        }

        // Fragment 생성 시 외부 태그 검색이 있다면 실행
        if (isExternalTagSearch && !selectedTag.isNullOrEmpty()) {
            // Fragment가 완전히 로드된 후 태그 검색 실행
            binding.root.post {
                Log.d("HomeFragment", "외부 태그 검색 자동 실행: $selectedTag")
                searchNotesByTag(selectedTag!!)
            }
        } else if (!isSearchByTag && !isExternalTagSearch) {
            // 일반적인 홈 화면 진입 시에만 전체 노트 로드
            Log.d("HomeFragment", "일반 홈 화면 진입 - 전체 노트 로드")
            noteViewModel.loadNotes(memberId = 1L, page = 0, size = 10)
        }
    }

    private fun resetNoteSearch() {
        Log.d("HomeFragment", "노트 목록 리셋 실행")

        // 전체 노트 목록으로 복원
        noteViewModel.resetNotes()
        noteViewModel.loadNotes(memberId = 1L, page = 0, size = 10)

        // HomeFragment의 상태 변수 초기화
        isSearchByTag = false
        selectedTag = null
        isExternalTagSearch = false // 외부 태그 검색 모드도 초기화

        // 선택된 태그 칩 초기화
        binding.horizontalTagContainer.removeAllViews()
        addedTags.clear()

        // 검색창도 클리어
        binding.etSearch.text.clear()
    }

    fun searchNotesByTag(tagName: String) {
        Log.d("HomeFragment", "태그 검색 시작: $tagName")

        // ViewModel을 통한 태그 검색
        noteViewModel.searchNotesByTag(tagName, 1L, 0, 10)

        // LiveData 관찰 - 기존 옵저버 제거하고 새로 등록
        noteViewModel.notes.removeObservers(viewLifecycleOwner)
        noteViewModel.notes.observe(viewLifecycleOwner) { notes ->
            Log.d("HomeFragment", "태그 검색 결과 받음: ${notes.size}개")
            if (notes.isEmpty()) {
                //Toast.makeText(requireContext(), "'$tagName' 태그가 포함된 노트가 없습니다", Toast.LENGTH_SHORT).show()
            } else {
                //Toast.makeText(requireContext(), "'$tagName' 태그 검색 결과: ${notes.size}개의 노트", Toast.LENGTH_SHORT).show()
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
            Log.d("HomeFragment", "태그 칩 추가: $name")
        } else {
            Log.d("HomeFragment", "태그 칩 이미 존재: $name")
        }
    }

    private fun addChipForTag(tag: Tag) {
        val chipView = LayoutInflater.from(requireContext())
            .inflate(R.layout.item_tag_chip, null)

        val tagText = chipView.findViewById<TextView>(R.id.tagName)
        val btnDelete = chipView.findViewById<ImageView>(R.id.btnDelete)

        tagText.text = "# ${tag.name}"

        btnDelete.setOnClickListener {
            Log.d("HomeFragment", "태그 칩 삭제 클릭: ${tag.name}")
            addedTags.removeIf { it.name.equals(tag.name, ignoreCase = true) }
            (chipView.parent as? ViewGroup)?.removeView(chipView)

            // 선택된 태그를 삭제한 경우, 원래 노트 목록으로 돌아가기
            if (isSearchByTag && selectedTag == tag.name) {
                resetNoteSearch()
            }
        }

        // 태그 클릭 시 해당 태그로 검색 (재검색)
        chipView.setOnClickListener {
            Log.d("HomeFragment", "태그 칩 클릭: ${tag.name}")
            selectedTag = tag.name
            isSearchByTag = true
            searchNotesByTag(selectedTag!!)
        }

        // 마진 설정하여 칩 간 여백 주기
        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        layoutParams.setMargins(0, 0, 16, 0) // 오른쪽 마진 16dp
        chipView.layoutParams = layoutParams

        binding.horizontalTagContainer.addView(chipView)
        Log.d("HomeFragment", "태그 칩이 컨테이너에 추가됨: ${tag.name}")
    }

    /**
     * 외부에서 태그 검색을 요청할 때 사용하는 메소드
     * (버블 차트에서 태그 클릭 시 호출됨)
     */
    fun searchFromExternalTag(tagName: String) {
        Log.d("HomeFragment", "외부에서 태그 검색 요청: $tagName")

        // 외부 태그 검색 모드 활성화
        isExternalTagSearch = true

        // 기존 검색 상태는 초기화하지 않고, 새로운 태그만 추가
        // 기존 태그들은 유지하면서 새 태그 추가
        addTagIfNotExists(tagName)

        // 태그 검색 실행
        selectedTag = tagName
        isSearchByTag = true

        // 검색 실행
        searchNotesByTag(tagName)

        // 검색 결과 메시지 표시
        //Toast.makeText(requireContext(), "'$tagName' 태그로 검색합니다", Toast.LENGTH_SHORT).show()

        Log.d("HomeFragment", "현재 태그 컨테이너 자식 수: ${binding.horizontalTagContainer.childCount}")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun getCurrentTagSearchState(): Pair<Boolean, String?> {
        return Pair(isSearchByTag, selectedTag)
    }
}