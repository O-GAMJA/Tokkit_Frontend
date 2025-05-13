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

class CardViewFragment : Fragment() {

    private var _binding: FragmentCardViewBinding? = null
    private val binding get() = _binding!!
    private val noteViewModel: NoteViewModel by activityViewModels()
    private lateinit var adapter: NoteAdapter

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
        adapter = NoteAdapter(
            onItemClick = { note ->
                val intent = Intent(requireContext(), SearchDetailActivity::class.java).apply {
                    putExtra("NOTE_ID", note.id)
                }
                startActivity(intent)
            },
            useCardLayout = true
        )

        binding.recyclerCardView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerCardView.adapter = adapter
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
        // 실제 로그인 사용자 ID로 대체해야 함
        noteViewModel.loadNotes(memberId = 1L)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}