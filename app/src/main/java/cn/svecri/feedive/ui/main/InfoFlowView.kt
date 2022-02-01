package cn.svecri.feedive.ui.main

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import cn.svecri.feedive.R
import cn.svecri.feedive.model.Subscription
import cn.svecri.feedive.ui.theme.FeediveTheme
import cn.svecri.feedive.utils.RssParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import okhttp3.*
import java.io.IOException
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

data class ArticleInfo(
    val title: String,
    val picUrl: String = "",
    val sourceName: String,
    val time: LocalDateTime? = null,
    val abstract: String = "",
    val hasRead: Boolean = false,
    val protocol: String = "rss",
    val starred: Boolean = false,
)

data class ArticleInfoSet(
    val primary: ArticleInfo,
    val others: List<ArticleInfo> = arrayListOf(),
)

class InfoFlowViewModel : ViewModel() {
    var articles by mutableStateOf(listOf<ArticleInfoSet>())
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .callTimeout(Duration.ofMillis(5000))
        .build()
    private var currentRefreshJob: Job? = null;

    private val dateTimeFormatters: List<DateTimeFormatter> = arrayListOf(
        DateTimeFormatter.RFC_1123_DATE_TIME,
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
        DateTimeFormatter.BASIC_ISO_DATE,
    )

    val rssRequestBuilder: (String) -> Request = { url ->
        Request.Builder()
            .url(url)
            .build()
    }

    private fun subscriptions(): List<Subscription> {
        return arrayListOf(
            Subscription("笔吧评测室", "http://feedive.app.cloudendpoint.cn/rss/wechat?id=611ce7048fae751e2363fc8b"),
            Subscription("Imobile", "http://news.imobile.com.cn/rss/news.xml", ""),
            Subscription("Sample", "https://www.rssboard.org/files/sample-rss-2.xml", ""),
        )
    }

    private fun fetch(
        url: String,
        client: OkHttpClient,
        request: Request.Builder = Request.Builder(),
    ) = callbackFlow {
        val req = request.url(url).build()
        client.newCall(req).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    trySend(response)
                        .onFailure {
                            Log.e("InfoFlow", "Send Response Failed to Flow", it)
                        }
                } else {
                    cancel("bad http code")
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                cancel("okhttp error", e)
            }
        })
        awaitClose { }
    }

    private fun fetchSubscription(subscription: Subscription) =
        run { fetch(subscription.url, httpClient) }

    private fun fetchAllSubscriptions() = run {
        subscriptions().asFlow()
            .onEach {
                Log.d("InfoFlow", "${it.name} Flow Start: ${Thread.currentThread().name}")
            }
            .flatMapMerge { subscription ->
                fetchSubscription(subscription)
                    .map { response ->
                        Log.d(
                            "InfoFlow",
                            "Process Response of ${subscription.name}: ${Thread.currentThread().name}"
                        )
                        response.body?.byteStream()?.use { inputStream ->
                            RssParser().parse(inputStream)
                        }
                    }
                    .filterNotNull()
                    .flatMapConcat { channel ->
                        channel.articles.asFlow()
                    }
                    .filter { article ->
                        article.title.isNotEmpty()
                    }
                    .onEach { article ->
                        Log.d("InfoFlow", article.toString())
                    }
                    .map { article ->
                        var pubDate: LocalDateTime? = null
                        for (formatter in dateTimeFormatters) {
                            try {
                                pubDate = LocalDateTime.parse(
                                    article.pubDate,
                                    formatter
                                )
                                break
                            } catch (e: DateTimeParseException) {
                                Log.d("InfoFlow", "Unrecognized DateTime: ${article.pubDate}")
                            }
                        }
                        ArticleInfo(
                            title = article.title,
                            sourceName = subscription.name,
                            time = pubDate
                        )
                    }
                    .map { articleInfo ->
                        Log.d("InfoFlow", articleInfo.toString())
                        ArticleInfoSet(
                            primary = articleInfo,
                            others = listOf()
                        )
                    }
            }
            .runningFold(listOf<ArticleInfoSet>()) { acc, value ->
                acc + listOf(value)
            }
    }

    fun refresh() {
        currentRefreshJob?.cancel()
        currentRefreshJob = viewModelScope.launch(Dispatchers.Default) {
            fetchAllSubscriptions().collect { articleInfoSets ->
                launch(Dispatchers.Main) {
                    Log.d("InfoFlow", "${articleInfoSets.size}: ${Thread.currentThread().name}")
                    articles = articleInfoSets
                }
            }
        }
    }
}

@Composable
fun InfoFlowView(vm: InfoFlowViewModel = viewModel()) {
    Scaffold(
        topBar = { TopAppBarWithTab { vm.refresh() } }
    ) {
        InfoFlowList(vm.articles)
    }
}

@Composable
fun InfoFlowList(articles: List<ArticleInfoSet>) {
    val listState = rememberLazyListState()

    Log.d("InfoFlow", "InfoFlowList Recompose")
    LazyColumn(
        state = listState
    ) {
        items(articles) { articleInfoSet ->
            Log.d("InfoFlow", "New Item ${articleInfoSet.primary.title}")
            ArticleSetItem(infoSet = articleInfoSet)
        }
    }
}

