package org.ligand.app.editor.presentation.utils

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
import org.ligand.app.editor.domain.DrawingPaneConstants.HYDROGEN_COUNT_FONT_SIZE
import org.ligand.app.editor.domain.DrawingPaneConstants.SYMBOL_FONT_SIZE
import org.ligand.app.editor.domain.LabelOrientation

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
    labelOrientation: LabelOrientation,
    atomOffset: Offset,
): AtomLabelLayout {
    val symbolWidth = symbolLayout?.size?.width?.toFloat() ?: 0f
    val symbolHeight = symbolLayout?.size?.height?.toFloat() ?: 0f
    val hydrogenWidth = hydrogenLayout?.size?.width?.toFloat() ?: 0f
    val hydrogenHeight = hydrogenLayout?.size?.height?.toFloat() ?: 0f
    
    val symbolLeft = atomOffset.x - symbolWidth / 2f
    val symbolTop = atomOffset.y - symbolHeight / 2f
    val symbolTopLeft = Offset(symbolLeft, symbolTop)

    val verticalGap = symbolHeight * 0.75f
    val hydrogenTopLeft = when (labelOrientation) {
        LabelOrientation.EAST -> Offset(
            x = symbolLeft + symbolWidth,
            y = symbolTop
        )

        LabelOrientation.WEST -> Offset(
            x = symbolLeft - hydrogenWidth,
            y = symbolTop
        )

        LabelOrientation.NORTH -> Offset(
            x = atomOffset.x - hydrogenWidth / 2f,
            y = atomOffset.y - verticalGap - hydrogenHeight / 2f
        )

        LabelOrientation.SOUTH -> Offset(
            x = atomOffset.x - hydrogenWidth / 2f,
            y = atomOffset.y + verticalGap - hydrogenHeight / 2f
        )
    }

    val hasHydrogen = hydrogenLayout != null
    val left = if (hasHydrogen) minOf(symbolTopLeft.x, hydrogenTopLeft.x) else symbolTopLeft.x
    val top = if (hasHydrogen) minOf(symbolTopLeft.y, hydrogenTopLeft.y) else symbolTopLeft.y
    val right = if (hasHydrogen) {
        maxOf(symbolTopLeft.x + symbolWidth, hydrogenTopLeft.x + hydrogenWidth)
    } else {
        symbolTopLeft.x + symbolWidth
    }
    val bottom = if (hasHydrogen) {
        maxOf(symbolTopLeft.y + symbolHeight, hydrogenTopLeft.y + hydrogenHeight)
    } else {
        symbolTopLeft.y + symbolHeight
    }

    return AtomLabelLayout(
        boundingRect = Rect(left, top, right, bottom),
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
