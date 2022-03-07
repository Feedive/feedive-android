package cn.svecri.feedive.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface FeedInGroupDao {
    @Insert
    fun insert(feedInGroup: FeedInGroup)

    @Query("select * from feed_in_group")
    fun getAll(): List<FeedInGroup>
}