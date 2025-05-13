package com.example.tokkit.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.tokkit.data.remote.model.Note
import com.example.tokkit.databinding.ItemArticleCardBinding
import com.example.tokkit.databinding.ItemArticleListBinding
import io.noties.markwon.Markwon

class NoteAdapter(
    private val onItemClick: (Note) -> Unit,
    private val useCardLayout: Boolean,  //  true면 card, false면 list
    private val markwon: Markwon
) : ListAdapter<Note, RecyclerView.ViewHolder>(NoteDiffCallback()) {

    override fun getItemViewType(position: Int): Int {
        return if (useCardLayout) 1 else 2
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        return when (viewType) {
            1 -> {
                val binding = ItemArticleCardBinding.inflate(inflater, parent, false)
                CardViewHolder(binding)
            }
            2 -> {
                val binding = ItemArticleListBinding.inflate(inflater, parent, false)
                ListViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Unknown viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val note = getItem(position)

        when (holder) {
            is CardViewHolder -> {
                with(holder.binding) {
                    tvTitle.text = note.title
                    markwon.setMarkdown(tvContent, note.content.take(100).plus("..."))
                    tvDate.text = if (note.createdAt != null && note.createdAt.length >= 10) {
                        note.createdAt.substring(0, 10) // "2025-05-13"
                    } else {
                        "날짜 정보 없음" // 또는 다른 기본값
                    }
                    if (!note.imageUrl.isNullOrEmpty()) {
                        Glide.with(ivArticle.context).load(note.imageUrl).into(ivArticle)
                    }
                    root.setOnClickListener { onItemClick(note) }
                }
            }

            is ListViewHolder -> {
                with(holder.binding) {
                    tvTitle.text = note.title
                    markwon.setMarkdown(tvContent, note.content.take(80).plus("..."))
                    tvDate.text = if (note.createdAt != null && note.createdAt.length >= 10) {
                        note.createdAt.substring(0, 10) // "2025-05-13"
                    } else {
                        "날짜 정보 없음" // 또는 다른 기본값
                    }
                    if (!note.imageUrl.isNullOrEmpty()) {
                        Glide.with(ivArticle.context).load(note.imageUrl).into(ivArticle)
                    }
                    root.setOnClickListener { onItemClick(note) }
                }
            }
        }
    }

    class CardViewHolder(val binding: ItemArticleCardBinding) : RecyclerView.ViewHolder(binding.root)
    class ListViewHolder(val binding: ItemArticleListBinding) : RecyclerView.ViewHolder(binding.root)

    private class NoteDiffCallback : DiffUtil.ItemCallback<Note>() {
        override fun areItemsTheSame(oldItem: Note, newItem: Note): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Note, newItem: Note): Boolean {
            return oldItem == newItem
        }
    }
}
