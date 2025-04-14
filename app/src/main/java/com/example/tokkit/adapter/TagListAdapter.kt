package com.example.tokkit.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.tokkit.data.local.entities.Tag
import com.example.tokkit.databinding.ItemTagTextBinding

class TagListAdapter(private val onTagClick: (Tag) -> Unit) :
    RecyclerView.Adapter<TagListAdapter.TagViewHolder>() {

    private var tagList: List<Tag> = listOf()

    inner class TagViewHolder(private val binding: ItemTagTextBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(tag: Tag) {
            binding.tagName.text = "# ${tag.name}"
            binding.root.setOnClickListener {
                onTagClick(tag)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TagViewHolder {
        val binding = ItemTagTextBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TagViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TagViewHolder, position: Int) {
        holder.bind(tagList[position])
    }

    override fun getItemCount(): Int = tagList.size

    fun submitList(list: List<Tag>) {
        tagList = list
        notifyDataSetChanged()
    }
}
