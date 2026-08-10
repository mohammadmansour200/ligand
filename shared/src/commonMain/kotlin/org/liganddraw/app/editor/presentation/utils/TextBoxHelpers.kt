package org.liganddraw.app.editor.presentation.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import org.liganddraw.app.editor.domain.TextBox

fun TextBox.toAnnotatedString(
    density: Density,
    fontFamilyResolver: (String) -> FontFamily = { FontFamily.Default },
): AnnotatedString {
    val length = content.length

    return AnnotatedString.Builder(content).apply {
        runs.forEach { run ->
            val start = run.startIndex.coerceIn(0, length)
            val end = run.endIndex.coerceIn(0, length)
            if (end <= start) return@forEach

            addStyle(
                style = SpanStyle(
                    color = Color(run.colorArgb),
                    fontSize = with(density) { run.fontSizePx.toSp() },
                    fontFamily = fontFamilyResolver(run.fontFamily),
                    fontWeight = if (run.bold) FontWeight.Bold else FontWeight.Normal,
                    fontStyle = if (run.italic) FontStyle.Italic else FontStyle.Normal,
                ),
                start = start,
                end = end,
            )
        }
    }.toAnnotatedString()
}

fun getTextBoxLayout(id: String, cache: Map<String, TextLayoutResult>): TextLayoutResult? {
    return cache[id]
}