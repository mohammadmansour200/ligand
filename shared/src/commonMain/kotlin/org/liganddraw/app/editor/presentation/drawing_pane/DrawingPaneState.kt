package org.liganddraw.app.editor.presentation.drawing_pane

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.IntSize
import org.liganddraw.app.editor.domain.Atom
import org.liganddraw.app.editor.domain.Molecule
import org.liganddraw.app.editor.domain.TextBox
import org.liganddraw.app.editor.domain.Tool

@Immutable
data class DrawingDocument(
    val molecules: List<Molecule> = emptyList(),
    val textBoxes: List<TextBox> = emptyList()
)

@Immutable
data class DrawingPaneState(
    val document: DrawingDocument = DrawingDocument(),
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val selectedTool: Tool = Tool.Pan,
    val symbolLabelDimensionsCache: Map<String, IntSize> = emptyMap(),
    val hydrogenLabelDimensionsCache: Map<Long, IntSize> = emptyMap(),
    /**Currently hovered atom as a pair of molecule index and atom index*/
    val hoveredAtomId: Pair<Int, Int>? = null,
    /**Currently hovered bond as a pair of molecule index and bond index*/
    val hoveredBondId: Pair<Int, Int>? = null,
    val selectedMoleculeIndex: Int? = null,
    val valenceViolationExplanationAtom: Atom? = null,
)
