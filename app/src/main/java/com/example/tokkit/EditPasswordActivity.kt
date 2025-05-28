package com.example.tokkit

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.tokkit.databinding.ActivityEditPasswordBinding
import com.example.tokkit.util.CustomToastUtil

class EditPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditPasswordBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        binding.btnSave.setOnClickListener {
            val pw1 = binding.editPassword.text.toString()
            val pw2 = binding.editPasswordCheck.text.toString()

            when {
                !validatePassword(pw1) -> {
                    binding.tvPasswordError.text = "비밀번호는 8자 이상, 숫자, 영문자, 특수문자를 포함해야 합니다."
                    binding.tvPasswordError.visibility = View.VISIBLE
                }

                pw1 != pw2 -> {
                    binding.tvPasswordError.text = "비밀번호가 일치하지 않습니다."
                    binding.tvPasswordError.visibility = View.VISIBLE
                }

                else -> {
                    val resultIntent = Intent()
                    resultIntent.putExtra("NEW_PASSWORD", pw1)
                    setResult(RESULT_OK, resultIntent)

                    binding.tvPasswordError.visibility = View.GONE
                    CustomToastUtil.showToast(
                        context = this,
                        message = "비밀번호가 저장되었습니다",
                        iconResId = R.drawable.ic_bot
                    )
                    finish()
                }
            }
        }


    }

    private fun validatePassword(password: String): Boolean {
        val lengthPattern = ".{8,}".toRegex()
        val numberPattern = ".*[0-9].*".toRegex()
        val alphabetPattern = ".*[a-zA-Z].*".toRegex()
        val specialCharPattern = ".*[!@#\$%^&*()\\-_=+\\\\|\\[{\\]};:'\",<.>/?].*".toRegex()

        return password.matches(lengthPattern) &&
                password.matches(numberPattern) &&
                password.matches(alphabetPattern) &&
                password.matches(specialCharPattern)
    }
}
