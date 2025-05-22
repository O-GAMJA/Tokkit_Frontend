package com.example.tokkit.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.tokkit.R
import com.example.tokkit.data.remote.model.Comment

class CommentAdapter(
    private val rootComments: MutableList<Comment>,
    private val onLongClickListener: OnCommentLongClickListener,
    private val onLikeClickListener: OnLikeClickListener,
    private val onReplyClickListener: OnReplyClickListener
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_COMMENT = 1
        private const val VIEW_TYPE_REPLY = 2
        private const val TAG = "CommentAdapter"
    }

    // 평탄화된 댓글 목록 (표시 순서대로)
    private val flattenedComments = mutableListOf<Pair<Comment, Boolean>>() // (댓글, 대댓글여부)

    init {
        flattenComments()
        Log.d(TAG, "Adapter initialized with ${flattenedComments.size} total items")
    }

    // 계층 구조의 댓글을 평탄화하여 표시 순서대로 변환
    private fun flattenComments() {
        flattenedComments.clear()

        Log.d(TAG, "Flattening comments, root comments: ${rootComments.size}")
        rootComments.forEach { rootComment ->
            // 부모 댓글 추가
            flattenedComments.add(Pair(rootComment, false))
            Log.d(TAG, "Added root comment: ${rootComment.commentId}")

            // 대댓글들을 재귀적으로 추가
            addRepliesToFlattenedList(rootComment.replies, 1)
        }

        Log.d(TAG, "Flattened comments: ${flattenedComments.size} total items")
    }

    // 대댓글을 재귀적으로 평탄화 리스트에 추가
    private fun addRepliesToFlattenedList(replies: List<Comment>, depth: Int) {
        replies.forEach { reply ->
            flattenedComments.add(Pair(reply, true))
            Log.d(TAG, "Added reply (depth $depth): ${reply.commentId}")

            // 대댓글의 대댓글도 재귀적으로 추가
            if (reply.replies.isNotEmpty()) {
                addRepliesToFlattenedList(reply.replies, depth + 1)
            }
        }
    }

    class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvUsername: TextView = itemView.findViewById(R.id.tv_username)
        val tvTime: TextView = itemView.findViewById(R.id.tv_time)
        val tvComment: TextView = itemView.findViewById(R.id.tv_comment)
        val tvLikeCount: TextView = itemView.findViewById(R.id.tv_like_count)
        val btnLike: ImageButton = itemView.findViewById(R.id.btn_like)
        val btnReply: TextView = itemView.findViewById(R.id.btn_reply)
    }

    class ReplyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvUsername: TextView = itemView.findViewById(R.id.tv_username)
        val tvTime: TextView = itemView.findViewById(R.id.tv_time)
        val tvComment: TextView = itemView.findViewById(R.id.tv_comment)
        val tvLikeCount: TextView = itemView.findViewById(R.id.tv_like_count)
        val btnLike: ImageButton = itemView.findViewById(R.id.btn_like)
//        val btnReply: TextView = itemView.findViewById(R.id.btn_reply) // 대댓글 답글
    }

    override fun getItemViewType(position: Int): Int {
        return if (flattenedComments[position].second) VIEW_TYPE_REPLY else VIEW_TYPE_COMMENT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        Log.d(TAG, "onCreateViewHolder called for viewType: $viewType")

        return when (viewType) {
            VIEW_TYPE_COMMENT -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_comment, parent, false)
                CommentViewHolder(view)
            }
            VIEW_TYPE_REPLY -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_comment_reply, parent, false)
                ReplyViewHolder(view)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val (comment, isReply) = flattenedComments[position]
        Log.d(TAG, "Binding comment at position $position, isReply: $isReply, commentId: ${comment.commentId}")

        when (holder) {
            is CommentViewHolder -> bindCommentViewHolder(holder, comment)
            is ReplyViewHolder -> bindReplyViewHolder(holder, comment)
        }
    }

    private fun bindCommentViewHolder(holder: CommentViewHolder, comment: Comment) {
        holder.tvUsername.text = comment.username
        holder.tvTime.text = comment.time
        holder.tvComment.text = comment.content
        holder.tvLikeCount.text = comment.likeCount.toString()

        // 좋아요 버튼 색상 변경
        if (comment.isLiked) {
            holder.btnLike.setColorFilter(
                ContextCompat.getColor(holder.itemView.context, R.color.main),
                android.graphics.PorterDuff.Mode.SRC_IN
            )
        } else {
            holder.btnLike.setColorFilter(
                ContextCompat.getColor(holder.itemView.context, R.color.gray),
                android.graphics.PorterDuff.Mode.SRC_IN
            )
        }

        // 좋아요 버튼 클릭 이벤트
        holder.btnLike.setOnClickListener {
            onLikeClickListener.onClick(comment)
        }

        // 답글 버튼 클릭 이벤트
        holder.btnReply.setOnClickListener {
            onReplyClickListener.onClick(comment)
        }

        // 롱클릭 시 팝업 전달
        holder.itemView.setOnLongClickListener {
            onLongClickListener.onLongClick(it, comment)
            true
        }
    }

    private fun bindReplyViewHolder(holder: ReplyViewHolder, comment: Comment) {
        holder.tvUsername.text = comment.username
        holder.tvTime.text = comment.time
        holder.tvComment.text = comment.content
        holder.tvLikeCount.text = comment.likeCount.toString()

        // 좋아요 버튼 색상 변경
        if (comment.isLiked) {
            holder.btnLike.setColorFilter(
                ContextCompat.getColor(holder.itemView.context, R.color.main),
                android.graphics.PorterDuff.Mode.SRC_IN
            )
        } else {
            holder.btnLike.setColorFilter(
                ContextCompat.getColor(holder.itemView.context, R.color.gray),
                android.graphics.PorterDuff.Mode.SRC_IN
            )
        }

        // 좋아요 버튼 클릭 이벤트
        holder.btnLike.setOnClickListener {
            onLikeClickListener.onClick(comment)
        }

//        // 대댓글 답글 버튼 클릭 이벤트
//        holder.btnReply.setOnClickListener {
//            onReplyClickListener.onClick(comment)
//        }

        // 롱클릭 시 팝업 전달
        holder.itemView.setOnLongClickListener {
            onLongClickListener.onLongClick(it, comment)
            true
        }
    }

    override fun getItemCount(): Int {
        Log.d(TAG, "getItemCount called, returning ${flattenedComments.size}")
        return flattenedComments.size
    }

    // 데이터 변경 시 호출할 메서드
    fun updateComments(newRootComments: List<Comment>) {
        Log.d("CommentDebug", "어댑터 업데이트: ${newRootComments.size}개의 루트 댓글")

        var totalCount = 0
        newRootComments.forEach { rootComment ->
            totalCount += 1 + countRepliesRecursively(rootComment)
            Log.d("CommentDebug", "루트 댓글 ID: ${rootComment.commentId}, 총 하위 댓글 수: ${countRepliesRecursively(rootComment)}")
        }
        Log.d("CommentDebug", "총 댓글 수(모든 대댓글 포함): $totalCount")

        rootComments.clear()
        rootComments.addAll(newRootComments)
        flattenComments()
        Log.d("CommentDebug", "평탄화 후 표시할 댓글 수: ${flattenedComments.size}")
        notifyDataSetChanged()
    }

    // 대댓글을 재귀적으로 카운트
    private fun countRepliesRecursively(comment: Comment): Int {
        var count = comment.replies.size
        comment.replies.forEach { reply ->
            count += countRepliesRecursively(reply)
        }
        return count
    }
}
