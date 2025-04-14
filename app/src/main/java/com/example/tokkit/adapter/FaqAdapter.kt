package com.example.tokkit.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.tokkit.R
import com.example.tokkit.databinding.ItemFaqBinding
import com.example.tokkit.model.FaqItem

class FaqAdapter(private val items: List<FaqItem>) :
    RecyclerView.Adapter<FaqAdapter.FaqViewHolder>() {

    inner class FaqViewHolder(val binding: ItemFaqBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FaqViewHolder {
        val binding = ItemFaqBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FaqViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FaqViewHolder, position: Int) {
        val item = items[position]
        with(holder.binding) {
            tvQuestion.text = item.question
            tvAnswer.text = item.answer

            // visibility 처리
            answerLayout.visibility = if (item.isExpanded) View.VISIBLE else View.GONE
            ivArrow.setImageResource(
                if (item.isExpanded) R.drawable.ic_arrow_up else R.drawable.ic_arrow_down
            )

            questionLayout.setOnClickListener {
                item.isExpanded = !item.isExpanded
                notifyItemChanged(position)
            }
        }
    }

    override fun getItemCount(): Int = items.size
}

