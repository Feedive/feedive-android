package cn.svecri.feedive.data

import androidx.room.*

@Dao
interface FeedDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeed(feed: Feed)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertFeeds(feeds: List<Feed>)

    @Delete
    fun deleteFeed(feed: Feed)

    @Delete
    fun deleteFeeds(feeds: List<Feed>)

    @Update
    fun updateFeed(feed: Feed)

    @Update
    fun updateFeeds(feeds: List<Feed>)

    @Query("select * from feed")
    suspend fun getAll(): List<Feed>
}