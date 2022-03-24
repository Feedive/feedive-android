package cn.svecri.feedive.ui.main

import android.app.Application
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import cn.svecri.feedive.R
import cn.svecri.feedive.data.AppDatabase
import cn.svecri.feedive.data.Feed
import cn.svecri.feedive.data.FeedGroup
import cn.svecri.feedive.ui.theme.Shapes
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map


data class FeedGroupWithChildren(val feedGroup: FeedGroup, val feeds: List<Feed>)


class NavigationDrawerViewModel(application: Application) : AndroidViewModel(application) {
    private val appDatabase = AppDatabase.getInstance(application)
    private val feedGroupDao = appDatabase.feedGroupDao()
    private val feedInGroupDao = appDatabase.feedInGroupDao()
    private val feedDao = appDatabase.feedDao()

    /**
     * Flow of saved feed groups as well as its children.
     */
    fun feedGroups() = run {
        feedGroupDao.getAll()
            .map {
                combine(it.map { feedGroup ->
                    feedInGroupDao.getGroupChildren(feedGroup.feedGroupId)
                        .map { feeds ->
                            FeedGroupWithChildren(feedGroup, feeds)
                        }
                }) { feedGroups ->
                    feedGroups.asList()
                }
            }.flatMapLatest { it }
    }

    /**
     * Flow of all feeds.
     */
    fun allFeeds() = run {
        feedDao.getAllFeedsFlow()
    }
}


/**
 * Drawer to Navigate to Different article info flow.
 */
@Composable
fun NavigationDrawer(
    navController: NavController,
    vm: NavigationDrawerViewModel = viewModel(),
    afterNav: () -> Unit = {}
) {
    val feedGroups by remember { vm.feedGroups() }.collectAsState(initial = listOf())
    val allFeeds by remember { vm.allFeeds() }.collectAsState(initial = listOf())
    val listState = rememberLazyListState()
    var navType by remember {
        mutableStateOf("all")
    }
    var navDetail by remember {
        mutableStateOf(0)
    }

    var currentFoldOut by remember {
        mutableStateOf(-1)
    }
    LazyColumn(state = listState, modifier = Modifier.fillMaxWidth()) {
        item {
            Text(
                text = "Hello, RSS",
                modifier = Modifier.padding(8.dp, 20.dp, 8.dp, 10.dp),
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colors.secondary,
                fontSize = 12.sp,
            )
        }
        item { Spacer(modifier = Modifier.padding(4.dp)) }
        item {
            DrawerItem(content = "收藏", active = navType == "starred", onClick = {
                navType = "starred"
                navController.navigate("info_flow/starred")
                afterNav()
            })
        }
        item {
            DrawerItem(content = "稍后阅读", active = navType == "laterRead", onClick = {
                navType = "laterRead"
                navController.navigate("info_flow/laterRead")
                afterNav()
            })
        }
        item { Spacer(modifier = Modifier.padding(4.dp)) }
        item {
            DrawerGroup(
                content = "全部",
                active = navType == "all",
                fold = currentFoldOut != 0,
                onFold = {
                    currentFoldOut = if (currentFoldOut == 0) {
                        -1
                    } else {
                        0
                    }
                },
                onClick = {
                    navType = "all"
                    navController.navigate("info_flow/all")
                    afterNav()
                })
        }
        items(allFeeds) { feed ->
            Crossfade(targetState = currentFoldOut == 0) {
                if (it) {
                    DrawerItem(
                        content = feed.feedName,
                        active = navType == "feed" && navDetail == feed.feedId,
                        offsetLeft = 32.dp,
                        onClick = {
                            navType = "feed"
                            navDetail = feed.feedId
                            navController.navigate("info_flow/feed?detail=${feed.feedId}")
                            afterNav()
                        })
                }
            }
        }
        item { Spacer(modifier = Modifier.padding(4.dp)) }

        for (feedGroup in feedGroups) {
            item {
                DrawerGroup(
                    content = feedGroup.feedGroup.feedGroupName,
                    active = navType == "group" && navDetail == feedGroup.feedGroup.feedGroupId,
                    fold = currentFoldOut != feedGroup.feedGroup.feedGroupId, onFold = {
                        currentFoldOut = if (currentFoldOut == feedGroup.feedGroup.feedGroupId) {
                            -1
                        } else {
                            feedGroup.feedGroup.feedGroupId
                        }
                    }, onClick = {
                        navType = "group"
                        navDetail = feedGroup.feedGroup.feedGroupId
                        navController.navigate("info_flow/group?detail=${feedGroup.feedGroup.feedGroupId}")
                        afterNav()
                    }
                )
            }
            items(feedGroup.feeds) { feed ->
                Crossfade(targetState = currentFoldOut == feedGroup.feedGroup.feedGroupId) {
                    if (it) {
                        DrawerItem(
                            content = feed.feedName,
                            active = navType == "feed" && navDetail == feed.feedId,
                            offsetLeft = 32.dp,
                            onClick = {
                                navType = "feed"
                                navDetail = feed.feedId
                                navController.navigate("info_flow/feed?detail=${feed.feedId}")
                                afterNav()
                            })
                    }
                }
            }
            item {
                Crossfade(targetState = currentFoldOut == feedGroup.feedGroup.feedGroupId) {
                    if (it) {
                        Spacer(modifier = Modifier.padding(4.dp))
                    }
                }
            }
        }
    }
}

