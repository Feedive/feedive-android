package cn.svecri.feedive.data

import android.util.Log
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import cn.svecri.feedive.model.RssArticle
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

@Entity(
    tableName = "article",
    indices = [
        Index(value = ["feed_id", "sort_time", "id"]),
        Index(value = ["storage_key"], unique = true),
        Index(value = ["later_read", "sort_time", "id"]),
        Index(value = ["has_read", "sort_time", "id"]),
        Index(value = ["starred", "sort_time", "id"]),
        Index(value = ["feed_id", "later_read", "has_read", "starred", "sort_time", "id"]),
    ]
)
data class Article(
    @PrimaryKey(autoGenerate = true) val id: Int = 0, // Insert methods treat 0 as not-set while inserting the item.
    val title: String = "",
    val link: String = "",
    val description: String = "",
    @ColumnInfo(name = "pic_url") val picUrl: String = "",
    val author: String = "",
    val protocol: String = "rss",
    @ColumnInfo(name = "pub_date") val pubDate: String = "",
    @ColumnInfo(name = "is_perma_link") val isPermaLink: Boolean = true,
    val guid: String = "",
    @ColumnInfo(name = "category_domain") val categoryDomain: String = "",
    val category: String = "",
    @ColumnInfo(name = "pub_time") val pubTime: LocalDateTime? = null,
    @ColumnInfo(name = "update_time") val updateTime: LocalDateTime = LocalDateTime.now(),
    @ColumnInfo(name = "sort_time") val sortTime: LocalDateTime,
    @ColumnInfo(name = "storage_key") val storageKey: String,
    @ColumnInfo(name = "feed_id") val feedId: Int,
    @ColumnInfo(name = "source_name") val sourceName: String,
    @ColumnInfo(name = "last_read_time") val lastReadTime: LocalDateTime? = null,
    @ColumnInfo(name = "has_read") val hasRead: Boolean = false,
    @ColumnInfo(name = "starred") val starred: Boolean = false,
    @ColumnInfo(name = "later_read") val laterRead: Boolean = false,
) {
    companion object {
        private fun getFirstImageUrl(html: String): String? =
            "<img [^<>]*src=\"([^\"]+)\"[^<>]*>".toRegex().find(html)?.groupValues?.get(1)

        private val dateTimeFormatters: List<DateTimeFormatter> = arrayListOf(
            DateTimeFormatter.RFC_1123_DATE_TIME,
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.BASIC_ISO_DATE,
        )

        // 使用前应过滤掉所有标题为空的文章
        fun fromRss(article: RssArticle, feedId: Int, sourceName: String) = run {
            var pubDate: LocalDateTime? = null
            for (formatter in dateTimeFormatters) {
                try {
                    pubDate = LocalDateTime.parse(
                        article.pubDate,
                        formatter
                    )
                    break
                } catch (e: DateTimeParseException) {
                }
            }
            if (pubDate == null) {
                Log.d("InfoFlow", "Unrecognized DateTime: ${article.pubDate}")
            }
            val now = LocalDateTime.now()
            val sortTime = pubDate ?: now
            Article(
                title = article.title,
                link = article.link,
                description = article.description,
                picUrl = getFirstImageUrl(article.description).orEmpty(),
                author = article.author,
                protocol = "rss",
                pubDate = article.pubDate,
                isPermaLink = article.guid.isPermaLink,
                guid = article.guid.value,
                categoryDomain = article.category.domain,
                category = article.category.category,
                pubTime = pubDate,
                updateTime = now,
                sortTime = sortTime,
                storageKey = article.guid.value.ifEmpty {
                    article.title.substring(
                        0 until article.title.length.coerceAtMost(
                            30
                        )
                    )
                },
                feedId = feedId,
                sourceName = sourceName,
                lastReadTime = null,
                starred = false,
                laterRead = false,
            )
        }
    }
}