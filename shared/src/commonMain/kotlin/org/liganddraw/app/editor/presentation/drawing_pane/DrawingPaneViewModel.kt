package org.liganddraw.app.editor.presentation.drawing_pane

import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.fastForEachIndexed
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.RDKit.Bond
import org.liganddraw.app.core.domain.onSuccess
import org.liganddraw.app.editor.domain.CheminformaticsDataSource
import org.liganddraw.app.editor.domain.Tool
import org.liganddraw.app.editor.presentation.utils.getHydrogenLabelDimensions
import org.liganddraw.app.editor.presentation.utils.getSymbolLabelDimensions
import org.liganddraw.app.editor.presentation.utils.labelRect
import org.liganddraw.app.editor.presentation.utils.offsetPx
import kotlin.io.path.absolutePathString
import kotlin.io.path.createTempFile
import kotlin.io.path.deleteIfExists
import kotlin.io.path.writeText

class DrawingPaneViewModel(private val cheminformaticsDataSource: CheminformaticsDataSource) :
    ViewModel() {
    private val _state = MutableStateFlow(DrawingPaneState())
    val state = _state.asStateFlow()

    fun onAction(action: DrawingPaneAction) {
        when (action) {
            is DrawingPaneAction.OnFilePick -> parseFile(action.content, action.extension)
            is DrawingPaneAction.OnCacheLabelDimensions -> cacheLabelDimensions(
                action.uniqueSymbols,
                action.uniqueHydrogenCounts
            )

            is DrawingPaneAction.OnSelectTool -> selectTool(action.tool)
            is DrawingPaneAction.OnPointerMove -> handlePointerMove(action.x, action.y)
            is DrawingPaneAction.OnPointerPress -> handlePointerPress(action.x, action.y)
        }
    }

    private fun parseFile(content: String, extension: String) {
        viewModelScope.launch {
            // Create temporary file
            val tempFile = createTempFile()
            tempFile.writeText(content)

            // Parse file to Molecule
            val parseFile = when (extension.lowercase()) {
                "sdf" -> cheminformaticsDataSource.sdfFileToMolecule(tempFile.absolutePathString())
                else -> cheminformaticsDataSource.molFileToMolecule(tempFile.absolutePathString())
            }

            // Add molecule to existing molecules list
            val existingMols = _state.value.molecules
            parseFile.onSuccess { mols ->
                _state.update { it.copy(molecules = existingMols + mols) }
            }

            // Delete temporary file
            tempFile.deleteIfExists()
        }
    }

    private fun cacheLabelDimensions(
        symbols: Map<String, IntSize>,
        hydrogens: Map<Long, IntSize>
    ) {
        _state.update {
            it.copy(
                symbolLabelDimensionsCache = symbols,
                hydrogenLabelDimensionsCache = hydrogens
            )
        }
    }

    private fun selectTool(tool: Tool) {
        _state.update { it.copy(selectedTool = tool) }
    }

    private fun handlePointerPress(x: Float, y: Float) {
        // TODO(Handle null hit)
        // TODO(Handle bond hit)
        val hitAtomId = findAtomByPosition(x, y) ?: return
        val moleculeIdx = hitAtomId.first
        val atomIdx = hitAtomId.second

        when (val currentTool = _state.value.selectedTool) {
            is Tool.SingleBond -> addBond(
                moleculeIdx,
                atomIdx,
                Bond.BondType.SINGLE
            )

            is Tool.WedgeBond -> addBond(
                moleculeIdx,
                atomIdx,
                Bond.BondType.SINGLE,
                Bond.BondDir.BEGINWEDGE
            )

            is Tool.HashedWedgeBond -> addBond(
                moleculeIdx,
                atomIdx,
                Bond.BondType.SINGLE,
                Bond.BondDir.BEGINDASH
            )

            is Tool.Element -> {
                val selectedSymbol = currentTool.symbol
                replaceAtom(
                    moleculeIdx,
                    atomIdx,
                    selectedSymbol
                )
            }

            else -> {}
        }
    }

    private fun handlePointerMove(x: Float, y: Float) {
        // TODO(Handle bond hit)
        val hitAtomId = findAtomByPosition(x, y)

        if (state.value.hoveredAtomId != hitAtomId) {
            _state.update { it.copy(hoveredAtomId = hitAtomId) }
        }
    }

    private fun findAtomByPosition(x: Float, y: Float): Pair<Int, Int>? {
        _state.value.molecules.fastForEachIndexed { molIndex, molecule ->
            molecule.atoms.fastForEachIndexed { atomIndex, atom ->
                val rect = labelRect(
                    symbolDimensions = getSymbolLabelDimensions(
                        atom.symbol,
                        _state.value.symbolLabelDimensionsCache
                    ),
                    hydrogenDimensions = if (!atom.isLabelVisible) IntSize.Zero else getHydrogenLabelDimensions(
                        atom.numImplicitHydrogen, _state.value.hydrogenLabelDimensionsCache
                    ),
                    isReversed = atom.isLabelReversed,
                    atomOffset = atom.offsetPx()
                )

                if (x in rect.left..rect.right && y in rect.top..rect.bottom) {
                    return Pair(molIndex, atomIndex)
                }
            }
        }
        return null
    }

    private fun addBond(
        moleculeIdx: Int,
        atomIdx: Int,
        type: Bond.BondType,
        dir: Bond.BondDir = Bond.BondDir.NONE
    ) {
        val molecule = _state.value.molecules[moleculeIdx]
        viewModelScope.launch {
            cheminformaticsDataSource.addBond(
                molecule,
                atomIdx.toLong(),
                type,
                dir
            ).onSuccess { mol ->
                val editedMolecules = _state.value.molecules.toMutableList()
                editedMolecules[moleculeIdx] = mol
                _state.update { it.copy(molecules = editedMolecules) }
            }
        }
    }

    private fun replaceAtom(
        moleculeIdx: Int,
        atomIdx: Int,
        newSymbol: String
    ) {
        val molecule = _state.value.molecules[moleculeIdx]
        viewModelScope.launch {
            cheminformaticsDataSource.replaceAtom(
                molecule,
                atomIdx.toLong(),
                newSymbol,
            ).onSuccess { mol ->
                val editedMolecules = _state.value.molecules.toMutableList()
                editedMolecules[moleculeIdx] = mol
                _state.update { it.copy(molecules = editedMolecules) }
            }
        }
    }
}