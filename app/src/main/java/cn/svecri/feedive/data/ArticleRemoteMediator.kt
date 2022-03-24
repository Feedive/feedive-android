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
    private val onFetchFinished: suspend (Feed, Throwable?) -> Unit = { _, _ -> },
) : RemoteMediator<Int, Article>() {
    enum class RefreshType {
        REMOTE,
        NO_REMOTE,
    }

    sealed class RemoteFetchType {
        class All(val sourcePriority: Int) : RemoteFetchType()
        class Group(val groupId: Int, val sourcePriority: Int) : RemoteFetchType()
        class Feed(val feedId: Int) : RemoteFetchType()
        object None : RemoteFetchType()
    }

    class FetchCondition(
        var remoteFetchType: RemoteFetchType,
        var refreshType: RefreshType = RefreshType.REMOTE,
    )

    private val articleDao: ArticleDao = database.articleDao()
    private val feedDao: FeedDao = database.feedDao()

    private fun feeds(): Flow<Feed> {
        return when (fetchCondition.remoteFetchType) {
            is RemoteFetchType.None -> flowOf()
            is RemoteFetchType.Feed -> {
                flow { emit(feedDao.getById((fetchCondition.remoteFetchType as RemoteFetchType.Feed).feedId)) }
            }
            is RemoteFetchType.Group -> {
                val group = fetchCondition.remoteFetchType as RemoteFetchType.Group
                flow { emit(feedDao.getByGroupId(group.groupId)) }
                    .flatMapConcat { it.asFlow() }
                    .filter { feed -> feed.feedPriority <= group.sourcePriority }
            }
            is RemoteFetchType.All -> {
                val all = fetchCondition.remoteFetchType as RemoteFetchType.All
                flow { emit(feedDao.getAllFeeds()) }
                    .flatMapConcat {
                        it.asFlow()
                    }
                    .filter { feed -> feed.feedPriority <= all.sourcePriority }
            }
        }
    }

    private fun fetchAllFeeds() = run {
        feeds()
            .onEach {
                Log.d("InfoFlow", "${it.feedName} Flow Start: ${Thread.currentThread().name}")
            }
            .flatMapMerge { feed ->
                fetchFeedArticleInfo(feed).onCompletion {
                    onFetchFinished(feed, it)
                }
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