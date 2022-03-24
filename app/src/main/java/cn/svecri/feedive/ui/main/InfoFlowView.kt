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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.consumePositionChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
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
import coil.compose.AsyncImage
import coil.compose.rememberImagePainter
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Scale
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
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
    val laterRead: Boolean = false,
    val link: String = "",
) : Parcelable

class ArticleInfoWithState(
    val info: ArticleInfo,
) {
//    val revealed: MutableState<Boolean> = mutableStateOf(initRevealed)
}

class InfoFlowViewModel(application: Application) : AndroidViewModel(application) {
    val httpClient: HttpWrapper = HttpWrapper()
    val appDatabase = AppDatabase.getInstance(application)
    val articleDao = appDatabase.articleDao()
    val feedDao = appDatabase.feedDao()
    val feedGroupDao = appDatabase.feedGroupDao()
    var priority by mutableStateOf(5)
    var hasReadCondition by mutableStateOf(listOf(true, false))
    var starredCondition by mutableStateOf(listOf(true, false))
    var laterReadCondition by mutableStateOf(listOf(true, false))

    fun toggleHasRead() {
        hasReadCondition = if (hasReadCondition.size == 2) {
            listOf(false)
        } else {
            listOf(true, false)
        }
    }

    fun updateArticleStarred(articleId: Int, starred: Boolean) {
        viewModelScope.launch {
            articleDao.updateArticleStarred(articleId, starred)
        }
    }

    fun updateArticleHasRead(articleId: Int, hasRead: Boolean) {
        viewModelScope.launch {
            articleDao.updateArticleHasRead(articleId, hasRead)
        }
    }

    fun updateArticleLaterRead(articleId: Int, laterRead: Boolean) {
        viewModelScope.launch {
            articleDao.updateArticleLaterRead(articleId, laterRead)
        }
    }
}

sealed class ArticleFetchType {
    object All : ArticleFetchType();
    data class Group(val groupId: Int) : ArticleFetchType();
    data class Feed(val feedId: Int) : ArticleFetchType();
    object Starred : ArticleFetchType();
    object LaterRead : ArticleFetchType();

    fun intoRemoteFetchType(priority: Int?): ArticleRemoteMediator.RemoteFetchType {
        return when (this) {
            is All -> ArticleRemoteMediator.RemoteFetchType.All((priority ?: 5).coerceIn(1, 5))
            is Group -> ArticleRemoteMediator.RemoteFetchType.Group(
                groupId,
                (priority ?: 5).coerceIn(1, 5)
            )
            is Feed -> ArticleRemoteMediator.RemoteFetchType.Feed(feedId)
            else -> ArticleRemoteMediator.RemoteFetchType.None
        }
    }

    companion object {
        fun buildFromArgs(type: String?, detail: String?): ArticleFetchType {
            when (type) {
                "group" -> {
                    when (detail?.toIntOrNull()) {
                        is Int -> return Group(detail.toInt())
                    }
                }
                "feed" -> {
                    when (detail?.toIntOrNull()) {
                        is Int -> return Feed(detail.toInt())
                    }
                }
                "starred" -> return Starred
                "laterRead" -> return LaterRead
            }
            return All
        }
    }
}

