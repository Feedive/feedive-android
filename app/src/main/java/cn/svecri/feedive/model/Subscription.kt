package cn.svecri.feedive.model

import java.time.LocalDateTime

data class Subscription(
    val name: String,
    val url: String,
    val iconUrl: String = "",
    val protocol: String = "rss",
    val priority: Int = 1,
    val enabled: Boolean = true,
    val lastUse: LocalDateTime? = null,
    val lastUpdate: LocalDateTime? = null,
)