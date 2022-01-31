package cn.svecri.feedive.ui.main

import android.os.Build
import android.os.Bundle
import android.webkit.WebSettings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cn.svecri.feedive.ui.theme.FeediveTheme


class ArticleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FeediveTheme {
                // A surface container using the 'background' color from the theme
                Surface(color = MaterialTheme.colors.background) {
//                    ArticleView("<p>1234</p>")
                    Text("123456dr")
                }
            }
        }
    }
}

@Composable
fun ArticleView(htmlStr:String){
    var rememberWebViewProgress:Int by remember { mutableStateOf(-1) }
    Box {
        CustomWebView(
            modifier = Modifier.fillMaxSize(),
//            url = "https://www.baidu.com/",
            htmlStr = htmlStr,
            onProgressChange = { progress ->
                rememberWebViewProgress = progress
            },
            initSettings = { settings ->
                settings?.apply {
                    //支持js交互
                    javaScriptEnabled = true
                    //将图片调整到适合webView的大小
                    useWideViewPort = true
                    //缩放至屏幕的大小
                    loadWithOverviewMode = true
                    //缩放操作
                    setSupportZoom(true)
                    builtInZoomControls = true
                    displayZoomControls = true
                    //是否支持通过JS打开新窗口
                    javaScriptCanOpenWindowsAutomatically = true
                    //不加载缓存内容
                    cacheMode = WebSettings.LOAD_NO_CACHE
                }
            }, onBack = { webView ->
                if (webView?.canGoBack() == true) {
                    webView.goBack()
                } else {
                    //finish()
                }
            }, onReceivedError = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    //Log.d("AAAA", ">>>>>>${it?.description}")
                }
            }
        )
        LinearProgressIndicator(
            progress = rememberWebViewProgress * 1.0F / 100F,
            modifier = Modifier
                .fillMaxWidth()
                .height(if (rememberWebViewProgress == 100) 0.dp else 5.dp),
            color = Color.Red
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ArticlePreview() {
    val testHtml = "<html> \n" +
            "<head> \n" +
            "<style type=\"text/css\"> \n" +
            "body {font-size:13px;}\n" +
            "</style> \n" +
            "</head> \n" +
            "<body>" +
            "<h1>Hello,WebView!</h1>" +
            "<img src=\"https://logodownload.org/wp-content/uploads/2015/05/android-logo-3-2.png\" />" +
            "</body>" +
            "</html>"
    FeediveTheme {
//        Text("231")
        ArticleView(testHtml)
    }
}