@Composable
fun InfoFlowView(
    vm: InfoFlowViewModel = viewModel(),
    type: ArticleFetchType,
    navController: NavController
) {

    val flowType: ArticleFetchType by remember { mutableStateOf(type) }
    val remoteFetchCondition = remember {
        ArticleRemoteMediator.FetchCondition(
            flowType.intoRemoteFetchType(vm.priority),
            ArticleRemoteMediator.RefreshType.REMOTE
        )
    }
    val title by remember {
        when (flowType) {
            is ArticleFetchType.All -> flowOf("All")
            is ArticleFetchType.LaterRead -> flowOf("Later Read")
            is ArticleFetchType.Starred -> flowOf("Star")
            is ArticleFetchType.Group -> {
                vm.feedGroupDao.getById((flowType as ArticleFetchType.Group).groupId)
                    .map { it.feedGroupName }
            }
            is ArticleFetchType.Feed -> {
                vm.feedDao.getFlowById((flowType as ArticleFetchType.Feed).feedId)
                    .map { it.feedName }
            }
        }
    }.collectAsState(initial = "Unknown")

    @OptIn(ExperimentalPagingApi::class)
    val pager = Pager(
        PagingConfig(
            pageSize = 20,
        ),
        remoteMediator = ArticleRemoteMediator(vm.appDatabase, vm.httpClient, remoteFetchCondition)
    ) {
        when (flowType) {
            is ArticleFetchType.All -> {
                vm.articleDao.queryArticlesAll(
                    hasReadCondition = vm.hasReadCondition,
                    starredCondition = vm.starredCondition,
                    laterReadCondition = vm.laterReadCondition,
                    vm.priority
                )
            }
            is ArticleFetchType.Group -> {
                vm.articleDao.queryArticlesByGroup(
                    hasReadCondition = vm.hasReadCondition,
                    starredCondition = vm.starredCondition,
                    laterReadCondition = vm.laterReadCondition,
                    groupId = (flowType as ArticleFetchType.Group).groupId,
                    vm.priority
                )
            }
            is ArticleFetchType.Feed -> {
                vm.articleDao.queryArticlesByFeed(
                    hasReadCondition = vm.hasReadCondition,
                    starredCondition = vm.starredCondition,
                    laterReadCondition = vm.laterReadCondition,
                    feedId = (flowType as ArticleFetchType.Feed).feedId,
                    vm.priority
                )
            }
            is ArticleFetchType.Starred -> {
                vm.articleDao.queryArticlesAll(
                    hasReadCondition = vm.hasReadCondition,
                    starredCondition = listOf(true),
                    laterReadCondition = vm.laterReadCondition,
                    vm.priority
                )
            }
            is ArticleFetchType.LaterRead -> {
                vm.articleDao.queryArticlesAll(
                    hasReadCondition = vm.hasReadCondition,
                    starredCondition = vm.starredCondition,
                    laterReadCondition = listOf(true),
                    vm.priority
                )
            }
        }
    }
    val articles = remember {
        pager.flow.map {
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
                        article.laterRead,
                        article.link,
                    )
                }
                .map { articleInfo ->
                    ArticleInfoWithState(articleInfo)
                }
        }
    }.collectAsLazyPagingItems()

    val scope = rememberCoroutineScope()
    val setAllHasRead = suspend {
        when (flowType) {
            is ArticleFetchType.All -> {
                vm.articleDao.setArticlesHasRead(
                    starredCondition = vm.starredCondition,
                    laterReadCondition = vm.laterReadCondition,
                    vm.priority
                )
            }
            is ArticleFetchType.Group -> {
                vm.articleDao.setArticlesHasReadByGroup(
                    starredCondition = vm.starredCondition,
                    laterReadCondition = vm.laterReadCondition,
                    groupId = (flowType as ArticleFetchType.Group).groupId,
                    vm.priority
                )
            }
            is ArticleFetchType.Feed -> {
                vm.articleDao.setArticlesHasReadByFeed(
                    starredCondition = vm.starredCondition,
                    laterReadCondition = vm.laterReadCondition,
                    feedId = (flowType as ArticleFetchType.Feed).feedId,
                    vm.priority
                )
            }
            is ArticleFetchType.Starred -> {
                vm.articleDao.setArticlesHasRead(
                    starredCondition = listOf(true),
                    laterReadCondition = vm.laterReadCondition,
                    vm.priority
                )
            }
            is ArticleFetchType.LaterRead -> {
                vm.articleDao.setArticlesHasRead(
                    starredCondition = vm.starredCondition,
                    laterReadCondition = listOf(true),
                    vm.priority
                )
            }
        }
    }

    val isRefreshing = articles.loadState.refresh == LoadState.Loading
    Log.d("InfoFlow", "Main View Recompose ${articles.loadState.refresh}")
    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing = isRefreshing)

    Scaffold(
        topBar = {
            TopAppBarWithTab(
                title = title,
                hasReadCondition = vm.hasReadCondition,
                priority = vm.priority,
                onPriorityChange = {
                    vm.priority = it
                    remoteFetchCondition.refreshType =
                        ArticleRemoteMediator.RefreshType.NO_REMOTE
                    articles.refresh()
                },
                onRefresh = {
                    remoteFetchCondition.refreshType =
                        ArticleRemoteMediator.RefreshType.REMOTE
                    articles.refresh()
                },
                onToggleHasRead = {
                    vm.toggleHasRead()
                    remoteFetchCondition.refreshType =
                        ArticleRemoteMediator.RefreshType.NO_REMOTE
                    articles.refresh()
                },
                onSetAllHasRead = {
                    scope.launch(Dispatchers.IO) {
                        setAllHasRead()
                    }
                }
            )
        }
    ) {
        SwipeRefresh(
            state = swipeRefreshState,
            onRefresh = {
                remoteFetchCondition.refreshType = ArticleRemoteMediator.RefreshType.REMOTE
                articles.refresh()
            }
        ) {
            InfoFlowList(
                articles,
                offsetTop = with(LocalDensity.current) { swipeRefreshState.indicatorOffset.toDp() },
                navController = navController,
            )
        }
    }
}

