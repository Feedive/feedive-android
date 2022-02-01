package cn.svecri.feedive.model

data class ChannelCategory(
    val domain: String = "",
    val value: String,
)

data class ChannelCloud(
    val domain: String,
    val port: Int,
    val path: String,
    val registerProcedure: String,
    val protocol: String,
)

data class ChannelImage(
    val url: String,
    val title: String,
    val link: String,
    val width: Int = 88,
    val height: Int = 31,
    val description: String,
)

/**
 * Following element is ignored：
 * - rating	The PICS rating for the channel.
 * - textInput	Specifies a text input box that can be displayed with the channel. More info here.
 * - skipHours	A hint for aggregators telling them which hours they can skip. This element contains up to 24 <hour> sub-elements whose value is a number between 0 and 23, representing a time in GMT, when aggregators, if they support the feature, may not read the channel on hours listed in the <skipHours> element. The hour beginning at midnight is hour zero.
 * - skipDays	A hint for aggregators telling them which days they can skip. This element contains up to seven <day> sub-elements whose value is Monday, Tuesday, Wednesday, Thursday, Friday, Saturday or Sunday. Aggregators may not read the channel during days listed in the <skipDays> element.
 */
data class Channel(
    val title: String,
    val link: String,
    val description: String,
    val language: String = "",
    val copyright: String = "",
    val managingEditor: String = "",
    val webMaster: String = "",
    val pubDate: String = "",
    val lastBuildDate: String = "",
    val category: ChannelCategory? = null,
    val generator: String = "",
    val docs: String = "",
    val cloud: ChannelCloud? = null,
    val ttl: Int = 0,
    val image: ChannelImage? = null,
    val articles: List<Article>
)
