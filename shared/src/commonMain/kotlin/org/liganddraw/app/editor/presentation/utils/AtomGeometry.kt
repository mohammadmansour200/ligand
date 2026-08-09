package org.liganddraw.app.editor.presentation.utils

import androidx.compose.ui.geometry.Offset
import org.liganddraw.app.editor.domain.Atom
import org.liganddraw.app.editor.domain.DrawingPaneConstants.ROTATION_SNAP_DEGREES
import org.liganddraw.app.editor.domain.DrawingPaneConstants.SCALE_FACTOR
import kotlin.math.roundToLong

/**
 * Converts Angstrom coordinates to screen-space pixels.
 * Y is flipped because in initial coordinates Y grows upward (center based), screen-space grows downward (top-left based).
 */
fun Atom.offsetPx(scaleFactor: Float = SCALE_FACTOR): Offset =
    Offset(
        x = (x * scaleFactor).toFloat(),
        y = -(y * scaleFactor).toFloat()
    )


/**
 * Converts screen-space Pixels into Angstrom coordinates as a pair of Pair.first which is X, Pair.second which is Y
 */
fun Offset.toPositionAngstrom(scaleFactor: Float = SCALE_FACTOR): Pair<Double, Double> =
    Pair(
        (x / scaleFactor).toDouble(),
        (-(y / scaleFactor)).toDouble()
    )

/**
 * Snaps a raw angle to the nearest multiple of a specified snap interval.
 *
 * Used to constrain rotations to fixed directional increments (e.g., 15°, 30°, or 60° increments).
 *
 * @param angle The raw angle in radians (typically calculated via `atan2`).
 * @param snapInterval The angular step size in radians to snap toward (e.g., `Math.toRadians(ROTATION_SNAP_DEGREES)`).
 * @return The snapped angle in radians.
 */
fun snapAngle(
    angle: Double,
    snapInterval: Double = Math.toRadians(ROTATION_SNAP_DEGREES)
): Double =
    (angle / snapInterval).roundToLong() * snapInterval