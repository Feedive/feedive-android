package cn.svecri.feedive.ui.main

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.os.Parcelable
import android.util.Log
import android.webkit.WebSettings
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.*
import androidx.lifecycle.viewmodel.compose.viewModel
import cn.svecri.feedive.data.AppDatabase
import cn.svecri.feedive.data.Article
import cn.svecri.feedive.model.RssArticle
import cn.svecri.feedive.model.ArticleCategory
import cn.svecri.feedive.model.ArticleGuid
import cn.svecri.feedive.model.ArticleSource
import cn.svecri.feedive.ui.theme.FeediveTheme
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

@Parcelize
data class ReaderSettings(
    var textSize:Int,
    val lineIndent: Int,
    val theme:String?,
): Parcelable

class ArticleViewModel(application: Application) : AndroidViewModel(application) {
    private val appDatabase = AppDatabase.getInstance(application)
    private val articleDao = appDatabase.articleDao()
    var rssArticle by mutableStateOf<RssArticle?>(null)
        private set
    var loading by mutableStateOf<Boolean>(true)
        private set
    @JvmName("setRssArticle1")
    fun setRssArticle(article:RssArticle){
        rssArticle = article
    }
    @JvmName("setLoading1")
    fun setLoading(flag:Boolean){
        loading = flag
    }
    fun queryArticleById(id: Int){
        viewModelScope.launch {
            val article: Article = articleDao.queryArticleById(id).get(0)

            setRssArticle(RssArticle(
                title = article.title,
                link = article.link,
                description = article.description,
                author = article.author,
                category = ArticleCategory(
                    domain = article.categoryDomain,
                    category = article.category
                ),
                comment = "",
                pubDate = article.pubDate,
                guid = ArticleGuid(isPermaLink = article.isPermaLink, value = article.guid),
                source = ArticleSource(url = "", name = article.sourceName)
            ))
            setLoading(false)
        }
    }
    //todo: webView Settings
//    private
}

@SuppressLint("SetJavaScriptEnabled", "CommitPrefEdits")
@Composable
fun ArticleView(
    articleId:Int,
    vm:ArticleViewModel = viewModel()
){
    val loading = vm.loading
    val rssArticle = vm.rssArticle
    vm.queryArticleById(articleId)

    val sharedPref = LocalContext.current.getSharedPreferences("WebViewSettings",Context.MODE_PRIVATE)
    val userSettings = ReaderSettings(
        textSize = sharedPref.getInt("textSize",14),
        lineIndent = sharedPref.getInt("lineIndent",16),
        theme = sharedPref.getString("theme","yellow"),
    )
    Box(
        Modifier.padding()
    ){
        if(loading){
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
            ) {
                CircularProgressIndicator()
            }
        }
        else{
            rssArticle?.let {
                Log.d("ArticleView",it.description)
                CustomWebView(
                    modifier = Modifier.fillMaxSize(),
                    //            url = link,
                    articleTitle = it.title,
                    htmlStr = generateHtmlStr(it,userSettings),
                    initSettings = { settings ->
                        settings?.apply {
                            //支持js交互
                            javaScriptEnabled = true
                            //将图片调整到适合webView的大小
                            //useWideViewPort = true
                            //缩放至屏幕的大小
                            loadWithOverviewMode = true
                            //缩放操作
                            setSupportZoom(false)
                            builtInZoomControls = true
                            displayZoomControls = true
                            //是否支持通过JS打开新窗口
                            javaScriptCanOpenWindowsAutomatically = true
                            //加载缓存内容
                            cacheMode = WebSettings.LOAD_DEFAULT
                            allowFileAccess = true
                            allowContentAccess = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            textZoom = 100
                        }
                    },
                    onBack = { webView ->
                        if (webView?.canGoBack() == true) {
                            webView.goBack()
                        }
                    },
                    onReceivedError = {
                        Log.d("ArticleView", ">>>>>>${it?.description}")
                    }
                )
            }
        }
    }
}



