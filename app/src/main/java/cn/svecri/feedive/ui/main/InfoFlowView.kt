package cn.svecri.feedive.ui.main

import android.os.Parcelable
import android.util.Log
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.consumePositionChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import cn.svecri.feedive.R
import cn.svecri.feedive.model.Article
import cn.svecri.feedive.model.Subscription
import cn.svecri.feedive.ui.theme.FeediveTheme
import cn.svecri.feedive.utils.HttpWrapper
import cn.svecri.feedive.utils.RssParser
import coil.compose.rememberImagePainter
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

@Parcelize
data class ArticleInfo(
    val title: String,
    val picUrl: String = "",
    val sourceName: String,
    val time: LocalDateTime? = null,
    val abstract: String = "",
    val hasRead: Boolean = false,
    val protocol: String = "rss",
    val starred: Boolean = false,
) : Parcelable

@Parcelize
class ArticleInfoWithState(
    val info: ArticleInfo,
) : Parcelable {
    @IgnoredOnParcel
    val revealed: MutableState<Boolean> = mutableStateOf(false)
}

class InfoFlowViewModel : ViewModel() {
    private val httpClient: HttpWrapper = HttpWrapper()
    private val _articles = MutableStateFlow(listOf<ArticleInfoWithState>())
    val articles: StateFlow<List<ArticleInfoWithState>> get() = _articles

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> get() = _isRefreshing
    private var currentRefreshJob: Job? = null;

    private val dateTimeFormatters: List<DateTimeFormatter> = arrayListOf(
        DateTimeFormatter.RFC_1123_DATE_TIME,
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
        DateTimeFormatter.BASIC_ISO_DATE,
    )

    private fun getFirstImageUrl(html: String): String? =
        "<img [^<>]*src=\"([^\"]+)\"[^<>]*>".toRegex().find(html)?.groupValues?.get(1)

    private fun subscriptions(): List<Subscription> {
        return arrayListOf(
            Subscription(
                "笔吧评测室",
                "http://feedive.app.cloudendpoint.cn/rss/wechat?id=611ce7048fae751e2363fc8b"
            ),
            Subscription("Imobile", "http://news.imobile.com.cn/rss/news.xml", ""),
            Subscription("Sample", "https://www.rssboard.org/files/sample-rss-2.xml", ""),
        )
    }

    private fun fetchSubscription(subscription: Subscription) =
        run { httpClient.fetchAsFlow(subscription.url).onCompletion { Log.d("InfoFlow", "${subscription.name} fetch Completed") } }

    private fun fetchAllArticleInfo(subscription: Subscription) =
        run {
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
                    this.articles.add(article)
//                    Log.d("InfoFlow", this.articles.size.toString())
                }
                .asDisplay(subscription.name)
        }
    private fun Flow<Article>.asDisplay(sourceName: String) =
        map { article ->
            var pubDate: LocalDateTime? = null
            for (formatter in dateTimeFormatters) {
                try {
                    pubDate = LocalDateTime.parse(
                        article.pubDate,
                        formatter
                    )
                    break
                } catch (e: DateTimeParseException) {
                }
            }
            if (pubDate == null) {
                Log.d("InfoFlow", "Unrecognized DateTime: ${article.pubDate}")
            }
            ArticleInfo(
                title = article.title,
                picUrl = getFirstImageUrl(article.description).orEmpty(),
                sourceName = sourceName,
                time = pubDate,
            )
        }.map { articleInfo ->
            ArticleInfoWithState(
                info = articleInfo
            )
        }

    private fun Flow<ArticleInfoWithState>.collectAsList() =
        runningFold(listOf<ArticleInfoWithState>()) { acc, value ->
            acc + listOf(value)
        }

    private fun fetchAllSubscriptions() = run {
        subscriptions().asFlow()
            .onEach {
                Log.d("InfoFlow", "${it.name} Flow Start: ${Thread.currentThread().name}")
            }
            .flatMapMerge { subscription ->
                fetchAllArticleInfo(subscription)
            }
            .collectAsList()
    }

    fun refresh() {
        currentRefreshJob?.cancel()
        currentRefreshJob = viewModelScope.launch(Dispatchers.Default) {
            _isRefreshing.emit(true)
            fetchAllSubscriptions()
                .onCompletion {
                    if (it is java.util.concurrent.CancellationException) {
                        Log.i("InfoFlow", "Refresh Job Cancelled")
                    } else {
                        _isRefreshing.emit(false)
                    }
                }
                .collect { articleInfoList ->
                    launch(Dispatchers.Main) {
                        _articles.emit(articleInfoList)
                    }
                }
        }
    }
}

