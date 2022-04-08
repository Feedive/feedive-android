package cn.svecri.feedive.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "feed")
data class Feed(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "feed_id")
    val feedId: Int,
    @ColumnInfo(name = "feed_name")
    var feedName: String,
    @ColumnInfo(name = "feed_type")
    var feedType: String,
    @ColumnInfo(name = "feed_url")
    var feedUrl: String,
    @ColumnInfo(name = "feed_priority")
    var feedPriority: Int,
    @ColumnInfo(name = "is_enable", defaultValue = "true")
    var isEnable: Boolean
) {
    constructor(
        feedId: Int,
        feedName: String,
        feedType: String,
        feedUrl: String,
        feedPriority: Int
    ) :this(feedId, feedName, feedType, feedUrl, feedPriority, true)

}
