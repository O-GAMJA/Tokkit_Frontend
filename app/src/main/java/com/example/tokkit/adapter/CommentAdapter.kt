package com.example.tokkit.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.tokkit.R
import com.example.tokkit.data.remote.model.Comment

class CommentAdapter(
    private val comments: MutableList<Comment>,
    private val onLongClickListener: OnCommentLongClickListener) :
    RecyclerView.Adapter<CommentAdapter.CommentViewHolder>() {

    init {
        Log.d("CommentAdapter", "Adapter initialized with ${comments.size} comments")
    }

    class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvUsername: TextView = itemView.findViewById(R.id.tv_username)
        val tvTime: TextView = itemView.findViewById(R.id.tv_time)
        val tvComment: TextView = itemView.findViewById(R.id.tv_comment)
        val tvLikeCount: TextView = itemView.findViewById(R.id.tv_like_count)
        val btnLike: ImageButton = itemView.findViewById(R.id.btn_like)
        val btnReply: TextView = itemView.findViewById(R.id.btn_reply)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        Log.d("CommentAdapter", "onCreateViewHolder called")
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_comment, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        Log.d("CommentAdapter", "Binding comment at position $position")
        val comment = comments[position]

        holder.tvUsername.text = comment.username
        holder.tvTime.text = comment.time
        holder.tvComment.text = comment.content
        holder.tvLikeCount.text = comment.likeCount.toString()

        // 좋아요 버튼 클릭 이벤트
        holder.btnLike.setOnClickListener {
            // 실제 구현에서는 좋아요 카운트를 서버에 업데이트하고 UI를 갱신예정
            val currentLikes = comment.likeCount
            holder.tvLikeCount.text = (currentLikes + 1).toString()
        }

        // 답글 버튼 클릭 이벤트
        holder.btnReply.setOnClickListener {
            // 답글 기능 구현
        }

        // 롱클릭 시 팝업 전달
        holder.itemView.setOnLongClickListener {
            onLongClickListener.onLongClick(it, comment)
            true
        }
    }

    override fun getItemCount(): Int {
        Log.d("CommentAdapter", "getItemCount called, returning ${comments.size}")
        return comments.size
    }

    // 데이터 변경 시 호출할 메서드 추가
    fun updateComments(newComments: List<Comment>) {
        Log.d("CommentAdapter", "Updating comments: old size=${comments.size}, new size=${newComments.size}")
        comments.clear()
        comments.addAll(newComments)
        notifyDataSetChanged()
    }

    fun appendComments(newComments: List<Comment>) {
        val start = comments.size
        comments.addAll(newComments)
        notifyItemRangeInserted(start, newComments.size)
    }
}
