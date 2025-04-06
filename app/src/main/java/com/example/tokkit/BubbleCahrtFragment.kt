package com.example.tokkit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.tokkit.R
import com.example.tokkit.databinding.FragmentBubblechartBinding

class BubbleChartFragment : Fragment() {

    private var _binding: FragmentBubblechartBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBubblechartBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // 버블 차트 초기화 및 설정
        setupBubbleChart()
    }

    private fun setupBubbleChart() {
        // 버블 차트 데이터 및 시각화 설정
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}