package dev.crushingdigits.littlebird.data.news.datasource.remote

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader

class FeedParser {

    fun parse(xml: String, sourceId: String): List<RawFeedItem> {
        val parser = Xml.newPullParser().apply {
            setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            setInput(StringReader(xml))
        }
        val items = mutableListOf<RawFeedItem>()
        var event = parser.eventType
        var current: MutableMap<String, String>? = null
        var inItem = false

        while (event != XmlPullParser.END_DOCUMENT) {
            val name = if (event == XmlPullParser.START_TAG || event == XmlPullParser.END_TAG) parser.name else null
            when (event) {
                XmlPullParser.START_TAG -> when (name) {
                    "item", "entry" -> { inItem = true; current = mutableMapOf() }
                    "title", "guid", "id", "pubDate", "published", "updated",
                    "description", "summary", "content", "dc:creator" ->
                        if (inItem && current != null) {
                            current[name!!] = readText(parser)
                        }
                    "link" ->
                        if (inItem && current != null) {
                            val href = parser.getAttributeValue(null, "href")
                            if (href != null) {
                                // Atom self-closing <link href="..."/> — skip body, already consumed
                                current["link"] = href
                                // self-closing tags have no body; readText would mis-advance, so skip it
                            } else {
                                current["link"] = readText(parser)
                            }
                        }
                    "author" ->
                        if (inItem && current != null) {
                            current["author"] = readAuthor(parser)
                        }
                    "media:content", "media:thumbnail", "enclosure" ->
                        if (inItem && current != null) {
                            parser.getAttributeValue(null, "url")?.let { current["image"] = it }
                        }
                    "category" ->
                        if (inItem && current != null) {
                            val c = readText(parser)
                            if (c.isNotBlank()) {
                                current["category"] = (current["category"]?.plus("|") ?: "") + c
                            }
                        }
                }
                XmlPullParser.END_TAG -> if (name == "item" || name == "entry") {
                    current?.let { items += it.toRawFeedItem(sourceId) }
                    inItem = false
                    current = null
                }
            }
            event = parser.next()
        }
        return items
    }

    /**
     * Reads the text content of the current element, handling CDATA.
     * Stops at the matching END_TAG for the element whose START_TAG was already consumed.
     * Skips any nested START_TAG/END_TAG pairs (depth-aware).
     */
    private fun readText(parser: XmlPullParser): String {
        val sb = StringBuilder()
        var depth = 1
        while (depth > 0) {
            when (parser.next()) {
                XmlPullParser.TEXT, XmlPullParser.CDSECT -> if (depth == 1) sb.append(parser.text)
                XmlPullParser.START_TAG -> depth++
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.END_DOCUMENT -> break
            }
        }
        return sb.toString().trim()
    }

    /**
     * Reads an <author> element which in Atom is:
     *   <author><name>Some Name</name></author>
     * and in RSS dc:creator is a flat text element (handled separately).
     *
     * This method consumes everything up to and including </author>, extracting
     * the text of the nested <name> element if present, otherwise raw text.
     */
    private fun readAuthor(parser: XmlPullParser): String {
        var result = ""
        var depth = 1
        while (depth > 0) {
            when (val ev = parser.next()) {
                XmlPullParser.START_TAG -> {
                    depth++
                    if (parser.name == "name") {
                        // Read the text of <name> directly (it is a flat element)
                        val nameSb = StringBuilder()
                        loop@ while (true) {
                            when (parser.next()) {
                                XmlPullParser.TEXT, XmlPullParser.CDSECT -> nameSb.append(parser.text)
                                XmlPullParser.END_TAG -> break@loop
                                XmlPullParser.END_DOCUMENT -> break@loop
                            }
                        }
                        // We consumed the END_TAG for <name>, so depth decrements
                        depth--
                        result = nameSb.toString().trim()
                    }
                }
                XmlPullParser.TEXT, XmlPullParser.CDSECT -> {
                    // Flat <author>text</author> (RSS style, though dc:creator handles most RSS)
                    if (result.isEmpty()) result = (result + parser.text).trim()
                }
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.END_DOCUMENT -> break
            }
        }
        return result
    }

    private fun Map<String, String>.toRawFeedItem(sourceId: String) = RawFeedItem(
        sourceId = sourceId,
        title = (this["title"] ?: "").trim(),
        link = (this["link"] ?: this["id"] ?: "").trim(),
        description = (this["description"] ?: this["summary"] ?: this["content"] ?: "").trim(),
        pubDate = this["pubDate"] ?: this["published"] ?: this["updated"],
        author = this["dc:creator"] ?: this["author"],
        categories = this["category"]?.split("|")?.filter { it.isNotBlank() } ?: emptyList(),
        imageUrl = this["image"],
        guid = this["guid"] ?: this["id"],
    )
}
