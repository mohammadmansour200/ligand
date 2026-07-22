package org.liganddraw.app.editor.presentation.drawing_pane.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.util.fastForEachIndexed
import org.liganddraw.app.editor.domain.Bond
import org.liganddraw.app.editor.domain.BondDir
import org.liganddraw.app.editor.domain.DoubleBondAlignment
import org.liganddraw.app.editor.domain.Tool
import org.liganddraw.app.editor.presentation.drawing_pane.DrawingPaneAction
import org.liganddraw.app.editor.presentation.drawing_pane.DrawingPaneState
import org.liganddraw.app.editor.presentation.utils.getHydrogenLabelDimensions
import org.liganddraw.app.editor.presentation.utils.getSymbolLabelDimensions
import org.liganddraw.app.editor.presentation.utils.labelRect
import org.liganddraw.app.editor.presentation.utils.toLabel
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

// TODO(refactor)
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
val highlightStroke = Stroke(width = 2f)
val highlightCornerRadius = CornerRadius(x = 2f, y = 2f)

@Composable
fun DrawingCanvas(state: DrawingPaneState, onAction: (DrawingPaneAction) -> Unit) {
    val textMeasurer = rememberTextMeasurer()
    val color = MaterialTheme.colorScheme.inverseSurface
    val primaryColor = MaterialTheme.colorScheme.primary
    val background = MaterialTheme.colorScheme.outlineVariant

    var canvasScale by remember { mutableFloatStateOf(1f) }
    var canvasOffset by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(state.molecules) {
        val uniqueSymbols = mutableMapOf<String, IntSize>()
        val uniqueHydrogenCounts = mutableMapOf<Long, IntSize>()
        state.molecules.forEach { mol ->
            mol.atoms.forEach { atom ->
                val symbol = atom.symbol
                if (!uniqueSymbols.containsKey(symbol)) {
                    val measuredSymbol = textMeasurer.measure(
                        text = symbol,
                        style = getSymbolStyle(Color.Unspecified)
                    )

                    uniqueSymbols[symbol] = measuredSymbol.size
                }

                val hydrogenCount = atom.numImplicitHydrogen
                if (hydrogenCount > 0 && !uniqueHydrogenCounts.containsKey(hydrogenCount)) {
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

                    uniqueHydrogenCounts[hydrogenCount] = measuredHydrogenLabel.size
                }
            }
        }

        onAction(DrawingPaneAction.OnCacheLabelDimensions(uniqueSymbols, uniqueHydrogenCounts))
    }

    Canvas(
        modifier = Modifier.pointerHoverIcon(
            when (state.selectedTool) {
                Tool.Pan -> PointerIcon.Hand
                else -> PointerIcon.Default
            }
        )
            .fillMaxSize()
            .clip(RoundedCornerShape(10.dp))
            .background(background)
            .transformable(state = rememberTransformableState { zoomChange, offsetChange, _ ->
                canvasScale *= zoomChange
                when (state.selectedTool) {
                    Tool.Pan -> canvasOffset += offsetChange
                    else -> {}
                }
            }).pointerInput(state.selectedTool) {
                if (state.selectedTool == Tool.Pan) return@pointerInput
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val pointerPosition =
                            event.changes.firstOrNull()?.position ?: return@awaitPointerEventScope

                        val x = (pointerPosition.x - canvasOffset.x) / canvasScale
                        val y = (pointerPosition.y - canvasOffset.y) / canvasScale

                        when (event.type) {
                            PointerEventType.Move -> onAction(DrawingPaneAction.OnPointerMove(x, y))

                            PointerEventType.Press -> onAction(
                                DrawingPaneAction.OnPointerPress(
                                    x,
                                    y
                                )
                            )
                        }
                    }
                }
            }
    ) {
        translate(canvasOffset.x, canvasOffset.y) {
            scale(canvasScale, pivot = Offset.Zero) {
                state.molecules.fastForEachIndexed { moleculeIndex, mol ->
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
                        if (beginAtom.isLabelVisible) {
                            val beginLabelRect =
                                labelRect(
                                    symbolDimensions = getSymbolLabelDimensions(
                                        beginAtom.symbol,
                                        state.symbolLabelDimensionsCache
                                    ),
                                    hydrogenDimensions = getHydrogenLabelDimensions(
                                        beginAtom.numImplicitHydrogen,
                                        state.hydrogenLabelDimensionsCache
                                    ),
                                    isReversed = beginAtom.isLabelReversed,
                                    atomOffset = beginAtomOffset
                                )

                            drawContext.canvas.clipRect(
                                rect = beginLabelRect,
                                clipOp = ClipOp.Difference
                            )
                        }
                        if (endAtom.isLabelVisible) {
                            val endLabelRect =
                                labelRect(
                                    symbolDimensions = getSymbolLabelDimensions(
                                        endAtom.symbol,
                                        state.symbolLabelDimensionsCache
                                    ),
                                    hydrogenDimensions = getHydrogenLabelDimensions(
                                        endAtom.numImplicitHydrogen,
                                        state.hydrogenLabelDimensionsCache
                                    ),
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
                    mol.atoms.fastForEachIndexed { atomIndex, atom ->
                        val atomId = Pair(moleculeIndex, atomIndex)

                        val atomXPositionPx = ((atom.x * scaleFactor)).toFloat()
                        val atomYPositionPx = -((atom.y * scaleFactor)).toFloat()
                        val atomOffset = Offset(atomXPositionPx, atomYPositionPx)

                        val labelRect =
                            labelRect(
                                symbolDimensions = getSymbolLabelDimensions(
                                    atom.symbol,
                                    state.symbolLabelDimensionsCache
                                ),
                                hydrogenDimensions = if (!atom.isLabelVisible) IntSize.Zero else getHydrogenLabelDimensions(
                                    atom.numImplicitHydrogen, state.hydrogenLabelDimensionsCache
                                ),
                                isReversed = atom.isLabelReversed,
                                atomOffset = atomOffset
                            )

                        if (state.hoveredAtomId == atomId) drawRoundRect(
                            color = primaryColor,
                            topLeft = labelRect.topLeft,
                            size = labelRect.size,
                            style = highlightStroke,
                            cornerRadius = highlightCornerRadius
                        )

                        if (atom.isLabelVisible)
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


