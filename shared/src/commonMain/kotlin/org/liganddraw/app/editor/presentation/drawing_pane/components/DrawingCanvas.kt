package org.liganddraw.app.editor.presentation.drawing_pane.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.center
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import org.liganddraw.app.editor.domain.Bond
import org.liganddraw.app.editor.domain.BondDir
import org.liganddraw.app.editor.domain.DoubleBondAlignment
import org.liganddraw.app.editor.presentation.drawing_pane.DrawingPaneState
import org.liganddraw.app.editor.presentation.utils.toLabel
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

const val scaleFactor = 40f
const val strokeWidth = 2f
const val bondSpacing = 6f
const val centeredDoubleBondSpacing = 3f
val symbolFontSize = 1.2.em
val subscriptFontSize = .85.em
val getSymbolStyle =
    { color: Color -> TextStyle(fontSize = symbolFontSize, color = color) }
val subscriptStyle =
    SpanStyle(fontSize = subscriptFontSize, baselineShift = BaselineShift.Subscript)

@Composable
fun DrawingCanvas(state: DrawingPaneState) {
    val textMeasurer = rememberTextMeasurer()
    val color = MaterialTheme.colorScheme.inverseSurface
    val background = MaterialTheme.colorScheme.outlineVariant

    var canvasScale by remember { mutableFloatStateOf(1f) }
    var canvasOffset by remember { mutableStateOf(Offset.Zero) }

    val sizesCache = remember(state.molecules) {
        val symbolSizes = mutableMapOf<String, IntSize>()
        val hydrogenSizes = mutableMapOf<Long, IntSize>()
        state.molecules.forEach { mol ->
            mol.atoms.forEach { atom ->
                val symbol = atom.symbol
                if (!symbolSizes.containsKey(symbol)) {
                    val measuredSymbol = textMeasurer.measure(
                        text = symbol,
                        style = getSymbolStyle(Color.Unspecified)
                    )

                    symbolSizes[symbol] = measuredSymbol.size
                }

                val hydrogenCount = atom.numImplicitHydrogen
                if (hydrogenCount > 0 && !hydrogenSizes.containsKey(hydrogenCount)) {
                    val hydrogenLabel = buildAnnotatedString {
                        append("H")
                        if (hydrogenCount > 1) {
                            pushStyle(
                                subscriptStyle
                            )
                            append(hydrogenCount.toString())
                            pop()
                        }
                    }

                    val measuredHydrogenLabel =
                        textMeasurer.measure(hydrogenLabel, getSymbolStyle(Color.Unspecified))

                    hydrogenSizes[hydrogenCount] = measuredHydrogenLabel.size
                }
            }
        }

        Pair(symbolSizes, hydrogenSizes)
    }
    val symbolSize = { symbol: String -> sizesCache.first.getOrDefault(symbol, IntSize.Zero) }
    val hydrogenSize =
        { hydrogenCount: Long -> sizesCache.second.getOrDefault(hydrogenCount, IntSize.Zero) }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(10.dp))
            .background(background)
            .transformable(state = rememberTransformableState { zoomChange, offsetChange, _ ->
                canvasScale *= zoomChange
                canvasOffset += offsetChange
            })
    ) {
        translate(canvasOffset.x, canvasOffset.y) {
            scale(canvasScale, pivot = Offset.Zero) {
                state.molecules.forEach { mol ->
                    // --- DRAW BONDS ---
                    // To apply clipRect only on bonds not atom symbols
                    drawContext.canvas.save()
                    mol.bonds.forEach { bond ->
                        val beginAtom = mol.atoms[bond.beginAtomIndex.toInt()]
                        val endAtom = mol.atoms[bond.endAtomIndex.toInt()]

                        val beginAtomXPositionPx =
                            ((beginAtom.x * scaleFactor)).toFloat()
                        val beginAtomYPositionPx =
                            -((beginAtom.y * scaleFactor)).toFloat()
                        val beginAtomOffset = Offset(beginAtomXPositionPx, beginAtomYPositionPx)

                        val endAtomXPositionPx = ((endAtom.x * scaleFactor)).toFloat()
                        val endAtomYPositionPx = -((endAtom.y * scaleFactor)).toFloat()
                        val endAtomOffset = Offset(endAtomXPositionPx, endAtomYPositionPx)

                        // clipRect to hide bond overlapping with atom symbol
                        if (beginAtom.symbol != "C") {
                            val beginLabelRect =
                                labelRect(
                                    symbolSize = symbolSize(beginAtom.symbol),
                                    hydrogenSize = hydrogenSize(beginAtom.numImplicitHydrogen),
                                    isReversed = beginAtom.isLabelReversed,
                                    atomOffset = beginAtomOffset
                                )

                            drawContext.canvas.clipRect(
                                rect = beginLabelRect,
                                clipOp = ClipOp.Difference
                            )
                        }
                        if (endAtom.symbol != "C") {
                            val endLabelRect =
                                labelRect(
                                    symbolSize = symbolSize(endAtom.symbol),
                                    hydrogenSize = hydrogenSize(endAtom.numImplicitHydrogen),
                                    isReversed = endAtom.isLabelReversed,
                                    atomOffset = endAtomOffset
                                )
                            drawContext.canvas.clipRect(
                                rect = endLabelRect,
                                clipOp = ClipOp.Difference
                            )
                        }

                        when (bond) {
                            is Bond.Single -> {
                                when (bond.direction) {
                                    BondDir.NONE -> drawLine(
                                        color = color,
                                        strokeWidth = strokeWidth,
                                        start = beginAtomOffset,
                                        end = endAtomOffset
                                    )

                                    BondDir.BEGINWEDGE -> drawWedgeBond(
                                        start = beginAtomOffset,
                                        end = endAtomOffset,
                                        strokeWidth = strokeWidth,
                                        color = color
                                    )

                                    BondDir.BEGINDASH -> drawDashBond(
                                        start = beginAtomOffset,
                                        end = endAtomOffset,
                                        strokeWidth = strokeWidth,
                                        color = color
                                    )
                                }
                            }

                            is Bond.Hydrogen -> drawHydrogenBond(
                                start = beginAtomOffset,
                                end = endAtomOffset,
                                strokeWidth = strokeWidth,
                                color = color
                            )

                            is Bond.Triple -> drawTripleBond(
                                start = beginAtomOffset,
                                end = endAtomOffset,
                                strokeWidth = strokeWidth,
                                color = color
                            )

                            is Bond.Double -> {
                                if (bond.alignment == DoubleBondAlignment.CENTERED)
                                    drawCenteredDoubleBond(
                                        start = beginAtomOffset,
                                        end = endAtomOffset,
                                        strokeWidth = strokeWidth,
                                        color = color
                                    ) else {
                                    val side =
                                        if (bond.alignment == DoubleBondAlignment.POSITIVE) 1f else -1f
                                    drawAsymmetricDoubleBond(
                                        start = beginAtomOffset,
                                        end = endAtomOffset,
                                        strokeWidth = strokeWidth,
                                        color = color,
                                        offset = bondSpacing * side
                                    )
                                }
                            }

                            is Bond.Ionic -> {}
                        }

                    }
                    drawContext.canvas.restore()

                    // --- DRAW ATOM SYMBOL ---
                    mol.atoms.forEach { atom ->
                        if (atom.symbol != "C") {
                            val atomXPositionPx = ((atom.x * scaleFactor)).toFloat()
                            val atomYPositionPx = -((atom.y * scaleFactor)).toFloat()
                            val atomOffset = Offset(atomXPositionPx, atomYPositionPx)

                            val labelRect =
                                labelRect(
                                    symbolSize = symbolSize(atom.symbol),
                                    hydrogenSize = hydrogenSize(atom.numImplicitHydrogen),
                                    isReversed = atom.isLabelReversed,
                                    atomOffset = atomOffset
                                )

                            drawText(
                                textMeasurer = textMeasurer,
                                text = atom.toLabel(),
                                topLeft = labelRect.topLeft,
                                style = getSymbolStyle(color),
                                size = labelRect.size
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawWedgeBond(
    start: Offset,
    end: Offset,
    strokeWidth: Float,
    color: Color
) {
    val wedgeWidth = strokeWidth * 4f
    val angle = atan2(end.y - start.y, end.x - start.x)
    val perpendicularAngle = angle + (Math.PI / 2).toFloat()

    val dx = (wedgeWidth / 2f) * cos(perpendicularAngle)
    val dy = (wedgeWidth / 2f) * sin(perpendicularAngle)

    val path = Path().apply {
        moveTo(start.x, start.y)
        lineTo(end.x + dx, end.y + dy)
        lineTo(end.x - dx, end.y - dy)
        close()
    }

    drawPath(path = path, color = color)
}

private fun DrawScope.drawDashBond(
    start: Offset,
    end: Offset,
    strokeWidth: Float,
    color: Color,
) {
    val maxWedgeWidth = strokeWidth * 4f
    val angle = atan2(end.y - start.y, end.x - start.x)
    val perpAngle = angle + (Math.PI / 2).toFloat()

    val dashCount = 10
    for (i in 0 until dashCount) {
        val t = i.toFloat() / (dashCount - 1)

        val currentX = start.x + t * (end.x - start.x)
        val currentY = start.y + t * (end.y - start.y)

        val currentWidth = t * maxWedgeWidth
        val dx = (currentWidth / 2f) * cos(perpAngle)
        val dy = (currentWidth / 2f) * sin(perpAngle)

        drawLine(
            color = color,
            start = Offset(currentX - dx, currentY - dy),
            end = Offset(currentX + dx, currentY + dy),
            strokeWidth = strokeWidth
        )
    }
}

fun DrawScope.drawAsymmetricDoubleBond(
    start: Offset,
    end: Offset,
    strokeWidth: Float,
    color: Color,
    offset: Float = bondSpacing
) {
    drawLine(
        color = color,
        start = start,
        end = end,
        strokeWidth = strokeWidth
    )

    val (sideStart, sideEnd) = offsetLine(
        start = start,
        end = end,
        offsetDistance = offset,
        lerpStart = 0.1f,
        lerpEnd = 0.9f
    )
    drawLine(
        color = color,
        start = sideStart,
        end = sideEnd,
        strokeWidth = strokeWidth
    )
}

fun DrawScope.drawCenteredDoubleBond(
    start: Offset,
    end: Offset,
    strokeWidth: Float,
    color: Color,
) {
    val (positiveSideStart, positiveSideEnd) = offsetLine(
        start = start,
        end = end,
        offsetDistance = centeredDoubleBondSpacing,
        lerpStart = -0.05f,
        lerpEnd = 1.05f
    )
    drawLine(
        color = color,
        start = positiveSideStart,
        end = positiveSideEnd,
        strokeWidth = strokeWidth
    )

    val (negativeSideStart, negativeSideEnd) = offsetLine(
        start = start,
        end = end,
        offsetDistance = -centeredDoubleBondSpacing,
        lerpStart = -0.05f,
        lerpEnd = 1.05f
    )
    drawLine(
        color = color,
        start = negativeSideStart,
        end = negativeSideEnd,
        strokeWidth = strokeWidth
    )
}

fun DrawScope.drawTripleBond(
    start: Offset,
    end: Offset,
    strokeWidth: Float,
    color: Color,
) {
    drawLine(
        color = color,
        start = start,
        end = end,
        strokeWidth = strokeWidth
    )

    val (positiveSideStart, positiveSideEnd) = offsetLine(
        start = start,
        end = end,
        offsetDistance = bondSpacing,
    )
    drawLine(
        color = color,
        start = positiveSideStart,
        end = positiveSideEnd,
        strokeWidth = strokeWidth
    )

    val (negativeSideStart, negativeSideEnd) = offsetLine(
        start = start,
        end = end,
        offsetDistance = -bondSpacing,
    )
    drawLine(
        color = color,
        start = negativeSideStart,
        end = negativeSideEnd,
        strokeWidth = strokeWidth
    )
}

fun DrawScope.drawHydrogenBond(
    start: Offset,
    end: Offset,
    strokeWidth: Float,
    color: Color
) {
    val pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)

    drawLine(
        color = color,
        start = start,
        end = end,
        strokeWidth = strokeWidth,
        pathEffect = pathEffect
    )
}

private fun offsetLine(
    start: Offset,
    end: Offset,
    offsetDistance: Float,
    lerpStart: Float = 0.2f,
    lerpEnd: Float = 0.8f
): Pair<Offset, Offset> {
    val angle = atan2(end.y - start.y, end.x - start.x)
    val perpAngle = angle + (Math.PI / 2).toFloat()

    val dx = offsetDistance * cos(perpAngle)
    val dy = offsetDistance * sin(perpAngle)

    val trimmedStart = lerp(start, end, lerpStart)
    val trimmedEnd = lerp(start, end, lerpEnd)

    val finalStart = Offset(trimmedStart.x + dx, trimmedStart.y + dy)
    val finalEnd = Offset(trimmedEnd.x + dx, trimmedEnd.y + dy)

    return Pair(finalStart, finalEnd)
}

private fun labelRect(
    symbolSize: IntSize,
    hydrogenSize: IntSize,
    isReversed: Boolean, // Reversed: [H][Symbol], Not reversed: [Symbol][H]
    atomOffset: Offset, // Atom X and Y coordinates in pixels
): Rect {
    val totalWidth = symbolSize.width + hydrogenSize.width
    val maxHeight = maxOf(symbolSize.height, hydrogenSize.height)

    val leftOffset = if (isReversed) hydrogenSize.width + symbolSize.center.x
    else symbolSize.center.x

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


