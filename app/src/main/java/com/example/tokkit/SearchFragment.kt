package com.example.tokkit

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tokkit.adapter.GenericArticleAdapter
import com.example.tokkit.databinding.FragmentSearchBinding
import com.example.tokkit.model.Article

class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    // 임시 데이터
    private val allArticles = mutableListOf<Article>()
    private lateinit var adapter: GenericArticleAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 임시 데이터 로드
        loadSampleData()

        // 리사이클러뷰 설정
        setupRecyclerView()

        // 검색 입력 리스너 설정
        setupSearchListener()

        // 초기 상태는 검색 결과 없음 표시
        updateUI("")

        // 초기 필터 선택 (유사도순)
        updateFilterSelection(0)
    }

    private fun setupRecyclerView() {
        binding.recyclerSearchResults.layoutManager = LinearLayoutManager(requireContext())
        adapter = GenericArticleAdapter(emptyList()) { article ->
            val intent = Intent(requireContext(), SearchDetailActivity::class.java)
            intent.putExtra("ARTICLE_TITLE", article.title)
            intent.putExtra("ARTICLE_CONTENT", article.content)
            intent.putExtra("ARTICLE_IMAGE", article.imageResId)
            startActivity(intent)
        }
        binding.recyclerSearchResults.adapter = adapter
    }

    private fun setupSearchListener() {
        // 검색어 입력 리스너
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim()
                updateUI(query)
            }
        })

        // X 버튼 클릭 - 검색어 지우기
        binding.ivClearSearch.setOnClickListener {
            binding.etSearch.text.clear()
            updateUI("")
        }

        // 필터 버튼 선택 처리
        binding.btnSimilarity.setOnClickListener { updateFilterSelection(0) }
        binding.btnScrap.setOnClickListener { updateFilterSelection(1) }
        binding.btnLatest.setOnClickListener { updateFilterSelection(2) }
    }

    private fun updateFilterSelection(selectedIndex: Int) {
        // 모든 버튼 초기화
        binding.btnSimilarity.apply {
            background = ContextCompat.getDrawable(requireContext(), R.drawable.rounded_stage_bg2)
            setTextColor(ContextCompat.getColor(requireContext(), R.color.gray))
        }
        binding.btnScrap.apply {
            background = ContextCompat.getDrawable(requireContext(), R.drawable.rounded_stage_bg2)
            setTextColor(ContextCompat.getColor(requireContext(), R.color.gray))
        }
        binding.btnLatest.apply {
            background = ContextCompat.getDrawable(requireContext(), R.drawable.rounded_stage_bg2)
            setTextColor(ContextCompat.getColor(requireContext(), R.color.gray))
        }

        // 선택된 버튼 강조
        when(selectedIndex) {
            0 -> binding.btnSimilarity.apply {
                background = ContextCompat.getDrawable(requireContext(), R.drawable.rounded_stage_bg)
                setTextColor(ContextCompat.getColor(requireContext(), R.color.stageBtn))
            }
            1 -> binding.btnScrap.apply {
                background = ContextCompat.getDrawable(requireContext(), R.drawable.rounded_stage_bg)
                setTextColor(ContextCompat.getColor(requireContext(), R.color.stageBtn))
            }
            2 -> binding.btnLatest.apply {
                background = ContextCompat.getDrawable(requireContext(), R.drawable.rounded_stage_bg)
                setTextColor(ContextCompat.getColor(requireContext(), R.color.stageBtn))
            }
        }

        // 여기에 필터에 따른 정렬 로직 추가
    }

    private fun updateUI(query: String) {
        if (query.isEmpty()) {
            // 검색어가 없을 때
            binding.recyclerSearchResults.visibility = View.GONE
            binding.emptyResultView.visibility = View.VISIBLE
            adapter.submitList(emptyList())
        } else {
            // 검색어로 필터링
            val filteredList = allArticles.filter {
                it.title.contains(query, ignoreCase = true) ||
                        it.content.contains(query, ignoreCase = true)
            }

            if (filteredList.isEmpty()) {
                // 검색 결과 없음
                binding.recyclerSearchResults.visibility = View.GONE
                binding.emptyResultView.visibility = View.VISIBLE
                binding.tvEmptyResult.text = "'$query'에 대한 검색 결과가 없습니다."
            } else {
                // 검색 결과 표시
                binding.recyclerSearchResults.visibility = View.VISIBLE
                binding.emptyResultView.visibility = View.GONE
                adapter.submitList(filteredList)
            }
        }
    }

    private fun loadSampleData() {
        // 샘플 데이터 - 실제로는 DB나 API에서 가져오는 로직이 들어갈 것
        allArticles.add(
            Article(
                "운영체제 1-1",
                "# 운영체제란?\n운영체제는 사용자와 하드웨어 간의 **인터페이스**를 제공하여 시스템 자원을 효율적으로 관리하는 소프트웨어입니다.\n\n## 주요 역할\n- **자원 관리:** CPU, 메모리, 저장장치, 입력장치 등의 자원을 할당 및 회수\n- **작업 제어:** 다중 사용자/다중 작업 상황에서 자원을 조율\n\n## 예시\n대표적인 운영체제로는 **Windows, Linux, macOS** 등이 있으며, 각각의 구조와 기능이 다르지만 사용자 요구를 충족시키는 방향으로 발전하고 있습니다.",
                "2024.01.04",
                R.drawable.ic_tcp_ip
            )
        )
        allArticles.add(
            Article(
                "네트워크 - OSI 7계층",
                "OSI 7계층은 네트워크 통신을 7개의 계층으로 나눈 표준 모델로, 각 계층은 서로 다른 역할을 수행합니다.",
                "2024.01.05",
                R.drawable.ic_tcp_ip
            )
        )
        allArticles.add(
            Article(
                "HTTP 프로토콜",
                "HTTP는 웹 상에서 클라이언트와 서버 간에 요청/응답으로 데이터를 주고 받는 프로토콜입니다.",
                "2024.01.06",
                R.drawable.ic_tcp_ip
            )
        )
        allArticles.add(
            Article(
                "자바 프로그래밍 기초",
                "자바는 객체 지향 프로그래밍 언어로, 플랫폼 독립적인 특징을 가지고 있습니다.",
                "2024.01.07",
                R.drawable.ic_tcp_ip
            )
        )
        allArticles.add(
            Article(
                "데이터베이스 설계 원칙",
                "효율적인 데이터베이스 설계를 위한 정규화와 인덱싱 전략에 대한 설명입니다.",
                "2024.01.08",
                R.drawable.ic_tcp_ip
            )
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}