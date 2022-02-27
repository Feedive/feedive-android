package cn.svecri.feedive.ui.main

import android.app.Application
import android.os.Parcelable
import android.util.Log
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import androidx.paging.*
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemsIndexed
import cn.svecri.feedive.R
import cn.svecri.feedive.data.AppDatabase
import cn.svecri.feedive.data.ArticleRemoteMediator
import cn.svecri.feedive.ui.theme.FeediveTheme
import cn.svecri.feedive.utils.HttpWrapper
import coil.compose.rememberImagePainter
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import java.time.Duration
import java.time.LocalDateTime

@Parcelize
data class ArticleInfo(
    val articleId: Int,
    val title: String,
    val picUrl: String = "",
    val sourceName: String,
    val time: LocalDateTime? = null,
    val abstract: String = "",
    val hasRead: Boolean = false,
    val protocol: String = "rss",
    val starred: Boolean = false,
    val link: String = "",
) : Parcelable

@Parcelize
class ArticleInfoWithState(
    val info: ArticleInfo,
) : Parcelable {
    @IgnoredOnParcel
    val revealed: MutableState<Boolean> = mutableStateOf(false)
}

class InfoFlowViewModel(application: Application) : AndroidViewModel(application) {
    private val httpClient: HttpWrapper = HttpWrapper()
    private val appDatabase = AppDatabase.getInstance(application)
    private val articleDao = appDatabase.articleDao()
    val groupName by mutableStateOf("All")

    @OptIn(ExperimentalPagingApi::class)
    val pager = Pager(
        PagingConfig(
            pageSize = 20,
        ),
        remoteMediator = ArticleRemoteMediator(appDatabase, httpClient, groupName, 5)
    ) {
        articleDao.queryArticles(
            listOf(true, false),
            listOf(true, false),
            listOf(true, false),
            groupName,
            5
        )
    }
    val articleFlow = pager.flow.map {
        it
            .map { article ->
                ArticleInfo(
                    article.id,
                    article.title,
                    article.picUrl,
                    article.sourceName,
                    article.pubTime,
                    article.description,
                    article.hasRead,
                    article.protocol,
                    article.starred,
                    article.link,
                )
            }
            .map { articleInfo ->
                ArticleInfoWithState(articleInfo)
            }
    }.cachedIn(viewModelScope)
}

@Composable
fun InfoFlowView(vm: InfoFlowViewModel = viewModel(), navController: NavController) {
    val articles = vm.articleFlow.collectAsLazyPagingItems()
    val isRefreshing = articles.loadState.refresh == LoadState.Loading
    Log.d("InfoFlow", "Main View Recompose ${articles.loadState.refresh}")
    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing = isRefreshing)

    var groupName = vm.groupName

    Scaffold(
        topBar = {
            TopAppBarWithTab(groupName = groupName, onRefresh = { articles.refresh() }) {
                groupName = it
            }
        }
    ) {
        SwipeRefresh(
            state = swipeRefreshState,
            onRefresh = { articles.refresh() }
        ) {
            InfoFlowList(
                articles,
                offsetTop = with(LocalDensity.current) { swipeRefreshState.indicatorOffset.toDp() },
                navController,
            )
        }
    }
}

@Composable
fun InfoFlowList(
    articles: LazyPagingItems<ArticleInfoWithState>,
    offsetTop: Dp = 0f.dp,
    navController: NavController
) {
    val listState = rememberLazyListState()
    val resetAllArticlesRevealState = {
        var anyRevealed = false
        articles.itemSnapshotList.forEach { info ->
            if (info?.revealed?.value == true) {
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
        itemsIndexed(articles) { _, item ->
            item?.let { articleInfoWithState ->
                var isRevealed by articleInfoWithState.revealed
                InteractiveArticleItem(
                    info = articleInfoWithState.info,
                    isRevealed = isRevealed,
                    onClick = {
                        if (!resetAllArticlesRevealState()) {
                            Log.d("InfoFlow", "${articleInfoWithState.info.title} clicked")
                            Log.d("InfoFlow", "navigate to ${articleInfoWithState.info.link}")
                            navController.navigate("article?link=${articleInfoWithState.info.link}")
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
}

@Composable
fun TopAppBarWithTab(
    groupName: String,
    onRefresh: () -> Unit = {},
    setGroupName: (String) -> Unit = {}
) {
    val groups = remember { listOf("All", "Computer") }
    var tabIndex = groups.indexOf(groupName)
    if (tabIndex < 0) {
        tabIndex = 0
        setGroupName(groups[0])
    }
    TopAppBar(
        title = {
            ScrollableTabRow(
                selectedTabIndex = tabIndex,
                modifier = Modifier.fillMaxHeight()
            ) {
                for (group in groups) {
                    Tab(
                        selected = true,
                        modifier = Modifier.fillMaxHeight(),
                        onClick = { setGroupName(group) }
                    ) {
                        Text(text = group)
                    }
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
            IconButton(onClick = onRefresh) {
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
        InfoFlowView(navController = navController)
    }
}

@Preview(showBackground = true, group = "Item")
@Composable
fun PreviewArticleSetItem() {
    FeediveTheme {
        ArticleInfoWithState(
            info = ArticleInfo(
                articleId = 0,
                title = "Rainbond对接Istio原理讲解和代码实现分析",
                picUrl = "",
                sourceName = "Dockone",
                time = LocalDateTime.of(2022, 1, 1, 0, 0),
                abstract = "",
                hasRead = false,
                "RSS",
                false,
            ),
        )
    }
}

@Preview(showBackground = true, group = "Item")
@Composable
fun PreviewInfoFlowList() {
    val navController = rememberNavController()
    val articleFlow = flowOf(
        PagingData.from(
            arrayListOf(
                ArticleInfoWithState(
                    info = ArticleInfo(
                        articleId = 0,
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
                        articleId = 1,
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
        )
    )
    FeediveTheme {
        InfoFlowList(
            articles = articleFlow.collectAsLazyPagingItems(), navController = navController
        )
    }
}