/**
 * Drawer Item which has a fold indicator. Used for FeedGroup.
 */
@Composable
fun DrawerGroup(
    content: String,
    active: Boolean = false,
    fold: Boolean = true,
    offsetLeft: Dp = 0.dp,
    fontSize: TextUnit = 16.sp,
    onClick: () -> Unit = {},
    onFold: () -> Unit = {},
) {
    BaseDrawerItem(
        content = content,
        active = active,
        group = true,
        fold = fold,
        offsetLeft = offsetLeft,
        fontSize = fontSize,
        onClick = onClick,
        onFold = onFold,
    )
}

/**
 * Drawer Item used for Single Feed.
 */
@Composable
fun DrawerItem(
    content: String,
    active: Boolean = false,
    offsetLeft: Dp = 0.dp,
    fontSize: TextUnit = 16.sp,
    onClick: () -> Unit = {},
) {
    BaseDrawerItem(
        content,
        active,
        offsetLeft = offsetLeft,
        fontSize = fontSize,
        onClick = onClick,
    )
}

/**
 * Column Item used in [NavigationDrawer].
 * @param content: text displayed in the center of drawer item
 * @param active: if active, the drawer item will background highlighted
 * @param group: if group, there will be a triangle fold indicator on left of the content
 * @param fold: if group, fold imply the rotation of triangle fold indicator
 * @param offsetLeft: margin to the left
 * @param fontSize: the size of content font
 * @param onClick: lambda to invoke when click the item
 * @param onFold: lambda to invoke when toggle fold state
 */
@Composable
fun BaseDrawerItem(
    content: String,
    active: Boolean = false,
    group: Boolean = false,
    fold: Boolean = true,
    offsetLeft: Dp = 0.dp,
    fontSize: TextUnit = 16.sp,
    onClick: () -> Unit = {},
    onFold: () -> Unit = {},
) {
    val foldState = remember {
        MutableTransitionState(fold)
    }.apply {
        targetState = fold
    }
    val foldTransition = updateTransition(transitionState = foldState, label = "foldTransition")
    val foldRotation by foldTransition.animateFloat(
        label = "foldTransitionRotation",
        transitionSpec = { tween(durationMillis = 500) }
    ) {
        if (it) -90f else 0f
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(offsetLeft + 4.dp, 4.dp, 4.dp)
            .clickable { onClick() },
        shape = Shapes.medium,
        color = if (!active) MaterialTheme.colors.onPrimary else MaterialTheme.colors.primary,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (group) {
                Icon(
                    modifier = Modifier
                        .size(28.dp)
                        .rotate(foldRotation)
                        .padding(4.dp, 0.dp, 0.dp, 0.dp)
                        .clickable { onFold() },
                    painter = painterResource(id = R.drawable.ic_fold),
                    tint = if (active) MaterialTheme.colors.onPrimary else Color.Black,
                    contentDescription = "Fold Icon",
                )
            }
            Text(
                modifier = Modifier.padding(4.dp, 4.dp), text = content,
                fontSize = fontSize,
                color = if (active) MaterialTheme.colors.onPrimary else Color.Unspecified
            )
        }
    }
}