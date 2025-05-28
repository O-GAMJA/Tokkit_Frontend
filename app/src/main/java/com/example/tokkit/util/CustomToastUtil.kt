package com.example.tokkit.util

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.example.tokkit.R

object CustomToastUtil {

    fun showToast(context: Context, message: String, iconResId: Int, isLong: Boolean = false) {
        val layout = LayoutInflater.from(context).inflate(R.layout.custom_toast, null)

        val icon = layout.findViewById<ImageView>(R.id.toast_icon)
        val text = layout.findViewById<TextView>(R.id.toast_text)

        icon.setImageResource(iconResId)
        text.text = message

        Toast(context).apply {
            duration = if (isLong) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
            view = layout
            setGravity(Gravity.BOTTOM, 0, 100)
            show()
        }
    }
}
