package com.suvojeet.notenext.data

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode

object MarkdownExporter {

    fun convertHtmlToMarkdown(html: String): String {
        val doc = Jsoup.parseBodyFragment(html)
        val sb = StringBuilder()
        
        traverse(doc.body(), sb)
        
        return sb.toString().trim()
    }

    private fun traverse(element: Element, sb: StringBuilder) {
        for (node in element.childNodes()) {
            if (node is TextNode) {
                sb.append(node.text())
            } else if (node is Element) {
                when (node.tagName()) {
                    "b", "strong" -> {
                        sb.append("**")
                        traverse(node, sb)
                        sb.append("**")
                    }
                    "i", "em" -> {
                        sb.append("*")
                        traverse(node, sb)
                        sb.append("*")
                    }
                    "u" -> {
                        traverse(node, sb)
                    }
                    "br" -> sb.append("  \n")
                    "p", "div" -> {
                        traverse(node, sb)
                        sb.append("\n\n")
                    }
                    "h1" -> { sb.append("# "); traverse(node, sb); sb.append("\n\n") }
                    "h2" -> { sb.append("## "); traverse(node, sb); sb.append("\n\n") }
                    "h3" -> { sb.append("### "); traverse(node, sb); sb.append("\n\n") }
                    "h4" -> { sb.append("#### "); traverse(node, sb); sb.append("\n\n") }
                    "h5" -> { sb.append("##### "); traverse(node, sb); sb.append("\n\n") }
                    "h6" -> { sb.append("###### "); traverse(node, sb); sb.append("\n\n") }
                    "ul" -> {
                        traverse(node, sb)
                        sb.append("\n")
                    }
                    "li" -> {
                        sb.append("- ")
                        traverse(node, sb)
                        sb.append("\n")
                    }
                    "a" -> {
                        val href = node.attr("href").trim()
                        if (href.isEmpty()) {
                            traverse(node, sb)
                        } else {
                            sb.append("[")
                            traverse(node, sb)
                            sb.append("](")
                            sb.append(href)
                            sb.append(")")
                        }
                    }
                    else -> traverse(node, sb)
                }
            }
        }
    }
}
