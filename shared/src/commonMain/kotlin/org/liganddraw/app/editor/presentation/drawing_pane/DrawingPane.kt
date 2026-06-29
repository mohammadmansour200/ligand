package org.liganddraw.app.editor.presentation.drawing_pane

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import liganddraw.shared.generated.resources.Res
import liganddraw.shared.generated.resources.drag_pan
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel
import org.liganddraw.app.editor.presentation.drawing_pane.components.DrawingPaneTopBar

@Composable
fun DrawingPaneRoot(viewModel: DrawingPaneViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    DrawingPane(state, viewModel::onAction)
}

const val scaleFactor = 60f

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DrawingPane(state: DrawingPaneState, onAction: (DrawingPaneAction) -> Unit) {


    Scaffold(
        topBar = {
            DrawingPaneTopBar(onAction = { onAction(it) })
        },
    ) {
        Row(modifier = Modifier.fillMaxSize().padding(it)) {
            // --- DUMMY TOOLBAR ---
            Column(modifier = Modifier.fillMaxHeight()) {
                IconButton(onClick = {}) {
                    Icon(vectorResource(Res.drawable.drag_pan), contentDescription = null)
                }
                IconButton(onClick = {}) {
                    Icon(vectorResource(Res.drawable.drag_pan), contentDescription = null)
                }
                IconButton(onClick = {}) {
                    Icon(vectorResource(Res.drawable.drag_pan), contentDescription = null)
                }
                IconButton(onClick = {}) {
                    Icon(vectorResource(Res.drawable.drag_pan), contentDescription = null)
                }
                IconButton(onClick = {}) {
                    Icon(vectorResource(Res.drawable.drag_pan), contentDescription = null)
                }
            }


            // --- DRAWING CANVAS ---
            val textMeasurer = rememberTextMeasurer()
            val color = MaterialTheme.colorScheme.inverseSurface
            val background = MaterialTheme.colorScheme.surfaceBright

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
                translate(size.width / 2f + offset.x, size.height / 2f + offset.y) {
                    scale(scale, pivot = Offset.Zero) {
                        state.molecules.forEach { mol ->
                            // Used to place molecule at canvas center
                            val molCenterXAngstrom =
                                (mol.atoms.maxOf { atom -> atom.x } + mol.atoms.minOf { atom -> atom.x }) / 2.0
                            val molCenterYAngstrom =
                                (mol.atoms.maxOf { atom -> atom.y } + mol.atoms.minOf { atom -> atom.y }) / 2.0

                            val molCenterXPx = molCenterXAngstrom * scaleFactor
                            val molCenterYPx = molCenterYAngstrom * scaleFactor

                            // --- DRAW BONDS ---
                            // To apply clipRect only on bonds not atom symbols
                            drawContext.canvas.save()
                            mol.bonds.forEach { bond ->
                                val beginAtom = mol.atoms[bond.beginAtomIndex.toInt()]
                                val endAtom = mol.atoms[bond.endAtomIndex.toInt()]

                                val beginAtomX =
                                    ((beginAtom.x * scaleFactor) - molCenterXPx).toFloat()
                                val beginAtomY =
                                    -((beginAtom.y * scaleFactor) - molCenterYPx).toFloat()
                                val beginAtomOffset = Offset(beginAtomX, beginAtomY)

                                val endAtomX = ((endAtom.x * scaleFactor) - molCenterXPx).toFloat()
                                val endAtomY = -((endAtom.y * scaleFactor) - molCenterYPx).toFloat()
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

                                // Single Bond drawing
                                drawLine(
                                    color = color,
                                    strokeWidth = 2f,
                                    start = beginAtomOffset,
                                    end = endAtomOffset
                                )
                            }
                            drawContext.canvas.restore()

                            // --- DRAW ATOM SYMBOL ---
                            mol.atoms.forEach { atom ->
                                if (atom.symbol != "C") {
                                    val atomX = ((atom.x * scaleFactor) - molCenterXPx).toFloat()
                                    val atomY = -((atom.y * scaleFactor) - molCenterYPx).toFloat()
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
    }
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


