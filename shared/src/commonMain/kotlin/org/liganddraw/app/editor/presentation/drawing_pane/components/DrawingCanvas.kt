package org.liganddraw.app.editor.presentation.drawing_pane.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.window.core.layout.WindowSizeClass
import org.liganddraw.app.editor.domain.Bond
import org.liganddraw.app.editor.domain.BondDir
import org.liganddraw.app.editor.domain.DoubleBondAlignment
import org.liganddraw.app.editor.domain.DrawingPaneConstants.BOND_HIT_TOLERANCE
import org.liganddraw.app.editor.domain.DrawingPaneConstants.BOND_LINES_SPACING
import org.liganddraw.app.editor.domain.DrawingPaneConstants.BOND_STROKE_WIDTH
import org.liganddraw.app.editor.domain.DrawingPaneConstants.CENTERED_DOUBLE_BOND_LINES_SPACING
import org.liganddraw.app.editor.domain.DrawingPaneConstants.HIGHLIGHT_CORNER_RADIUS
import org.liganddraw.app.editor.domain.DrawingPaneConstants.HIGHLIGHT_STROKE_WIDTH
import org.liganddraw.app.editor.domain.Molecule
import org.liganddraw.app.editor.domain.TextBox
import org.liganddraw.app.editor.domain.Tool
import org.liganddraw.app.editor.presentation.drawing_pane.DrawingPaneAction
import org.liganddraw.app.editor.presentation.drawing_pane.DrawingPaneState
import org.liganddraw.app.editor.presentation.utils.chargeLabel
import org.liganddraw.app.editor.presentation.utils.getAtomLabelLayout
import org.liganddraw.app.editor.presentation.utils.getHydrogenCountStyle
import org.liganddraw.app.editor.presentation.utils.getHydrogenLabelLayout
import org.liganddraw.app.editor.presentation.utils.getSymbolLabelLayout
import org.liganddraw.app.editor.presentation.utils.getSymbolStyle
import org.liganddraw.app.editor.presentation.utils.getTextBoxLayout
import org.liganddraw.app.editor.presentation.utils.offsetPx
import org.liganddraw.app.editor.presentation.utils.shortenBondToRectBoundary
import org.liganddraw.app.editor.presentation.utils.toAnnotatedString
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

