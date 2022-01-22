package cn.svecri.feedive.ui.main

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import cn.svecri.feedive.R
import cn.svecri.feedive.model.Subscription
import cn.svecri.feedive.ui.theme.FeediveTheme
import java.time.Duration
import java.time.LocalDateTime

data class ArticleInfo(
    val title: String,
    val picUrl: String,
    val sourceName: String,
    val time: LocalDateTime?,
    val abstract: String,
    val hasRead: Boolean,
    val protocol: String,
    val starred: Boolean,
)

data class ArticleInfoSet(
    val primary: ArticleInfo,
    val others: List<ArticleInfo>,
)

class InfoFlowViewModel : ViewModel() {
    fun subscriptions(): List<Subscription> {
        return arrayListOf(Subscription("Imobile", "http://news.imobile.com.cn/rss/news.xml", ""))
    }
}

@Composable
fun InfoFlowView() {
    Scaffold(
        topBar = { TopAppBarWithTab() }
    ) {
        InfoFlowList()
    }
}

@Composable
fun InfoFlowList() {
    val listState = rememberLazyListState()

    LazyColumn(
        state = listState
    ) {
        items(arrayOf(0..2)) { _ ->
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
                )
            )
        }
    }
}

@Composable
fun TopAppBarWithTab() {
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
            IconButton(onClick = { /*TODO*/ }) {
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