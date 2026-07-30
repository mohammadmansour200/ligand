package org.liganddraw.app.editor.presentation.utils

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect

fun shortenBondToRectBoundary(
    atomCenter: Offset,
    labelAtomCenter: Offset,
    labelRect: Rect,
): Offset {
    val direction = labelAtomCenter - atomCenter
    val segmentLength = direction.getDistance()
    if (segmentLength == 0f) return labelAtomCenter

    var tEntry = 0f

    if (direction.x != 0f) {
        val tAtLeftEdge = (labelRect.left - atomCenter.x) / direction.x
        val tAtRightEdge = (labelRect.right - atomCenter.x) / direction.x
        tEntry = maxOf(tEntry, minOf(tAtLeftEdge, tAtRightEdge))
    } else if (atomCenter.x < labelRect.left || atomCenter.x > labelRect.right) {
        return labelAtomCenter
    }

    if (direction.y != 0f) {
        val tAtTopEdge = (labelRect.top - atomCenter.y) / direction.y
        val tAtBottomEdge = (labelRect.bottom - atomCenter.y) / direction.y
        tEntry = maxOf(tEntry, minOf(tAtTopEdge, tAtBottomEdge))
    } else if (atomCenter.y < labelRect.top || atomCenter.y > labelRect.bottom) {
        return labelAtomCenter
    }

    if (tEntry !in 0f..1f) return labelAtomCenter

    val boundaryPoint = atomCenter + direction * tEntry
    val unitDirection = direction / segmentLength
    return boundaryPoint - unitDirection
}