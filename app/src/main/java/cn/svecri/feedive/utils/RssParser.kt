package cn.svecri.feedive.utils

import cn.svecri.feedive.model.Article
import cn.svecri.feedive.model.Channel
import java.io.InputStream

//import org.w3c.dom.Document
//import org.w3c.dom.NodeList
//import javax.xml.parsers.DocumentBuilder
//import javax.xml.parsers.DocumentBuilderFactory
//import javax.xml.xpath.XPath
//import javax.xml.xpath.XPathConstants
//import javax.xml.xpath.XPathFactory

class RssParser {

//    private val builderFactory: DocumentBuilderFactory = DocumentBuilderFactory.newInstance()
//    private val xpathFactory: XPathFactory = XPathFactory.newInstance()

    fun parse(inputStream: InputStream): Channel? {
//        var article: Article = Article()
//        val builder: DocumentBuilder = builderFactory.newDocumentBuilder()
//        val document: Document = builder.parse(inputStream)
//        val xpath: XPath = xpathFactory.newXPath()
//        val title: String =
//            xpath.compile("/rss/channel/title//text()").evaluate(document, XPathConstants.STRING) as String
//        val link: String =
//            xpath.compile("/rss/channel/link//text()").evaluate(document, XPathConstants.STRING) as String
//        val description: String =
//            xpath.compile("/rss/channel/description//text()").evaluate(document, XPathConstants.STRING) as String
//        (xpath.compile("/rss/channel/item").evaluate(document, XPathConstants.NODESET) as NodeList)
        return Channel("fake", "", "", arrayListOf(
            Article("fake title 1", author = "fake author 1"),
            Article("fake title 2", author = "fake author 2"),
        ))
    }

}