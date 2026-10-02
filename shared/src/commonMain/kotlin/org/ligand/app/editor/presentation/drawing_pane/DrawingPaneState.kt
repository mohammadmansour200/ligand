package org.ligand.app.editor.presentation.drawing_pane

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.IntSize
import org.ligand.app.editor.domain.Atom
import org.ligand.app.editor.domain.Molecule
import org.ligand.app.editor.domain.ReactionArrow
import org.ligand.app.editor.domain.TextBox
import org.ligand.app.editor.domain.Tool

@Immutable
data class DrawingDocument(
    val molecules: List<Molecule> = emptyList(),
    val textBoxes: List<TextBox> = emptyList(),
    val reactionArrows: List<ReactionArrow> = emptyList()
)

@Immutable
data class DrawingPaneState(
    val canvasSize: IntSize = IntSize.Zero,
    val canvasOffset: Offset = Offset.Zero,
    val canvasScale: Float = 1f,
    val document: DrawingDocument = DrawingDocument(),
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val selectedTool: Tool = Tool.Pan,
    val symbolLabelLayoutCache: Map<String, TextLayoutResult> = emptyMap(),
    val hydrogenLabelLayoutCache: Map<Long, TextLayoutResult> = emptyMap(),
    val textBoxLayoutCache: Map<String, TextLayoutResult> = emptyMap(),
    /**Currently hovered atom as a pair of molecule index and atom index*/
    val hoveredAtomId: Pair<Int, Int>? = null,
    /**Currently hovered bond as a pair of molecule index and bond index*/
    val hoveredBondId: Pair<Int, Int>? = null,
    val hoveredArrowId: String? = null,
    val followedMoleculeIndex: Int = 0,
    val followedMoleculeHasPreviousMolecule: Boolean = false,
    val followedMoleculeHasNextMolecule: Boolean = false,
    val valenceViolationExplanationAtom: Atom? = null,
)
