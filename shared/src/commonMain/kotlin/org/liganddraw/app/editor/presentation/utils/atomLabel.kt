package org.liganddraw.app.editor.presentation.utils

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.center
import org.liganddraw.app.editor.domain.Atom
import org.liganddraw.app.editor.presentation.drawing_pane.components.subscriptStyle

fun Atom.toLabel(): AnnotatedString {
    return buildAnnotatedString {
        if (numImplicitHydrogen == 0L) append(symbol)
        else {
            if (isLabelReversed) {
                appendHydrogens(numImplicitHydrogen)
                append(symbol)
            } else {
                append(symbol)
                appendHydrogens(numImplicitHydrogen)
            }
        }
    }
}

private fun AnnotatedString.Builder.appendHydrogens(count: Long) {
    append("H")
    if (count > 1L) {
        pushStyle(subscriptStyle)
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