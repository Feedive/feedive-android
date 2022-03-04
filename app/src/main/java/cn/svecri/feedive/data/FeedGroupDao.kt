package cn.svecri.feedive.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FeedGroupDao {
    @Insert
    fun insertGroup(feedGroup: FeedGroup)

    @Query("select * from feed_group")
    fun getAll(): Flow<List<FeedGroup>>
}