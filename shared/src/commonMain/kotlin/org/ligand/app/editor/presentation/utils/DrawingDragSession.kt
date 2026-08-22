package org.ligand.app.editor.presentation.utils

import org.RDKit.Bond
import org.ligand.app.editor.domain.ArrowHandle
import org.ligand.app.editor.domain.ReactionArrowType
import org.ligand.app.editor.presentation.drawing_pane.DrawingDocument

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

data class ArrowDragSession(
    val baselineDocument: DrawingDocument,
    val arrowId: String? = null,
    val arrowType: ReactionArrowType = ReactionArrowType.FORWARD,
    val handle: ArrowHandle = ArrowHandle.END,
    val startX: Float,
    val startY: Float,
    var lastX: Float? = null,
    var lastY: Float? = null,
    var lastCurveBow: Float? = null
)