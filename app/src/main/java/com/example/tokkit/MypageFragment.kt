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
            Toast.makeText(requireContext(), "프로필 수정 클릭됨", Toast.LENGTH_SHORT).show()
        }

        // 알림 설정 클릭
        binding.alarmForward.setOnClickListener {
            Toast.makeText(requireContext(), "알림 설정 이동", Toast.LENGTH_SHORT).show()
            val intent = Intent(requireContext(), AlarmSettingActivity::class.java)
            startActivity(intent)
        }

        // 기능 설정 클릭
        binding.functionForward.setOnClickListener {
            Toast.makeText(requireContext(), "기능 설정 이동", Toast.LENGTH_SHORT).show()
            // TODO: 기능 설정 Activity로 이동
        }

        // 로그아웃 클릭
        binding.logout.setOnClickListener {
            Toast.makeText(requireContext(), "로그아웃 클릭됨", Toast.LENGTH_SHORT).show()
            // TODO: 로그아웃 처리
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
