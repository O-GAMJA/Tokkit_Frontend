package com.example.tokkit.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.tokkit.data.remote.model.ReviewStat
import com.example.tokkit.databinding.ItemReviewStatBinding

class ReviewStatAdapter(private val items: List<ReviewStat>) :
    RecyclerView.Adapter<ReviewStatAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemReviewStatBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(stat: ReviewStat) {
            binding.tvType.text = when (stat.type) {
                "QUIZ" -> "퀴즈"
                "TALK" -> "말하기"
                else -> stat.type
            }
            binding.tvScore.text = when (stat.type) {
                "TALK" -> "-"
                else -> "${stat.score}"
            }
            binding.tvDate.text = stat.reviewedAt.substring(0, 10)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemReviewStatBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}
