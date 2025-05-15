package com.example.tokkit.fragment

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tokkit.NoteViewModel
import com.example.tokkit.databinding.FragmentListViewBinding
import com.example.tokkit.SearchDetailActivity
import com.example.tokkit.adapter.NoteAdapter
import io.noties.markwon.Markwon

class ListViewFragment : Fragment() {

    private var _binding: FragmentListViewBinding? = null
    private val binding get() = _binding!!
    private val noteViewModel: NoteViewModel by activityViewModels()
    private lateinit var adapter: NoteAdapter
    private var currentPage: Int = 0

    private val detailLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val deleted = result.data?.getBooleanExtra("noteDeleted", false) ?: false
            val modified = result.data?.getBooleanExtra("noteModified", false) ?: false

            if (deleted || modified) {
                noteViewModel.loadNotes(memberId = 1L, page = currentPage, size = 10)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentListViewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeViewModel()
        setupRecyclerView()
        // CardViewFragment에서 이미 데이터를 로드하므로 여기서는 중복 로드하지 않음
        // 두 프래그먼트가 동일한 ViewModel을 공유하기 때문
    }

    private fun setupRecyclerView() {
        val markwon = Markwon.create(requireContext())

        adapter = NoteAdapter(
            onItemClick = { note ->
                val intent = Intent(requireContext(), SearchDetailActivity::class.java)
                intent.putExtra("NOTE_ID", note.id)
                detailLauncher.launch(intent)
            },
            useCardLayout = false,
            markwon = markwon
        )

        val layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerListView.layoutManager = layoutManager
        binding.recyclerListView.adapter = adapter

        binding.recyclerListView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val totalItemCount = layoutManager.itemCount
                val lastVisibleItem = layoutManager.findLastVisibleItemPosition()

                if (lastVisibleItem + 3 >= totalItemCount) {
                    // 현재 리스트의 끝 근처에 도달했을 때 다음 페이지 요청
                    noteViewModel.loadMoreNotes(memberId = 1L)
                }
            }
        })
    }

    private fun observeViewModel() {
        // 노트 데이터 관찰
        noteViewModel.notes.observe(viewLifecycleOwner) { notes ->
            adapter.submitList(notes)
        }

        // 로딩 상태 관찰
        noteViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar?.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        // 에러 상태 관찰
        noteViewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}