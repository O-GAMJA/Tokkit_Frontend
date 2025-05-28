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
import com.example.tokkit.data.remote.model.ApiResponse
import com.example.tokkit.data.remote.model.SearchResponse
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

    private var recommendedNotes: List<SimilarNoteItem> = emptyList()

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

        val memberId = 1L  // TODO: 실제 로그인된 사용자 ID로 대체
        loadRecommendedNotes(memberId)

        setupRecyclerView()
        setupSearchListener()

        updateUI("")
    }

    private fun setupRecyclerView() {
        binding.recyclerSearchResults.layoutManager = LinearLayoutManager(requireContext())
        adapter = SearchResultAdapter { noteItem ->
            val intent = Intent(requireContext(), SearchDetailActivity::class.java)
            intent.putExtra("NOTE_ID", noteItem.noteId)
            startActivity(intent)
        }
        binding.recyclerSearchResults.adapter = adapter
    }

    private fun setupSearchListener() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim()
                searchJob?.cancel()

                if (query.isEmpty()) {
                    updateUI("")
                    return
                }

                searchJob = lifecycleScope.launch {
                    delay(300)
                    performSearch(query)
                }
            }
        })

        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = binding.etSearch.text.toString().trim()
                if (query.isNotEmpty()) {
                    searchJob?.cancel()
                    lifecycleScope.launch {
                        performSearch(query)
                    }
                }
                true
            } else false
        }

        binding.ivClearSearch.setOnClickListener {
            binding.etSearch.text.clear()
            updateUI("")
        }
    }

    private fun performSearch(query: String) {
        Log.d(TAG, "검색 시작: $query")

        binding.recyclerSearchResults.visibility = View.GONE
        binding.emptyResultView.visibility = View.GONE
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.createService(SearchApiService::class.java)
                    .searchFullText(query, 0, 20)

                if (response.isSuccess) {
                    val searchResults = response.result.noteSearchResults
                    Log.d(TAG, "검색 결과: ${searchResults.size}개 항목")
                    updateUIWithResults(query, searchResults)
                } else {
                    Log.e(TAG, "검색 API 결과 실패: ${response.message}")
                    showEmptyResult(query)
                }
            } catch (e: Exception) {
                Log.e(TAG, "검색 중 오류 발생", e)
                showEmptyResult(query)
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun loadRecommendedNotes(memberId: Long) {
        binding.progressBar.visibility = View.VISIBLE
        binding.recyclerSearchResults.visibility = View.GONE
        binding.emptyResultView.visibility = View.GONE

        lifecycleScope.launch {
            var response: ApiResponse<SearchResponse>? = null

            try {
                Log.d(TAG, "추천 노트 API 요청 시작 (memberId=$memberId)")
                response = RetrofitClient.noteApi.getRecommendedNotes(memberId, 0, 10)

                if (response.isSuccess) {
                    Log.d(TAG, "추천 노트 API 성공 - noteSearchResults: ${response.result.noteSearchResults.size}개")
                    recommendedNotes = response.result.noteSearchResults
                    updateUIWithResults("", recommendedNotes)
                } else {
                    Log.e(TAG, "추천 노트 API 실패 - code: ${response.code}, message: ${response.message}")
                    showEmptyResult("")
                }
            } catch (e: Exception) {
                Log.e(TAG, "추천 노트 API 호출 중 예외 발생", e)
                showEmptyResult("")
            } finally {
                binding.progressBar.visibility = View.GONE

                // 예외 여부 관계없이 마지막으로 로그 찍기
                if (response?.result?.noteSearchResults != null) {
                    Log.d(TAG, "finally 블록 - 캐싱 및 UI 업데이트 수행")
                    recommendedNotes = response.result.noteSearchResults
                    updateUIWithResults("", recommendedNotes)
                } else {
                    Log.d(TAG, "finally 블록 - noteSearchResults가 null이거나 비어 있음")
                }
            }
        }
    }


    private fun updateUIWithResults(query: String, results: List<SimilarNoteItem>) {
        if (results.isEmpty()) {
            showEmptyResult(query)
            return
        }

        val sortedResults = results.sortedByDescending { it.score }

        Log.d(TAG, "정렬된 결과: ${sortedResults.map { "${it.noteTitle} (score: ${it.score})" }}")

        adapter.setSearchQuery(query)
        binding.recyclerSearchResults.visibility = View.VISIBLE
        binding.emptyResultView.visibility = View.GONE
        adapter.submitList(sortedResults)
    }

    private fun showEmptyResult(query: String) {
        if (query.isBlank()) {
            if (recommendedNotes.isNotEmpty()) {
                binding.recyclerSearchResults.visibility = View.VISIBLE
                binding.emptyResultView.visibility = View.GONE
                adapter.setSearchQuery("")
                adapter.submitList(recommendedNotes)
            } else {
                binding.recyclerSearchResults.visibility = View.GONE
                binding.emptyResultView.visibility = View.VISIBLE
                binding.tvEmptyResult.text = "검색어를 입력하세요."
            }
        } else {
            binding.recyclerSearchResults.visibility = View.GONE
            binding.emptyResultView.visibility = View.VISIBLE
            binding.tvEmptyResult.text = "'$query'에 대한 검색 결과가 없습니다."
        }
    }

    private fun updateUI(query: String) {
        if (query.isEmpty()) {
            if (recommendedNotes.isNotEmpty()) {
                binding.recyclerSearchResults.visibility = View.VISIBLE
                binding.emptyResultView.visibility = View.GONE
                adapter.setSearchQuery("")
                adapter.submitList(recommendedNotes)
            } else {
                binding.recyclerSearchResults.visibility = View.GONE
                binding.emptyResultView.visibility = View.VISIBLE
                binding.tvEmptyResult.text = "검색어를 입력하세요."
                adapter.submitList(emptyList())
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        searchJob?.cancel()
        _binding = null
    }
}
