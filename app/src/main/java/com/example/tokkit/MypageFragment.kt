package com.example.tokkit

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.tokkit.databinding.FragmentMypageBinding
import com.example.tokkit.util.RetrofitClient
import com.example.tokkit.data.remote.model.MemberProfileResponse
import android.util.Log
import com.bumptech.glide.Glide
import kotlinx.coroutines.launch

class MypageFragment : Fragment() {

    private var _binding: FragmentMypageBinding? = null
    private val binding get() = _binding!!

    private var memberProfile: MemberProfileResponse? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMypageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fetchProfileImage()

        // 프로필 수정 클릭
        binding.editProfile.setOnClickListener {
            memberProfile?.let { profile ->
                val intent = Intent(requireContext(), EditProfileActivity::class.java).apply {
                    putExtra("email", profile.email)
                    putExtra("nickname", profile.nickname)
                    putExtra("notificationAgree", profile.notificationAgree)
                    putExtra("profileImageUrl", profile.profileImageUrl)
                    putExtra("memberId", profile.memberId)
                }
                startActivity(intent)
            }
        }

        // 알림 설정 클릭
        binding.alarmForward.setOnClickListener {
            val intent = Intent(requireContext(), AlarmSettingActivity::class.java)
            startActivity(intent)
        }

        // 기능 설정 클릭
        binding.functionForward.setOnClickListener {
            val intent = Intent(requireContext(), FunctionSettingActivity::class.java)
            startActivity(intent)
        }

        // 로그아웃 클릭
        binding.logout.setOnClickListener {
            // TODO: 로그아웃 처리
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun fetchProfileImage() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.memberApi.getProfile()
                if (response.isSuccess) {
                    val profile = response.result
                    memberProfile = profile // 전체 객체 저장

                    if (!profile.profileImageUrl.isNullOrEmpty()) {
                        Glide.with(this@MypageFragment)
                            .load(profile.profileImageUrl)
                            .circleCrop()
                            .into(binding.profileImage)
                        Log.d("Mypage", "Fetched profile image URL: ${profile.profileImageUrl}")
                    } else {
                        Log.w("Mypage", "Image URL is null or empty")
                    }
                } else {
                    Log.e("Mypage", "Fail: ${response.message}")
                }
            } catch (e: Exception) {
                Log.e("Mypage", "API Error", e)
            }
        }
    }
}
