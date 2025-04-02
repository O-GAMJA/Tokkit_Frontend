package com.example.tokkit

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.tokkit.databinding.ActivityRegisterIdBinding

class RegisterIdActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterIdBinding
    private var isIdValid = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterIdBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
        updateUI(false) // 초기 상태는 유효하지 않은 ID
    }

    private fun setupListeners() {
        // 아이디 입력 리스너
        binding.etId.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val id = s.toString().trim()
                validateId(id)
            }
        })

        // 다음 버튼 클릭 리스너
        binding.btnNext.setOnClickListener {
            if (isIdValid) {
                val userId = binding.etId.text.toString().trim()
                val intent = Intent(this, RegisterPasswordActivity::class.java)
                intent.putExtra("USER_ID", userId)
                startActivity(intent)
            }
        }
    }

    private fun validateId(id: String) {
        // 5~30자 영문, 숫자 체크 (DB 없이 간단한 검증만)
        val isValidFormat = id.matches(Regex("^[a-zA-Z0-9]{5,30}$"))

        // 테스트용 중복 체크 (DB 없이 'admin'만 중복으로 처리)
        val isDuplicate = id == "admin"

        isIdValid = isValidFormat && !isDuplicate

        // UI 업데이트
        updateUI(isIdValid, isDuplicate)
    }

    private fun updateUI(isValid: Boolean, isDuplicate: Boolean = false) {
        if (isValid) {
            // 유효한 아이디
            binding.ivAlert.visibility = View.GONE
            binding.tvStatusAvailable.visibility = View.VISIBLE
            binding.tvStatusError.visibility = View.GONE
            binding.btnNext.visibility = View.VISIBLE
            binding.btnNextDisabled.visibility = View.GONE
        } else {
            // 유효하지 않은 아이디
            if (binding.etId.text.toString().trim().isNotEmpty()) {
                if (isDuplicate) {
                    // 중복된 아이디
                    binding.ivAlert.visibility = View.VISIBLE
                    binding.tvStatusAvailable.visibility = View.GONE
                    binding.tvStatusError.visibility = View.VISIBLE
                    binding.tvStatusError.text = "이미 존재하는 아이디 입니다."
                } else {
                    // 형식이 틀린 아이디
                    binding.ivAlert.visibility = View.VISIBLE
                    binding.tvStatusAvailable.visibility = View.GONE
                    binding.tvStatusError.visibility = View.VISIBLE
                    binding.tvStatusError.text = "5~30자의 영문, 숫자만 사용 가능합니다."
                }
            } else {
                // 빈 입력 필드
                binding.ivAlert.visibility = View.GONE
                binding.tvStatusAvailable.visibility = View.GONE
                binding.tvStatusError.visibility = View.GONE
            }

            binding.btnNext.visibility = View.GONE
            binding.btnNextDisabled.visibility = View.VISIBLE
        }
    }
}