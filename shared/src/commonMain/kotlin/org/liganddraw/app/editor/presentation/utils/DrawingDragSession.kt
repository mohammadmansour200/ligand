package org.liganddraw.app.editor.presentation.utils

import org.RDKit.Bond
import org.liganddraw.app.editor.presentation.drawing_pane.DrawingDocument

data class BondDragSession(
    val baselineDocument: DrawingDocument,
    val moleculeIdx: Int,
    val pivotAtomIdx: Int,
    val atomSymbol: String,
    val bondType: Bond.BondType,
    val bondDir: Bond.BondDir = Bond.BondDir.NONE,
    var lastHit: Pair<Int, Int>? = null,
    var lastSnappedAngle: Double? = null
)

data class ChainDragSession(
    val baselineDocument: DrawingDocument,
    val moleculeIdx: Int?,
    val pivotAtomIdx: Int?,
    val originX: Double,
    val originY: Double,
    var lastAtomCount: Int? = null,
    var lastSnappedAngle: Double? = null
)