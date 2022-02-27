package cn.svecri.feedive.data

import android.util.Log
import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import cn.svecri.feedive.model.RssArticle
import cn.svecri.feedive.model.Subscription
import cn.svecri.feedive.utils.HttpWrapper
import cn.svecri.feedive.utils.RssParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext

@OptIn(ExperimentalPagingApi::class)
class ArticleRemoteMediator(
    private val database: AppDatabase,
    private val httpClient: HttpWrapper,
    private val groupName: String,
    private val sourcePriority: Int,
) : RemoteMediator<Int, Article>() {
    private val articleDao: ArticleDao = database.articleDao()

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

    private fun fetchAllSubscriptions() = run {
        subscriptions().asFlow()
            .onEach {
                Log.d("InfoFlow", "${it.name} Flow Start: ${Thread.currentThread().name}")
            }
            .flatMapMerge { subscription ->
                fetchSubscriptionArticleInfo(subscription)
            }
    }

    private fun fetchSubscriptionArticleInfo(subscription: Subscription) =
        run {
            fetchSubscription(subscription)
                .map { response ->
                    Log.d(
                        "ArticleRemoteMediator",
                        "Process Response of ${subscription.name}: ${Thread.currentThread().name}"
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
                .asStorage(subscription.name)
                .filter { article ->
                    article.storageKey.isNotEmpty()
                }
        }

    private fun fetchSubscription(subscription: Subscription) =
        run {
            httpClient.fetchAsFlow(subscription.url)
                .onCompletion { Log.d("InfoFlow", "${subscription.name} fetch Completed") }
        }

    private fun Flow<RssArticle>.asStorage(sourceName: String) =
        map { article ->
            Article.fromRss(article, sourceName)
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
                withContext(Dispatchers.IO) {
                    fetchAllSubscriptions().collect {
                        articleDao.insertAll(listOf(it))
                    }
                }
                MediatorResult.Success(endOfPaginationReached = true)
            }
        }
    }
}