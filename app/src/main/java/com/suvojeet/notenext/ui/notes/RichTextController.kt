package com.suvojeet.notenext.ui.notes

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import javax.inject.Inject

import androidx.compose.ui.graphics.Color

class RichTextController @Inject constructor() {

    companion object {
        /**
         * Single, calm link colour used everywhere (editor spans, HTML round-trip and
         * previews). The old hard-coded purple/blue made links look like a different
         * app; this blue reads well on both light and dark surfaces.
         */
        val LinkColor = Color(0xFF3B82F6)

        /** Normalises user input such as "example.com" into a real, clickable URL. */
        fun normalizeUrl(raw: String): String {
            val trimmed = raw.trim()
            if (trimmed.isEmpty()) return trimmed
            val lower = trimmed.lowercase()
            return when {
                lower.startsWith("http://") ||
                    lower.startsWith("https://") ||
                    lower.startsWith("mailto:") ||
                    lower.startsWith("tel:") -> trimmed
                Regex("^[\\w.+-]+@[\\w-]+\\.[\\w.-]+$").matches(trimmed) -> "mailto:$trimmed"
                else -> "https://$trimmed"
            }
        }
    }

    private val WikiLinkStyle = SpanStyle(color = LinkColor, textDecoration = TextDecoration.Underline)
    private val UrlLinkStyle = SpanStyle(color = LinkColor, textDecoration = TextDecoration.Underline)

    fun getHeadingStyle(level: Int): SpanStyle {
        return when (level) {
            1 -> SpanStyle(fontSize = 24.sp)
            2 -> SpanStyle(fontSize = 20.sp)
            3 -> SpanStyle(fontSize = 18.sp)
            4 -> SpanStyle(fontSize = 16.sp)
            5 -> SpanStyle(fontSize = 14.sp)
            6 -> SpanStyle(fontSize = 12.sp)
            else -> SpanStyle()
        }
    }

    fun processContentChange(
        oldContent: TextFieldValue,
        newContent: TextFieldValue,
        activeStyles: Set<SpanStyle>,
        activeHeadingStyle: Int
    ): TextFieldValue {
        // Safe limit for content length
        if (newContent.text.length > 100_000) {
            return oldContent.copy(selection = newContent.selection)
        }

        if (newContent.text == oldContent.text) {
            return oldContent.copy(selection = newContent.selection)
        }

        val oldText = oldContent.text
        val newText = newContent.text

        val prefixLength = commonPrefixWith(oldText, newText).length
        val oldRemainder = oldText.substring(prefixLength)
        val newRemainder = newText.substring(prefixLength)
        val suffixLength = commonSuffixWith(oldRemainder, newRemainder).length
        val newChangedPart = newRemainder.substring(0, newRemainder.length - suffixLength)

        val newAnnotatedString = buildAnnotatedString {
            append(oldContent.annotatedString.subSequence(0, prefixLength))

            val headingSpanStyle = getHeadingStyle(activeHeadingStyle)
            val styleToApply = (activeStyles + headingSpanStyle).reduceOrNull { a, b -> a.merge(b) } ?: SpanStyle()

            withStyle(styleToApply) {
                append(newChangedPart)
            }

            append(oldContent.annotatedString.subSequence(oldText.length - suffixLength, oldText.length))
        }
        
        return reapplyWikiLinks(
            preserveLinkAnnotations(
                newContent.copy(annotatedString = newAnnotatedString),
                newContent
            )
        )
    }

    /**
     * [TextFieldValue] adjusts its string annotations as the user types. The
     * rebuild above only carries span styles across, so re-apply the URL/email/
     * phone annotations from the incoming value; otherwise typing inside or next
     * to a link silently turned it into plain text.
     */
    private fun preserveLinkAnnotations(
        content: TextFieldValue,
        source: TextFieldValue
    ): TextFieldValue {
        val tags = listOf("URL", "EMAIL", "PHONE")
        val annotations = tags.flatMap { tag ->
            source.annotatedString
                .getStringAnnotations(tag, 0, source.text.length)
                .map { tag to it }
        }
        if (annotations.isEmpty()) return content

        val builder = AnnotatedString.Builder(content.annotatedString)
        annotations.forEach { (tag, range) ->
            if (range.start >= 0 && range.end <= content.text.length && range.start < range.end) {
                builder.addStringAnnotation(tag, range.item, range.start, range.end)
                builder.addStyle(UrlLinkStyle, range.start, range.end)
            }
        }
        return content.copy(annotatedString = builder.toAnnotatedString())
    }

