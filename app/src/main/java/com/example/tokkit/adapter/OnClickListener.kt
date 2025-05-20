package com.example.tokkit.adapter

import android.view.View
import com.example.tokkit.data.remote.model.Comment

interface OnCommentLongClickListener {
    fun onLongClick(view: View, comment: Comment)
}

interface OnLikeClickListener {
    fun onClick(comment: Comment)
}