@Composable
fun InfoFlowList(
    articles: LazyPagingItems<ArticleInfoWithState>,
    offsetTop: Dp = 0f.dp,
    navController: NavController,
) {
    val listState = rememberLazyListState()
    var revealedId by remember { mutableStateOf(-1) }
    val resetAllArticlesRevealState = {
        var anyRevealed = false
        if (revealedId >= 0) {
            anyRevealed = true
            revealedId = -1
        }
        anyRevealed
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.offset(y = offsetTop)
    ) {
        itemsIndexed(
            items = articles,
            key = { _, item ->
                item.info.articleId
            }
        ) { _, item ->
            item?.let { articleInfoWithState ->
                val isRevealed by
                remember(key1 = articleInfoWithState.info.articleId) {
                    derivedStateOf {
                        articleInfoWithState.info.articleId == revealedId
                    }
                }
                InteractiveArticleItem(
                    info = articleInfoWithState.info,
                    isRevealed = articleInfoWithState.info.articleId == revealedId,
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
                            revealedId = item.info.articleId
                        }
                    },
                    onCollapse = {
                        if (isRevealed) {
                            Log.d("InfoFlow", "${articleInfoWithState.info.title} collapse")
                            revealedId = -1
                        }
                    },
                )
            }
        }
    }
}

@Composable
fun TopAppBarWithTab(
    title: String,
    priority: Int = 5,
    hasReadCondition: List<Boolean> = listOf(true, false),
    onPriorityChange: (Int) -> Unit = {},
    onRefresh: () -> Unit = {},
    onToggleHasRead: () -> Unit = {},
    onSetAllHasRead: () -> Unit = {},
) {
    var priorityDisplay: Int by remember {
        mutableStateOf(priority)
    }
    var prioritySlideRevealed by remember {
        mutableStateOf(false)
    }
    TopAppBar(
        title = {
            Text(text = title)
        },
        actions = {
            IconButton(onClick = { prioritySlideRevealed = true }) {
                Column(
                    modifier = Modifier.width(40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "PRIOR", fontSize = 9.sp)
                    Text(text = "$priorityDisplay", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
            DropdownSlide(
                modifier = Modifier.width(150.dp),
                value = priorityDisplay,
                onValueChange = { priorityDisplay = it },
                onValueChangeFinished = { onPriorityChange(priorityDisplay) },
                expand = prioritySlideRevealed,
                onDismissRequest = { prioritySlideRevealed = false })
            IconButton(onClick = onRefresh) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_baseline_refresh_24),
                    contentDescription = "Refresh Icon"
                )
            }
            IconButton(onClick = onToggleHasRead) {
                Column(
                    modifier = Modifier.width(40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (hasReadCondition.contains(true)) {
                        Text(text = "全部", fontSize = 14.sp)
                    } else {
                        Text(text = "未读", fontSize = 14.sp)
                    }
                }
            }
            IconButton(onClick = onSetAllHasRead) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_email_read_24),
                    modifier = Modifier.size(24.dp),
                    contentDescription = "Switch Read or Unread or all articles"
                )
            }
        }
    )
}

