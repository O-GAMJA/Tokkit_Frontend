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
}