@Preview(showBackground = true)
@Composable
fun ArticlePreview() {
    FeediveTheme {
        val article = RssArticle(
            title = "干货 | 论文解读：基于动态词表的对话生成研究",
            link = "http://blog.sina.com.cn/s/blog_4caedc7a0102x3o1.html",
            description = "<![CDATA[ <p>&#8203; 编者按：近年来，聊天机器人在飞速发展，很多人也对机器人的对话系统产生了很大兴趣。近期，北京航空航天大学—微软亚洲研究院联合培养博士生吴俣应邀参加了PaperWeekly优质论文线上直播分享活动，带大家回顾了近几年来聊天机器人的发展，对比了检索式和生成式聊天机器人的优缺点，并以第一作者的身份解读了北京航空航天大学和微软亚洲研究院在AAAI 2018上发表的有关基于动态词表对话生成研究的论文Neural Response Generation with Dynamic Vocabularies。&nbsp;一起来看看吧！文章转载自公众号“PaperWeekly”。</P> <p><br /></P> <h2>浅析对话系统</H2> <p><img SRC=\"https://wx3.sinaimg.cn/large/4caedc7agy1fnajgdv1y5j20zk0k076m.jpg\" /></P> <p>&#8203;</P> <p> 对话系统主要分为两类，一类是任务型，另一类是非任务型。任务型对话系统主要应用于企业客服、订票、天气查询等场景，非任务型驱动对话系统则是指以微软小冰为代表的聊天机器人形式。&nbsp;</P> <p> 之所以强调这一点，是因为今年我在ACL发表了一篇论文，有同学发邮件问我为什么参考了论文和源代码，还是无法让聊天机器人帮忙订披萨。我只能说，目前聊天机器人实在种类繁多，有的机器人只负责闲聊，有的机器人可以帮你完成某些特定任务。&nbsp;<br /> </P> <p>本次 Talk 会更侧重于介绍闲聊机器人，也就是非任务驱动型对话系统。首先我想给大家推荐一篇关于聊天机器人的综述文章 —&nbsp;A Survey on Dialogue Systems: Recent Advances and New Frontiers。</P> <p>这篇文章来自京东数据科学团队，是一篇较为全面的对话系统综述，其中引用了 121 篇相关论文，并对论文进行了归类。不仅非常适合初学者，也能让大家对聊天机器人领域有一个更为全面的认识。</P> <p><img SRC=\"https://wx3.sinaimg.cn/large/4caedc7agy1fnajgzw2ozj20zk0k0jwh.jpg\" /></P> <p> &#8203;面向任务的对话系统主要分为知识库构造、自然语言理解、状态跟踪和策略选择。针对知识库构造，假设我们的使用场景为酒店预订，那首先我们需要构建一些和酒店相关的知识，比如酒店房型、报价以及酒店位置。</P> <p> 具备了这些基础知识之后，接下来就需要展开对话，通过自然语言理解去分辨问题类型（酒店类型、房间类型等）。确认好相关类型后，我们需要借助 policy 模块，让系统切换到下一个需要向用户确认的信息。更直观地说，我们需要循循善诱引导用户将右表信息填写完整。<br /></P> <p><br /></P> <h2>聊天机器人类型</H2> <p><img SRC=\"https://wx1.sinaimg.cn/large/4caedc7agy1fnajhdqr3rj20zk0k042o.jpg\" /></P> <p>普遍来说，聊天机器人主要分为两类，我认为准确来说应该分为三类。</P> <p> 比较早期的研究基本属于第一类：<b>基于模板的聊天机器人，它会定义一些规则，对你的话语进行分析得到某些实体，然后再将这些实体和已经定义好的规则去进行组合，从而给出回复。</B>这类回复往往都是基于模板的，比如说填空。<br /> </P> <p>除了聊天机器人，这种基于模板的文本形成方式还可以应用于很多其他领域，比如自动写稿机器人。<br /></P> <p> 目前比较热门的聊天机器人应该是另外两类，一类是检索型，另一类则是生成型。<b>检索型聊天机器人，主要是指从事先定义好的索引中进行搜索。</B>这需要我们先从互联网上获取一些对话 pairs，然后基于这些数据构造一个搜索引擎，再根据文本相似度进行查找。</P> <p> <b>生成型聊天机器人目前是研究界的一个热点。</B>和检索型聊天机器人不同的是，它可以生成一种全新的回复，因此相对更为灵活。但它也有自身的缺点，就像图中的婴儿那样，它有时候会出现语法错误，或者生成一些没营养的回复。</P> <p><img SRC=\"https://wx4.sinaimg.cn/large/4caedc7agy1fnajhyv051j20zk0k0aci.jpg\" /></P> <p>&#8203;<b>检索型聊天机器人首先需要构建一些文本和回复的 pairs，然后再训练匹配模型，上线之后先做检索再做匹配。</B>相似度算法有很多种选择，现在一般都采用深度学习，如果是做系统的话，肯定需要融合很多相似度的特征。</P> <p><img SRC=\"https://wx4.sinaimg.cn/large/4caedc7agy1fnajiduj42j20zk0k00vt.jpg\" /></P> <p><br /></P> <p><b>生成模型大多都是基于 Seq2Seq 框架进行修改</B>，所谓万变不离其宗，不管大家怎么做，都会是以这个框架为基础。文本生成也是如此，在 RNN 语言模型和 Seq2Seq 出来之后，几乎所有生成模型都是基于这个框架。即使把 RNN 换成 CNN 或 Attention is All You Need，也仍然离不开这个框架。</P> <p><br /></P> <h2>检索型VS生成型</H2> <p> <b>检索型聊天机器人的最大优点在于它的回复多样且流畅，其次，这个系统对编程者的入门门槛较低。</B>即使你对深度学习和自然语言处理不甚了解，但只要你善于写代码，并且能从网上抓取一定量的数据，就可以搭建一个检索型聊天机器人。</P> <p>另外，对于研究人员来说，检索型聊天机器人比较易于评测，借助 MAP、MRR、NDCG 等传统信息检索方法即可完成。&nbsp;</P> <p><b>检索型聊天机器人的缺点在于它过于依赖数据质量</B>。如果你抓取的数据质量欠佳，那就很有可能前功尽弃。</P> <p> 就工业界来说，要评估某个检索型聊天机器人，首先我们会看其背后的排序算法，其次不能忽略的则是数据质量和规模，最后再看其回复数据是否足够有趣，以上几个因素共同决定着检索型聊天机器人的整体质量。&nbsp;</P> <p><b>生成模型的最大优势在于有一套通用 code，可以忽略语言直接开跑。</B>只要在某种语言上跑得较为顺利，就可以将其应用到所有语言上。</P> <p><b>很多人认为 safe responses 是生成式聊天机器人的一个缺点，但其实从某种角度上来说，这也是它的一个优点。</B>相比检索型聊天机器人，它生成的回复质量较为稳定。</P> <p> <b>生成模型的另一个优点是，它非常容易实现多轮对话，并且能够偏向于某种情感。</B>假设我希望生成一句高兴的话，那么用生成模型会比检索模型更容易实现。&nbsp;</P> <p> 对于早期的研究者来说，<b>生成模型的最大缺点在于它的回复较为单一</B>。其次，<b>由于缺乏自动评评测手段，研究人员难以对生成式聊天机器人进行评估</B>。一个模型的好坏，更多需要靠人进行标注。此外，对工业界而言，生成式聊天机器人的门槛会相对较高。</P> <p><br /></P> <h2>怎样提高生成的多样性</H2> <p><img SRC=\"https://wx1.sinaimg.cn/large/4caedc7agy1fnajka3mhcj20zk0k00vd.jpg\" /></P> <p><br /></P> <p><img SRC=\"https://wx2.sinaimg.cn/large/4caedc7agy1fnajkg7s52j20zk0k040p.jpg\" /></P> <p>&#8203;<b>第一种方法是将模型做得更复杂</B>，比如上图这篇论文使用了 latent variable 来解决 boring responses 这个问题。</P> <p><img SRC=\"https://wx2.sinaimg.cn/large/4caedc7agy1fnajkvmdg8j20zk0k0juz.jpg\" /></P> <p>上图中的论文，则是<b>在生成时将概率 bias 到一些特定的主题词</B>。假设某个词是主题词，我们就在生成过程中相应提高它被选中的概率。</P> <p><img SRC=\"https://wx1.sinaimg.cn/large/4caedc7agy1fnajl6h48xj20zk0k0408.jpg\" /></P> <p> &#8203;<b>第二个流派是采用重排序方法</B>，目前最简单有效的方法是先用生成模型生成大量回复，再用分类器对回复进行排序，排名越靠前效果越好。只要生成的回复数量够多，该方法就一定可行。</P> <p><img SRC=\"https://wx1.sinaimg.cn/large/4caedc7agy1fnajlr9jqnj20zk0k0gov.jpg\" /></P> <p>&#8203;</P> <p><b>第三种方法是基于增强学习的。</B>增强学习有两种不同方法，一种基于策略，另一种基于价值。</P> <p>基于策略的代表作来自李纪为，其基本思路是：假设已有生成模型 G，给定一个 input 并生成 20 个回复，利用排序公式 P(S|T) λP(T|S) 对回复进行评分作为 reward。Reward 值越大，梯度更新则相应越大。</P> <p><b>我们再来看看 GAN 的相关方法。</B>李纪为对 SeqGAN 的模型加以改进，将其用在了回复生成上。</P> <p>其基本思路是，每生成一个词的同时，用搜索的方法去搜索其最后生成的完整句子，然后用 discriminator D 对其进行评分，分值越高，意味着词的 reward 也越高。之后的思路则跟 SeqGAN 一模一样。</P> <p><br /></P> <h2>本文思路</H2> <p><img SRC=\"https://wx3.sinaimg.cn/large/4caedc7agy1fnajm7nvaaj20zk0k041f.jpg\" /></P> <p><br /></P> <p><img SRC=\"https://wx2.sinaimg.cn/large/4caedc7agy1fnajmuwyefj20zk0k0dib.jpg\" /></P> <p>&#8203;</P> <p><b>我们做这篇论文的初衷，是为了提出一种不借助繁重工具或算法的回复生成方法。</B>因为无论是复杂模型、后排序、增强学习还是 GAN，都属于一种用时间换精度的方法。&nbsp;</P> <p>我们希望在避免产生大量时间开销的同时，还能提高回复生成的质量。<b>提升效率的关键在于 Seq2Seq 的最后一层 — 投影层</B>，这个投影往往会是一个大型矩阵。</P> <p> 我们认为其实没有必要形成这个大型矩阵，因为有用词汇仅有很小一部分，而这一小部分词汇就足够生成一句非常流畅、高度相关的话。比如对“的地得”这类功能词和与 input 相关度较高的词做一个并集，就可以仅用一个小规模字典生成极为流畅的有效回复。&nbsp;</P> <p>详细来说，我们会给每一个 input 构建一个动态词典。这么做的目的是为了减少在线 decoding 时间，同时对不相关词进行剔除。</P> <p><img SRC=\"https://wx3.sinaimg.cn/large/4caedc7agy1fnajn8qf52j20zk0k0788.jpg\" /></P> <p><br /></P> <p><b>本文其实是在 Seq2Seq 的基础上加了一个动态词表，每给一个 input，我们会生成两类词。</B></P> <p> <b>第一类词的生成完全基于规则，即静态词典。</B>静态词典主要包含一些功能词，功能词主要起润滑剂的作用，它们能让一句话的语法变得通畅。静态词典是基于词性构建的，主要包含代词和助词，名词和动词不包含在内。&nbsp;</P> <p><b>第二类词是内容词，即动态词典。</B>动态词典是根据 input 猜测与其相关的词，即我们可以用哪些词对 input 进行回复。这个词典的构建不能再像静态词典那样基于词性，而是要借助分类器或词预测模型，预测哪些词和已给定的 input 相关。&nbsp;</P> <p><b>有了这两个词之后，我们就可以给每个 input 构建一个词典。</B>这个词典的规模会很小，很小的概念是指原来的词典规模是 3 万，现在能缩减到 1000-2000 这个量级。</P> <p>从矩阵乘法角度来看，如果能把一个矩阵从 N 乘以三万的维度，缩减到 N 乘以一千的维度，就可以大大提升矩阵乘法的运算速度。</P> <p><br /></P> <h2>词预测模型</H2> <p><br /></P> <p><img SRC=\"https://wx1.sinaimg.cn/large/4caedc7agy1fnajntwdxaj20zk0k0tb5.jpg\" /></P> <p><br /></P> <p>接下来我们来看如何做词预测，即如何对内容词（content words）进行预测。内容词的 input vector 是 encoder 生成的 vector。有了 vector 后，我们需要过一个回归模型（MLP），继而预测需要用到哪些词。这个预测的损失就是将最后出现在回复里的词作为正例（标签为 1），再随机采样一些负例作为 0 标签的词，从而进行预测。&nbsp;</P> <p>如何采样负例非常关键。剔除掉一句话中的功能词，大概会剩下 10-15 个正例的词。我们需要通过频率对负例进行采样，如果一个词经常出现，那么它被采样为负例的几率就会很大。&nbsp;</P> <p>通过对负例进行采样，我们在进行词预测时，可以更准确地预测出内容词是什么。反之，这个词预测模型会跟 Seq2Seq 生成回复模型出现同样的问题，即总是出现高频词。只有利用频率对负例进行采样，才能让高频词在构建词典时就被剔除。</P> <p><br /></P> <h2>时间复杂度</H2> <p>&#8203;</P> <p><img SRC=\"https://wx4.sinaimg.cn/large/4caedc7agy1fnajod30ikj20zk0k076x.jpg\" /></P> <p>&#8203;</P> <p>在介绍完词预测方法后，我们来看时间复杂度的计算，即以什么样的速度进行 decoding。</P> <p>首先将 Seq2Seq 和本文的 decoding 方法进行对比，可以发现二者在 GRU 和 Attention 上花费的时间完全一致，但是<b>本文方法在 Projection 上花的时间会少很多</B>。</P> <p>原因在于 Seq2Seq 的 vocabulary size 通常都很大，比如 3 万这种级别乘出来的数。而本文这个 T 可能只有几千，并且<b>我们无需为每个词建立一个表，而是为每一句话建立一个表</B>。因此，我们构建词典的时间开销要远小于从全局字典里进行词预测。</P> <p><b>当然，这种情况的前提是回复词数需大于 1</B>。当回复词数等于 1 时，即逐词预测，本文方法反而会比 Seq2Seq 变得更慢。也就是说，在词的数量越多的时候，词典规模越小，节省的时间也就越多。</P> <p>经实验证明，<b>这种方法相比 Seq2Seq 能节省约 40% 的时间</B>。</P> <p><br /></P> <h2>模型训练&#8203;</H2> <p><img SRC=\"https://wx1.sinaimg.cn/large/4caedc7agy1fnajoxx06ij20zk0k0mzi.jpg\" /></P> <p>&#8203;</P> <p><img SRC=\"https://wx3.sinaimg.cn/large/4caedc7agy1fnajp5wvifj20zk0k0770.jpg\" /></P> <p>&#8203;</P> <p><img SRC=\"https://wx1.sinaimg.cn/large/4caedc7agy1fnajpd3u5mj20zk0k0tbc.jpg\" /></P> <p>&#8203;</P> <p>如果只对动态词典进行训练，将导致训练和预测的时候出现一些 gap。即使在训练的时候就采用动态词表这种方法，也依然会面临一个问题，就是你不知道选词这个过程会对你做回复造成什么影响。</P> <p> 为了解决这个问题，<b>我们在训练时选择将动态词典作为一个隐变量来处理</B>。针对公式的详细推导，大家可以参考论文。&nbsp;</P> <p>由于是隐变量，假设动态词典 T 是完全变例，即一个词有选或者不选这两种可能。如果有 3 万个词，那么 T 就有 2 的三万次方这么大，因此这种方法是不可行的。那我们应该怎么做呢？</P> <p><img SRC=\"https://wx1.sinaimg.cn/large/4caedc7agy1fnal3fsgm8j20is02haac.jpg\" /></P> <p> &#8203;这样一来，我们就可以把词典构建和回复生成这两个损失串在一起，相当于放入一同一个公式里表示，而不是将词典和生成分开单独训练。利用逐次采样得出的结果，来评估动态词典在不同情况下，其相对应的回复生成的损失是什么。&nbsp;</P> <p>由于这个损失是通过采样得出，因此它会和 RL 一样存在 variance。因此我们加了一个 baseline BK 用于梯度更新，从而使训练更为稳定。</P> <p><br /></P> <h2>实验</H2> <p><img SRC=\"https://wx4.sinaimg.cn/large/4caedc7agy1fnajq7khz3j20zk0k0abo.jpg\" /></P> <p>&#8203; 本文实验所用数据来自我们之前的一篇文章，这些词可以覆盖约 99% 的词。</P> <p>&#8203;</P> <p><img SRC=\"https://wx2.sinaimg.cn/large/4caedc7agy1fnajqjb97qj20zk0k0gqp.jpg\" ALT=\"本文使用的开源 baseline\" /></P> <p>&#8203;</P> <p><img SRC=\"https://wx1.sinaimg.cn/large/4caedc7agy1fnajqori3ej20zk0k0wi6.jpg\" /></P> <p>&#8203;</P> <p><b>目前研究界仍未找到一个很好的自动指标，能用于回复生成或对话评测。</B></P> <p>现有的方法可分为四类：</P> <p><b>第一类方法是计算 BLEU 值</B>，也就是直接计算 word overlap、ground truth 和你生成的回复。由于一句话可能存在多种回复，因此从某些方面来看，BLEU 可能不太适用于对话评测。</P> <p><b>第二类方法是计算 embedding 的距离</B>，这类方法分三种情况：直接相加求平均、先取绝对值再求平均和贪婪匹配。</P> <p><b>第三类方法是衡量多样性</B>，主要取决于 distinct-ngram 的数量和 entropy 值的大小。</P> <p><b>最后一种方法是图灵测试</B>，用 retrieval 的 discriminator 来评价回复生成。</P> <p><img SRC=\"https://wx1.sinaimg.cn/large/4caedc7agy1fnajr9lydvj20zk0k0n0y.jpg\" /></P> <p>&#8203;</P> <p>表 1 中的前四行是 baseline，DVS2S 是将词预测和 Seq2Seq 的损失串在一起计算，S-DVS2S 则是对这两个 loss 分别进行计算。从结果可以看出，DVS2S 的效果均优于其他方法。&nbsp;</P> <p>表 2 是人工标注结果，数值 0 和 2 分别代表最差效果和最优效果，Kappa 则指三者的一致性。人工标注得到的 Kappa 值通常较低，也就是说，即使让真人来进行评测，也很难得到一致性的评价。</P> <p><img SRC=\"https://wx3.sinaimg.cn/large/4caedc7agy1fnajrjha12j20zk0k0gp3.jpg\" ALT=\"速度对比：本文模型可节省40%的时间\" /></P> <p>&#8203;</P> <p><img SRC=\"https://wx2.sinaimg.cn/large/4caedc7agy1fnajtt7gfqj20zk0k0dj3.jpg\" ALT=\"案例效果对比\" /></P> <p>&#8203;</P> <h2>总结</H2> <p> 首先，我们将静态词典换成了动态词典，用于聊天机器人中的回复生成。其次，我们提出了一种全新的方法，将词预测损失和回复生成的损失融入到同一个损失函数，以 joint 的方式对这个函数进行优化。最后，我们基于一个大规模数据集，对本文模型的效率和效果进行了验证。<br /></P> <p><br /></P> <p>&#8203;</P><br /><img src=\"http://simg.sinajs.cn/blog7style/images/special/1265.gif\">&nbsp; ]]>",
            author = "微软亚洲研究院",
            category = ArticleCategory(),
            comment = "",
            pubDate = "Tue, 09 Jan 2018 21:08:04 +0800",
            guid = ArticleGuid(),
            source = ArticleSource("MSRA")
        )
        ArticleView(articleId = article.hashCode())
    }
}