@Composable
fun TopAppBarWithTab(
    refresh: () -> Unit = {}
) {
    TopAppBar(
        title = {
            ScrollableTabRow(
                selectedTabIndex = 0,
                modifier = Modifier.fillMaxHeight()
            ) {
                Tab(
                    selected = true,
                    modifier = Modifier.fillMaxHeight(),
                    onClick = { /*TODO*/ }
                ) {
                    Text(text = "All")
                }
                Tab(
                    selected = false,
                    modifier = Modifier.fillMaxHeight(),
                    onClick = { /*TODO*/ }
                ) {
                    Text(text = "Computer")
                }
            }
        },
        actions = {
            IconButton(onClick = { /*TODO*/ }) {
                Column(
                    modifier = Modifier.width(40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "PRIOR", fontSize = 9.sp)
                    Text(text = "4", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
            IconButton(onClick = refresh) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_baseline_refresh_24),
                    contentDescription = "Refresh Icon"
                )
            }
            IconButton(onClick = { /*TODO*/ }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_email_read_24),
                    contentDescription = "Switch Read or Unread or all articles"
                )
            }
        }
    )
}

@Composable
fun ArticleSetItem(infoSet: ArticleInfoSet) {
    Surface(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            ArticleDetailedItem(infoSet.primary)
            for (info in infoSet.others) {
                ArticleAbstractItem(info)
            }
        }
    }
}

@Composable
fun ArticleDetailedItem(info: ArticleInfo) {
    Surface(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .height(IntrinsicSize.Max)
                .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
            ) {
                Text(text = info.title, fontSize = 14.sp)
                Text(
                    text = "${info.sourceName}${
                        if (info.time != null) " / ${
                            Duration.between(
                                info.time,
                                LocalDateTime.now()
                            ).toHours()
                        } hr ago" else ""
                    }${if (info.hasRead) " - has read" else ""}", fontSize = 10.sp
                )
            }
            if (info.picUrl.isNotEmpty()) {
                Image(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(120.dp),
                    painter = painterResource(id = R.drawable.placeholder),
                    contentScale = ContentScale.Crop,
                    contentDescription = "image"
                )
            }
        }
    }
}

@Composable
fun ArticleAbstractItem(info: ArticleInfo) {
    Surface(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .height(IntrinsicSize.Max)
                .padding(start = 20.dp, top = 5.dp, bottom = 5.dp, end = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
            ) {
                Text(text = info.title, fontSize = 14.sp)
            }
            if (info.picUrl.isNotEmpty()) {
                Image(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(90.dp),
                    painter = painterResource(id = R.drawable.placeholder),
                    contentScale = ContentScale.Crop,
                    contentDescription = "image"
                )
            }
        }
    }
}

@Preview(showBackground = true, group = "Overview")
@Composable
fun PreviewHomeInfoFlow() {
    FeediveTheme {
        InfoFlowView()
    }
}

@Preview(showBackground = true, group = "Item")
@Composable
fun PreviewArticleSetItem() {
    FeediveTheme {
        ArticleSetItem(
            ArticleInfoSet(
                primary = ArticleInfo(
                    title = "Rainbond对接Istio原理讲解和代码实现分析",
                    picUrl = "",
                    sourceName = "Dockone",
                    time = LocalDateTime.of(2022, 1, 1, 0, 0),
                    abstract = "",
                    hasRead = false,
                    "RSS",
                    false,
                ),
                others = arrayListOf()
            )
        )
    }
}

@Preview(showBackground = true, group = "Item")
@Composable
fun PreviewInfoFlowList() {
    FeediveTheme {
        InfoFlowList(
            articles = arrayListOf(
                ArticleInfoSet(
                    primary = ArticleInfo(
                        title = "Rainbond对接Istio原理讲解和代码实现分析",
                        picUrl = "",
                        sourceName = "Dockone",
                        time = LocalDateTime.of(2022, 1, 1, 0, 0),
                        abstract = "",
                        hasRead = false,
                        "RSS",
                        false,
                    ),
                    others = arrayListOf(
                        ArticleInfo(
                            title = "谐云DevOps产品可信源管理从容应对Apache Log4j2高危漏洞",
                            picUrl = "",
                            sourceName = "Dockone",
                            time = LocalDateTime.of(2022, 1, 1, 0, 0),
                            abstract = "",
                            hasRead = false,
                            "RSS",
                            false,
                        )
                    )
                ),
                ArticleInfoSet(
                    primary = ArticleInfo(
                        title = "Rainbond对接Istio原理讲解和代码实现分析",
                        picUrl = "",
                        sourceName = "Dockone",
                        time = LocalDateTime.of(2022, 1, 1, 0, 0),
                        abstract = "",
                        hasRead = false,
                        "RSS",
                        false,
                    )
                ),
            )
        )
    }
}