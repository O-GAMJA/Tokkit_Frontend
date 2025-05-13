package com.example.tokkit.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.tokkit.data.remote.model.Note
import com.example.tokkit.databinding.ItemArticleCardBinding

class NoteAdapter(
    private val onItemClick: (Note) -> Unit
) : ListAdapter<Note, NoteAdapter.NoteViewHolder>(NoteDiffCallback()) {

    class NoteViewHolder(val binding: ItemArticleCardBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val binding = ItemArticleCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return NoteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val note = getItem(position)

        with(holder.binding) {
            tvTitle.text = note.title

            // 내용이 너무 길 경우 잘라서 표시
            tvContent.text = if (note.content.length > 100) {
                note.content.substring(0, 100) + "..."
            } else {
                note.content
            }

            tvDate.text = if (note.createdAt != null && note.createdAt.length >= 10) {
                note.createdAt.substring(0, 10) // "2025-05-13"
            } else {
                "날짜 정보 없음" // 또는 다른 기본값
            }


            // 이미지 URL이 비어있지 않은 경우에만 이미지 로드
            if (!note.imageUrl.isNullOrEmpty()) {
                Glide.with(ivArticle.context)
                    .load(note.imageUrl)
                    .into(ivArticle)
            }

            root.setOnClickListener {
                onItemClick(note)
            }
        }
    }

    // DiffUtil을 사용하여 효율적인 리스트 업데이트
    private class NoteDiffCallback : DiffUtil.ItemCallback<Note>() {
        override fun areItemsTheSame(oldItem: Note, newItem: Note): Boolean {
            return oldItem.id.equals(newItem.id, ignoreCase = true)
        }

        override fun areContentsTheSame(oldItem: Note, newItem: Note): Boolean {
            return oldItem.title == newItem.title &&
                    oldItem.content == newItem.content &&
                    oldItem.imageUrl == newItem.imageUrl &&
                    oldItem.createdAt == newItem.createdAt
        }
    }
}