@Preview
@Composable
fun ProgressCircularLoopDemo() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
    ) {
        CircularProgressIndicator()
    }
}

//TODO：生成网页的样式修改
fun generateHtmlStr(rssArticle: RssArticle,userSettings: ReaderSettings): String {
    val textSize = userSettings.textSize.toString()
    val lineIndent = userSettings.lineIndent.toString()
    val theme = userSettings.theme
    var bgColor = ""
    var textColor = ""
    when(theme){
        "white"->{
            bgColor = "#FFFFFF"
            textColor = "#000000"
        }
        "yellow"->{
            bgColor = "#e9dfc7"
            textColor = "#000000"
        }
        "green"->{
            bgColor = "#ADD678"
            textColor = "#000000"
        }
        "black"->{
            bgColor = "#696969"
            textColor = "#FFFFFF"
        }
        "blue"->{
            bgColor = "#336699"
            textColor = "#FFFFFF"
        }
    }

   val styleStr = "<style>\n" +
           "*[hidefocus],input,textarea,a{outline:0}body,div,dl,dt,dd,ul,ol,li,h1,h2,h3,h4,h5,h6,pre,form,fieldset,input,textarea,p,blockquote,th,td{padding:0;margin:0}\n" +
           "fieldset,img,html,body,iframe{border:0}table{border-collapse:collapse;border-spacing:0}li{list-style:none}\n" +
           "h1,h2,h3,h4,h5,h6{font-size:100%}caption,th{font-weight:normal;font-style:normal;text-align:left}em,strong{font-weight:bold;font-style:normal}\n" +
           "body,textarea,select,input,pre{font-family:arial,microsoft yahei,helvetica,sans-serif;font-size:14px;color:#555}\n" +
           "body{line-height:1.5em;-webkit-text-size-adjust:none}a,button{cursor:pointer}textarea{resize:none;overflow:auto}\n" +
           "pre{white-space:pre-wrap}a{color:#333;text-decoration:none}input{-webkit-tap-highlight-color:rgba(255,255,255,0);-webkit-user-modify:read-white-plaintext-only}\n" +
           "button{-webkit-tap-highlight-color:rgba(255,255,255,0)}html{width:100%;height:100%;overflow-x:hidden}" +
           "body {text-align: left;width: 100%;overflow: hidden;\n" +"background: "+ bgColor +";\n}\n" +
           "      p {\n" + "        text-indent: 2em;\n" + "        margin: 0.5em 0;\n" + "        letter-spacing: 0px;\n" + "        line-height: "+(textSize.toInt()+lineIndent.toInt()).toString() +"px;\n" + "        font-size: "+ textSize +"px;\n" + "        background-color: "+ bgColor +";"+ "        color: " + textColor +";" + "      }\n" +
           "      img {max-width: 100%;height: auto;}\n" +
           "      #main {\n" +"font-size: "+ textSize +"px;\n"+ "        color: "+ textColor +";\n" + "        line-height: "+(textSize.toInt()+lineIndent.toInt()).toString() +"px;\n" + "        padding: 15px;\n}" +
           "      .nav-pannel-bk {\n" + "        position: fixed;\n" + "        bottom: 0px;\n" + "        height: 150px;\n" + "        width: 100%;\n" + "        background: #000;\n" + "        opacity: 0.9;\n" + "        z-index: 10000;}\n" +
           "      .nav-pannel {\n" + "        position: fixed;\n" + "        bottom: 0px;\n" + "        height: 150px;\n" + "        widows: 100%;\n" + "        background: none;\n" + "        color: #fff;\n" + "        flex-direction: column;\n" + "        z-index: 10001;\n}" +
           "      .child-mod {\n" + "        padding: 5px 10px;\n" + "        margin: 1px 15px;\n}" +
           "      .bk-container {\n" + "        position: relative;\n" + "        width: 30px;\n" + "        height: 30px;\n" + "        border-radius: 15px;\n" + "        background: #ffffff;\n" + "        display: inline-block;\n" + "        vertical-align: -14px;\n" + "        margin-left: 10px;}\n" +
           "      .bk-container-current {\n" + "        position: absolute;\n" + "        width: 32px;\n" + "        height: 32px;\n" + "        border-radius: 16px;\n" + "        border: 1px #ff7800 solid;\n" + "        display: inline-block;\n" + "        top: -2px;\n" + "        left: -2px;\n}" +
           "      .child-mod span {\n" + "        display: inline-block;\n" + "        padding-right: 20px;\n" + "        padding-left: 10px;\n}" +
           "      .child-mod button {\n" + "        border-radius: 16px;\n" + "        background: none;\n" + "        border: 1px #c8c8c8 solid;\n" + "        padding: 5px 40px;\n" + "        color: #fff;\n}" +
           "      .child-mod button:nth-child(2) {\n" + "        margin-right: 10px;\n}" +
           "      .child-mod div:nth-child(3) {\n" + "        background: #e9dfc7;\n}" +
           "      .child-mod div:nth-child(4) {\n" + "        background: #add678;\n}" +
           "      .child-mod div:nth-child(5) {\n" + "        background: #696969;\n}" +
           "      .child-mod div:nth-child(6) {\n" + "        background: #336699;\n}" +
           "      .icon-text {\n" + "        position: absolute;\n" + "        top: 25px;\n" + "        font-size: 10px;\n}" +
           "    </style>"
        val settingJS = "<script>\n" +
            "  var main = document.getElementById(\"main\");\n" +
            "  var p = document.getElementsByTagName(\"p\");\n" +
            "  var body = document.getElementById(\"body\");\n" +
            "  var config_panel = document.getElementById(\"font-container\");\n" +
            "  var config_panel_bk = document.getElementById(\"nav-pannel-bk\");\n" +
            "  var text_size = "+ textSize + ";\n" +
            "  var line_indent = "+ lineIndent +";\n" +
            "  var theme = \""+ theme +"\";\n" +
            "  var large_font = document.getElementById(\"large-font\");\n" +
            "  var small_font = document.getElementById(\"small-font\");\n" +
            "  var large_indent = document.getElementById(\"large-indent\");\n" +
            "  var small_indent = document.getElementById(\"small-indent\");\n" +
            "  large_font.onclick = text_size_inc;\n" +
            "  small_font.onclick = text_size_dec;\n" +
            "  large_indent.onclick = line_indent_inc;\n" +
            "  small_indent.onclick = line_indent_dec;\n" +
            "  function updateText() {\n" +
            "    main.style.fontSize = text_size + \"px\";\n" +
            "    main.style.lineHeight = text_size + line_indent + \"px\";\n" +
            "    for (var item = 0; item < p.length; item++) {\n" + "p[item].style.fontSize = text_size + \"px\";\n" + "      p[item].style.lineHeight = text_size + line_indent + \"px\";\n" + "    }\n" + "    store();\n" + "  }\n" +
            "  function text_size_inc() {\n" + "    if (text_size < 30) text_size++;\n" + "    updateText();\n" + "  }\n" +
            "  function text_size_dec() {\n" + "    if (text_size > 8) text_size--;\n" + "    updateText();\n" + "  }\n" +
            "  function line_indent_inc() {\n" + "    if (line_indent < 30) line_indent++;\n" + "    updateText();\n" + "  }\n" +
            "  function line_indent_dec() {\n" + "    if (line_indent > 2) line_indent--;\n" + "    updateText();\n" + "  }\n" +
            "  function show() {\n" + "    config_panel.style.display = \"flex\";\n" + "    config_panel_bk.style.display = \"flex\";\n" + "store();\n"+ "  }\n" +
            "  function dismiss() {\n" + "    config_panel.style.display = \"none\";\n" + "    config_panel_bk.style.display = \"none\";\n" + "  }\n" +
            "  var bk_container_white = document.getElementById(\"bk_container_white\");\n" +
            "  var bk_container_yellow = document.getElementById(\"bk_container_yellow\");\n" +
            "  var bk_container_green = document.getElementById(\"bk_container_green\");\n" +
            "  var bk_container_black = document.getElementById(\"bk_container_black\");\n" +
            "  var bk_container_blue = document.getElementById(\"bk_container_blue\");\n" +
            "  bk_container_white.onclick = bgcolor_white;\n" +
            "  bk_container_yellow.onclick = bgcolor_yellow;\n" +
            "  bk_container_green.onclick = bgcolor_green;\n" +
            "  bk_container_black.onclick = bgcolor_black;\n" +
            "  bk_container_blue.onclick = bgcolor_blue;\n" +
            "  function bgcolor_white() {\n" + "    theme = \"white\";\n" + "    background_color(\"#FFFFFF\");\n" + "    text_color(\"#000000\");\n" + "    store();\n" + "  }\n" +
            "  function bgcolor_yellow() {\n" + "    theme = \"yellow\";\n" + "    background_color(\"#e9dfc7\");\n" + "    text_color(\"#000000\");\n" + "    store();\n" + "  }\n" +
            "  function bgcolor_green() {\n" + "    theme = \"green\";\n" + "    background_color(\"#ADD678\");\n" + "    text_color(\"#000000\");\n" + "    store();\n" + "  }\n" +
            "  function bgcolor_black() {\n" + "    theme = \"black\";\n" + "    background_color(\"#696969\");\n" + "    text_color(\"#FFFFFF\");\n" + "    store();\n" + "  }\n" +
            "  function bgcolor_blue() {\n" + "    theme = \"blue\";\n" + "    background_color(\"#336699\");\n" + "    text_color(\"#FFFFFF\");\n" + "    store();\n" + "  }\n" +
            "  function background_color(color) {\n" + "    body.style.background = color;\n" + "    for (var item = 0; item < p.length; item++) {\n" + "      p[item].style.backgroundColor = color;\n" + "    }\n" + "  }\n" +
            "  function text_color(color) {\n" + "    main.style.color = color;\n" + "    for (var item = 0; item < p.length; item++) {\n" + "      p[item].style.cssText += \"color:\" + color + \";\";\n" + "    }\n" + "  }\n" +
            "  function store() {\n" + "android.spstore(text_size,line_indent,theme);\n" + "  }\n" +
            "  function initial() {\n" + "    switch (theme) {\n" + "      case \"white\":\n" + "        bgcolor_white();\n" + "        break;\n" + "      case \"yellow\":\n" + "        bgcolor_yellow();\n" + "        break;\n" + "      case \"green\":\n" + "        bgcolor_green();\n" + "        break;\n" + "      case \"black\":\n" + "        bgcolor_black();\n" + "        break;\n" + "      case \"blue\":\n" + "        bgcolor_blue();\n" + "        break;\n" + "      default:\n" + "        bgcolor_yellow();\n" + "    }\n" + "    updateText();\n" + "  }\n" +
            "  window.onload = function () {\n" + "    initial();\n" + "  };\n" +
            "</script>\n"
        val htmlStr = "<html> \n" +
            "<head> \n" +
            "<title>${rssArticle.title}</title>" +
            styleStr +
            "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1,user-scalable=no,minimal-ui\">\n" +
            "<meta name=\"format-detection\" content=\"telephone=no\">" +
            "</head> \n" +
            "<body id=\"body\">" +
            "<div id=\"main\">${rssArticle.description}</div>" +
            "<div id=\"nav-pannel-bk\" class=\"nav-pannel-bk\" style=\"display: none\"></div>\n" +
            "    <div id=\"font-container\" class=\"nav-pannel\" style=\"display: none\">\n" +
            "      <div class=\"child-mod\">\n" +
            "        <span>字号</span>\n" +
            "        <button id=\"large-font\" class=\"size-button\">大</button>\n" +
            "        <button id=\"small-font\" class=\"size-button\">小</button>\n" +
            "      </div>\n" +
            "      <div class=\"child-mod\">\n" +
            "        <span>行距</span>\n" +
            "        <button id=\"large-indent\" class=\"size-button\">大</button>\n" +
            "        <button id=\"small-indent\" class=\"size-button\">小</button>\n" +
            "      </div>\n" +
            "      <div class=\"child-mod\">\n" +
            "        <span>背景</span>\n" +
            "        <div class=\"bk-container\" id=\"bk_container_white\">\n" + "          <div class=\"bk-container-current\"></div>\n" + "        </div>\n" +
            "        <div class=\"bk-container\" id=\"bk_container_yellow\">\n" + "          <div class=\"bk-container-current\"></div>\n" + "        </div>\n" +
            "        <div class=\"bk-container\" id=\"bk_container_green\">\n" + "          <div class=\"bk-container-current\"></div>\n" + "        </div>\n" +
            "        <div class=\"bk-container\" id=\"bk_container_black\">\n" + "          <div class=\"bk-container-current\"></div>\n" + "        </div>\n" +
            "        <div class=\"bk-container\" id=\"bk_container_blue\">\n" + "          <div class=\"bk-container-current\"></div>\n" + "        </div>\n" +
            "      </div>\n" +
            "    </div>" +
            "</body>" +
            "</html>" + settingJS

    return htmlStr
}