    private fun reapplyWikiLinks(content: TextFieldValue): TextFieldValue {
        val text = content.text
        
        // Optimization: Only re-apply if the text contains potential link markers
        if (!text.contains("[[")) return content

        val styles = content.annotatedString.spanStyles
        
        // Find existing NOTE_LINK annotations to identify which styles to remove
        val oldLinkAnnotations = content.annotatedString.getStringAnnotations("NOTE_LINK", 0, text.length)
        
        // Filter out styles that exactly match the WikiLinkStyle and overlap with old link annotations
        val cleanedStyles = styles.filterNot { styleRange ->
            oldLinkAnnotations.any { linkRange ->
                linkRange.start == styleRange.start && linkRange.end == styleRange.end && styleRange.item == WikiLinkStyle
            }
        }
        
        // Filter out existing NOTE_LINK annotations
        val cleanedAnnotations = content.annotatedString.getStringAnnotations(0, text.length).filter { it.tag != "NOTE_LINK" }

        val builder = AnnotatedString.Builder(text)
        
        // Add back preserved styles
        cleanedStyles.forEach { builder.addStyle(it.item, it.start, it.end) }
        
        // Add back preserved annotations
        cleanedAnnotations.forEach { builder.addStringAnnotation(it.tag, it.item, it.start, it.end) }

        // Find and apply new Wiki Links - use a faster search for the whole string
        val wikiLinkRegex = "\\[\\[(.*?)\\]\\]".toRegex()
        wikiLinkRegex.findAll(text).forEach { matchResult ->
            val start = matchResult.range.first
            val end = matchResult.range.last + 1
            val title = matchResult.groupValues[1]
            if (title.isNotBlank()) {
                builder.addStringAnnotation("NOTE_LINK", title, start, end)
                builder.addStyle(WikiLinkStyle, start, end)
            }
        }

        return content.copy(annotatedString = builder.toAnnotatedString())
    }

    data class StyleToggleResult(
        val updatedContent: TextFieldValue? = null,
        val updatedActiveStyles: Set<SpanStyle>? = null
    )

    fun toggleStyle(
        content: TextFieldValue,
        styleToToggle: SpanStyle,
        currentActiveStyles: Set<SpanStyle>,
        isBoldActive: Boolean,
        isItalicActive: Boolean,
        isUnderlineActive: Boolean
    ): StyleToggleResult {
        val selection = content.selection
        if (selection.collapsed) {
            val activeStyles = currentActiveStyles.toMutableSet()

            val isBold = styleToToggle.fontWeight == FontWeight.Bold
            val isItalic = styleToToggle.fontStyle == FontStyle.Italic
            val isUnderline = styleToToggle.textDecoration == TextDecoration.Underline

            val wasBold = activeStyles.any { it.fontWeight == FontWeight.Bold }
            val wasItalic = activeStyles.any { it.fontStyle == FontStyle.Italic }
            val wasUnderline = activeStyles.any { it.textDecoration == TextDecoration.Underline }

            if (isBold) {
                if (wasBold) activeStyles.removeAll { it.fontWeight == FontWeight.Bold }
                else activeStyles.add(SpanStyle(fontWeight = FontWeight.Bold))
            }
            if (isItalic) {
                if (wasItalic) activeStyles.removeAll { it.fontStyle == FontStyle.Italic }
                else activeStyles.add(SpanStyle(fontStyle = FontStyle.Italic))
            }
            if (isUnderline) {
                if (wasUnderline) activeStyles.removeAll { it.textDecoration == TextDecoration.Underline }
                else activeStyles.add(SpanStyle(textDecoration = TextDecoration.Underline))
            }
            return StyleToggleResult(updatedActiveStyles = activeStyles)
        } else {
            val newAnnotatedString = AnnotatedString.Builder(content.annotatedString).apply {
                val isApplyingBold = styleToToggle.fontWeight == FontWeight.Bold
                val isApplyingItalic = styleToToggle.fontStyle == FontStyle.Italic
                val isApplyingUnderline = styleToToggle.textDecoration == TextDecoration.Underline

                val styleToApply = when {
                    isApplyingBold -> if (isBoldActive) SpanStyle(fontWeight = FontWeight.Normal) else SpanStyle(fontWeight = FontWeight.Bold)
                    isApplyingItalic -> if (isItalicActive) SpanStyle(fontStyle = FontStyle.Normal) else SpanStyle(fontStyle = FontStyle.Italic)
                    isApplyingUnderline -> if (isUnderlineActive) SpanStyle(textDecoration = TextDecoration.None) else SpanStyle(textDecoration = TextDecoration.Underline)
                    else -> styleToToggle
                }
                addStyle(styleToApply, selection.start, selection.end)
            }.toAnnotatedString()

            return StyleToggleResult(updatedContent = content.copy(annotatedString = newAnnotatedString))
        }
    }