@Composable
fun DropdownSlide(
    value: Int,
    expand: Boolean,
    modifier: Modifier = Modifier,
    onValueChange: (Int) -> Unit = {},
    onValueChangeFinished: () -> Unit = {},
    onDismissRequest: () -> Unit,
    offset: DpOffset = DpOffset(0.dp, 0.dp),
    properties: PopupProperties = PopupProperties(focusable = true),
) {
    val expandedStates = remember {
        MutableTransitionState(false)
    }.apply { targetState = expand }
    val density = LocalDensity.current

    val popupPositionProvider = remember {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize
            ): IntOffset {
                val contentOffsetX = with(density) { offset.x.roundToPx() }
                val contentOffsetY = with(density) { offset.y.roundToPx() }

                val toLeft = anchorBounds.left + contentOffsetX
                val toRight = anchorBounds.right - contentOffsetX - popupContentSize.width
                val toDisplayLeft = windowSize.width - popupContentSize.width
                val toDisplayRight = 0
                val x = if (layoutDirection == LayoutDirection.Ltr) {
                    sequenceOf(
                        toLeft,
                        toRight,
                        if (anchorBounds.left >= 0) toDisplayLeft else toDisplayRight
                    )
                } else {
                    sequenceOf(
                        toRight,
                        toLeft,
                        if (anchorBounds.right <= windowSize.width) toDisplayRight else toDisplayLeft
                    )
                }.firstOrNull {
                    it >= 0 && it + popupContentSize.width <= windowSize.width
                } ?: toRight
                val toBottom = anchorBounds.bottom + contentOffsetY
                val toTop = anchorBounds.top - contentOffsetY - popupContentSize.height
                val toCenter = anchorBounds.top - popupContentSize.height / 2
                val toDisplayBottom = windowSize.height - popupContentSize.height
                val y = sequenceOf(toBottom, toTop, toCenter, toDisplayBottom).firstOrNull {
                    it >= 0 &&
                            it + popupContentSize.height <= windowSize.height
                } ?: toTop
                return IntOffset(x, y)
            }
        }
    }

    if (expandedStates.currentState || expandedStates.targetState) {
        Popup(
            onDismissRequest = onDismissRequest,
            popupPositionProvider = popupPositionProvider,
            properties = properties
        ) {
            Surface {
                Slider(
                    modifier = modifier,
                    value = value.toFloat(),
                    onValueChange = { onValueChange((it+0.49).toInt()) },
                    valueRange = 1f..5f,
                    steps = 1,
                    onValueChangeFinished = onValueChangeFinished
                )
            }
        }
    }
}

