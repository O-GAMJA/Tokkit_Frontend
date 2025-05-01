package com.example.tokkit

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import android.widget.LinearLayout
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.viewpager2.widget.ViewPager2
import com.example.tokkit.adapter.HomePagerAdapter
import com.example.tokkit.adapter.TagListAdapter
import com.example.tokkit.data.local.entities.Tag
import com.example.tokkit.databinding.FragmentHomeBinding
import androidx.recyclerview.widget.LinearLayoutManager

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val tagViewModel: TagViewModel by viewModels()
    private val addedTags = mutableListOf<Tag>()
    private lateinit var tagAdapter: TagListAdapter

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
            override fun onPageSelected(position: Int) = updateTabs(position)
        })

        binding.tabCard.setOnClickListener { binding.viewPager.currentItem = 0 }
        binding.tabList.setOnClickListener { binding.viewPager.currentItem = 1 }

        // 어댑터 설정
        tagAdapter = TagListAdapter { clickedTag ->
            addTagIfNotExists(clickedTag.name)
            binding.etSearch.text.clear()
            binding.cardRecyclerWrapper.visibility = View.GONE
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
        }

        binding.etSearch.setOnEditorActionListener { _, _, _ ->
            val tagName = binding.etSearch.text.toString().trim()
            if (tagName.isNotEmpty()) {
                addTagIfNotExists(tagName)
                binding.etSearch.text.clear()
            }
            true
        }

        // LiveData 관찰 (원하면 RecyclerView로 보여주기 가능)
        tagViewModel.filteredTags.observe(viewLifecycleOwner) { tags ->
            // 예: Log 출력하거나 RecyclerView에 연동 가능
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
