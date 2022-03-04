package cn.svecri.feedive.data

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(primaryKeys = ["feed_id","feed_group_id"])
data class FeedInGroup(
    @ColumnInfo(name = "feed_id")
    val feedId:Int,
    @ColumnInfo(name="feed_group_id")
    val feedGroupId:Int
)
