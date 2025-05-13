package com.example.tokkit.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tokkit.NoteViewModel
import com.example.tokkit.databinding.FragmentListViewBinding
import com.example.tokkit.SearchDetailActivity
import com.example.tokkit.adapter.NoteAdapter

class ListViewFragment : Fragment() {

    private var _binding: FragmentListViewBinding? = null
    private val binding get() = _binding!!
    private val noteViewModel: NoteViewModel by activityViewModels()
    private lateinit var adapter: NoteAdapter

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
        adapter = NoteAdapter(
            onItemClick = { note ->
                val intent = Intent(requireContext(), SearchDetailActivity::class.java).apply {
                    putExtra("ARTICLE_TITLE", note.title)
                    putExtra("ARTICLE_CONTENT", note.content)
                    putExtra("ARTICLE_IMAGE", note.imageUrl)
                }
                startActivity(intent)
            },
            useCardLayout = false
        )

        binding.recyclerListView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerListView.adapter = adapter
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