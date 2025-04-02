package com.example.tokkit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.example.tokkit.adapter.HomePagerAdapter
import com.example.tokkit.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ViewPager 설정
        val pagerAdapter = HomePagerAdapter(this)
        binding.viewPager.adapter = pagerAdapter

        // 뷰페이저 페이지 변경 리스너
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateTabs(position)
            }
        })

        // 탭 클릭 리스너 설정
        binding.tabCard.setOnClickListener {
            binding.viewPager.currentItem = 0
        }

        binding.tabList.setOnClickListener {
            binding.viewPager.currentItem = 1
        }

        // 태그 리스너 설정
        binding.tagDataTransmission.setOnClickListener {
            // 데이터 통신 태그 클릭 처리
        }

        binding.tagTcpIp.setOnClickListener {
            // TCP/IP 태그 클릭 처리
        }
    }

    private fun updateTabs(position: Int) {
        when (position) {
            0 -> {
                binding.tabCard.backgroundTintList = resources.getColorStateList(R.color.purple, requireActivity().theme)
                binding.tabList.backgroundTintList = resources.getColorStateList(R.color.gray, requireActivity().theme)
            }
            1 -> {
                binding.tabCard.backgroundTintList = resources.getColorStateList(R.color.gray, requireActivity().theme)
                binding.tabList.backgroundTintList = resources.getColorStateList(R.color.purple, requireActivity().theme)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}