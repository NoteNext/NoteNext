package com.suvojeet.notenext.util

import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.text.style.URLSpan
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.core.text.HtmlCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object HtmlConverter {

    /** Keep link colour in sync with RichTextController.LinkColor. */
    private const val LINK_COLOR = 0xFF3B82F6.toInt()

    private val LinkSpanStyle = SpanStyle(
        color = androidx.compose.ui.graphics.Color(LINK_COLOR),
        textDecoration = TextDecoration.Underline
    )

    suspend fun annotatedStringToHtml(annotatedString: AnnotatedString): String = withContext(Dispatchers.Default) {
        val spanned = annotatedStringToSpanned(annotatedString)
        HtmlCompat.toHtml(spanned, HtmlCompat.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE)
    }

    private fun annotatedStringToSpanned(annotatedString: AnnotatedString): Spanned {
        val spannable = SpannableStringBuilder(annotatedString.text)
        annotatedString.spanStyles.forEach { range ->
            val style = range.item
            if (style.fontWeight == FontWeight.Bold && style.fontStyle == FontStyle.Italic) {
                spannable.setSpan(StyleSpan(android.graphics.Typeface.BOLD_ITALIC), range.start, range.end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            } else if (style.fontWeight == FontWeight.Bold) {
                spannable.setSpan(StyleSpan(android.graphics.Typeface.BOLD), range.start, range.end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            } else if (style.fontStyle == FontStyle.Italic) {
                spannable.setSpan(StyleSpan(android.graphics.Typeface.ITALIC), range.start, range.end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }

            if (style.textDecoration == TextDecoration.Underline) {
                spannable.setSpan(UnderlineSpan(), range.start, range.end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }

        // Persist links as real <a href> tags. Without this the URL annotation only
        // lived in memory and disappeared the moment the note was saved/reloaded.
        val emittedLinks = mutableSetOf<String>()
        listOf("URL", "EMAIL", "PHONE").forEach { tag ->
            annotatedString.getStringAnnotations(tag, 0, annotatedString.length).forEach { range ->
                val url = range.item
                if (url.isNotBlank() && emittedLinks.add("${range.start}:${range.end}:$url")) {
                    spannable.setSpan(
                        URLSpan(url),
                        range.start,
                        range.end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }
        }
        return spannable
    }

    fun cleanHtmlToPlain(html: String): String {
        if (html.isBlank()) return ""
        var s = html
        var prev = ""
        var pass = 0
        while (s != prev && pass < 3) {
            prev = s
            if (s.contains("&lt;") || s.contains("&gt;") || s.contains("&amp;") || s.contains("&#")) {
                s = HtmlCompat.fromHtml(s, HtmlCompat.FROM_HTML_MODE_LEGACY).toString()
            }
            if (s.contains("<") && s.contains(">")) {
                s = HtmlCompat.fromHtml(s, HtmlCompat.FROM_HTML_MODE_COMPACT).toString()
            }
            pass++
        }
        if (s.contains("<") && s.contains(">")) {
            s = s.replace(Regex("<[^>]*>"), "")
        }
        return s.replace(Regex("\n{3,}"), "\n\n").trim()
    }

    suspend fun htmlToPlainText(html: String): String = withContext(Dispatchers.Default) {
        cleanHtmlToPlain(html)
    }

    suspend fun htmlToAnnotatedString(html: String): AnnotatedString = withContext(Dispatchers.Default) {
        if (html.isBlank()) return@withContext AnnotatedString("")
        var rawHtml = html
        if (rawHtml.contains("&lt;") || rawHtml.contains("&gt;") || rawHtml.contains("&amp;") || rawHtml.contains("&#")) {
            rawHtml = HtmlCompat.fromHtml(rawHtml, HtmlCompat.FROM_HTML_MODE_LEGACY).toString()
        }
        val spanned = HtmlCompat.fromHtml(rawHtml, HtmlCompat.FROM_HTML_MODE_LEGACY)
        var spannedText = spanned.toString()
        if (spannedText.contains("<") && spannedText.contains(">")) {
            spannedText = cleanHtmlToPlain(spannedText)
        }
        spannedText = spannedText.replace(Regex("\n{3,}"), "\n\n").trim()

        buildAnnotatedString {
            append(spannedText)
            spanned.getSpans(0, spanned.length, Any::class.java).forEach { span ->
                val start = spanned.getSpanStart(span)
                val end = spanned.getSpanEnd(span)
                if (start in 0..spannedText.length && end in 0..spannedText.length && start < end) {
                    when (span) {
                        is URLSpan -> {
                            val url = span.url
                            if (!url.isNullOrBlank()) {
                                val tag = when {
                                    url.startsWith("mailto:", ignoreCase = true) -> "EMAIL"
                                    url.startsWith("tel:", ignoreCase = true) -> "PHONE"
                                    else -> "URL"
                                }
                                addStringAnnotation(tag, url, start, end)
                                addStyle(LinkSpanStyle, start, end)
                            }
                        }
                        is StyleSpan -> {
                            when (span.style) {
                                android.graphics.Typeface.BOLD -> addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, end)
                                android.graphics.Typeface.ITALIC -> addStyle(SpanStyle(fontStyle = FontStyle.Italic), start, end)
                                android.graphics.Typeface.BOLD_ITALIC -> addStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic), start, end)
                            }
                        }
                        is UnderlineSpan -> addStyle(SpanStyle(textDecoration = TextDecoration.Underline), start, end)
                    }
                }
            }
            
            // Detect [[Note Title]] and add styling/annotation
            val text = spannedText
            val noteLinkRegex = "\\[\\[(.*?)\\]\\]".toRegex()
            noteLinkRegex.findAll(text).forEach { matchResult ->
                val start = matchResult.range.first
                val end = matchResult.range.last + 1
                val title = matchResult.groupValues[1]
                addStyle(
                    SpanStyle(
                        color = androidx.compose.ui.graphics.Color(LINK_COLOR),
                        textDecoration = TextDecoration.Underline,
                        fontWeight = FontWeight.Medium
                    ),
                    start,
                    end
                )
                addStringAnnotation(
                    tag = "NOTE_LINK",
                    annotation = title,
                    start = start,
                    end = end
                )
            }

            // Detect URLs
            val urlRegex = "(https?://\\S+|www\\.\\S+)".toRegex()
            urlRegex.findAll(text).forEach { matchResult ->
                val start = matchResult.range.first
                val end = matchResult.range.last + 1
                val url = matchResult.value
                addStyle(
                    SpanStyle(
                        color = androidx.compose.ui.graphics.Color(LINK_COLOR), // Shared link colour
                        textDecoration = TextDecoration.Underline
                    ),
                    start,
                    end
                )
                addStringAnnotation(
                    tag = "URL",
                    annotation = url,
                    start = start,
                    end = end
                )
            }

            // Detect Emails
            val emailRegex = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+".toRegex()
            emailRegex.findAll(text).forEach { matchResult ->
                val start = matchResult.range.first
                val end = matchResult.range.last + 1
                val email = matchResult.value
                addStyle(
                    SpanStyle(
                        color = androidx.compose.ui.graphics.Color(LINK_COLOR),
                        textDecoration = TextDecoration.Underline
                    ),
                    start,
                    end
                )
                addStringAnnotation(
                    tag = "EMAIL",
                    annotation = "mailto:$email",
                    start = start,
                    end = end
                )
            }

            // Detect Phone Numbers - ONLY 10 consecutive digits (Indian phone numbers)
            // Matches: 9876543210, +919876543210 (not shorter or longer numbers)
            val phoneRegex = "(?<!\\d)(\\+91)?\\d{10}(?!\\d)".toRegex()
            phoneRegex.findAll(text).forEach { matchResult ->
                val start = matchResult.range.first
                val end = matchResult.range.last + 1
                val phone = matchResult.value
                addStyle(
                    SpanStyle(
                        color = androidx.compose.ui.graphics.Color(LINK_COLOR),
                        textDecoration = TextDecoration.Underline
                    ),
                    start,
                    end
                )
                addStringAnnotation(
                    tag = "PHONE",
                    annotation = "tel:$phone",
                    start = start,
                    end = end
                )
            }
        }
    }
}
