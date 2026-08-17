package com.lifepilot.designsystem.text

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

/** One styled run of text produced by [MarkdownInline]. */
data class MarkdownRun(val text: String, val bold: Boolean = false, val italic: Boolean = false)

/**
 * A tiny, dependency-free Markdown parser for chat display. Handles the subset the AI actually uses —
 * **bold**, __bold__, *italic*, _italic_, and `#` headers (rendered bold) — and strips backticks.
 * Everything else is passed through verbatim. Pure Kotlin so it is unit-testable without Compose; the
 * [String.toDisplayAnnotatedString] extension turns the runs into a Compose [AnnotatedString].
 *
 * We roll our own instead of a library because the maintained Compose-Markdown renderers require
 * Kotlin 2.2 / JitPack and this project is on Kotlin 2.0.
 */
object MarkdownInline {

    private val EMPHASIS = Regex("\\*\\*(.+?)\\*\\*|__(.+?)__|\\*(.+?)\\*|_(.+?)_")
    private val HEADER = Regex("^\\s{0,3}#{1,6}\\s+(.*)$")

    fun parse(src: String): List<MarkdownRun> {
        val runs = mutableListOf<MarkdownRun>()
        src.split("\n").forEachIndexed { idx, rawLine ->
            if (idx > 0) runs.add(MarkdownRun("\n"))
            val header = HEADER.find(rawLine)
            val line = (header?.groupValues?.get(1) ?: rawLine).replace("`", "")
            parseInline(line, forceBold = header != null, out = runs)
        }
        return runs
    }

    private fun parseInline(text: String, forceBold: Boolean, out: MutableList<MarkdownRun>) {
        var last = 0
        for (m in EMPHASIS.findAll(text)) {
            if (m.range.first > last) {
                out.add(MarkdownRun(text.substring(last, m.range.first), forceBold))
            }
            val bold = m.groupValues[1].ifEmpty { m.groupValues[2] }
            val italic = m.groupValues[3].ifEmpty { m.groupValues[4] }
            if (bold.isNotEmpty()) {
                out.add(MarkdownRun(bold, bold = true))
            } else {
                out.add(MarkdownRun(italic, bold = forceBold, italic = true))
            }
            last = m.range.last + 1
        }
        if (last < text.length) {
            out.add(MarkdownRun(text.substring(last), forceBold))
        }
    }
}

/** Renders a small Markdown subset (see [MarkdownInline]) as a styled [AnnotatedString]. */
fun toDisplayAnnotatedString(text: String): AnnotatedString = buildAnnotatedString {
    MarkdownInline.parse(text).forEach { run ->
        if (run.bold || run.italic) {
            withStyle(
                SpanStyle(
                    fontWeight = if (run.bold) FontWeight.Bold else null,
                    fontStyle = if (run.italic) FontStyle.Italic else null,
                ),
            ) {
                append(run.text)
            }
        } else {
            append(run.text)
        }
    }
}
