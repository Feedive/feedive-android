package cn.svecri.feedive.data

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ArticleDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(articles: List<Article>)

    @Query(
        "select article.* from article " +
                "inner join feed on article.feed_id = feed.feed_id " +
                "where article.has_read in (:hasReadCondition) " +
                "and article.starred in (:starredCondition) " +
                "and article.later_read in (:laterReadCondition) and feed.feed_priority <= :sourcePriority " +
                "order by article.sort_time desc, article.id desc"
    )
    fun queryArticlesAll(
        hasReadCondition: List<Boolean>,
        starredCondition: List<Boolean>,
        laterReadCondition: List<Boolean>,
        sourcePriority: Int,
    ): PagingSource<Int, Article>

    @Query(
        "update article set has_read = 1 where id in (" +
                "select article.id from article " +
                "inner join feed on article.feed_id = feed.feed_id " +
                "where article.has_read == 0 " +
                "and article.starred in (:starredCondition) " +
                "and article.later_read in (:laterReadCondition) and feed.feed_priority <= :sourcePriority )"
    )
    suspend fun setArticlesHasRead(
        starredCondition: List<Boolean>,
        laterReadCondition: List<Boolean>,
        sourcePriority: Int,
    )

    @Query(
        "select article.* from article " +
                "inner join feed on article.feed_id = feed.feed_id " +
                "inner join feed_in_group on feed.feed_id = feed_in_group.feed_id " +
                "where article.has_read in (:hasReadCondition) " +
                "and article.starred in (:starredCondition) " +
                "and article.later_read in (:laterReadCondition) and feed_in_group.feed_group_id = :groupId and feed.feed_priority <= :sourcePriority " +
                "order by article.sort_time desc, article.id desc"
    )
    fun queryArticlesByGroup(
        hasReadCondition: List<Boolean>,
        starredCondition: List<Boolean>,
        laterReadCondition: List<Boolean>,
        groupId: Int,
        sourcePriority: Int,
    ): PagingSource<Int, Article>

    @Query(
        "update article set has_read = 1 where id in (" +
                "select article.id from article " +
                "inner join feed on article.feed_id = feed.feed_id " +
                "inner join feed_in_group on feed.feed_id = feed_in_group.feed_id " +
                "where article.has_read == 0 " +
                "and article.starred in (:starredCondition) " +
                "and article.later_read in (:laterReadCondition) and feed_in_group.feed_group_id = :groupId and feed.feed_priority <= :sourcePriority )"
    )
    suspend fun setArticlesHasReadByGroup(
        starredCondition: List<Boolean>,
        laterReadCondition: List<Boolean>,
        groupId: Int,
        sourcePriority: Int,
    )

    @Query(
        "select article.* from article " +
                "inner join feed on article.feed_id = feed.feed_id " +
                "where article.has_read in (:hasReadCondition) " +
                "and article.starred in (:starredCondition) " +
                "and article.later_read in (:laterReadCondition) and article.feed_id = :feedId and feed.feed_priority <= :sourcePriority " +
                "order by article.sort_time desc, article.id desc"
    )
    fun queryArticlesByFeed(
        hasReadCondition: List<Boolean>,
        starredCondition: List<Boolean>,
        laterReadCondition: List<Boolean>,
        feedId: Int,
        sourcePriority: Int,
    ): PagingSource<Int, Article>

    @Query(
        "update article set has_read = 1 where id in (" +
                "select article.id from article " +
                "inner join feed on article.feed_id = feed.feed_id " +
                "where article.has_read == 0 " +
                "and article.starred in (:starredCondition) " +
                "and article.later_read in (:laterReadCondition) and article.feed_id = :feedId and feed.feed_priority <= :sourcePriority )"
    )
    suspend fun setArticlesHasReadByFeed(
        starredCondition: List<Boolean>,
        laterReadCondition: List<Boolean>,
        feedId: Int,
        sourcePriority: Int,
    )

    @Query("select * from article where id = :articleId")
    suspend fun queryArticleById(articleId:Int):List<Article>


    @Query("delete from article")
    suspend fun clearAll()

    @Query("update article set starred=:starred where id=:articleId")
    suspend fun updateArticleStarred(articleId: Int, starred: Boolean)

    @Query("update article set has_read=:hasRead where id=:articleId")
    suspend fun updateArticleHasRead(articleId: Int, hasRead: Boolean)

    @Query("update article set later_read=:laterRead where id=:articleId")
    suspend fun updateArticleLaterRead(articleId: Int, laterRead: Boolean)
}