package org.liganddraw.app.editor.presentation.utils

import androidx.compose.ui.geometry.Offset
import org.liganddraw.app.editor.domain.Atom
import org.liganddraw.app.editor.domain.DrawingPaneConstants.SCALE_FACTOR

/**
 * Converts Angstrom coordinates to screen-space pixels.
 * Y is flipped because in initial coordinates Y grows upward (center based), screen-space grows downward (top-left based).
 */
fun Atom.offsetPx(scaleFactor: Float = SCALE_FACTOR): Offset =
    Offset(
        x = (x * scaleFactor).toFloat(),
        y = -(y * scaleFactor).toFloat()
    )
