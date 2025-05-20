package com.example.tokkit

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tokkit.adapter.SearchResultAdapter
import com.example.tokkit.data.remote.api.SearchApiService
import com.example.tokkit.data.remote.model.SimilarNoteItem
import com.example.tokkit.databinding.FragmentSearchBinding
import com.example.tokkit.util.RetrofitClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: SearchResultAdapter
    private var searchJob: Job? = null
    private val TAG = "SearchFragment"

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

        // 리사이클러뷰 설정
        setupRecyclerView()

        // 검색 입력 리스너 설정
        setupSearchListener()

        // 초기 상태는 검색 결과 없음 표시
        updateUI("")
    }

    private fun setupRecyclerView() {
        binding.recyclerSearchResults.layoutManager = LinearLayoutManager(requireContext())
        adapter = SearchResultAdapter { noteItem ->
            // 검색 결과 항목 클릭시 상세 화면으로 이동
            val intent = Intent(requireContext(), SearchDetailActivity::class.java)
            intent.putExtra("NOTE_ID", noteItem.noteId)
            startActivity(intent)
        }
        binding.recyclerSearchResults.adapter = adapter
    }

    private fun setupSearchListener() {
        // 검색어 입력 리스너 (타이핑 지연 적용)
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim()

                // 이전 검색 작업 취소
                searchJob?.cancel()

                if (query.isEmpty()) {
                    updateUI("")
                    return
                }

                // 타이핑 중 API 호출 지연 (300ms)
                searchJob = lifecycleScope.launch {
                    delay(300) // 타이핑 딜레이
                    performSearch(query)
                }
            }
        })

        // 키보드 검색 버튼 클릭 시
        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = binding.etSearch.text.toString().trim()
                if (query.isNotEmpty()) {
                    searchJob?.cancel()
                    lifecycleScope.launch {
                        performSearch(query)
                    }
                }
                return@setOnEditorActionListener true
            }
            false
        }

        // X 버튼 클릭 - 검색어 지우기
        binding.ivClearSearch.setOnClickListener {
            binding.etSearch.text.clear()
            updateUI("")
        }
    }

    private fun performSearch(query: String) {
        Log.d(TAG, "검색 시작: $query")

        // 로딩 표시
        binding.recyclerSearchResults.visibility = View.GONE
        binding.emptyResultView.visibility = View.GONE
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.createService(SearchApiService::class.java)
                    .searchFullText(query, 0, 20) // 페이지 0, 최대 20개 결과

                if (response.isSuccess) {
                    // 검색 성공
                    val searchResults = response.result.noteSearchResults
                    Log.d(TAG, "검색 결과: ${searchResults.size}개 항목")

                    // UI 업데이트
                    updateUIWithResults(query, searchResults)
                } else {
                    // API 응답은 성공했지만 결과가 실패인 경우
                    Log.e(TAG, "검색 API 결과 실패: ${response.message}")
                    showEmptyResult(query)
                }
            } catch (e: Exception) {
                // 예외 발생 시
                Log.e(TAG, "검색 중 오류 발생", e)
                showEmptyResult(query)
            } finally {
                // 로딩 표시 숨기기
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun updateUIWithResults(query: String, results: List<SimilarNoteItem>) {
        if (results.isEmpty()) {
            showEmptyResult(query)
            return
        }

        // 검색 결과를 score(관련도) 값이 높은 순으로 정렬
        val sortedResults = results.sortedByDescending { it.score }

        Log.d(TAG, "정렬된 결과: ${sortedResults.map { "${it.noteTitle} (score: ${it.score})" }}")

        // 검색어를 어댑터에 전달하여 하이라이트 적용
        (adapter as SearchResultAdapter).setSearchQuery(query)

        // 검색 결과 표시
        binding.recyclerSearchResults.visibility = View.VISIBLE
        binding.emptyResultView.visibility = View.GONE
        adapter.submitList(sortedResults)
    }

    private fun showEmptyResult(query: String) {
        binding.recyclerSearchResults.visibility = View.GONE
        binding.emptyResultView.visibility = View.VISIBLE
        binding.tvEmptyResult.text = "'$query'에 대한 검색 결과가 없습니다."
    }

    private fun updateUI(query: String) {
        if (query.isEmpty()) {
            // 검색어가 없을 때
            binding.recyclerSearchResults.visibility = View.GONE
            binding.emptyResultView.visibility = View.VISIBLE
            binding.tvEmptyResult.text = "검색어를 입력하세요"
            adapter.submitList(emptyList())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // 실행 중인 검색 작업 취소
        searchJob?.cancel()
        _binding = null
    }
}