val highlightStroke = Stroke(HIGHLIGHT_STROKE_WIDTH)
val highlightCornerRadius = CornerRadius(HIGHLIGHT_CORNER_RADIUS, HIGHLIGHT_CORNER_RADIUS)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DrawingCanvas(state: DrawingPaneState, onAction: (DrawingPaneAction) -> Unit) {
    val density = LocalDensity.current

    val textMeasurer = rememberTextMeasurer()

    // --- CANVAS THEME
    val structureColor = MaterialTheme.colorScheme.onSurface
    val symbolStyle = getSymbolStyle(density, structureColor)

    val structureErrorColor = MaterialTheme.colorScheme.error

    val highlightColor = MaterialTheme.colorScheme.secondary
    val selectionColor = MaterialTheme.colorScheme.secondary.copy(alpha = .8f)

    val background = MaterialTheme.colorScheme.surface

    // --- CANVAS ZOOMING AND PANNING ---
    val windowSizeClass = currentWindowAdaptiveInfoV2().windowSizeClass
    val isWindowCompact =
        !windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)

    var canvasScale by remember { mutableFloatStateOf(if (isWindowCompact) 2.5f else 1f) }
    var canvasOffset by remember { mutableStateOf(Offset.Zero) }

    val molecules = state.document.molecules
    val textBoxes = state.document.textBoxes

    // --- CACHE TEXT BOX LAYOUT ---
    LaunchedEffect(textBoxes) {
        val existing = state.textBoxLayoutCache
        val layouts = textBoxes.associate { box ->
            val annotated = box.toAnnotatedString(density = density)
            val cached = existing[box.id]
            box.id to if (cached != null && cached.layoutInput.text == annotated) {
                cached
            } else {
                textMeasurer.measure(annotated)
            }
        }
        onAction(DrawingPaneAction.OnCacheTextBoxLayouts(layouts))
    }

    // --- CACHE ATOM LABEL LAYOUT ---
    LaunchedEffect(molecules) {
        val symbolLayouts = mutableMapOf<String, TextLayoutResult>()
        val hydrogenCountLayouts = mutableMapOf<Long, TextLayoutResult>()

        molecules.forEach { mol ->
            mol.atoms.forEach { atom ->
                val symbol = atom.symbol
                symbolLayouts.getOrPut(symbol) {
                    textMeasurer.measure(text = symbol, style = symbolStyle)
                }

                val hydrogenCount = atom.numImplicitHydrogen
                if (hydrogenCount > 0) {
                    hydrogenCountLayouts.getOrPut(hydrogenCount) {
                        val hydrogenLabel = buildAnnotatedString {
                            append("H")
                            if (hydrogenCount > 1) {
                                pushStyle(getHydrogenCountStyle(density))
                                append(hydrogenCount.toString())
                                pop()
                            }
                        }
                        textMeasurer.measure(hydrogenLabel, symbolStyle)
                    }
                }
            }
        }

        onAction(DrawingPaneAction.OnCacheLabelLayouts(symbolLayouts, hydrogenCountLayouts))
    }

    // --- CACHE ATOM CHARGE LAYOUT ---
    val positiveChargeLayout = remember(symbolStyle) {
        textMeasurer.measure(chargeLabel(charge = 1, symbolStyle), symbolStyle)
    }
    val negativeChargeLayout = remember(symbolStyle) {
        textMeasurer.measure(chargeLabel(charge = -1, symbolStyle), symbolStyle)
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
            .transformable(
                state = rememberTransformableState { centroid, zoomChange, offsetChange, _ ->
                    val anchoredOffset = centroid - (centroid - canvasOffset) * zoomChange

                    canvasOffset = when (state.selectedTool) {
                        Tool.Pan -> anchoredOffset + offsetChange
                        else -> anchoredOffset
                    }
                    canvasScale *= zoomChange
                }
            ).pointerInput(state.selectedTool) {
                if (state.selectedTool == Tool.Pan) return@pointerInput
                detectTapGestures(
                    onLongPress = { offset ->
                        val x = (offset.x - canvasOffset.x) / canvasScale
                        val y = (offset.y - canvasOffset.y) / canvasScale
                        onAction(DrawingPaneAction.OnPointerLongPress(x, y))
                    },
                    onTap = { offset ->
                        val x = (offset.x - canvasOffset.x) / canvasScale
                        val y = (offset.y - canvasOffset.y) / canvasScale
                        onAction(
                            DrawingPaneAction.OnPointerPress(
                                x,
                                y
                            )
                        )
                    },
                )
            }
            .pointerInput(state.selectedTool) {
                if (state.selectedTool == Tool.Pan) return@pointerInput
                detectDragGestures(
                    onDragStart = { offset ->
                        val x = (offset.x - canvasOffset.x) / canvasScale
                        val y = (offset.y - canvasOffset.y) / canvasScale
                        onAction(DrawingPaneAction.OnDragStart(x, y))
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val x = (change.position.x - canvasOffset.x) / canvasScale
                        val y = (change.position.y - canvasOffset.y) / canvasScale
                        onAction(DrawingPaneAction.OnDrag(x, y))
                    },
                    onDragEnd = {
                        onAction(DrawingPaneAction.OnDragEnd)
                    }
                )
            }
            .pointerInput(state.selectedTool) {
                if (state.selectedTool == Tool.Pan) return@pointerInput
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val pointerPosition =
                            event.changes.firstOrNull()?.position ?: return@awaitPointerEventScope

                        val x = (pointerPosition.x - canvasOffset.x) / canvasScale
                        val y = (pointerPosition.y - canvasOffset.y) / canvasScale

                        // Pointer hovering (highlighting hovered atom/bond)
                        when (event.type) {
                            PointerEventType.Move -> onAction(DrawingPaneAction.OnPointerMove(x, y))
                        }
                    }
                }
            }
    ) {
        withTransform({
            translate(left = canvasOffset.x, top = canvasOffset.y)
            scale(scale = canvasScale, pivot = Offset.Zero)
        }) {
            // --- TEXT BOX --
            textBoxes.forEach { box ->
                val layout = getTextBoxLayout(box.id, state.textBoxLayoutCache) ?: return@forEach
                drawTextBox(box, layout)
            }

            // --- MOLECULE --
            molecules.fastForEachIndexed { moleculeIndex, mol ->
                if (state.selectedMoleculeIndex == moleculeIndex) {
                    drawMoleculeHighlightRect(mol, highlightColor)
                }

                // --- DRAW ATOM SYMBOL ---
                mol.atoms.fastForEachIndexed { atomIndex, atom ->
                    val atomId = Pair(moleculeIndex, atomIndex)

                    val symbolLayout =
                        getSymbolLabelLayout(atom.symbol, state.symbolLabelLayoutCache)
                    val hydrogenLayout = if (atom.isLabelVisible) getHydrogenLabelLayout(
                        atom.numImplicitHydrogen,
                        state.hydrogenLabelLayoutCache
                    ) else null

                    val labelLayout = getAtomLabelLayout(
                        symbolLayout = symbolLayout,
                        hydrogenLayout = hydrogenLayout,
                        isReversed = atom.isLabelReversed,
                        atomOffset = atom.offsetPx(),
                    )

                    val labelRect = labelLayout.boundingRect

                    if (atom.hasValenceViolation) drawAtomHighlightRect(
                        rect = labelRect,
                        color = structureErrorColor,
                        alpha = .4f,
                        style = Fill,
                    )

                    if (state.hoveredAtomId == atomId) drawAtomHighlightRect(
                        labelRect,
                        highlightColor,
                        highlightStroke,
                    )

                    if (state.selectedMoleculeIndex == moleculeIndex) drawAtomHighlightRect(
                        labelRect,
                        selectionColor,
                        Fill,
                    )

                    if (atom.charge != 0) {
                        val chargeLayout =
                            if (atom.charge > 0) positiveChargeLayout else negativeChargeLayout
                        drawChargeText(layoutResult = chargeLayout, labelRect = labelRect)
                    }

                    if (atom.isLabelVisible) {
                        symbolLayout?.let { layoutResult ->
                            drawText(
                                textLayoutResult = layoutResult,
                                topLeft = labelLayout.symbolTopLeft
                            )
                        }
                        hydrogenLayout?.let { layoutResult ->
                            drawText(
                                textLayoutResult = layoutResult,
                                topLeft = labelLayout.hydrogenTopLeft
                            )
                        }
                    }
                }

                // --- DRAW BONDS ---
                mol.bonds.fastForEachIndexed { bondIndex, bond ->
                    val beginAtom = mol.atoms[bond.beginAtomIndex.toInt()]
                    val endAtom = mol.atoms[bond.endAtomIndex.toInt()]

                    var beginAtomOffset = beginAtom.offsetPx()
                    var endAtomOffset = endAtom.offsetPx()

                    if (beginAtom.isLabelVisible) {
                        val symbolLayout =
                            getSymbolLabelLayout(beginAtom.symbol, state.symbolLabelLayoutCache)
                        val hydrogenLayout = getHydrogenLabelLayout(
                            beginAtom.numImplicitHydrogen,
                            state.hydrogenLabelLayoutCache
                        )

                        val beginAtomLabelRect = getAtomLabelLayout(
                            symbolLayout = symbolLayout,
                            hydrogenLayout = hydrogenLayout,
                            isReversed = beginAtom.isLabelReversed,
                            atomOffset = beginAtom.offsetPx(),
                        ).boundingRect

                        beginAtomOffset = shortenBondToRectBoundary(
                            endAtomOffset,
                            beginAtomOffset,
                            beginAtomLabelRect
                        )
                    }

                    if (endAtom.isLabelVisible) {
                        val symbolLayout =
                            getSymbolLabelLayout(endAtom.symbol, state.symbolLabelLayoutCache)
                        val hydrogenLayout = getHydrogenLabelLayout(
                            endAtom.numImplicitHydrogen,
                            state.hydrogenLabelLayoutCache
                        )

                        val endAtomLabelRect = getAtomLabelLayout(
                            symbolLayout = symbolLayout,
                            hydrogenLayout = hydrogenLayout,
                            isReversed = endAtom.isLabelReversed,
                            atomOffset = endAtom.offsetPx(),
                        ).boundingRect

                        endAtomOffset =
                            shortenBondToRectBoundary(
                                beginAtomOffset,
                                endAtomOffset,
                                endAtomLabelRect
                            )
                    }

                    val bondId = Pair(moleculeIndex, bondIndex)
                    if (state.hoveredBondId == bondId) drawBondHighlightRect(
                        endAtomOffset,
                        beginAtomOffset,
                        highlightColor,
                        highlightStroke
                    )

                    if (state.selectedMoleculeIndex == moleculeIndex) drawBondHighlightRect(
                        endAtomOffset,
                        beginAtomOffset,
                        selectionColor,
                        Fill
                    )

                    val bondColor =
                        if (beginAtom.hasValenceViolation || endAtom.hasValenceViolation) structureErrorColor else structureColor
                    when (bond) {
                        is Bond.Single -> {
                            when (bond.direction) {
                                BondDir.NONE -> drawLine(
                                    color = bondColor,
                                    strokeWidth = BOND_STROKE_WIDTH,
                                    start = beginAtomOffset,
                                    end = endAtomOffset
                                )

                                BondDir.BEGINWEDGE -> drawWedgeBond(
                                    start = beginAtomOffset,
                                    end = endAtomOffset,
                                    strokeWidth = BOND_STROKE_WIDTH,
                                    color = bondColor
                                )

                                BondDir.BEGINDASH -> drawDashBond(
                                    start = beginAtomOffset,
                                    end = endAtomOffset,
                                    strokeWidth = BOND_STROKE_WIDTH,
                                    color = bondColor
                                )
                            }
                        }

                        is Bond.Hydrogen -> drawHydrogenBond(
                            start = beginAtomOffset,
                            end = endAtomOffset,
                            strokeWidth = BOND_STROKE_WIDTH,
                            color = bondColor
                        )

                        is Bond.Triple -> drawTripleBond(
                            start = beginAtomOffset,
                            end = endAtomOffset,
                            strokeWidth = BOND_STROKE_WIDTH,
                            color = bondColor
                        )

                        is Bond.Double -> {
                            if (bond.alignment == DoubleBondAlignment.CENTERED)
                                drawCenteredDoubleBond(
                                    start = beginAtomOffset,
                                    end = endAtomOffset,
                                    strokeWidth = BOND_STROKE_WIDTH,
                                    color = bondColor
                                ) else {
                                val side =
                                    if (bond.alignment == DoubleBondAlignment.POSITIVE) 1f else -1f
                                drawAsymmetricDoubleBond(
                                    start = beginAtomOffset,
                                    end = endAtomOffset,
                                    strokeWidth = BOND_STROKE_WIDTH,
                                    color = bondColor,
                                    spacing = BOND_LINES_SPACING * side
                                )
                            }
                        }

                        is Bond.Ionic -> {}
                    }

                }
            }
        }
    }
}

