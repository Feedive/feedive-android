package cn.svecri.feedive.ui.main

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Resources
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import android.webkit.*
import androidx.activity.compose.BackHandler
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.viewinterop.AndroidView
import cn.svecri.feedive.R
import kotlinx.coroutines.launch


@Composable
fun CustomWebView(
    modifier: Modifier = Modifier,
    articleTitle: String = "",
    htmlStr: String,
    onBack: (webView: WebView?) -> Unit,
    onProgressChange: (progress: Int) -> Unit = {},
    initSettings: (webSettings: WebSettings?) -> Unit = {},
    onReceivedError: (error: WebResourceError?) -> Unit = {},
){
    val webViewChromeClient = remember {
        object: WebChromeClient(){
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                //回调网页内容加载进度
                onProgressChange(newProgress)
                super.onProgressChanged(view, newProgress)
            }
        }
    }

    val webViewClient = remember{
        object: WebViewClient(){
            override fun onPageStarted(view: WebView?, url: String?,
                                       favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                onProgressChange(-1)
            }
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                onProgressChange(100)
            }
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                if(null == request?.url) return false
                val showOverrideUrl = request.url.toString()
                try {
                    if (!showOverrideUrl.startsWith("http://")
                        && !showOverrideUrl.startsWith("https://")) {
                        //处理非http和https开头的链接地址
                        Intent(Intent.ACTION_VIEW, Uri.parse(showOverrideUrl)).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            view?.context?.applicationContext?.startActivity(this)
                        }
                        return true
                    }
                }catch (e:Exception){
                    //没有安装和找到能打开(「xxxx://openlink.cc....」、「weixin://xxxxx」等)协议的应用
                    return true
                }
                return super.shouldOverrideUrlLoading(view, request)
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                //自行处理....
                onReceivedError(error)
            }

            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
//                val url:String = request?.url.toString()
//                if (url.contains("jquery")){
//                    todo:webresponse
//                }
                return super.shouldInterceptRequest(view, request)
            }
        }
    }
    var webView:WebView? by remember {
        mutableStateOf(null)
    }
    val coroutineScope = rememberCoroutineScope()
    val mimeType = "text/html"
    val enCoding = "utf-8"

    var isPanelShow by remember{ mutableStateOf(false)}
    fun setPanel() {
        if(isPanelShow){
            webView?.loadUrl("javascript:dismiss()")
        }
        else{
            webView?.loadUrl("javascript:show()")
        }
        isPanelShow = !isPanelShow
    }

    val sharedPref = LocalContext.current.getSharedPreferences("WebViewSettings", Context.MODE_PRIVATE)
    val editor: SharedPreferences.Editor =  sharedPref.edit()

    Scaffold(
        topBar = {
            TopAppBarWithSettings(
                articleTitle = articleTitle,
                onSettingsChange = {
                    setPanel()
                    Log.d("ArticleView","sharedPreference:"+sharedPref.getString("theme","")+" "+sharedPref.getInt("textSize",0).toString())
                }
            )
        }
    ) {
        AndroidView(modifier = modifier, factory = { ctx ->
            WebView(ctx).apply {
                this.webViewClient = webViewClient
                this.webChromeClient = webViewChromeClient
                //回调webSettings供调用方设置webSettings的相关配置
                initSettings(this.settings)
                webView = this
                this.addJavascriptInterface(AndroidtoJs(editor),"android")
                this.loadDataWithBaseURL(null, htmlStr, mimeType, enCoding, null)
                Log.d("ArticleView", "webView created")
            }
        })
        BackHandler {
            coroutineScope.launch {
                //控制点击了返回按键之后，关闭页面还是返回上一级网页
                onBack(webView)
            }
        }
    }
}

@Composable
fun TopAppBarWithSettings(
    articleTitle:String,
    onSettingsChange: ()->Unit = {}
) {
    val settingsDialogOpen = remember { mutableStateOf(false) }
    TopAppBar(
        title = {
            Text(text = articleTitle)
        },
        actions = {
            IconButton(onClick = {
                onSettingsChange()
            }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_email_read_24),
                    contentDescription = "Switch Read or Unread or all articles"
                )
            }
        }
    )
}

class AndroidtoJs(e:SharedPreferences.Editor): Any() {
    // 定义JS需要调用的方法
    val editor = e
    @JavascriptInterface
    fun spstore(textSize:Int,lineIndent:Int,theme:String) {
        editor.putInt("textSize",textSize)
        editor.putInt("lineIndent",lineIndent)
        editor.putString("theme",theme)
        editor.commit()
        Log.d("ArticleView","textsize:"+textSize.toString()+"lineIndent:"+lineIndent+"theme:"+theme)
    }
}

fun Resources.getStatusBarHeight():Int {
    var statusBarHeight = 0
    val resourceId = getIdentifier("status_bar_height", "dimen", "android")
    if(resourceId >0 ){
        statusBarHeight = getDimensionPixelSize(resourceId)
    }
    return statusBarHeight
}
