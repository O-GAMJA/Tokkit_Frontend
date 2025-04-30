package com.example.tokkit.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.tokkit.databinding.ItemRelatedArticleBinding
import com.example.tokkit.model.Article

// 검색 상세화면 다른 글 뷰페이저 어댑터
class RelatedArticlesAdapter(
    private var articles: List<Article>,
    private val onItemClick: (Article) -> Unit
) : RecyclerView.Adapter<RelatedArticlesAdapter.ArticleViewHolder>() {

    class ArticleViewHolder(val binding: ItemRelatedArticleBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArticleViewHolder {
        val binding = ItemRelatedArticleBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ArticleViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ArticleViewHolder, position: Int) {
        val article = articles[position]

        with(holder.binding) {
            tvRelatedTitle.text = article.title
            tvRelatedContent.text = article.content
            tvRelatedDate.text = article.date

            // 내용이 2줄 이상인지 확인하기 위한 Layout 설정
            tvRelatedContent.post {
                if (tvRelatedContent.lineCount > 2) {
                    tvReadMore.visibility = View.VISIBLE
                } else {
                    tvReadMore.visibility = View.GONE
                }
            }

            // 이미지 로드
            Glide.with(ivRelatedArticle.context)
                .load(article.imageResId)
                .into(ivRelatedArticle)

            // 아이템 전체 또는 더보기 텍스트 클릭 시 상세 페이지로 이동
            val clickListener = View.OnClickListener { onItemClick(article) }
            root.setOnClickListener(clickListener)
            tvReadMore.setOnClickListener(clickListener)
        }
    }

    override fun getItemCount(): Int = articles.size
}