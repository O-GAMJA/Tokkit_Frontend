package com.example.tokkit

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.tokkit.databinding.ActivityAlarmSettingBinding
import com.example.tokkit.util.CustomToastUtil

class AlarmSettingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAlarmSettingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAlarmSettingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnSave.setOnClickListener {
            CustomToastUtil.showToast(
                context = this,
                message = "설정이 저장되었습니다.",
                iconResId = R.drawable.ic_bot
            )
            finish()
        }

        binding.switchReview.setOnCheckedChangeListener { _, isChecked ->
            // TODO: 복습 알림 설정 저장
        }

        binding.switchOther.setOnCheckedChangeListener { _, isChecked ->
            // TODO: 기타 알림 설정 저장
        }
    }
}
