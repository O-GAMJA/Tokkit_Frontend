package com.example.tokkit.adapter

import android.view.View
import com.example.tokkit.model.Comment

interface OnCommentLongClickListener {
    fun onLongClick(view: View, comment: Comment)
}
