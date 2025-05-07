package com.example.tokkit.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tokkit.databinding.FragmentCardViewBinding
import com.example.tokkit.model.Article
import com.example.tokkit.R
import com.example.tokkit.SearchDetailActivity
import com.example.tokkit.adapter.GenericArticleAdapter

class CardViewFragment : Fragment() {

    private var _binding: FragmentCardViewBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: GenericArticleAdapter

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

        // 리사이클러뷰 설정
        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        val articles = getArticles()
        binding.recyclerCardView.layoutManager = LinearLayoutManager(requireContext())
        adapter = GenericArticleAdapter(articles) { article ->
            val intent = Intent(requireContext(), SearchDetailActivity::class.java)
            intent.putExtra("ARTICLE_TITLE", article.title)
            intent.putExtra("ARTICLE_CONTENT", article.content)
            intent.putExtra("ARTICLE_IMAGE", article.imageResId)
            startActivity(intent)
        }
        binding.recyclerCardView.adapter = adapter
    }

    private fun getArticles(): List<Article> {
        // 샘플 데이터
        return listOf(
            Article(
                "데이터 통신 - TCP/IP",
                "TCP/IP (Transmission Control) 인터넷을 포함한 대부분의 네트워크에서 사용되는 프로토콜 스택..",
                "2024.01.04",
                R.drawable.ic_tcp_ip
            ),
            Article(
                "데이터 통신 - TCP/IP",
                "TCP/IP (Transmission Control) 인터넷을 포함한 대부분의 네트워크에서 사용되는 프로토콜 스택..",
                "2024.01.04",
                R.drawable.ic_tcp_ip
            ),
            Article(
                "데이터 통신 - TCP/IP",
                "TCP/IP (Transmission Control) 인터넷을 포함한 대부분의 네트워크에서 사용되는 프로토콜 스택..",
                "2024.01.04",
                R.drawable.ic_tcp_ip
            ),
            Article(
                "데이터 통신 - TCP/IP",
                "TCP/IP (Transmission Control) 인터넷을 포함한 대부분의 네트워크에서 사용되는 프로토콜 스택..",
                "2024.01.04",
                R.drawable.ic_tcp_ip
            )

        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}