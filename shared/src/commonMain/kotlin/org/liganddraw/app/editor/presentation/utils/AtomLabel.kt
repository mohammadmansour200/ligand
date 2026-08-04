package org.liganddraw.app.editor.presentation.utils

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.center
import org.liganddraw.app.editor.domain.Atom
import org.liganddraw.app.editor.domain.DrawingPaneConstants.HYDROGEN_COUNT_FONT_SIZE
import org.liganddraw.app.editor.domain.DrawingPaneConstants.SYMBOL_FONT_SIZE

fun Atom.toLabel(density: Density): AnnotatedString {
    return buildAnnotatedString {
        if (numImplicitHydrogen == 0L) append(symbol)
        else {
            if (isLabelReversed) {
                appendHydrogens(density, numImplicitHydrogen)
                append(symbol)
            } else {
                append(symbol)
                appendHydrogens(density, numImplicitHydrogen)
            }
        }
    }
}

fun Atom.toChargeLabel(baseStyle: TextStyle): AnnotatedString {
    val magnitude = kotlin.math.abs(charge)
    val symbol = if (charge > 0) "⊕" else "⊖"

    return buildAnnotatedString {
        if (magnitude != 1) {
            withStyle(
                SpanStyle(fontSize = baseStyle.fontSize * .8f)
            ) {
                append(magnitude.toString())
            }
        }
        withStyle(SpanStyle(fontSize = baseStyle.fontSize)) {
            append(symbol)
        }
    }
}


private fun AnnotatedString.Builder.appendHydrogens(density: Density, count: Long) {
    append("H")
    if (count > 1L) {
        pushStyle(getHydrogenCountStyle(density))
        append(count.toString())
        pop()
    }
}

fun labelRect(
    symbolDimensions: IntSize,
    hydrogenDimensions: IntSize,
    isReversed: Boolean, // Reversed: [H][Symbol], Not reversed: [Symbol][H]
    atomOffset: Offset, // Atom X and Y coordinates in pixels
): Rect {
    val totalWidth = symbolDimensions.width + hydrogenDimensions.width
    val maxHeight = maxOf(symbolDimensions.height, hydrogenDimensions.height)

    val leftOffset = if (isReversed) hydrogenDimensions.width + symbolDimensions.center.x
    else symbolDimensions.center.x

    val left = atomOffset.x - leftOffset
    val right = left + totalWidth

    val top = atomOffset.y - (maxHeight / 2)
    val bottom = top + maxHeight

    return Rect(
        left,
        top,
        right,
        bottom
    )
}

fun getSymbolLabelDimensions(symbol: String, dimensionsCache: Map<String, IntSize>): IntSize {
    return dimensionsCache[symbol] ?: IntSize.Zero
}

fun getHydrogenLabelDimensions(hydrogenCount: Long, dimensionsCache: Map<Long, IntSize>): IntSize {
    return dimensionsCache[hydrogenCount] ?: IntSize.Zero
}

fun getSymbolStyle(density: Density, color: Color): TextStyle =
    TextStyle(fontSize = with(density) { SYMBOL_FONT_SIZE.toSp() }, color = color)

fun getHydrogenCountStyle(density: Density): SpanStyle = SpanStyle(
    fontSize = with(density) { HYDROGEN_COUNT_FONT_SIZE.toSp() },
    baselineShift = BaselineShift.Subscript
)
