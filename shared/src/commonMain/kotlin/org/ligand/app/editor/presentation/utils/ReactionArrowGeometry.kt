package org.ligand.app.editor.presentation.utils

import androidx.compose.ui.geometry.Offset
import org.ligand.app.editor.domain.ArrowHeadHalf
import org.ligand.app.editor.domain.ReactionArrow
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt


fun ReactionArrow.ElectronPushing.centerPosition(): Offset {
    val start = Offset(this.startX, this.startY)
    val end = Offset(this.endX, this.endY)
    if (this.curveBow == 0f) {
        return Offset((start.x + end.x) / 2f, (start.y + end.y) / 2f)
    }

    val mid = Offset((start.x + end.x) / 2f, (start.y + end.y) / 2f)
    val dx = end.x - start.x
    val dy = end.y - start.y
    val len = sqrt(dx * dx + dy * dy)
    val perpX = if (len == 0f) 0f else -dy / len
    val perpY = if (len == 0f) 0f else dx / len
    val control = Offset(mid.x + perpX * this.curveBow, mid.y + perpY * this.curveBow)
    // Point on the quadratic bezier at t=0.5
    return Offset(
        0.25f * start.x + 0.5f * control.x + 0.25f * end.x,
        0.25f * start.y + 0.5f * control.y + 0.25f * end.y
    )
}

/**
 * Determines which relative half ('left' or 'right' of the shaft's own travel direction)
 * points in the same direction as `outwardX, outwardY` (a vector pointing away from centerline).
 */
fun halfTowardOutward(
    angleRadians: Float,
    outwardX: Float,
    outwardY: Float
): ArrowHeadHalf {
    // "left" relative to travel direction points toward angleRadians - 90°.
    val leftDirX = cos(angleRadians - PI.toFloat() / 2f)
    val leftDirY = sin(angleRadians - PI.toFloat() / 2f)
    val dot = leftDirX * outwardX + leftDirY * outwardY
    return if (dot > 0f) ArrowHeadHalf.BOTTOM else ArrowHeadHalf.TOP
}

fun halfAwayFromBow(start: Offset, end: Offset, curveBow: Float): ArrowHeadHalf {
    if (curveBow == 0f) return ArrowHeadHalf.TOP

    val dx = end.x - start.x
    val dy = end.y - start.y
    val len = sqrt(dx * dx + dy * dy)
    if (len == 0f) return ArrowHeadHalf.TOP

    val perpX = -dy / len
    val perpY = dx / len

    val awayX = perpX * curveBow
    val awayY = perpY * curveBow

    val angle = atan2(dy, dx)
    return halfTowardOutward(angle, awayX, awayY)
}