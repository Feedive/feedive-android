package cn.svecri.feedive.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "feed")
data class Feed(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "feed_id")
    val feed_id: Int,
    @ColumnInfo(name = "feed_name")
    val feedName: String,
    @ColumnInfo(name = "feed_type")
    val feedType: String,
    @ColumnInfo(name = "feed_url")
    val feedUrl: String,
    @ColumnInfo(name = "feed_priority")
    val feedPriority: Int
)
