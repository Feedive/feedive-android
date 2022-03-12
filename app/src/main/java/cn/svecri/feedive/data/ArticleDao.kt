package cn.svecri.feedive.data

import androidx.paging.PagingSource
import androidx.room.*

@Dao
interface ArticleDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(articles: List<Article>)

    @Query("select * from article " +
            "where article.has_read in (:hasReadCondition) " +
            "and article.starred in (:starredCondition) " +
            "and article.later_read in (:laterReadCondition) and :groupName == :groupName and :sourcePriority == :sourcePriority " +
            "order by article.sort_time desc")
    fun queryArticles(
        hasReadCondition: List<Boolean>,
        starredCondition: List<Boolean>,
        laterReadCondition: List<Boolean>,
        groupName: String,
        sourcePriority: Int,
    ): PagingSource<Int, Article>

    @Query("delete from article")
    suspend fun clearAll()

    @Query("update article set starred=:starred where id=:articleId")
    suspend fun updateArticleStarred(articleId: Int, starred: Boolean)

    @Query("update article set has_read=:hasRead where id=:articleId")
    suspend fun updateArticleHasRead(articleId: Int, hasRead: Boolean)

    @Query("update article set later_read=:laterRead where id=:articleId")
    suspend fun updateArticleLaterRead(articleId: Int, laterRead: Boolean)
}