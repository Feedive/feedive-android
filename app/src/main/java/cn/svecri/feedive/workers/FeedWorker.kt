package cn.svecri.feedive.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import cn.svecri.feedive.data.AppDatabase
import cn.svecri.feedive.data.Feed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FeedWorker(context: Context, workerParams: WorkerParameters): CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val database = AppDatabase.getInstance(applicationContext)
            database.feedDao().insertFeeds(
                listOf(
                    Feed(
                        0, "笔吧评测室", "rss",
                        "http://feedive.app.cloudendpoint.cn/rss/wechat?id=611ce7048fae751e2363fc8b",
                        1,
                    ),
                    Feed(0, "Imobile", "rss", "http://news.imobile.com.cn/rss/news.xml", 2),
                    Feed(0, "SampleRss", "rss", "https://www.rssboard.org/files/sample-rss-2.xml", 4),
                )
            )
            Result.success()
        } catch (e: Exception) {
            Log.e("FeedWorker", "Insert Error on Init Database", e)
            Result.failure()
        }
    }
}