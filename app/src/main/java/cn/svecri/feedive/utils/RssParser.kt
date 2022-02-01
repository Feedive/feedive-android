package cn.svecri.feedive.utils

import android.util.Log
import cn.svecri.feedive.model.Article
import cn.svecri.feedive.model.ArticleCategory
import cn.svecri.feedive.model.ArticleGuid
import cn.svecri.feedive.model.Channel
import org.w3c.dom.Document
import org.w3c.dom.NodeList
import java.io.EOFException
import java.io.InputStream
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPath
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory

class RssParser {

    private val builderFactory: DocumentBuilderFactory = DocumentBuilderFactory.newInstance()
    private val xpathFactory: XPathFactory = XPathFactory.newInstance()

    fun parse(inputStream: InputStream): Channel? {
        val builder: DocumentBuilder = builderFactory.newDocumentBuilder()
        val document: Document = try {
            builder.parse(inputStream)
        } catch (e: EOFException) {
            Log.e("RssParser", "Parse RSS Error: EOF")
            return null;
        }
        val xpath: XPath = xpathFactory.newXPath()
        val title: String =
            xpath.compile("/rss/channel/title//text()")
                .evaluate(document, XPathConstants.STRING) as String
        val link: String =
            xpath.compile("/rss/channel/link//text()")
                .evaluate(document, XPathConstants.STRING) as String
        val description: String =
            xpath.compile("/rss/channel/description//text()")
                .evaluate(document, XPathConstants.STRING) as String
        val articles = arrayListOf<Article>()
        val articleList = xpath.compile("/rss/channel/item")
            .evaluate(document, XPathConstants.NODESET) as NodeList
        for (i in 1..articleList.length) {
            articleList.item(i)?.let { articleNode ->
                val articleTitle = xpath.compile("title//text()")
                    .evaluate(articleNode, XPathConstants.STRING) as String
                val articleLink = xpath.compile("link//text()")
                    .evaluate(articleNode, XPathConstants.STRING) as String
                val articleDescription = xpath.compile("description//text()")
                    .evaluate(articleNode, XPathConstants.STRING) as String
                val articleAuthor = xpath.compile("author//text()")
                    .evaluate(articleNode, XPathConstants.STRING) as String
                val articleCategory = xpath.compile("category//text()")
                    .evaluate(articleNode, XPathConstants.STRING) as String
                val articleCategoryDomain = xpath.compile("category//@domain")
                    .evaluate(articleNode, XPathConstants.STRING) as String
                val articlePubDate = xpath.compile("pubDate//text()")
                    .evaluate(articleNode, XPathConstants.STRING) as String
                val articleGuid = xpath.compile("guid//text()")
                    .evaluate(articleNode, XPathConstants.STRING) as String
                val articleGuidIsPermaLink = xpath.compile("guid//@isPermaLink")
                    .evaluate(articleNode, XPathConstants.BOOLEAN) as Boolean
                val articleComment = xpath.compile("comment//text()")
                    .evaluate(articleNode, XPathConstants.STRING) as String
                articles.add(
                    Article(
                        title = articleTitle,
                        link = articleLink,
                        description = articleDescription,
                        author = articleAuthor,
                        category = ArticleCategory(
                            domain = articleCategoryDomain,
                            category = articleCategory
                        ),
                        pubDate = articlePubDate,
                        guid = ArticleGuid(
                            isPermaLink = articleGuidIsPermaLink,
                            value = articleGuid,
                        ),
                        comment = articleComment,
                    )
                )
            }
        }
        return Channel(title, link, description, articles)
    }

}