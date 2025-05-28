package com.example.tokkit

import com.example.tokkit.adapter.TagListAdapter
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tokkit.data.local.database.AppDatabase
import com.example.tokkit.data.local.entities.Tag
import com.example.tokkit.databinding.ActivityTagManageBinding
import kotlinx.coroutines.launch

class TagManageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTagManageBinding
    private val viewModel: TagViewModel by viewModels()
    private lateinit var adapter: TagListAdapter
    private val addedTags = mutableListOf<Tag>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTagManageBinding.inflate(layoutInflater)
        setContentView(binding.root)

//        insertDummyTagsIfEmpty()

        val preSelectedTags = intent.getStringArrayListExtra("existingTags") ?: arrayListOf()

        for (tagName in preSelectedTags) {
            val tag = Tag(name = tagName)
            if (addedTags.none { it.name.equals(tag.name, ignoreCase = true) }) {
                addedTags.add(tag)
                addTagToContainer(tag)
            }
        }

        // 뒤로가기
        binding.btnBack.setOnClickListener { finish() }

        // 어댑터 설정
        adapter = TagListAdapter { clickedTag ->
            addNewTagIfNotExists(clickedTag.name)
            binding.searchInput.text.clear()
        }
//        binding.recyclerView.apply {
//            layoutManager = LinearLayoutManager(this@TagManageActivity)
//            adapter = this@TagManageActivity.adapter
//        }

        val dividerItemDecoration = DividerItemDecoration(this, LinearLayoutManager.VERTICAL)
        ContextCompat.getDrawable(this, R.drawable.recycler_divider)?.let {
            dividerItemDecoration.setDrawable(it)
        }
//        binding.recyclerView.addItemDecoration(dividerItemDecoration)
//
//        // 실시간 검색
//        binding.searchInput.addTextChangedListener(object : TextWatcher {
//            override fun afterTextChanged(s: Editable?) {
//                val query = s.toString().trim()
//                if (query.isNotEmpty()) {
//                    viewModel.searchTags(query)
//                } else {
//                    adapter.submitList(emptyList())
//                    binding.cardRecyclerWrapper.visibility = View.GONE
//                }
//            }
//
//            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
//            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
//        })

        // 엔터 입력 시 태그 추가
        binding.searchInput.setOnEditorActionListener { _, _, _ ->
            val text = binding.searchInput.text.toString().trim()
            if (text.isNotEmpty()) {
                addNewTagIfNotExists(text)
                binding.searchInput.text.clear()
            }
            true
        }

//        // ViewModel 관찰
//        viewModel.filteredTags.observe(this) { tags ->
//            if (tags.isNotEmpty()) {
//                adapter.submitList(tags)
//                binding.cardRecyclerWrapper.visibility = View.VISIBLE
//            } else {
//                adapter.submitList(emptyList())
//                binding.cardRecyclerWrapper.visibility = View.GONE
//            }
//        }


        // 검색창 클리어
        binding.iconClear.setOnClickListener {
            binding.searchInput.text.clear()
        }

        // 저장 버튼 (데이터 전달)
        binding.btnSave.setOnClickListener {
            val selectedTags = ArrayList(addedTags.map { it.name })
            val resultIntent = Intent().apply {
                putStringArrayListExtra("selectedTags", selectedTags)
            }
            setResult(RESULT_OK, resultIntent)
            finish()
        }
    }

    private fun addTagToContainer(tag: Tag) {
        val chipView = LayoutInflater.from(this).inflate(R.layout.item_tag_chip, binding.tagContainer, false)
        val tagText = chipView.findViewById<TextView>(R.id.tagName)
        val btnDelete = chipView.findViewById<ImageView>(R.id.btnDelete)

        tagText.text = "# ${tag.name}"

        // 삭제 버튼 클릭 이벤트
        btnDelete.setOnClickListener {
            binding.tagContainer.removeView(chipView)  // 화면에서 삭제
            addedTags.removeIf { it.name.equals(tag.name, ignoreCase = true) }  // 리스트에서 삭제
        }

        binding.tagContainer.addView(chipView)
    }

    private fun addNewTagIfNotExists(name: String) {
        val newTag = Tag(name = name)
        if (addedTags.none { it.name.equals(name, ignoreCase = true) }) {
            addedTags.add(newTag)
            addTagToContainer(newTag)
        }
    }

//    private fun insertDummyTagsIfEmpty() {
//        val dao = AppDatabase.getDatabase(this).tagDao()
//        lifecycleScope.launch {
//            if (dao.getAllTags().isEmpty()) {
//                val dummyTags = listOf(
//                    Tag(name = "JAVA"),
//                    Tag(name = "TCP/IP"),
//                    Tag(name = "데이터 통신"),
//                    Tag(name = "Android"),
//                    Tag(name = "Kotlin")
//                )
//                dummyTags.forEach { dao.insert(it) }
//            }
//        }
//    }

}
