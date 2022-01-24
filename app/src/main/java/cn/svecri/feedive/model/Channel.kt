package cn.svecri.feedive.model

import javax.xml.bind.annotation.XmlRootElement

@XmlRootElement( name = "rss" )
data class Rss(
    val channel: Channel,
)

data class Channel(
    val title: String,
    val link: String,
    val description: String,
    // TODO: https://validator.w3.org/feed/docs/rss2.html
    val articles: List<Article>
)