fun DrawScope.drawTextBox(
    textBox: TextBox,
    layoutResult: TextLayoutResult,
) {
    val centeredTopLeft = Offset(
        x = textBox.anchorX - layoutResult.size.width / 2f,
        y = textBox.anchorY - layoutResult.size.height / 2f,
    )

    drawText(
        textLayoutResult = layoutResult,
        topLeft = centeredTopLeft,
    )
}

private fun DrawScope.drawChargeText(
    layoutResult: TextLayoutResult,
    labelRect: Rect,
) {
    val x = labelRect.right
    val y = labelRect.center.y - labelRect.height
    val offset = Offset(x, y)
    drawText(
        textLayoutResult = layoutResult,
        topLeft = offset,
    )
}

private fun DrawScope.drawAtomHighlightRect(
    rect: Rect,
    color: Color,
    style: DrawStyle,
    alpha: Float = 1f,
    horizontalPaddingPx: Float = 4f
) {
    val paddedTopLeft = Offset(rect.left - horizontalPaddingPx, rect.top)
    val paddedBottomRight = Offset(rect.right + horizontalPaddingPx, rect.bottom)

    val highlightRect = Rect(paddedTopLeft, paddedBottomRight)
    drawRoundRect(
        color = color,
        topLeft = highlightRect.topLeft,
        size = highlightRect.size,
        alpha = alpha,
        style = style,
        cornerRadius = highlightCornerRadius
    )
}