@Composable
fun InteractiveArticleItem(
    vm: InfoFlowViewModel = viewModel(),
    info: ArticleInfo,
    onClick: () -> Unit = {},
    isRevealed: Boolean = false,
    onExpand: () -> Unit = {},
    onCollapse: () -> Unit = {},
) {
    DraggableCardWithButton(
        modifier = Modifier.fillMaxWidth(),
        isRevealed = isRevealed,
        cardOffset = 150.dp,
        hasRead = info.hasRead,
        starred = info.starred,
        laterRead = info.laterRead,
        onExpand = onExpand,
        onCollapse = onCollapse,
        onStar = {
            vm.updateArticleStarred(info.articleId, !info.starred)
        },
        onHasRead = {
            vm.updateArticleHasRead(info.articleId, !info.hasRead)
        },
        onLaterRead = {
            vm.updateArticleLaterRead(info.articleId, !info.laterRead)
        },
    ) {
        ArticleDetailedItem(info, onClick = onClick)
    }
}

@Composable
fun DraggableCardWithButton(
    modifier: Modifier = Modifier,
    isRevealed: Boolean,
    cardOffset: Dp = 150.dp,
    hasRead: Boolean = false,
    starred: Boolean = false,
    laterRead: Boolean = false,
    onExpand: () -> Unit,
    onCollapse: () -> Unit,
    onHasRead: () -> Unit = {},
    onStar: () -> Unit = {},
    onLaterRead: () -> Unit = {},
    content: @Composable () -> Unit,
) {
    val actionButtonModifier = { width: Dp ->
        Modifier
            .fillMaxHeight()
            .padding(0.dp)
            .width(width = width)
    }
    val buttonWidth = cardOffset / 3
    Box(
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .matchParentSize()
        ) {
            Button(
                modifier = actionButtonModifier(buttonWidth),
                shape = RectangleShape,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = if (hasRead) {
                        MaterialTheme.colors.background
                    } else {
                        MaterialTheme.colors.primarySurface
                    }
                ),
                onClick = onHasRead,
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_has_read),
                    tint = if (hasRead) {
                        MaterialTheme.colors.onBackground
                    } else {
                        MaterialTheme.colors.onPrimary
                    },
                    contentDescription = "Not Read"
                )
            }
            Button(
                modifier = actionButtonModifier(buttonWidth),
                shape = RectangleShape,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = if (!starred) {
                        MaterialTheme.colors.background
                    } else {
                        MaterialTheme.colors.primarySurface
                    }
                ),
                onClick = onStar
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_star),
                    tint = if (!starred) {
                        MaterialTheme.colors.onBackground
                    } else {
                        MaterialTheme.colors.onPrimary
                    },
                    contentDescription = "Not Starred"
                )
            }
            Button(
                modifier = actionButtonModifier(buttonWidth),
                shape = RectangleShape,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = if (!laterRead) {
                        MaterialTheme.colors.background
                    } else {
                        MaterialTheme.colors.primarySurface
                    }
                ),
                onClick = onLaterRead
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_later),
                    tint = if (!laterRead) {
                        MaterialTheme.colors.onBackground
                    } else {
                        MaterialTheme.colors.onPrimary
                    },
                    contentDescription = "Starred"
                )
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
                    text = buildAnnotatedString {
                        append(
                            "${info.sourceName}${
                                if (info.time != null) " / ${
                                    Duration.between(
                                        info.time,
                                        LocalDateTime.now()
                                    ).toHours()
                                } hr ago" else ""
                            }"
                        )
                        withStyle(style = SpanStyle(color = MaterialTheme.colors.error)) {
                            append(if (!info.hasRead) " - not read" else "")
                        }
                    },
                    fontSize = 10.sp,
                    maxLines = 1,
                )
            }
            if (info.picUrl.isNotEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(info.picUrl)
                        .crossfade(true)
                        .diskCacheKey(info.picUrl)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .size(200)
                        .scale(Scale.FIT)
                        .build(),
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(120.dp),
                    contentScale = ContentScale.Crop,
                    contentDescription = info.title
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
//
//@Preview(showBackground = true, group = "Overview")
//@Composable
//fun PreviewHomeInfoFlow() {
//    val navController = rememberNavController()
//    FeediveTheme {
//        InfoFlowView(navController = navController)
//    }
//}

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