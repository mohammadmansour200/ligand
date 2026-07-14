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
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import org.liganddraw.app.editor.domain.Bond
import org.liganddraw.app.editor.domain.BondDir
import org.liganddraw.app.editor.domain.DoubleBondAlignment
import org.liganddraw.app.editor.presentation.drawing_pane.DrawingPaneState
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

const val scaleFactor = 40f
const val strokeWidth = 2f
const val bondSpacing = 6f
const val centeredDoubleBondSpacing = 3f

@Composable
fun DrawingCanvas(state: DrawingPaneState) {
    val textMeasurer = rememberTextMeasurer()
    val color = MaterialTheme.colorScheme.inverseSurface
    val background = MaterialTheme.colorScheme.outlineVariant

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Canvas(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .fillMaxSize()
            .transformable(state = rememberTransformableState { zoomChange, offsetChange, _ ->
                scale *= zoomChange
                offset += offsetChange
            })
            .background(background)
    ) {
        translate(offset.x, offset.y) {
            scale(scale, pivot = Offset.Zero) {
                state.molecules.forEach { mol ->
                    // --- DRAW BONDS ---
                    // To apply clipRect only on bonds not atom symbols
                    drawContext.canvas.save()
                    mol.bonds.forEach { bond ->
                        val beginAtom = mol.atoms[bond.beginAtomIndex.toInt()]
                        val endAtom = mol.atoms[bond.endAtomIndex.toInt()]

                        val beginAtomX =
                            ((beginAtom.x * scaleFactor)).toFloat()
                        val beginAtomY =
                            -((beginAtom.y * scaleFactor)).toFloat()
                        val beginAtomOffset = Offset(beginAtomX, beginAtomY)

                        val endAtomX = ((endAtom.x * scaleFactor)).toFloat()
                        val endAtomY = -((endAtom.y * scaleFactor)).toFloat()
                        val endAtomOffset = Offset(endAtomX, endAtomY)

                        // clipRect to hide bond overlapping with atom symbol
                        if (beginAtom.symbol != "C") {
                            val beginAtomRect =
                                atomSymbolRect(
                                    textMeasurer,
                                    beginAtom.symbol,
                                    beginAtomOffset
                                )
                            drawContext.canvas.clipRect(
                                rect = beginAtomRect,
                                clipOp = ClipOp.Difference
                            )
                        }
                        if (endAtom.symbol != "C") {
                            val endAtomRect =
                                atomSymbolRect(textMeasurer, endAtom.symbol, endAtomOffset)
                            drawContext.canvas.clipRect(
                                rect = endAtomRect,
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
                            val atomX = ((atom.x * scaleFactor)).toFloat()
                            val atomY = -((atom.y * scaleFactor)).toFloat()
                            val atomOffset = Offset(atomX, atomY)

                            val atomRect =
                                atomSymbolRect(textMeasurer, atom.symbol, atomOffset)

                            drawText(
                                textMeasurer = textMeasurer,
                                text = atom.symbol,
                                topLeft = atomRect.topLeft,
                                style = TextStyle(color = color)
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

private fun atomSymbolRect(textMeasurer: TextMeasurer, symbol: String, atomCenter: Offset): Rect {
    val measuredText = textMeasurer.measure(symbol)
    val boxSize = measuredText.size

    val halfWidth = boxSize.width / 2f
    val halfHeight = boxSize.height / 2f

    val left = atomCenter.x - halfWidth
    val top = atomCenter.y - halfHeight
    val right = atomCenter.x + halfWidth
    val bottom = atomCenter.y + halfHeight

    return Rect(
        left,
        top,
        right,
        bottom
    )
}