private fun DrawScope.drawMoleculeHighlightRect(
    molecule: Molecule,
    color: Color,
    paddingPx: Float = 16f
) {
    val atoms = molecule.atoms
    if (atoms.isEmpty()) return

    var minX = Float.MAX_VALUE
    var minY = Float.MAX_VALUE
    var maxX = -Float.MAX_VALUE
    var maxY = -Float.MAX_VALUE

    for (atom in atoms) {
        val atomOffsetPx = atom.offsetPx()
        val x = atomOffsetPx.x
        val y = atomOffsetPx.y
        if (x < minX) minX = x
        if (y < minY) minY = y
        if (x > maxX) maxX = x
        if (y > maxY) maxY = y
    }

    // Apply padding by expanding the bounding box outward
    val paddedTopLeft = Offset(minX - paddingPx, minY - paddingPx)
    val paddedBottomRight = Offset(maxX + paddingPx, maxY + paddingPx)

    val highlightRect = Rect(paddedTopLeft, paddedBottomRight)

    drawRoundRect(
        color = color,
        topLeft = highlightRect.topLeft,
        size = highlightRect.size,
        style = highlightStroke,
        cornerRadius = highlightCornerRadius
    )
}

private fun DrawScope.drawBondHighlightRect(
    endAtomOffset: Offset,
    beginAtomOffset: Offset,
    color: Color,
    style: DrawStyle
) {
    val dx = endAtomOffset.x - beginAtomOffset.x
    val dy = endAtomOffset.y - beginAtomOffset.y
    val bondLength = sqrt(dx * dx + dy * dy)
    val angleDegrees = atan2(dy, dx) * (180f / PI.toFloat())

    val rectHeight = BOND_HIT_TOLERANCE
    val midpoint = Offset(
        x = (beginAtomOffset.x + endAtomOffset.x) / 2f,
        y = (beginAtomOffset.y + endAtomOffset.y) / 2f
    )

    rotate(degrees = angleDegrees, pivot = midpoint) {
        drawRoundRect(
            color = color,
            topLeft = Offset(
                x = midpoint.x - bondLength / 2f,
                y = midpoint.y - rectHeight / 2f
            ),
            size = Size(width = bondLength, height = rectHeight),
            style = style,
            cornerRadius = highlightCornerRadius
        )
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
    spacing: Float = BOND_LINES_SPACING
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
        offsetDistance = spacing,
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
    spacing: Float = CENTERED_DOUBLE_BOND_LINES_SPACING
) {
    val (positiveSideStart, positiveSideEnd) = offsetLine(
        start = start,
        end = end,
        offsetDistance = spacing,
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
        offsetDistance = -spacing,
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
    spacing: Float = BOND_LINES_SPACING
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
        offsetDistance = spacing
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
        offsetDistance = -spacing
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


