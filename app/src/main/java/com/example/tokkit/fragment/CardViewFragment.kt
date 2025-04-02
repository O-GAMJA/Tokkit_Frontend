package com.example.tokkit.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tokkit.adapter.ArticleCardAdapter
import com.example.tokkit.databinding.FragmentCardViewBinding
import com.example.tokkit.model.Article
import com.example.tokkit.R

class CardViewFragment : Fragment() {

    private var _binding: FragmentCardViewBinding? = null
    private val binding get() = _binding!!

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

        binding.recyclerCardView.layoutManager = LinearLayoutManager(requireContext())

        val articles = getArticles()
        val adapter = ArticleCardAdapter(articles)
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