package org.liganddraw.app.editor.presentation.drawing_pane

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.fastForEachIndexed
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.extension
import io.github.vinceglb.filekit.readString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.RDKit.Bond
import org.liganddraw.app.core.domain.onSuccess
import org.liganddraw.app.editor.domain.CheminformaticsDataSource
import org.liganddraw.app.editor.domain.DrawingPaneConstants.BOND_HIT_TOLERANCE
import org.liganddraw.app.editor.domain.Tool
import org.liganddraw.app.editor.presentation.utils.getHydrogenLabelDimensions
import org.liganddraw.app.editor.presentation.utils.getSymbolLabelDimensions
import org.liganddraw.app.editor.presentation.utils.labelRect
import org.liganddraw.app.editor.presentation.utils.offsetPx
import org.liganddraw.app.editor.presentation.utils.toPositionAngstrom
import kotlin.io.path.absolutePathString
import kotlin.io.path.createTempFile
import kotlin.io.path.deleteIfExists
import kotlin.io.path.writeText
import kotlin.math.pow
import kotlin.math.sqrt

class DrawingPaneViewModel(private val cheminformaticsDataSource: CheminformaticsDataSource) :
    ViewModel() {
    private val _state = MutableStateFlow(DrawingPaneState())
    val state = _state.asStateFlow()

    fun onAction(action: DrawingPaneAction) {
        when (action) {
            is DrawingPaneAction.OnFilePick -> parseFile(action.file)
            is DrawingPaneAction.OnCacheLabelDimensions -> cacheLabelDimensions(
                action.uniqueSymbols,
                action.uniqueHydrogenCounts
            )

            is DrawingPaneAction.OnSelectTool -> selectTool(action.tool)
            is DrawingPaneAction.OnPointerMove -> handlePointerMove(action.x, action.y)
            is DrawingPaneAction.OnPointerPress -> handlePointerPress(action.x, action.y)
        }
    }

    private fun parseFile(file: PlatformFile) {
        viewModelScope.launch(Dispatchers.IO) {
            val content = file.readString()
            val extension = file.extension

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
        val hitAtomId = findAtomByPosition(x, y)
        if (hitAtomId != null) {
            handleAtomPress(hitAtomId.first, hitAtomId.second)
            return
        }

        val hitBondId = findBondByPosition(x, y)
        if (hitBondId != null) {
            handleBondPress(hitBondId.first, hitBondId.second)
            return
        }

        handleNullHit(x, y)
    }

    private fun handleAtomPress(moleculeIdx: Int, atomIdx: Int) {
        when (val currentTool = _state.value.selectedTool) {
            is Tool.StructureSelect -> handleStructureSelect(moleculeIdx)
            is Tool.Erase -> handleEraseAtom(moleculeIdx, atomIdx)
            is Tool.SingleBond -> handleAttachBondToAtom(
                moleculeIdx,
                atomIdx,
                Bond.BondType.SINGLE
            )

            is Tool.WedgeBond -> handleAttachBondToAtom(
                moleculeIdx,
                atomIdx,
                Bond.BondType.SINGLE,
                Bond.BondDir.BEGINWEDGE
            )

            is Tool.HashedWedgeBond -> handleAttachBondToAtom(
                moleculeIdx,
                atomIdx,
                Bond.BondType.SINGLE,
                Bond.BondDir.BEGINDASH
            )

            is Tool.Element -> {
                val selectedSymbol = currentTool.symbol
                handleReplaceAtomWithAtom(
                    moleculeIdx,
                    atomIdx,
                    selectedSymbol
                )
            }

            is Tool.Template -> {
                val smiles = currentTool.smiles
                handleReplaceAtomWithTemplate(
                    moleculeIdx,
                    atomIdx,
                    smiles
                )
            }

            else -> {}
        }
    }

    private fun handleBondPress(moleculeIdx: Int, bondIdx: Int) {
        when (val currentTool = _state.value.selectedTool) {
            is Tool.StructureSelect -> handleStructureSelect(moleculeIdx)
            is Tool.Erase -> handleEraseBond(moleculeIdx, bondIdx)
            is Tool.SingleBond -> handleCycleBondType(moleculeIdx, bondIdx)

            is Tool.WedgeBond -> {
                // TODO: set bond dir to BEGINWEDGE, or toggle/flip existing wedge
            }

            is Tool.HashedWedgeBond -> {
                // TODO: set bond dir to BEGINDASH
            }

            is Tool.Template -> {
                val smiles = currentTool.smiles
                handleFuseTemplateToBond(moleculeIdx, bondIdx, smiles)
            }

            else -> {}
        }
    }

    private fun handleNullHit(x: Float, y: Float) {
        if (_state.value.selectedMoleculeIndex != null) _state.update {
            it.copy(
                selectedMoleculeIndex = null
            )
        }

        val angstromPosition = Offset(x, y).toPositionAngstrom()
        when (val currentTool = _state.value.selectedTool) {
            is Tool.Element -> handleCreateMoleculeFromSmiles(
                currentTool.symbol,
                angstromPosition.first,
                angstromPosition.second
            )

            is Tool.Template -> handleCreateMoleculeFromSmiles(
                currentTool.smiles,
                angstromPosition.first,
                angstromPosition.second
            )

            else -> {}
        }
    }

    private fun handlePointerMove(x: Float, y: Float) {
        val hitAtomId = findAtomByPosition(x, y)
        val hitBondId = if (hitAtomId == null) findBondByPosition(x, y) else null

        if (state.value.hoveredAtomId != hitAtomId || state.value.hoveredBondId != hitBondId) {
            _state.update { it.copy(hoveredAtomId = hitAtomId, hoveredBondId = hitBondId) }
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

    private fun findBondByPosition(
        x: Float,
        y: Float,
    ): Pair<Int, Int>? {
        _state.value.molecules.fastForEachIndexed { molIndex, molecule ->
            molecule.bonds.fastForEachIndexed { bondIndex, bond ->
                val beginAtom = molecule.atoms[bond.beginAtomIndex.toInt()]
                val endAtom = molecule.atoms[bond.endAtomIndex.toInt()]

                if (isBondHit(
                        x, y, beginAtom.offsetPx(), endAtom.offsetPx(), BOND_HIT_TOLERANCE
                    )
                ) {
                    return Pair(molIndex, bondIndex)
                }
            }
        }
        return null
    }

    private fun isBondHit(
        px: Float, py: Float,
        a: Offset, b: Offset,
        width: Float
    ): Boolean {
        val dx = b.x - a.x
        val dy = b.y - a.y
        val bondLength = sqrt(dx.pow(2) + dy.pow(2))
        if (bondLength == 0f) return false

        // unit vector along the bond
        val ux = dx / bondLength
        val uy = dy / bondLength

        // vector from a to the point
        val vx = px - a.x
        val vy = py - a.y

        // distance along the bond axis (how far "down the line" the point projects)
        val along = vx * ux + vy * uy
        // distance perpendicular to the bond axis
        val across = vx * -uy + vy * ux

        return along in 0f..bondLength && across in -width / 2f..width / 2f
    }

    private fun handleAttachBondToAtom(
        moleculeIdx: Int,
        targetAtomIdx: Int,
        type: Bond.BondType,
        dir: Bond.BondDir = Bond.BondDir.NONE
    ) {
        val molecule = _state.value.molecules[moleculeIdx]
        viewModelScope.launch {
            cheminformaticsDataSource.attachBondToAtom(
                molecule,
                targetAtomIdx.toLong(),
                type,
                dir
            ).onSuccess { mol ->
                val editedMolecules = _state.value.molecules.toMutableList()
                editedMolecules[moleculeIdx] = mol
                _state.update { it.copy(molecules = editedMolecules) }
            }
        }
    }

    private fun handleCycleBondType(
        moleculeIdx: Int,
        targetBondIdx: Int,
    ) {
        val molecule = _state.value.molecules[moleculeIdx]
        viewModelScope.launch {
            cheminformaticsDataSource.cycleBondType(
                molecule,
                targetBondIdx.toLong(),
            ).onSuccess { mol ->
                val editedMolecules = _state.value.molecules.toMutableList()
                editedMolecules[moleculeIdx] = mol
                _state.update { it.copy(molecules = editedMolecules) }
            }
        }
    }

    private fun handleEraseBond(
        moleculeIdx: Int,
        targetBondIdx: Int,
    ) {
        val molecule = _state.value.molecules[moleculeIdx]
        viewModelScope.launch {
            cheminformaticsDataSource.eraseBond(
                molecule,
                targetBondIdx.toLong(),
            ).onSuccess { mol ->
                val editedMolecules = _state.value.molecules.toMutableList()

                if (mol.isEmpty()) editedMolecules.removeAt(moleculeIdx)
                else {
                    editedMolecules[moleculeIdx] = mol[0]
                    if (mol.size > 1) {
                        editedMolecules.addAll(moleculeIdx + 1, mol.drop(1))
                    }
                }

                _state.update { it.copy(molecules = editedMolecules) }
            }
        }
    }

    private fun handleEraseAtom(
        moleculeIdx: Int,
        targetAtomIdx: Int,
    ) {
        val molecule = _state.value.molecules[moleculeIdx]
        viewModelScope.launch {
            cheminformaticsDataSource.eraseAtom(
                molecule,
                targetAtomIdx.toLong(),
            ).onSuccess { mol ->
                val editedMolecules = _state.value.molecules.toMutableList()

                if (mol.isEmpty()) editedMolecules.removeAt(moleculeIdx)
                else {
                    editedMolecules[moleculeIdx] = mol[0]
                    if (mol.size > 1) {
                        editedMolecules.addAll(moleculeIdx + 1, mol.drop(1))
                    }
                }

                _state.update { it.copy(molecules = editedMolecules) }
            }
        }
    }

    private fun handleReplaceAtomWithAtom(
        moleculeIdx: Int,
        targetAtomIdx: Int,
        newAtomSymbol: String
    ) {
        val molecule = _state.value.molecules[moleculeIdx]
        viewModelScope.launch {
            cheminformaticsDataSource.replaceAtomWithAtom(
                molecule,
                targetAtomIdx.toLong(),
                newAtomSymbol,
            ).onSuccess { mol ->
                val editedMolecules = _state.value.molecules.toMutableList()
                editedMolecules[moleculeIdx] = mol
                _state.update { it.copy(molecules = editedMolecules) }
            }
        }
    }

    private fun handleReplaceAtomWithTemplate(
        moleculeIdx: Int,
        targetAtomIdx: Int,
        templateSmiles: String
    ) {
        val molecule = _state.value.molecules[moleculeIdx]
        viewModelScope.launch {
            cheminformaticsDataSource.replaceAtomWithTemplate(
                molecule,
                targetAtomIdx.toLong(),
                templateSmiles,
            ).onSuccess { mol ->
                val editedMolecules = _state.value.molecules.toMutableList()
                editedMolecules[moleculeIdx] = mol
                _state.update { it.copy(molecules = editedMolecules) }
            }
        }
    }

    private fun handleFuseTemplateToBond(
        moleculeIdx: Int,
        targetBondIdx: Int,
        templateSmiles: String
    ) {
        val molecule = _state.value.molecules[moleculeIdx]
        viewModelScope.launch {
            cheminformaticsDataSource.fuseTemplateToBond(
                molecule,
                targetBondIdx.toLong(),
                templateSmiles,
            ).onSuccess { mol ->
                val editedMolecules = _state.value.molecules.toMutableList()
                editedMolecules[moleculeIdx] = mol
                _state.update { it.copy(molecules = editedMolecules) }
            }
        }
    }

    private fun handleStructureSelect(moleculeIdx: Int) {
        _state.update { it.copy(selectedMoleculeIndex = moleculeIdx) }
    }

    private fun handleCreateMoleculeFromSmiles(
        smiles: String,
        xAngstrom: Double,
        yAngstrom: Double
    ) {
        viewModelScope.launch {
            val mol = cheminformaticsDataSource.createMoleculeFromSmiles(
                smiles,
                xAngstrom,
                yAngstrom,
            )

            val editedMolecules = _state.value.molecules.toMutableList()
            editedMolecules.add(mol)
            _state.update { it.copy(molecules = editedMolecules) }
        }
    }
}