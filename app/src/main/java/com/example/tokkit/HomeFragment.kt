package com.example.tokkit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.view.WindowCompat
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

        // 상태바 투명하게 설정
        makeStatusBarTransparent()

        // ViewPager 설정
        val pagerAdapter = HomePagerAdapter(this)
        binding.viewPager.adapter = pagerAdapter

        // 뷰페이저 페이지 변경 리스너
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateTabs(position)
                updateUIForTab(position)

            }
        })

        // 탭 클릭 리스너 설정
        binding.tabCard.setOnClickListener {
            binding.viewPager.currentItem = 0
        }

        binding.tabList.setOnClickListener {
            binding.viewPager.currentItem = 1
        }

        binding.tabBubble.setOnClickListener {
            binding.viewPager.currentItem = 2
        }

        // 태그 리스너 설정
        binding.tagDataTransmission.setOnClickListener {
            // 데이터 통신 태그 클릭 처리
        }

        binding.tagTcpIp.setOnClickListener {
            // TCP/IP 태그 클릭 처리
        }
    }

    private fun makeStatusBarTransparent() {
        activity?.window?.apply {
            clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            statusBarColor = android.graphics.Color.TRANSPARENT

            // API 30 이상에서는 다음 코드도 추가
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
                binding.tabBubble.backgroundTintList = resources.getColorStateList(R.color.gray2, requireActivity().theme)
            }
            1 -> {
                binding.tabCard.backgroundTintList = resources.getColorStateList(R.color.gray2, requireActivity().theme)
                binding.tabList.backgroundTintList = resources.getColorStateList(R.color.main, requireActivity().theme)
                binding.tabBubble.backgroundTintList = resources.getColorStateList(R.color.gray2, requireActivity().theme)
            }
            2 -> {
                binding.tabCard.backgroundTintList = resources.getColorStateList(R.color.gray2, requireActivity().theme)
                binding.tabList.backgroundTintList = resources.getColorStateList(R.color.gray2, requireActivity().theme)
                binding.tabBubble.backgroundTintList = resources.getColorStateList(R.color.main, requireActivity().theme)
            }
        }
    }
    private fun updateUIForTab(position: Int) {
        if (position == 2) { // 버블 차트 탭
            // 검색 UI 숨기기
            binding.searchContainer.visibility = View.GONE
            binding.scrollTags.visibility = View.GONE
        } else {
            // 다른 탭에서는 검색 UI 표시
            binding.searchContainer.visibility = View.VISIBLE
            binding.scrollTags.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}