    fun applyHeading(
        content: TextFieldValue,
        level: Int
    ): TextFieldValue? {
        val selection = content.selection
        
        val headingStyle = when (level) {
            1 -> SpanStyle(fontSize = 24.sp)
            2 -> SpanStyle(fontSize = 20.sp)
            3 -> SpanStyle(fontSize = 18.sp)
            4 -> SpanStyle(fontSize = 16.sp)
            5 -> SpanStyle(fontSize = 14.sp)
            6 -> SpanStyle(fontSize = 12.sp)
            else -> SpanStyle()
        }

        if (!selection.collapsed) {
             val newAnnotatedString = AnnotatedString.Builder(content.annotatedString).apply {
                addStyle(headingStyle, selection.start, selection.end)
            }.toAnnotatedString()
            return content.copy(annotatedString = newAnnotatedString)
        }
        return null
    }

    fun toggleBulletedList(content: TextFieldValue): TextFieldValue {
        val text = content.text
        val selection = content.selection
        val lines = text.split("\n")
        
        var currentOffset = 0
        val newLines = lines.map { line ->
            val lineStart = currentOffset
            val lineEnd = currentOffset + line.length
            val isLineSelected = if (selection.collapsed) {
                selection.start in lineStart..lineEnd
            } else {
                val overlapStart = maxOf(selection.start, lineStart)
                val overlapEnd = minOf(selection.end, lineEnd)
                overlapStart < overlapEnd || (selection.start == selection.end && selection.start in lineStart..lineEnd)
            }

            val newLine = if (isLineSelected) {
                if (line.trimStart().startsWith("• ")) {
                    line.replaceFirst("• ", "")
                } else {
                    "• $line"
                }
            } else {
                line
            }
            
            currentOffset += line.length + 1
            newLine
        }

        val newText = newLines.joinToString("\n")
        
        // Basic selection adjustment (could be more sophisticated)
        val diff = newText.length - text.length
        val newSelection = if (selection.collapsed) {
             TextRange(selection.start + if (diff > 0) 2 else if (diff < 0) -2 else 0)
        } else {
            TextRange(selection.start, selection.end + diff)
        }

        return content.copy(
            annotatedString = parseMarkdownToAnnotatedString(newText),
            selection = TextRange(
                newSelection.start.coerceIn(0, newText.length),
                newSelection.end.coerceIn(0, newText.length)
            )
        )
    }

    private fun commonPrefixWith(a: CharSequence, b: CharSequence): String {
        val minLength = minOf(a.length, b.length)
        for (i in 0 until minLength) {
            if (a[i] != b[i]) {
                return a.substring(0, i)
            }
        }
        return a.substring(0, minLength)
    }

