package com.example.tokkit

import TagListAdapter
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tokkit.data.local.database.AppDatabase
import com.example.tokkit.data.local.entities.Tag
import com.example.tokkit.databinding.ActivityTagManageBinding
import kotlinx.coroutines.launch

class TagManageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTagManageBinding
    private val viewModel: TagViewModel by viewModels()
    private lateinit var adapter: TagListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTagManageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        insertDummyTagsIfEmpty()

        // 뒤로가기
        binding.btnBack.setOnClickListener { finish() }

        // 어댑터 설정
        adapter = TagListAdapter()
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@TagManageActivity)
            adapter = this@TagManageActivity.adapter
        }

        // 입력 시 자동완성 검색
        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim()
                if (query.isNotEmpty()) {
                    viewModel.searchTags(query)
                } else {
                    adapter.submitList(emptyList())
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // ViewModel 관찰
        viewModel.filteredTags.observe(this) {
            adapter.submitList(it)
        }

        // 검색창 클리어
        binding.iconClear.setOnClickListener {
            binding.searchInput.text.clear()
        }
    }

    private fun insertDummyTagsIfEmpty() {
        val dao = AppDatabase.getDatabase(this).tagDao()
        lifecycleScope.launch {
            if (dao.getAllTags().isEmpty()) {
                val dummyTags = listOf(
                    Tag(name = "JAVA"),
                    Tag(name = "TCP/IP"),
                    Tag(name = "데이터 통신"),
                    Tag(name = "Android"),
                    Tag(name = "Kotlin")
                )
                dummyTags.forEach { dao.insert(it) }
            }
        }
    }

}
