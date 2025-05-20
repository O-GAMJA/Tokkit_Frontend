package com.example.tokkit.adapter

import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.tokkit.R
import com.example.tokkit.data.remote.model.SimilarNoteItem
import com.example.tokkit.databinding.ItemArticleCardBinding


//검색 결과를 표시 어댑터

class SearchResultAdapter(
    private val onItemClick: (SimilarNoteItem) -> Unit
) : ListAdapter<SimilarNoteItem, SearchResultAdapter.ViewHolder>(DiffCallback()) {

    private var searchQuery: String = ""

    fun setSearchQuery(query: String) {
        this.searchQuery = query.toLowerCase()
        notifyDataSetChanged()
    }

    inner class ViewHolder(private val binding: ItemArticleCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SimilarNoteItem) {
            with(binding) {
                // 제목에 검색어 하이라이트 적용
                tvTitle.text = highlightText(item.noteTitle, searchQuery)

                // 내용에 검색어 하이라이트 적용
                tvContent.text = highlightText(item.noteSnippet, searchQuery)

                // 작성자 및 유사도 점수 (관련성 표시)
                val formattedScore = String.format("%.1f", item.score * 100)
                tvDate.text = "작성자: ${item.memberInfo.nickname} | 관련도: ${formattedScore}%"

                // 이미지 로드
                if (!item.noteImageUrl.isNullOrEmpty()) {
                    Glide.with(ivArticle.context)
                        .load(item.noteImageUrl)
                        .into(ivArticle)
                }

                // 클릭 리스너
                root.setOnClickListener {
                    onItemClick(item)
                }
            }
        }

        private fun highlightText(text: String, searchQuery: String): SpannableString {
            val spannableString = SpannableString(text)
            if (searchQuery.isEmpty()) return spannableString

            val lowerCaseText = text.toLowerCase()
            var startIndex = 0

            while (startIndex < lowerCaseText.length) {
                val indexOfQuery = lowerCaseText.indexOf(searchQuery, startIndex)
                if (indexOfQuery == -1) {
                    break
                }

                // 굵게
                spannableString.setSpan(
                    StyleSpan(Typeface.BOLD),
                    indexOfQuery,
                    indexOfQuery + searchQuery.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )

                // 색상 적용
                val highlightColor = ContextCompat.getColor(binding.root.context, R.color.main)
                spannableString.setSpan(
                    ForegroundColorSpan(highlightColor),
                    indexOfQuery,
                    indexOfQuery + searchQuery.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )

                startIndex = indexOfQuery + searchQuery.length
            }

            return spannableString
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemArticleCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DiffCallback : DiffUtil.ItemCallback<SimilarNoteItem>() {
        override fun areItemsTheSame(oldItem: SimilarNoteItem, newItem: SimilarNoteItem): Boolean {
            return oldItem.noteId == newItem.noteId
        }

        override fun areContentsTheSame(oldItem: SimilarNoteItem, newItem: SimilarNoteItem): Boolean {
            return oldItem == newItem
        }
    }
}