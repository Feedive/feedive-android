package cn.svecri.feedive.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

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
    suspend fun updateFeed(feed: Feed)

    @Update
    fun updateFeeds(feeds: List<Feed>)

    @Query("select * from feed where feed_name = :name")
    suspend fun getFeedByName(name: String): Feed

    @Query("select * from feed")
    suspend fun getAllFeeds(): List<Feed>

    @Query("select * from feed")
    fun getAllFeedsFlow(): Flow<List<Feed>>

    @Query("select * from feed where feed_id = :feedId")
    suspend fun getById(feedId: Int): Feed

    @Query("select * from feed where feed_id = :feedId")
    fun getFlowById(feedId: Int): Flow<Feed>

    @Query(
        "select feed.* from feed " +
                "inner join feed_in_group on feed.feed_id = feed_in_group.feed_id " +
                "where feed_in_group.feed_group_id = :groupId"
    )
    suspend fun getByGroupId(groupId: Int): List<Feed>
}