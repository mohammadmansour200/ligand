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
import org.liganddraw.app.editor.domain.DrawingPaneConstants.BOND_LENGTH
import org.liganddraw.app.editor.domain.Molecule
import org.liganddraw.app.editor.domain.Tool
import org.liganddraw.app.editor.presentation.utils.BondDragSession
import org.liganddraw.app.editor.presentation.utils.ChainDragSession
import org.liganddraw.app.editor.presentation.utils.UndoRedoStack
import org.liganddraw.app.editor.presentation.utils.getHydrogenLabelDimensions
import org.liganddraw.app.editor.presentation.utils.getSymbolLabelDimensions
import org.liganddraw.app.editor.presentation.utils.labelRect
import org.liganddraw.app.editor.presentation.utils.offsetPx
import org.liganddraw.app.editor.presentation.utils.snapAngle
import org.liganddraw.app.editor.presentation.utils.toPositionAngstrom
import kotlin.io.path.absolutePathString
import kotlin.io.path.createTempFile
import kotlin.io.path.deleteIfExists
import kotlin.io.path.writeText
import kotlin.math.atan2
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

class DrawingPaneViewModel(private val cheminformaticsDataSource: CheminformaticsDataSource) :
    ViewModel() {
    private val history = UndoRedoStack(DrawingDocument())
    private val _state = MutableStateFlow(DrawingPaneState())
    val state = _state.asStateFlow()

    fun onAction(action: DrawingPaneAction) {
        when (action) {
            is DrawingPaneAction.OnRedo -> handleRedo()
            is DrawingPaneAction.OnUndo -> handleUndo()
            is DrawingPaneAction.OnFilePick -> parseFile(action.file)
            is DrawingPaneAction.OnCacheLabelDimensions -> cacheLabelDimensions(
                action.uniqueSymbols,
                action.uniqueHydrogenCounts
            )

            is DrawingPaneAction.OnSelectTool -> selectTool(action.tool)
            is DrawingPaneAction.OnPointerMove -> handlePointerMove(action.x, action.y)
            is DrawingPaneAction.OnPointerPress -> handlePointerPress(action.x, action.y)
            is DrawingPaneAction.OnPointerLongPress -> handlePointerLongPress(action.x, action.y)
            is DrawingPaneAction.OnDragStart -> handleDragStart(action.x, action.y)
            is DrawingPaneAction.OnDrag -> handleDrag(action.x, action.y)
            is DrawingPaneAction.OnDragEnd -> handleDragEnd()
            is DrawingPaneAction.OnDismissValenceViolationDialog -> handleValenceViolationDialogDismiss()
        }
    }

    private fun handleUndo() {
        history.undo()?.let { doc ->
            _state.update {
                it.copy(
                    document = doc,
                    canUndo = history.canUndo,
                    canRedo = history.canRedo,
                    hoveredAtomId = null,
                    hoveredBondId = null,
                    selectedMoleculeIndex = null
                )
            }
        }
    }

    private fun handleRedo() {
        history.redo()?.let { doc ->
            _state.update {
                it.copy(
                    document = doc,
                    canUndo = history.canUndo,
                    canRedo = history.canRedo,
                    hoveredAtomId = null,
                    hoveredBondId = null,
                    selectedMoleculeIndex = null
                )
            }
        }
    }

    private fun commitEdit(newDocument: DrawingDocument) {
        history.push(newDocument)
        _state.update {
            it.copy(document = newDocument, canUndo = history.canUndo, canRedo = history.canRedo)
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
            val existingMolecules = _state.value.document.molecules
            parseFile.onSuccess { newMolecules ->
                commitEdit(_state.value.document.copy(molecules = existingMolecules + newMolecules))
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
        _state.update {
            it.copy(
                selectedTool = tool,
                hoveredAtomId = null,
                hoveredBondId = null,
                selectedMoleculeIndex = null,
            )
        }
    }

    private var bondDragSession: BondDragSession? = null
    private var chainDragSession: ChainDragSession? = null
    private var dragMoveSequence = 0L
    private fun handleDragStart(x: Float, y: Float) {
        when (_state.value.selectedTool) {
            is Tool.SingleBond, is Tool.WedgeBond, is Tool.HashedWedgeBond,
            is Tool.DoubleBond, is Tool.TripleBond, is Tool.HydrogenBond, is Tool.Element ->
                handleBondDragStart(x, y)

            is Tool.Chain -> handleChainDragStart(x, y)

            is Tool.Erase -> handleDragErase(x, y)

            else -> {}
        }
    }

    private fun handleDrag(x: Float, y: Float) {
        when (_state.value.selectedTool) {
            is Tool.SingleBond, is Tool.WedgeBond, is Tool.HashedWedgeBond,
            is Tool.DoubleBond, is Tool.TripleBond, is Tool.HydrogenBond, is Tool.Element -> handleBondDrag(
                x,
                y
            )

            is Tool.Chain -> handleChainDrag(x, y)

            is Tool.Erase -> handleDragErase(x, y)

            else -> {}
        }
    }

    private fun handleDragEnd() {
        if (bondDragSession != null || chainDragSession != null) {
            commitEdit(_state.value.document)
        }
        bondDragSession = null
        chainDragSession = null
    }

    private fun handleBondDragStart(x: Float, y: Float) {
        val hitAtomId = findAtomByPosition(x, y) ?: return
        val (moleculeIdx, atomIdx) = hitAtomId

        val (symbol, bondType, bondDir) = when (val tool = _state.value.selectedTool) {
            is Tool.SingleBond -> Triple("C", Bond.BondType.SINGLE, Bond.BondDir.NONE)
            is Tool.WedgeBond -> Triple("C", Bond.BondType.SINGLE, Bond.BondDir.BEGINWEDGE)
            is Tool.HashedWedgeBond -> Triple("C", Bond.BondType.SINGLE, Bond.BondDir.BEGINDASH)
            is Tool.DoubleBond -> Triple("C", Bond.BondType.DOUBLE, Bond.BondDir.NONE)
            is Tool.TripleBond -> Triple("C", Bond.BondType.TRIPLE, Bond.BondDir.NONE)
            is Tool.HydrogenBond -> Triple("C", Bond.BondType.HYDROGEN, Bond.BondDir.NONE)
            is Tool.Element -> Triple(tool.symbol, Bond.BondType.SINGLE, Bond.BondDir.NONE)
            else -> return
        }

        bondDragSession = BondDragSession(
            baselineDocument = _state.value.document,
            moleculeIdx = moleculeIdx,
            pivotAtomIdx = atomIdx,
            atomSymbol = symbol,
            bondType = bondType,
            bondDir = bondDir
        )
        handleBondDrag(x, y)
    }

    private fun handleBondDrag(x: Float, y: Float) {
        val session = bondDragSession ?: return
        val currentSequence = ++dragMoveSequence

        viewModelScope.launch {
            val baselineMolecule = session.baselineDocument.molecules[session.moleculeIdx]
            val pivotAtom = baselineMolecule.atoms[session.pivotAtomIdx]

            val hit = findAtomByPosition(x, y, document = session.baselineDocument)
                ?.takeIf { !(it.first == session.moleculeIdx && it.second == session.pivotAtomIdx) }

            // --- BOND ATOM WITH ATOM ---
            val newDocument = if (hit != null) {
                val (hitMoleculeIdx, hitAtomIdx) = hit

                // --- BOND ATOMS WITHIN SAME MOLECULE ---
                if (hitMoleculeIdx == session.moleculeIdx) {
                    val bonded = cheminformaticsDataSource.bondSameMoleculeAtoms(
                        molecule = baselineMolecule,
                        atomIdxA = session.pivotAtomIdx.toLong(),
                        atomIdxB = hitAtomIdx.toLong(),
                        type = session.bondType,
                        dir = session.bondDir
                    )
                    replaceMolecule(session.baselineDocument, session.moleculeIdx, bonded)
                } else {
                    // --- BOND ATOMS ACROSS DIFFERENT MOLECULES ---
                    val otherMolecule = session.baselineDocument.molecules[hitMoleculeIdx]
                    val merged = cheminformaticsDataSource.bondDifferentMoleculesAtoms(
                        moleculeA = baselineMolecule, atomIdxA = session.pivotAtomIdx.toLong(),
                        moleculeB = otherMolecule, atomIdxB = hitAtomIdx.toLong(),
                        type = session.bondType, dir = session.bondDir
                    )

                    replaceTwoMoleculesWithMerged(
                        session.baselineDocument,
                        session.moleculeIdx,
                        hitMoleculeIdx,
                        merged
                    )
                }
            } else {
                // --- ATTACH NEW ATOM AT ANGLE ---
                val angstromPosition = Offset(x, y).toPositionAngstrom()
                val rawAngle = atan2(
                    angstromPosition.second - pivotAtom.y,
                    angstromPosition.first - pivotAtom.x
                )
                val snappedAngle = snapAngle(rawAngle)
                val withNewAtom = cheminformaticsDataSource.attachAtomToAtomAtAngle(
                    molecule = baselineMolecule,
                    targetAtomIdx = session.pivotAtomIdx.toLong(),
                    newAtomSymbol = session.atomSymbol,
                    type = session.bondType,
                    dir = session.bondDir,
                    angleRadians = snappedAngle
                )
                replaceMolecule(session.baselineDocument, session.moleculeIdx, withNewAtom)
            }

            if (currentSequence == dragMoveSequence) {
                _state.update { it.copy(document = newDocument) }
            }
        }
    }

    private fun handleChainDragStart(x: Float, y: Float) {
        val hit = findAtomByPosition(x, y)
        val (originX, originY) = if (hit != null) {
            val (moleculeIdx, atomIdx) = hit
            val atom = _state.value.document.molecules[moleculeIdx].atoms[atomIdx]
            atom.x to atom.y
        } else {
            Offset(x, y).toPositionAngstrom()
        }

        chainDragSession = ChainDragSession(
            baselineDocument = _state.value.document,
            moleculeIdx = hit?.first,
            pivotAtomIdx = hit?.second,
            originX = originX,
            originY = originY
        )
        handleChainDrag(x, y)
    }

    private fun handleChainDrag(x: Float, y: Float) {
        val session = chainDragSession ?: return
        val thisSequence = ++dragMoveSequence

        viewModelScope.launch {
            val angstromPosition = Offset(x, y).toPositionAngstrom()
            val dx = angstromPosition.first - session.originX
            val dy = angstromPosition.second - session.originY
            val distance = sqrt(dx * dx + dy * dy)

            val atomCount = (distance / BOND_LENGTH).roundToInt().coerceAtLeast(0)

            val rawAngle = atan2(
                dy, dx
            )
            val snappedAngle = snapAngle(rawAngle)

            val newDocument = when {
                atomCount == 0 -> session.baselineDocument

                session.moleculeIdx != null && session.pivotAtomIdx != null -> {
                    // --- EXTEND CHAIN FROM EXISTING ATOM ---
                    val baselineMolecule = session.baselineDocument.molecules[session.moleculeIdx]
                    val chained = cheminformaticsDataSource.buildChainFromAtom(
                        molecule = baselineMolecule,
                        pivotAtomIdx = session.pivotAtomIdx.toLong(),
                        atomCount = atomCount,
                        angleRadians = snappedAngle
                    )
                    replaceMolecule(session.baselineDocument, session.moleculeIdx, chained)
                }

                else -> {
                    // --- CREATE NEW CHAIN MOLECULE FROM POINT ---
                    val newMolecule = cheminformaticsDataSource.buildChainFromPoint(
                        x = session.originX,
                        y = session.originY,
                        atomCount = atomCount,
                        angleRadians = snappedAngle
                    )
                    val edited = session.baselineDocument.molecules.toMutableList()
                    edited.add(newMolecule)
                    session.baselineDocument.copy(molecules = edited)
                }
            }

            if (thisSequence == dragMoveSequence) {
                _state.update { it.copy(document = newDocument) }
            }
        }
    }

    private fun handleDragErase(x: Float, y: Float) {
        val hitAtomId = findAtomByPosition(x, y)
        if (hitAtomId != null) {
            handleEraseAtom(hitAtomId.first, hitAtomId.second)
            return
        }

        val hitBondId = findBondByPosition(x, y)
        if (hitBondId != null) {
            handleEraseBond(hitBondId.first, hitBondId.second)
            return
        }
    }

    private fun handlePointerLongPress(x: Float, y: Float) {
        val hitAtomId = findAtomByPosition(x, y)
        if (hitAtomId != null) {
            handleAtomLongPress(hitAtomId.first, hitAtomId.second)
        }
    }

    private fun handleAtomLongPress(moleculeIdx: Int, atomIdx: Int) {
        val atom = _state.value.document.molecules[moleculeIdx].atoms[atomIdx]
        if (atom.hasValenceViolation) {
            _state.update { it.copy(valenceViolationExplanationAtom = atom) }
        }
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

            is Tool.DoubleBond -> handleAttachBondToAtom(
                moleculeIdx,
                atomIdx,
                Bond.BondType.DOUBLE
            )

            is Tool.TripleBond -> handleAttachBondToAtom(
                moleculeIdx,
                atomIdx,
                Bond.BondType.TRIPLE
            )

            is Tool.HydrogenBond -> handleAttachBondToAtom(
                moleculeIdx,
                atomIdx,
                Bond.BondType.HYDROGEN
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

            is Tool.Plus -> handleChangeFormalCharge(moleculeIdx, atomIdx, +1)
            is Tool.Minus -> handleChangeFormalCharge(moleculeIdx, atomIdx, -1)
            else -> {}
        }
    }

    private fun handleBondPress(moleculeIdx: Int, bondIdx: Int) {
        when (val currentTool = _state.value.selectedTool) {
            is Tool.StructureSelect -> handleStructureSelect(moleculeIdx)
            is Tool.Erase -> handleEraseBond(moleculeIdx, bondIdx)
            is Tool.SingleBond -> handleCycleBondType(moleculeIdx, bondIdx)

            is Tool.WedgeBond -> handleSetBondType(
                moleculeIdx,
                bondIdx,
                Bond.BondType.SINGLE,
                Bond.BondDir.BEGINWEDGE
            )

            is Tool.HashedWedgeBond -> handleSetBondType(
                moleculeIdx,
                bondIdx,
                Bond.BondType.SINGLE,
                Bond.BondDir.BEGINDASH
            )

            is Tool.DoubleBond -> handleSetBondType(moleculeIdx, bondIdx, Bond.BondType.DOUBLE)
            is Tool.TripleBond -> handleSetBondType(moleculeIdx, bondIdx, Bond.BondType.TRIPLE)
            is Tool.HydrogenBond -> handleSetBondType(moleculeIdx, bondIdx, Bond.BondType.HYDROGEN)

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
            is Tool.Element -> handleCreateMoleculeFromAtom(
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

    private fun findAtomByPosition(
        x: Float,
        y: Float,
        document: DrawingDocument = _state.value.document
    ): Pair<Int, Int>? {
        document.molecules.fastForEachIndexed { moleculeIndex, molecule ->
            molecule.atoms.fastForEachIndexed { atomIndex, atom ->
                val rect = labelRect(
                    symbolDimensions = getSymbolLabelDimensions(
                        atom.symbol,
                        _state.value.symbolLabelDimensionsCache
                    ),
                    hydrogenDimensions = if (!atom.isLabelVisible) IntSize.Zero else getHydrogenLabelDimensions(
                        atom.numImplicitHydrogen,
                        _state.value.hydrogenLabelDimensionsCache
                    ),
                    isReversed = atom.isLabelReversed,
                    atomOffset = atom.offsetPx()
                )
                if (x in rect.left..rect.right && y in rect.top..rect.bottom) return Pair(
                    moleculeIndex,
                    atomIndex
                )
            }
        }
        return null
    }

    private fun findBondByPosition(
        x: Float,
        y: Float,
    ): Pair<Int, Int>? {
        _state.value.document.molecules.fastForEachIndexed { molIndex, molecule ->
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
        val targetMolecule = _state.value.document.molecules[moleculeIdx]
        viewModelScope.launch {
            val newMolecule = cheminformaticsDataSource.attachBondToAtom(
                targetMolecule,
                targetAtomIdx.toLong(),
                type,
                dir
            )

            val editedMolecules = _state.value.document.molecules.toMutableList()
            editedMolecules[moleculeIdx] = newMolecule
            commitEdit(_state.value.document.copy(molecules = editedMolecules))
        }
    }

    private fun handleCycleBondType(
        moleculeIdx: Int,
        targetBondIdx: Int,
    ) {
        val targetMolecule = _state.value.document.molecules[moleculeIdx]
        viewModelScope.launch {
            val newMolecule = cheminformaticsDataSource.cycleBondType(
                targetMolecule,
                targetBondIdx.toLong(),
            )

            val editedMolecules = _state.value.document.molecules.toMutableList()
            editedMolecules[moleculeIdx] = newMolecule
            commitEdit(_state.value.document.copy(molecules = editedMolecules))
        }
    }

    private fun handleSetBondType(
        moleculeIdx: Int,
        targetBondIdx: Int,
        type: Bond.BondType,
        dir: Bond.BondDir = Bond.BondDir.NONE
    ) {
        val targetMolecule = _state.value.document.molecules[moleculeIdx]
        viewModelScope.launch {
            val newMolecule = cheminformaticsDataSource.setBondType(
                targetMolecule,
                targetBondIdx.toLong(),
                type,
                dir
            )
            val editedMolecules = _state.value.document.molecules.toMutableList()
            editedMolecules[moleculeIdx] = newMolecule
            commitEdit(_state.value.document.copy(molecules = editedMolecules))
        }
    }

    private fun handleChangeFormalCharge(
        moleculeIdx: Int,
        targetAtomIdx: Int,
        delta: Int,
    ) {
        val targetMolecule = _state.value.document.molecules[moleculeIdx]
        viewModelScope.launch {
            val newMolecule = cheminformaticsDataSource.changeFormalCharge(
                molecule = targetMolecule,
                targetAtomIdx = targetAtomIdx.toLong(),
                delta = delta,
            )

            val editedMolecules = _state.value.document.molecules.toMutableList().apply {
                this[moleculeIdx] = newMolecule
            }
            commitEdit(_state.value.document.copy(molecules = editedMolecules))
        }
    }

    private fun handleEraseBond(
        moleculeIdx: Int,
        targetBondIdx: Int,
    ) {
        val targetMolecule = _state.value.document.molecules[moleculeIdx]
        viewModelScope.launch {
            val newMolecules = cheminformaticsDataSource.eraseBond(
                targetMolecule,
                targetBondIdx.toLong(),
            )

            val editedMolecules = _state.value.document.molecules.toMutableList()

            if (newMolecules.isEmpty()) editedMolecules.removeAt(moleculeIdx)
            else {
                editedMolecules[moleculeIdx] = newMolecules[0]
                if (newMolecules.size > 1) {
                    editedMolecules.addAll(moleculeIdx + 1, newMolecules.drop(1))
                }
            }

            commitEdit(_state.value.document.copy(molecules = editedMolecules))
        }
    }

    private fun handleEraseAtom(
        moleculeIdx: Int,
        targetAtomIdx: Int,
    ) {
        val targetMolecule = _state.value.document.molecules[moleculeIdx]
        viewModelScope.launch {
            val newMolecules = cheminformaticsDataSource.eraseAtom(
                targetMolecule,
                targetAtomIdx.toLong(),
            )

            val editedMolecules = _state.value.document.molecules.toMutableList()

            if (newMolecules.isEmpty()) editedMolecules.removeAt(moleculeIdx)
            else {
                editedMolecules[moleculeIdx] = newMolecules[0]
                if (newMolecules.size > 1) {
                    editedMolecules.addAll(moleculeIdx + 1, newMolecules.drop(1))
                }
            }

            commitEdit(_state.value.document.copy(molecules = editedMolecules))
        }
    }

    private fun handleReplaceAtomWithAtom(
        moleculeIdx: Int,
        targetAtomIdx: Int,
        newAtomSymbol: String
    ) {
        val targetMolecule = _state.value.document.molecules[moleculeIdx]
        viewModelScope.launch {
            val newMolecule = cheminformaticsDataSource.replaceAtomWithAtom(
                targetMolecule,
                targetAtomIdx.toLong(),
                newAtomSymbol,
            )

            val editedMolecules = _state.value.document.molecules.toMutableList()
            editedMolecules[moleculeIdx] = newMolecule
            commitEdit(_state.value.document.copy(molecules = editedMolecules))
        }
    }

    private fun handleReplaceAtomWithTemplate(
        moleculeIdx: Int,
        targetAtomIdx: Int,
        templateSmiles: String
    ) {
        val targetMolecule = _state.value.document.molecules[moleculeIdx]
        viewModelScope.launch {
            val newMolecule = cheminformaticsDataSource.replaceAtomWithTemplate(
                targetMolecule,
                targetAtomIdx.toLong(),
                templateSmiles,
            )

            val editedMolecules = _state.value.document.molecules.toMutableList()
            editedMolecules[moleculeIdx] = newMolecule
            commitEdit(_state.value.document.copy(molecules = editedMolecules))
        }
    }

    private fun handleFuseTemplateToBond(
        moleculeIdx: Int,
        targetBondIdx: Int,
        templateSmiles: String
    ) {
        val targetMolecule = _state.value.document.molecules[moleculeIdx]
        viewModelScope.launch {
            val newMolecule = cheminformaticsDataSource.fuseTemplateToBond(
                targetMolecule,
                targetBondIdx.toLong(),
                templateSmiles,
            )

            val editedMolecules = _state.value.document.molecules.toMutableList()
            editedMolecules[moleculeIdx] = newMolecule
            commitEdit(_state.value.document.copy(molecules = editedMolecules))
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
            val newMolecule = cheminformaticsDataSource.createMoleculeFromSmiles(
                smiles,
                xAngstrom,
                yAngstrom,
            )

            val editedMolecules = _state.value.document.molecules.toMutableList()
            editedMolecules.add(newMolecule)
            commitEdit(_state.value.document.copy(molecules = editedMolecules))
        }
    }

    private fun handleCreateMoleculeFromAtom(
        symbol: String,
        xAngstrom: Double,
        yAngstrom: Double
    ) {
        viewModelScope.launch {
            val newMolecule = cheminformaticsDataSource.createMoleculeFromAtom(
                symbol,
                xAngstrom,
                yAngstrom,
            )

            val editedMolecules = _state.value.document.molecules.toMutableList()
            editedMolecules.add(newMolecule)
            commitEdit(_state.value.document.copy(molecules = editedMolecules))
        }
    }

    private fun handleValenceViolationDialogDismiss() {
        _state.update { it.copy(valenceViolationExplanationAtom = null) }
    }

    private fun replaceTwoMoleculesWithMerged(
        document: DrawingDocument,
        idxA: Int,
        idxB: Int,
        merged: Molecule
    ): DrawingDocument {
        val edited = document.molecules.toMutableList()
        val (lo, hi) = listOf(idxA, idxB).sorted()
        edited.removeAt(hi)
        edited.removeAt(lo)
        edited.add(merged)
        return document.copy(molecules = edited)
    }

    private fun replaceMolecule(
        document: DrawingDocument,
        idx: Int,
        newMolecule: Molecule
    ): DrawingDocument {
        val edited = document.molecules.toMutableList()
        edited[idx] = newMolecule
        return document.copy(molecules = edited)
    }
}