    private fun commonSuffixWith(a: CharSequence, b: CharSequence): String {
        val minLength = minOf(a.length, b.length)
        for (i in 0 until minLength) {
            if (a[a.length - 1 - i] != b[b.length - 1 - i]) {
                return a.substring(a.length - i)
            }
        }
        return a.substring(a.length - minLength)
    }

    /**
     * Inserts a link at the current selection. When text is selected it becomes the
     * link label (or is replaced by [displayText] if the user edited the label);
     * when the selection is collapsed the label is inserted at the cursor.
     */
    fun insertLink(
        content: TextFieldValue,
        rawUrl: String,
        displayText: String? = null
    ): TextFieldValue {
        val text = content.text
        val selection = content.selection
        val start = selection.min.coerceIn(0, text.length)
        val end = selection.max.coerceIn(0, text.length)

        val url = normalizeUrl(rawUrl)
        if (url.isEmpty()) return content

        val selectedText = text.substring(start, end)
        val label = displayText?.takeIf { it.isNotBlank() }
            ?: selectedText.takeIf { it.isNotBlank() }
            ?: url

        val newAnnotatedString = buildAnnotatedString {
            append(content.annotatedString.subSequence(0, start))
            pushStringAnnotation(tag = "URL", annotation = url)
            withStyle(UrlLinkStyle) {
                append(label)
            }
            pop()
            append(content.annotatedString.subSequence(end, text.length))
        }

        val newCursor = (start + label.length).coerceIn(0, newAnnotatedString.length)
        return content.copy(
            annotatedString = newAnnotatedString,
            selection = TextRange(newCursor)
        )
    }

    fun parseMarkdownToAnnotatedString(text: String): AnnotatedString {
        return buildAnnotatedString {
            var currentText = text

            // Match bold **text** or __text__
            val boldRegex = "(\\s|^)(\\*\\*|__)(.*?)\\2".toRegex()
            // Match italic *text* or _text_
            val italicRegex = "(\\s|^)(\\*|_)(.*?)\\2".toRegex()
            // Match links [text](url)
            val linkRegex = "\\[(.*?)\\]\\((.*?)\\)".toRegex()
            // Match wiki links [[text]]
            val wikiLinkRegex = "\\[\\[(.*?)\\]\\]".toRegex()

            var lastIndex = 0
            val allMatches = (boldRegex.findAll(text) + italicRegex.findAll(text) + linkRegex.findAll(text) + wikiLinkRegex.findAll(text))
                .sortedBy { it.range.first }

            allMatches.forEach { match ->
                if (match.range.first >= lastIndex) {
                    append(text.substring(lastIndex, match.range.first))

                    when {
                        match.value.startsWith("**") || match.value.startsWith("__") || (match.value.trim().startsWith("**")) -> {
                            val content = match.groupValues[3]
                            // preserve prefix space if any
                            val prefix = match.groupValues[1]
                            append(prefix)
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                append(content)
                            }
                        }
                        match.value.startsWith("*") || match.value.startsWith("_") || (match.value.trim().startsWith("*")) -> {
                            val content = match.groupValues[3]
                            val prefix = match.groupValues[1]
                            append(prefix)
                            withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                                append(content)
                            }
                        }
                        match.value.startsWith("[[") -> {
                            val linkText = match.groupValues[1]
                            pushStringAnnotation(tag = "NOTE_LINK", annotation = linkText)
                            withStyle(WikiLinkStyle) {
                                append(linkText)
                            }
                            pop()
                        }
                        match.value.startsWith("[") -> {
                            val linkText = match.groupValues[1]
                            val url = match.groupValues[2]
                            pushStringAnnotation(tag = "URL", annotation = normalizeUrl(url))
                            withStyle(UrlLinkStyle) {
                                append(linkText)
                            }
                            pop()
                        }
                    }
                    lastIndex = match.range.last + 1
                }
            }

            if (lastIndex < text.length) {
                append(text.substring(lastIndex))
            }
        }
    }
}
