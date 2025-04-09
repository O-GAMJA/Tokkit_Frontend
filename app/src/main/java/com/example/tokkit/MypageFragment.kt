package com.example.tokkit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment

class MypageFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_mypage, container, false)

        // 설정 아이콘 클릭
        val settingsIcon = view.findViewById<ImageView>(R.id.settingsIcon)
        settingsIcon.setOnClickListener {
            Toast.makeText(requireContext(), "설정 클릭됨", Toast.LENGTH_SHORT).show()
        }

        // 프로필 수정 클릭
        val editProfile = view.findViewById<TextView>(R.id.editProfile)
        editProfile.setOnClickListener {
            Toast.makeText(requireContext(), "프로필 수정 클릭됨", Toast.LENGTH_SHORT).show()
        }

        // 알림 설정 클릭
        val alarmForward = view.findViewById<ImageButton>(R.id.alarmForward)
        alarmForward.setOnClickListener {
            Toast.makeText(requireContext(), "알림 설정 이동", Toast.LENGTH_SHORT).show()
            // TODO: 알림 설정 Fragment or Activity로 이동
        }

        // 기능 설정 클릭
        val functionForward = view.findViewById<ImageButton>(R.id.functionForward)
        functionForward.setOnClickListener {
            Toast.makeText(requireContext(), "기능 설정 이동", Toast.LENGTH_SHORT).show()
            // TODO: 기능 설정 Fragment or Activity로 이동
        }

        // 로그아웃 클릭
        val logout = view.findViewById<TextView>(R.id.logout)
        logout.setOnClickListener {
            Toast.makeText(requireContext(), "로그아웃 클릭됨", Toast.LENGTH_SHORT).show()
            // TODO: 로그아웃 처리
        }

        return view
    }
}
