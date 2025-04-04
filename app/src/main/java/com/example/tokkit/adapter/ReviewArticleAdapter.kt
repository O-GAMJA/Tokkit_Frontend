package com.example.tokkit.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.tokkit.ForgettingCurveActivity
import com.example.tokkit.databinding.ItemArticleCardBinding
import com.example.tokkit.model.Article


// 리뷰 프래그먼트에서만 사용하는 어댑터, 에빙하우스 망각곡선 화면으로 이동

class ReviewArticleAdapter(private val articles: List<Article>) :
    RecyclerView.Adapter<ReviewArticleAdapter.ArticleViewHolder>() {

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

            // 아이템 클릭 리스너 추가
            root.setOnClickListener {
                // 에빙하우스 망각곡선 화면으로 이동
                val intent = Intent(root.context, ForgettingCurveActivity::class.java)
                // 필요한 경우 아티클 정보를 전달
                intent.putExtra("ARTICLE_TITLE", article.title)
                intent.putExtra("ARTICLE_STAGE", article.stage)
                root.context.startActivity(intent)
            }
        }
    }

    override fun getItemCount() = articles.size
}