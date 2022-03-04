package cn.svecri.feedive.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "feed_group")
data class FeedGroup(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "feed_group_id")
    val feedGroupId: Int,
    @ColumnInfo(name = "feed_group_name")
    val feedGroupName: String
)
