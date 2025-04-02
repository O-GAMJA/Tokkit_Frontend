package com.example.tokkit.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.tokkit.databinding.ItemArticleCardBinding
import com.example.tokkit.model.Article

class ArticleCardAdapter(private val articles: List<Article>) :
    RecyclerView.Adapter<ArticleCardAdapter.ArticleViewHolder>() {

    class ArticleViewHolder(val binding: ItemArticleCardBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArticleViewHolder {
        val binding = ItemArticleCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ArticleViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ArticleViewHolder, position: Int) {
        val article = articles[position]

        with(holder.binding) {
            tvTitle.text = article.title
            tvContent.text = article.content
            tvDate.text = article.date

            Glide.with(ivArticle.context)
                .load(article.imageResId)
                .into(ivArticle)
        }
    }

    override fun getItemCount() = articles.size
}