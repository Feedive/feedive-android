package cn.svecri.feedive.data

import android.util.Log
import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import cn.svecri.feedive.model.RssArticle
import cn.svecri.feedive.utils.HttpWrapper
import cn.svecri.feedive.utils.RssParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext

@OptIn(ExperimentalPagingApi::class)
class ArticleRemoteMediator(
    private val database: AppDatabase,
    private val httpClient: HttpWrapper,
    private val fetchCondition: FetchCondition,
) : RemoteMediator<Int, Article>() {
    enum class RefreshType {
        REMOTE,
        NO_REMOTE,
    }

    class FetchCondition(
        var groupName: String,
        var sourcePriority: Int,
        var refreshType: ArticleRemoteMediator.RefreshType = RefreshType.REMOTE,)

    private val articleDao: ArticleDao = database.articleDao()

    private fun feeds(): List<Feed> {
        return arrayListOf(
            Feed(
                0, "笔吧评测室", "rss",
                "http://feedive.app.cloudendpoint.cn/rss/wechat?id=611ce7048fae751e2363fc8b",
                1,
            ),
            Feed(1, "Imobile", "rss",  "http://news.imobile.com.cn/rss/news.xml", 2),
            Feed(1, "Sample",  "rss", "https://www.rssboard.org/files/sample-rss-2.xml", 4),
        )
    }

    private fun fetchAllFeeds() = run {
        feeds().asFlow()
            .onEach {
                Log.d("InfoFlow", "${it.feedName} Flow Start: ${Thread.currentThread().name}")
            }
            .flatMapMerge { feed ->
                fetchFeedArticleInfo(feed)
            }
    }

    private fun fetchFeedArticleInfo(feed: Feed) =
        run {
            fetchFeed(feed)
                .map { response ->
                    Log.d(
                        "ArticleRemoteMediator",
                        "Process Response of ${feed.feedName}: ${Thread.currentThread().name}"
                    )
                    response.body?.byteStream()?.use { inputStream ->
                        RssParser().parse(inputStream)
                    }
                }
                .filterNotNull()
                .flatMapConcat { channel ->
                    channel.rssArticles.asFlow()
                }
                .filter { article ->
                    article.title.isNotEmpty()
                }
                .asStorage(feed.feedId, feed.feedName)
                .filter { article ->
                    article.storageKey.isNotEmpty()
                }
        }

    private fun fetchFeed(feed: Feed) =
        run {
            httpClient.fetchAsFlow(feed.feedUrl)
                .onCompletion { Log.d("InfoFlow", "${feed.feedName} fetch Completed") }
        }

    private fun Flow<RssArticle>.asStorage(feedId: Int, sourceName: String) =
        map { article ->
            Article.fromRss(article, feedId, sourceName)
        }

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, Article>
    ): MediatorResult {
        return when (loadType) {
            LoadType.PREPEND ->
                MediatorResult.Success(endOfPaginationReached = true)
            LoadType.APPEND ->
                MediatorResult.Success(endOfPaginationReached = true)
            LoadType.REFRESH -> {
                if (fetchCondition.refreshType == RefreshType.REMOTE) {
                    withContext(Dispatchers.IO) {
                        fetchAllFeeds().collect {
                            articleDao.insertAll(listOf(it))
                        }
                    }
                }
                MediatorResult.Success(endOfPaginationReached = true)
            }
        }
    }
}