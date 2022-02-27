package cn.svecri.feedive.model

data class ArticleCategory(
    val domain: String = "",
    val category: String = "",
)

data class ArticleEnclosure(
    val url: String = "",
    val length: Int = 0,
    val type: String = "",
)

data class ArticleGuid(
    val isPermaLink: Boolean = true,
    val value: String = "",
)

data class ArticleSource(
    val url: String = "",
    val name: String = "",
)

data class RssArticle(
    val title: String = "",
    val link: String = "",
    val description: String = "",
    val author: String = "",
    val category: ArticleCategory = ArticleCategory(),
    val comment: String = "",
    val enclosure: ArticleEnclosure = ArticleEnclosure(),
    val guid: ArticleGuid = ArticleGuid(),
    val pubDate: String = "",
    val source: ArticleSource = ArticleSource(),
)