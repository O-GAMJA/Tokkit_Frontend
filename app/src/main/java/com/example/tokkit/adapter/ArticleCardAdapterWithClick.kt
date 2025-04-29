package com.example.tokkit.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.tokkit.SearchDetailActivity
import com.example.tokkit.databinding.ItemArticleCardBinding
import com.example.tokkit.model.Article

class ArticleCardAdapterWithClick(private var articles: List<Article>) :
    RecyclerView.Adapter<ArticleCardAdapterWithClick.ArticleViewHolder>() {

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

            // 아이템 클릭 이벤트 추가
            root.setOnClickListener {
                val intent = Intent(root.context, SearchDetailActivity::class.java)
                intent.putExtra("ARTICLE_TITLE", article.title)
                intent.putExtra("ARTICLE_CONTENT", article.content)
                intent.putExtra("ARTICLE_IMAGE", article.imageResId)
                root.context.startActivity(intent)
            }
        }
    }

    override fun getItemCount() = articles.size

    // 리스트 업데이트 메서드
    fun submitList(list: List<Article>) {
        this.articles = list
        notifyDataSetChanged()
    }
}