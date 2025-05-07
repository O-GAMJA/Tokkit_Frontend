package com.example.tokkit

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tokkit.adapter.GenericArticleAdapter
import com.example.tokkit.databinding.FragmentReviewBinding
import com.example.tokkit.model.Article

class ReviewFragment : Fragment() {

    private var _binding: FragmentReviewBinding? = null
    private val binding get() = _binding!!

    private val stageButtons = mutableListOf<TextView>()
    private var selectedStage: Int = 0 // 0은 전체, 1-5는 각 단계

    // 모든 아티클 데이터
    private val allArticles = mutableListOf<Article>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 단계 버튼 초기화
        initStageButtons()

        // 리사이클러뷰 설정
        setupRecyclerView()

        // 샘플 데이터 로드
        loadSampleData()

        // 초기 데이터 표시
        updateArticleList()
    }

    private fun initStageButtons() {
        // 각 버튼을 리스트에 저장
        stageButtons.add(binding.btnAll)
        stageButtons.add(binding.btnStep1)
        stageButtons.add(binding.btnStep2)
        stageButtons.add(binding.btnStep3)
        stageButtons.add(binding.btnStep4)
        stageButtons.add(binding.btnStep5)

        // 버튼 클릭 이벤트 설정
        for (i in stageButtons.indices) {
            stageButtons[i].setOnClickListener {
                updateSelectedStage(i)
                updateArticleList()
            }
        }
    }

    private fun updateSelectedStage(stage: Int) {
        selectedStage = stage

        // 선택된 단계에 따라 버튼 스타일 변경
        for (i in stageButtons.indices) {
            if (i == stage) {
                // 선택된 버튼 스타일
                stageButtons[i].setBackgroundResource(R.drawable.rounded_stage_bg)
                stageButtons[i].setTextColor(ContextCompat.getColor(requireContext(), R.color.stageBtn))
            } else {
                // 선택되지 않은 버튼 스타일
                stageButtons[i].setBackgroundResource(R.drawable.rounded_stage_bg2)
                stageButtons[i].setTextColor(ContextCompat.getColor(requireContext(), R.color.gray))
            }
        }
    }

    private fun setupRecyclerView() {
        binding.recyclerReviewArticles.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun loadSampleData() {
        // 더미 데이터
        allArticles.add(
            Article(
                "1단계: 데이터 통신 기초",
                "데이터 통신의 기본 개념과 역사에 대한 설명. 통신 매체의 종류와 특징에 대해 학습합니다.",
                "2024.01.01",
                R.drawable.ic_tcp_ip,
                1
            )
        )
        allArticles.add(
            Article(
                "1단계: 네트워크 모델",
                "OSI 7계층 모델과 TCP/IP 모델의 비교 및 각 계층별 역할에 대한 소개입니다.",
                "2024.01.02",
                R.drawable.ic_tcp_ip,
                1
            )
        )
        allArticles.add(
            Article(
                "2단계: 물리 계층",
                "물리 계층의 역할과 특징, 전송 매체의 종류와 신호 처리 방법에 대해 설명합니다.",
                "2024.01.03",
                R.drawable.ic_tcp_ip,
                2
            )
        )
        allArticles.add(
            Article(
                "2단계: 데이터 링크 계층",
                "프레임 구조와 오류 검출/정정, MAC 주소, 이더넷 프로토콜에 대해 다룹니다.",
                "2024.01.04",
                R.drawable.ic_tcp_ip,
                2
            )
        )
        allArticles.add(
            Article(
                "3단계: 네트워크 계층",
                "IP 주소 체계와 라우팅 알고리즘, 서브넷 마스크에 대한 상세한 설명입니다.",
                "2024.01.05",
                R.drawable.ic_tcp_ip,
                3
            )
        )
        allArticles.add(
            Article(
                "3단계: IPv4와 IPv6",
                "IPv4와 IPv6의 차이점과 전환 과정, 주소 할당 방식에 대해 학습합니다.",
                "2024.01.06",
                R.drawable.ic_tcp_ip,
                3
            )
        )
        allArticles.add(
            Article(
                "4단계: 전송 계층 - TCP",
                "TCP/IP (Transmission Control Protocol)의 특징과 연결 설정 과정에 대해 설명합니다.",
                "2024.01.07",
                R.drawable.ic_tcp_ip,
                4
            )
        )
        allArticles.add(
            Article(
                "4단계: 전송 계층 - UDP",
                "UDP(User Datagram Protocol)의 특징과 TCP와의 비교, 적용 사례를 소개합니다.",
                "2024.01.08",
                R.drawable.ic_tcp_ip,
                4
            )
        )
        allArticles.add(
            Article(
                "5단계: 응용 계층 프로토콜",
                "HTTP, FTP, SMTP 등 주요 응용 계층 프로토콜의 기능과 특징에 대해 다룹니다.",
                "2024.01.09",
                R.drawable.ic_tcp_ip,
                5
            )
        )
        allArticles.add(
            Article(
                "5단계: 웹 서비스와 API",
                "REST API, SOAP, GraphQL 등 웹 서비스 아키텍처와 활용 방법을 설명합니다.",
                "2024.01.10",
                R.drawable.ic_tcp_ip,
                5
            )
        )
    }

    private fun updateArticleList() {
        // 선택된 단계에 따라 아티클 필터링
        val filteredArticles = if (selectedStage == 0) {
            // 전체 선택시 모든 아티클 표시
            allArticles
        } else {
            // 특정 단계 선택시 해당 단계 아티클만 필터링
            allArticles.filter { it.stage == selectedStage }
        }

        // 어댑터 설정
        val adapter = GenericArticleAdapter(filteredArticles) { article ->
            val intent = Intent(requireContext(), ForgettingCurveActivity::class.java)
            intent.putExtra("ARTICLE_TITLE", article.title)
            intent.putExtra("ARTICLE_STAGE", article.stage)
            startActivity(intent)
        }
        binding.recyclerReviewArticles.adapter = adapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}