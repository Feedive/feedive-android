package cn.svecri.feedive.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FeedInGroupDao {
    @Insert
    fun insert(feedInGroup: FeedInGroup)

    @Query("select * from feed_in_group")
    fun getAll(): List<FeedInGroup>

    @Query("select feed.* from feed_in_group " +
                "left join feed on feed_in_group.feed_id = feed.feed_id " +
                "where feed_in_group.feed_group_id = :groupId")
    fun getGroupChildren(groupId: Int): Flow<List<Feed>>
}