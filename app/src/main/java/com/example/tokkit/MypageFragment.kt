package com.example.tokkit

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.tokkit.databinding.FragmentMypageBinding

class MypageFragment : Fragment() {

    private var _binding: FragmentMypageBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMypageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 프로필 수정 클릭
        binding.editProfile.setOnClickListener {
            val intent = Intent(requireContext(), EditProfileActivity::class.java)
            startActivity(intent)
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
}