@Composable
fun InfoFlowView(vm: InfoFlowViewModel = viewModel()) {
    val articles by vm.articles.collectAsState()
    val isRefreshing by vm.isRefreshing.collectAsState()
    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing = isRefreshing)

    Scaffold(
        topBar = { TopAppBarWithTab { vm.refresh() } }
    ) {
        SwipeRefresh(
            state = swipeRefreshState,
            onRefresh = { vm.refresh() }
        ) {
            InfoFlowList(
                articles,
                offsetTop = with(LocalDensity.current) { swipeRefreshState.indicatorOffset.toDp() }
            )
        }
    }
}

@Composable
fun InfoFlowList(articles: List<ArticleInfoWithState>, offsetTop: Dp = 0f.dp) {
    val listState = rememberLazyListState()
    val resetAllArticlesRevealState = {
        var anyRevealed = false
        articles.forEach { info ->
            if (info.revealed.value) {
                anyRevealed = true
                info.revealed.value = false
            }
        }
        anyRevealed
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.offset(y = offsetTop)
    ) {
        items(articles) { articleInfoWithState ->
            var isRevealed by articleInfoWithState.revealed
            InteractiveArticleItem(
                info = articleInfoWithState.info,
                isRevealed = isRevealed,
                onClick = {
                    if (!resetAllArticlesRevealState()) {
                        Log.d("InfoFlow", "${articleInfoWithState.info.title} clicked")
                    }
                },
                onExpand = {
                    if (!isRevealed && !resetAllArticlesRevealState()) {
                        Log.d("InfoFlow", "${articleInfoWithState.info.title} revealed")
                        isRevealed = true
                    }
                },
                onCollapse = {
                    if (isRevealed) {
                        isRevealed = false
                    }
                },
            )
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
fun InteractiveArticleItem(
    info: ArticleInfo,
    onClick: () -> Unit = {},
    isRevealed: Boolean = false,
    onExpand: () -> Unit = {},
    onCollapse: () -> Unit = {},
) {
    DraggableCardWithButton(
        modifier = Modifier.fillMaxWidth(),
        isRevealed = isRevealed,
        cardOffset = 200.dp,
        onExpand = onExpand,
        onCollapse = onCollapse,
    ) {
        ArticleDetailedItem(info, onClick = onClick)
    }
}

@Composable
fun DraggableCardWithButton(
    modifier: Modifier = Modifier,
    isRevealed: Boolean,
    cardOffset: Dp,
    onExpand: () -> Unit,
    onCollapse: () -> Unit,
    content: @Composable () -> Unit,
) {
    val actionButtonModifier = { width: Dp ->
        Modifier
            .fillMaxHeight()
            .width(width = width)
    }
    Box(
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .matchParentSize()
        ) {
            Button(
                modifier = actionButtonModifier(50.dp),
                onClick = { /*TODO*/ }
            ) {
                Text(text = "已读")
            }
            Button(
                modifier = actionButtonModifier(50.dp),
                onClick = { /*TODO*/ }
            ) {
                Text(text = "收藏")
            }
            Button(
                modifier = actionButtonModifier(100.dp),
                onClick = { /*TODO*/ }
            ) {
                Text(text = "稍后阅读")
            }
        }
        DraggableCard(
            isRevealed = isRevealed,
            cardOffset = cardOffset,
            onExpand = onExpand,
            onCollapse = onCollapse,
            content = content
        )
    }
}

@Composable
fun DraggableCard(
    isRevealed: Boolean,
    cardOffset: Dp,
    onExpand: () -> Unit,
    onCollapse: () -> Unit,
    content: @Composable () -> Unit,
) {
    val cardOffsetValue: Float = cardOffset.value
    // offsetX is summed dragged offset by finger
    val offsetX = remember { mutableStateOf(0f) }
    val revealedTransitionState = remember {
        MutableTransitionState(isRevealed)
    }.apply {
        targetState = isRevealed
    }
    val transition =
        updateTransition(transitionState = revealedTransitionState, label = "cardOffsetTransition")
    // offsetTransition is used to fill the remaining offset, as a result:
    // offsetX + offsetTransition = cardOffset(revealed) or 0(unrevealed)
    val offsetTransition by transition.animateFloat(
        label = "cardOffsetTransition",
        transitionSpec = { tween(durationMillis = 500) },
        targetValueByState = {
            if (it) cardOffsetValue - offsetX.value else -offsetX.value
        }
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .offset((offsetX.value + offsetTransition).dp)
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, dragAmount ->
                    val original = Offset(offsetX.value, 0f)
                    val summed = original + Offset(x = dragAmount, y = 0f)
                    val newValue = Offset(summed.x.coerceIn(0f, cardOffsetValue), 0f)
                    if (newValue.x >= 10) {
                        onExpand()
                        return@detectHorizontalDragGestures
                    } else if (newValue.x <= 0) {
                        onCollapse()
                        return@detectHorizontalDragGestures
                    }
                    change.consumePositionChange()
                    offsetX.value = newValue.x
                }
            },
        content = content,
    )
}

