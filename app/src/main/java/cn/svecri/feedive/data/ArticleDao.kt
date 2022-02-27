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
}