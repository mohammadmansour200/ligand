package org.liganddraw.app.editor.presentation.drawing_pane.modifier

import androidx.compose.foundation.ScrollState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.fadingEdges(
    scrollState: ScrollState,
    isVertical: Boolean,
    edgeColor: Color,
    length: Dp = 18.dp
): Modifier = this.drawWithContent {
    drawContent()

    val fadeLengthPx = length.toPx()
    val canScrollStart = scrollState.value > 0
    val canScrollEnd = scrollState.value < scrollState.maxValue

    if (!isVertical) {
        if (canScrollStart) {
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(edgeColor, Color.Transparent),
                    startX = 0f,
                    endX = fadeLengthPx
                ),
                topLeft = Offset.Zero,
                size = Size(fadeLengthPx, size.height)
            )
        }
        if (canScrollEnd) {
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.Transparent, edgeColor),
                    startX = size.width - fadeLengthPx,
                    endX = size.width
                ),
                topLeft = Offset(size.width - fadeLengthPx, 0f),
                size = Size(fadeLengthPx, size.height)
            )
        }
    } else {
        if (canScrollStart) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(edgeColor, Color.Transparent),
                    startY = 0f,
                    endY = fadeLengthPx
                ),
                topLeft = Offset.Zero,
                size = Size(size.width, fadeLengthPx)
            )
        }
        if (canScrollEnd) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, edgeColor),
                    startY = size.height - fadeLengthPx,
                    endY = size.height
                ),
                topLeft = Offset(0f, size.height - fadeLengthPx),
                size = Size(size.width, fadeLengthPx)
            )
        }
    }
}