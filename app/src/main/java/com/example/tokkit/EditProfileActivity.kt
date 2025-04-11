package com.example.tokkit

import android.content.Intent
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.tokkit.databinding.ActivityEditProfileBinding

class EditProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditProfileBinding

    private lateinit var nicknameLauncher: ActivityResultLauncher<Intent>
    private lateinit var passwordLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        nicknameLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val newNickname = result.data?.getStringExtra("NEW_NICKNAME")
                binding.nicknameText.text = newNickname  // 닉네임 TextView ID에 맞게 적용
            }
        }

        passwordLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val newPassword = result.data?.getStringExtra("NEW_PASSWORD")
                // 필요시 처리
            }
        }

        // 뒤로가기
        binding.btnBack.setOnClickListener {
            finish()
        }

        // 닉네임 수정 화면으로 이동
        binding.nicknameLayout.setOnClickListener {
            val intent = Intent(this, EditNicknameActivity::class.java)
            nicknameLauncher.launch(intent)
        }


        // 비밀번호 수정 화면으로 이동
        binding.passwordLayout.setOnClickListener {
            val intent = Intent(this, EditPasswordActivity::class.java)
            passwordLauncher.launch(intent)
        }
    }
}