@Composable
fun ArticleDetailedItem(
    info: ArticleInfo,
    onClick: () -> Unit = {},
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onClick()
            }
    ) {
        // TODO: 未解决的排版错误：当带图片的一条文章标题比自己部分稍长时，item高度会仅为单行标题+小标题的高度，导致小标题被吞掉了
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
                Text(
                    text = info.title,
                    fontSize = 14.sp,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 3,
                )
                Text(
                    text = "${info.sourceName}${
                        if (info.time != null) " / ${
                            Duration.between(
                                info.time,
                                LocalDateTime.now()
                            ).toHours()
                        } hr ago" else ""
                    }${if (info.hasRead) " - has read" else ""}",
                    fontSize = 10.sp,
                    maxLines = 1,
                )
            }
            if (info.picUrl.isNotEmpty()) {
                Image(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(120.dp),
//                    painter = painterResource(id = R.drawable.placeholder),
                    painter = rememberImagePainter(info.picUrl),
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
//                    painter = painterResource(id = R.drawable.placeholder),
                    painter = rememberImagePainter(info.picUrl),
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
    val navController = rememberNavController()
    FeediveTheme {
        InfoFlowView(navController)
    }
}

@Preview(showBackground = true, group = "Item")
@Composable
fun PreviewArticleSetItem() {
    val navController = rememberNavController()
    FeediveTheme {
        ArticleInfoWithState(
            info = ArticleInfo(
                title = "Rainbond对接Istio原理讲解和代码实现分析",
                picUrl = "",
                sourceName = "Dockone",
                time = LocalDateTime.of(2022, 1, 1, 0, 0),
                abstract = "",
                hasRead = false,
                "RSS",
                false,
            )
        , articleItem = Article(), navController = navController
        )
    }
}

@Preview(showBackground = true, group = "Item")
@Composable
fun PreviewInfoFlowList() {
    val navController = rememberNavController()
    FeediveTheme {
        InfoFlowList(
            articles = arrayListOf(
                ArticleInfoWithState(
                    info = ArticleInfo(
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
                ArticleInfoWithState(
                    info = ArticleInfo(
                        title = "Rainbond对接Istio原理讲解和代码实现分析",
                        picUrl = "some",
                        sourceName = "Dockone",
                        time = LocalDateTime.of(2022, 1, 1, 0, 0),
                        abstract = "",
                        hasRead = false,
                        "RSS",
                        false,
                    )
                ),
            )
        , articles = arrayListOf(), navController = navController)
    }
}