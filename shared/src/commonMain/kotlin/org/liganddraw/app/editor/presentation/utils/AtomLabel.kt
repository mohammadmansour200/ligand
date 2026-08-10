package org.liganddraw.app.editor.presentation.utils

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.center
import org.liganddraw.app.editor.domain.DrawingPaneConstants.HYDROGEN_COUNT_FONT_SIZE
import org.liganddraw.app.editor.domain.DrawingPaneConstants.SYMBOL_FONT_SIZE

fun chargeLabel(charge: Int, baseStyle: TextStyle): AnnotatedString {
    val magnitude = kotlin.math.abs(charge)
    val symbol = if (charge > 0) "⊕" else "⊖"
    return buildAnnotatedString {
        if (magnitude != 1) {
            withStyle(SpanStyle(fontSize = baseStyle.fontSize * .8f)) {
                append(magnitude.toString())
            }
        }
        withStyle(SpanStyle(fontSize = baseStyle.fontSize)) {
            append(symbol)
        }
    }
}

data class AtomLabelLayout(
    val boundingRect: Rect,
    val symbolTopLeft: Offset,
    val hydrogenTopLeft: Offset,
)

fun getAtomLabelLayout(
    symbolLayout: TextLayoutResult?,
    hydrogenLayout: TextLayoutResult?,
    isReversed: Boolean,
    atomOffset: Offset,
): AtomLabelLayout {
    val symbolDimensions = symbolLayout?.size ?: IntSize.Zero
    val hydrogenDimensions = hydrogenLayout?.size ?: IntSize.Zero

    val width = symbolDimensions.width + hydrogenDimensions.width
    val height = symbolDimensions.height

    val leftOffset = if (isReversed) hydrogenDimensions.width + symbolDimensions.center.x
    else symbolDimensions.center.x
    val left = atomOffset.x - leftOffset
    val top = atomOffset.y - (height / 2f)

    val symbolTopLeft =
        if (isReversed) Offset(left + hydrogenDimensions.width, top) else Offset(
            left,
            top
        )
    val hydrogenTopLeft = if (isReversed) Offset(left, top) else Offset(
        left + symbolDimensions.width,
        top
    )

    return AtomLabelLayout(
        boundingRect = Rect(left, top, left + width, top + height),
        symbolTopLeft = symbolTopLeft,
        hydrogenTopLeft = hydrogenTopLeft,
    )
}

fun getSymbolLabelLayout(symbol: String, cache: Map<String, TextLayoutResult>): TextLayoutResult? {
    return cache[symbol]
}

fun getHydrogenLabelLayout(
    hydrogenCount: Long,
    cache: Map<Long, TextLayoutResult>
): TextLayoutResult? {
    return cache[hydrogenCount]
}

fun getSymbolStyle(density: Density, color: Color): TextStyle =
    TextStyle(fontSize = with(density) { SYMBOL_FONT_SIZE.toSp() }, color = color)

fun getHydrogenCountStyle(density: Density): SpanStyle = SpanStyle(
    fontSize = with(density) { HYDROGEN_COUNT_FONT_SIZE.toSp() },
    baselineShift = BaselineShift.Subscript
)
