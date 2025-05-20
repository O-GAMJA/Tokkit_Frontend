package com.example.tokkit.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.tokkit.data.remote.model.SimilarNoteItem
import com.example.tokkit.databinding.ItemRelatedArticleBinding

class SimilarNoteAdapter(
    private var notes: List<SimilarNoteItem>,
    private val onItemClick: (SimilarNoteItem) -> Unit
) : RecyclerView.Adapter<SimilarNoteAdapter.SimilarNoteViewHolder>() {

    inner class SimilarNoteViewHolder(val binding: ItemRelatedArticleBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SimilarNoteViewHolder {
        val binding = ItemRelatedArticleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SimilarNoteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SimilarNoteViewHolder, position: Int) {
        val note = notes[position]
        with(holder.binding) {
            tvRelatedTitle.text = note.noteTitle
            tvRelatedContent.text = note.noteSnippet
            tvRelatedDate.text = note.memberInfo.nickname

            if (!note.noteImageUrl.isNullOrBlank()) {
                Glide.with(ivRelatedArticle.context)
                    .load(note.noteImageUrl)
                    .into(ivRelatedArticle)
            } else {
                ivRelatedArticle.setImageDrawable(null)
            }

            root.setOnClickListener { onItemClick(note) }
        }
    }

    override fun getItemCount(): Int = notes.size

    fun updateData(newNotes: List<SimilarNoteItem>) {
        notes = newNotes
        notifyDataSetChanged()
    }
}