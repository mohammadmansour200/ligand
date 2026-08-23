package org.ligand.app.editor.presentation.drawing_pane

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.extension
import io.github.vinceglb.filekit.readString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.RDKit.Bond
import org.ligand.app.core.domain.onSuccess
import org.ligand.app.editor.domain.ArrowHandle
import org.ligand.app.editor.domain.ArrowHeadShape
import org.ligand.app.editor.domain.CheminformaticsDataSource
import org.ligand.app.editor.domain.DrawingPaneConstants.ARROW_HIT_TOLERANCE
import org.ligand.app.editor.domain.DrawingPaneConstants.ATOM_HIT_TOLERANCE
import org.ligand.app.editor.domain.DrawingPaneConstants.BOND_HIT_TOLERANCE
import org.ligand.app.editor.domain.DrawingPaneConstants.BOND_LENGTH
import org.ligand.app.editor.domain.Molecule
import org.ligand.app.editor.domain.ReactionArrow
import org.ligand.app.editor.domain.ReactionArrow.Equilibrium
import org.ligand.app.editor.domain.ReactionArrow.Forward
import org.ligand.app.editor.domain.ReactionArrow.Resonance
import org.ligand.app.editor.domain.ReactionArrowType
import org.ligand.app.editor.domain.Tool
import org.ligand.app.editor.presentation.utils.ArrowDragSession
import org.ligand.app.editor.presentation.utils.BondDragSession
import org.ligand.app.editor.presentation.utils.ChainDragSession
import org.ligand.app.editor.presentation.utils.UndoRedoStack
import org.ligand.app.editor.presentation.utils.centerPosition
import org.ligand.app.editor.presentation.utils.getAtomLabelLayout
import org.ligand.app.editor.presentation.utils.getHydrogenLabelLayout
import org.ligand.app.editor.presentation.utils.getSymbolLabelLayout
import org.ligand.app.editor.presentation.utils.offsetPx
import org.ligand.app.editor.presentation.utils.snapAngle
import org.ligand.app.editor.presentation.utils.toPositionAngstrom
import kotlin.io.path.absolutePathString
import kotlin.io.path.createTempFile
import kotlin.io.path.deleteIfExists
import kotlin.io.path.writeText
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.uuid.Uuid

class DrawingPaneViewModel(private val cheminformaticsDataSource: CheminformaticsDataSource) :
    ViewModel() {
    private val history = UndoRedoStack(DrawingDocument())
    private val _state = MutableStateFlow(DrawingPaneState())
    val state = _state.asStateFlow()

    private val _events = Channel<DrawingPaneEvent>()
    val events = _events.receiveAsFlow()

    fun onAction(action: DrawingPaneAction) {
        when (action) {
            is DrawingPaneAction.OnRedo -> handleRedo()
            is DrawingPaneAction.OnUndo -> handleUndo()
            is DrawingPaneAction.OnFilePick -> parseFile(action.file)
            is DrawingPaneAction.OnCacheLabelLayouts -> cacheAtomLabelLayouts(
                action.symbolLayouts,
                action.hydrogenLayouts
            )

            is DrawingPaneAction.OnCacheTextBoxLayouts -> handleCacheTextBoxLayouts(
                action.layouts,
            )

            is DrawingPaneAction.OnSelectTool -> selectTool(action.tool)
            is DrawingPaneAction.OnSelectPreviousMolecule -> handleSelectPreviousMolecule()
            is DrawingPaneAction.OnSelectNextMolecule -> handleSelectNextMolecule()
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
            val followedMoleculeIdx = _state.value.followedMoleculeIndex
                .takeIf { it in doc.molecules.indices } ?: 0
            val followedMolecule = doc.molecules.getOrNull(followedMoleculeIdx)
            viewModelScope.launch {
                _events.send(DrawingPaneEvent.CalculateMolecule3DAndProperties(followedMolecule))
            }

            _state.update { current ->
                current.copy(
                    document = doc,
                    canUndo = history.canUndo,
                    canRedo = history.canRedo,
                    hoveredAtomId = null,
                    hoveredBondId = null,
                    followedMoleculeIndex = followedMoleculeIdx,
                    followedMoleculeHasPreviousMolecule = followedMoleculeIdx > 0,
                    followedMoleculeHasNextMolecule = followedMoleculeIdx < doc.molecules.lastIndex
                )
            }
        }
    }

    private fun handleRedo() {
        history.redo()?.let { doc ->
            val followedMoleculeIdx = _state.value.followedMoleculeIndex
                .takeIf { it in doc.molecules.indices } ?: 0
            val followedMolecule = doc.molecules.getOrNull(followedMoleculeIdx)
            viewModelScope.launch {
                _events.send(DrawingPaneEvent.CalculateMolecule3DAndProperties(followedMolecule))
            }

            _state.update { current ->
                current.copy(
                    document = doc,
                    canUndo = history.canUndo,
                    canRedo = history.canRedo,
                    hoveredAtomId = null,
                    hoveredBondId = null,
                    followedMoleculeIndex = followedMoleculeIdx,
                    followedMoleculeHasPreviousMolecule = followedMoleculeIdx > 0,
                    followedMoleculeHasNextMolecule = followedMoleculeIdx < doc.molecules.lastIndex
                )
            }
        }
    }

    private fun commitEdit(newDocument: DrawingDocument) {
        val followedMoleculeIdx = _state.value.followedMoleculeIndex
            .takeIf { it in newDocument.molecules.indices } ?: 0
        val followedMolecule = newDocument.molecules.getOrNull(followedMoleculeIdx)
        viewModelScope.launch {
            _events.send(DrawingPaneEvent.CalculateMolecule3DAndProperties(followedMolecule))
        }

        history.push(newDocument)
        _state.update {
            it.copy(
                document = newDocument,
                canUndo = history.canUndo,
                canRedo = history.canRedo,
                followedMoleculeIndex = followedMoleculeIdx,
                followedMoleculeHasPreviousMolecule = followedMoleculeIdx > 0,
                followedMoleculeHasNextMolecule = followedMoleculeIdx < newDocument.molecules.lastIndex
            )
        }
    }

    private fun handleSelectNextMolecule() {
        val document = _state.value.document
        val nextIndex = _state.value.followedMoleculeIndex + 1
        if (nextIndex !in document.molecules.indices) return

        val molecule = document.molecules.getOrNull(nextIndex)
        viewModelScope.launch {
            _events.send(DrawingPaneEvent.CalculateMolecule3DAndProperties(molecule))
        }

        _state.update {
            it.copy(
                followedMoleculeIndex = nextIndex,
                followedMoleculeHasPreviousMolecule = nextIndex > 0,
                followedMoleculeHasNextMolecule = nextIndex < document.molecules.lastIndex
            )
        }
    }

    private fun handleSelectPreviousMolecule() {
        val document = _state.value.document
        val previousIndex = _state.value.followedMoleculeIndex - 1
        if (previousIndex !in document.molecules.indices) return

        val molecule = document.molecules.getOrNull(previousIndex)
        viewModelScope.launch {
            _events.send(DrawingPaneEvent.CalculateMolecule3DAndProperties(molecule))
        }

        _state.update {
            it.copy(
                followedMoleculeIndex = previousIndex,
                followedMoleculeHasPreviousMolecule = previousIndex > 0,
                followedMoleculeHasNextMolecule = previousIndex < document.molecules.lastIndex
            )
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

    private fun cacheAtomLabelLayouts(
        symbolLayouts: Map<String, TextLayoutResult>,
        hydrogenLayouts: Map<Long, TextLayoutResult>
    ) {
        _state.update {
            it.copy(
                symbolLabelLayoutCache = symbolLayouts,
                hydrogenLabelLayoutCache = hydrogenLayouts
            )
        }
    }

    private fun handleCacheTextBoxLayouts(layouts: Map<String, TextLayoutResult>) {
        _state.update { it.copy(textBoxLayoutCache = layouts) }
    }

    private fun selectTool(tool: Tool) {
        _state.update {
            it.copy(
                selectedTool = tool,
                hoveredAtomId = null,
                hoveredBondId = null,
            )
        }
    }

    private var bondDragSession: BondDragSession? = null
    private var chainDragSession: ChainDragSession? = null
    private var arrowDragSession: ArrowDragSession? = null
    private var dragMoveSequence = 0L

    private fun handleDragStart(x: Float, y: Float) {
        when (_state.value.selectedTool) {
            is Tool.SingleBond, is Tool.WedgeBond, is Tool.HashedWedgeBond,
            is Tool.DoubleBond, is Tool.TripleBond, is Tool.HydrogenBond, is Tool.Element ->
                handleBondDragStart(x, y)

            is Tool.Chain -> handleChainDragStart(x, y)
            is Tool.ForwardArrow -> handleArrowDragStart(x, y, ReactionArrowType.FORWARD)
            is Tool.ResonanceArrow -> handleArrowDragStart(x, y, ReactionArrowType.RESONANCE)
            is Tool.EquilibriumArrow -> handleArrowDragStart(x, y, ReactionArrowType.EQUILIBRIUM)
            is Tool.ElectronPairPushingArrow -> handleArrowDragStart(
                x,
                y,
                ReactionArrowType.ELECTRON_PAIR_PUSHING
            )

            is Tool.SingleElectronPushingArrow -> handleArrowDragStart(
                x,
                y,
                ReactionArrowType.SINGLE_ELECTRON_PUSHING
            )

            is Tool.Erase -> handleDragErase(x, y)
            else -> {}
        }
    }

    private fun handleDrag(x: Float, y: Float) {
        when (_state.value.selectedTool) {
            is Tool.SingleBond, is Tool.WedgeBond, is Tool.HashedWedgeBond,
            is Tool.DoubleBond, is Tool.TripleBond, is Tool.HydrogenBond, is Tool.Element ->
                handleBondDrag(x, y)

            is Tool.Chain -> handleChainDrag(x, y)
            is Tool.ForwardArrow, Tool.ResonanceArrow, Tool.EquilibriumArrow, Tool.ElectronPairPushingArrow, Tool.SingleElectronPushingArrow -> handleArrowDrag(
                x,
                y
            )

            is Tool.Erase -> handleDragErase(x, y)
            else -> {}
        }
    }

    private fun handleDragEnd() {
        if (bondDragSession != null || chainDragSession != null || arrowDragSession != null) {
            commitEdit(_state.value.document)
        }
        bondDragSession = null
        chainDragSession = null
        arrowDragSession = null
    }

    private fun handleArrowDragStart(x: Float, y: Float, arrowType: ReactionArrowType) {
        val arrow = findReactionArrowByPosition(
            x = x,
            y = y,
        )

        if (arrow == null) {
            arrowDragSession = ArrowDragSession(
                baselineDocument = _state.value.document,
                arrowId = null,
                arrowType = arrowType,
                handle = ArrowHandle.END,
                startX = x,
                startY = y
            )
        } else {
            val start = Offset(arrow.startX, arrow.startY)
            val end = Offset(arrow.endX, arrow.endY)

            val handle = when {
                isNearHandle(x, y, start) -> ArrowHandle.START
                isNearHandle(x, y, end) -> ArrowHandle.END
                arrow is ReactionArrow.ElectronPushing && isNearHandle(
                    x,
                    y,
                    arrow.centerPosition()
                ) -> ArrowHandle.CURVE

                else -> null
            }
            if (handle != null) {
                arrowDragSession = ArrowDragSession(
                    baselineDocument = _state.value.document,
                    arrowId = arrow.id,
                    arrowType = arrowType,
                    handle = handle,
                    startX = arrow.startX,
                    startY = arrow.startY
                )
            }
        }
    }

    private fun isNearHandle(
        x: Float,
        y: Float,
        handlePos: Offset,
        tolerance: Float = ARROW_HIT_TOLERANCE
    ): Boolean {
        val dx = x - handlePos.x
        val dy = y - handlePos.y
        return sqrt(dx * dx + dy * dy) <= tolerance
    }

    private fun handleArrowDrag(x: Float, y: Float) {
        val session = arrowDragSession ?: return

        if (session.arrowId == null) {
            // --- Drawing a new arrow ---
            val id = Uuid.random().toString()

            val dx = x - session.startX
            val dy = y - session.startY
            val distance = sqrt(dx * dx + dy * dy)
            val rawAngle = atan2(dy, dx)
            val snappedAngle = snapAngle(rawAngle.toDouble()).toFloat()

            val endX = session.startX + distance * cos(snappedAngle)
            val endY = session.startY + distance * sin(snappedAngle)

            if (endX == session.lastX && endY == session.lastY) return
            session.lastX = endX
            session.lastY = endY

            val newArrow = when (session.arrowType) {
                ReactionArrowType.FORWARD -> Forward(
                    id = id,
                    startX = session.startX, startY = session.startY,
                    endX = endX, endY = endY
                )

                ReactionArrowType.RESONANCE -> Resonance(
                    id = id,
                    startX = session.startX, startY = session.startY,
                    endX = endX, endY = endY
                )

                ReactionArrowType.EQUILIBRIUM -> Equilibrium(
                    id = id,
                    startX = session.startX, startY = session.startY,
                    endX = endX, endY = endY
                )

                ReactionArrowType.ELECTRON_PAIR_PUSHING -> ReactionArrow.ElectronPushing(
                    id = id,
                    startX = session.startX,
                    startY = session.startY,
                    endX = endX,
                    endY = endY,
                )

                ReactionArrowType.SINGLE_ELECTRON_PUSHING -> ReactionArrow.ElectronPushing(
                    id = id,
                    startX = session.startX,
                    startY = session.startY,
                    endX = endX,
                    endY = endY,
                    headShape = ArrowHeadShape.HALF
                )
            }

            val newDocument = session.baselineDocument.copy(
                reactionArrows = session.baselineDocument.reactionArrows + newArrow
            )
            _state.update { it.copy(document = newDocument) }
            return
        }

        // --- Editing an existing arrow's handle ---
        val arrows = session.baselineDocument.reactionArrows
        val arrow = arrows.find { it.id == session.arrowId } ?: return

        val newArrow = when (session.handle) {
            ArrowHandle.START -> {
                if (x == session.lastX && y == session.lastY) return
                session.lastX = x
                session.lastY = y
                arrow.withPositions(startX = x, startY = y, endX = arrow.endX, endY = arrow.endY)
            }

            ArrowHandle.END -> {
                if (x == session.lastX && y == session.lastY) return
                session.lastX = x
                session.lastY = y
                arrow.withPositions(
                    startX = arrow.startX,
                    startY = arrow.startY,
                    endX = x,
                    endY = y
                )
            }

            ArrowHandle.CURVE -> {
                if (arrow !is ReactionArrow.ElectronPushing) return

                val dx = arrow.endX - arrow.startX
                val dy = arrow.endY - arrow.startY
                val len = sqrt(dx * dx + dy * dy)
                if (len == 0f) return
                val perpX = -dy / len
                val perpY = dx / len
                val midX = (arrow.startX + arrow.endX) / 2f
                val midY = (arrow.startY + arrow.endY) / 2f
                val newBow = ((x - midX) * perpX + (y - midY) * perpY) * 2f

                if (newBow == session.lastCurveBow) return
                session.lastCurveBow = newBow

                arrow.copy(curveBow = newBow)
            }
        }

        val newArrows = arrows.toMutableList().map {
            if (it.id == arrow.id) newArrow else it
        }
        _state.update { it.copy(document = session.baselineDocument.copy(reactionArrows = newArrows)) }
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

        val hit = findAtomByPosition(x, y, document = session.baselineDocument)
            ?.takeIf { !(it.first == session.moleculeIdx && it.second == session.pivotAtomIdx) }

        val snappedAngle = if (hit == null) {
            val baselineMolecule = session.baselineDocument.molecules[session.moleculeIdx]
            val pivotAtom = baselineMolecule.atoms[session.pivotAtomIdx]
            val angstromPosition = Offset(x, y).toPositionAngstrom()
            val rawAngle = atan2(
                angstromPosition.second - pivotAtom.y,
                angstromPosition.first - pivotAtom.x
            )
            snapAngle(rawAngle)
        } else {
            null
        }

        if (hit == session.lastHit && snappedAngle == session.lastSnappedAngle) {
            return
        }

        session.lastHit = hit
        session.lastSnappedAngle = snappedAngle

        val currentSequence = ++dragMoveSequence

        viewModelScope.launch {
            val baselineMolecule = session.baselineDocument.molecules[session.moleculeIdx]

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
                val withNewAtom = cheminformaticsDataSource.attachAtomToAtomAtAngle(
                    molecule = baselineMolecule,
                    targetAtomIdx = session.pivotAtomIdx.toLong(),
                    newAtomSymbol = session.atomSymbol,
                    type = session.bondType,
                    dir = session.bondDir,
                    angleRadians = snappedAngle!!
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

        val angstromPosition = Offset(x, y).toPositionAngstrom()
        val dx = angstromPosition.first - session.originX
        val dy = angstromPosition.second - session.originY
        val distance = sqrt(dx * dx + dy * dy)

        val atomCount = (distance / BOND_LENGTH).roundToInt().coerceAtLeast(0)
        val rawAngle = atan2(dy, dx)
        val snappedAngle = snapAngle(rawAngle)

        if (atomCount == session.lastAtomCount && snappedAngle == session.lastSnappedAngle) {
            return
        }

        session.lastAtomCount = atomCount
        session.lastSnappedAngle = snappedAngle

        val thisSequence = ++dragMoveSequence

        viewModelScope.launch {
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
            is Tool.StructureSelect -> {}
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
            is Tool.StructureSelect -> {}
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

            is Tool.SingleBond -> handleCreateMoleculeFromEthane(
                bondType = Bond.BondType.SINGLE,
                xAngstrom = angstromPosition.first,
                yAngstrom = angstromPosition.second
            )

            is Tool.WedgeBond -> handleCreateMoleculeFromEthane(
                bondType = Bond.BondType.SINGLE,
                bondDir = Bond.BondDir.BEGINWEDGE,
                xAngstrom = angstromPosition.first,
                yAngstrom = angstromPosition.second
            )

            is Tool.HashedWedgeBond -> handleCreateMoleculeFromEthane(
                bondType = Bond.BondType.SINGLE,
                bondDir = Bond.BondDir.BEGINDASH,
                xAngstrom = angstromPosition.first,
                yAngstrom = angstromPosition.second
            )

            is Tool.DoubleBond -> handleCreateMoleculeFromEthane(
                bondType = Bond.BondType.DOUBLE,
                xAngstrom = angstromPosition.first,
                yAngstrom = angstromPosition.second
            )

            is Tool.TripleBond -> handleCreateMoleculeFromEthane(
                bondType = Bond.BondType.TRIPLE,
                xAngstrom = angstromPosition.first,
                yAngstrom = angstromPosition.second
            )

            else -> {}
        }
    }

    private fun handlePointerMove(x: Float, y: Float) {
        val hitArrowId = findReactionArrowByPosition(x, y)?.id
        val hitAtomId = if (hitArrowId == null) findAtomByPosition(x, y) else null
        val hitBondId = if (hitAtomId == null) findBondByPosition(x, y) else null
        if (state.value.hoveredAtomId != hitAtomId ||
            state.value.hoveredBondId != hitBondId ||
            state.value.hoveredArrowId != hitArrowId
        ) {
            _state.update {
                it.copy(
                    hoveredAtomId = hitAtomId,
                    hoveredBondId = hitBondId,
                    hoveredArrowId = hitArrowId
                )
            }
        }
    }

    private fun findAtomByPosition(
        x: Float,
        y: Float,
        document: DrawingDocument = _state.value.document
    ): Pair<Int, Int>? {
        document.molecules.fastForEachIndexed { moleculeIndex, molecule ->
            molecule.atoms.fastForEachIndexed { atomIndex, atom ->
                val symbolLayout =
                    getSymbolLabelLayout(atom.symbol, _state.value.symbolLabelLayoutCache)
                val hydrogenLayout = if (atom.isLabelVisible) getHydrogenLabelLayout(
                    atom.numImplicitHydrogen,
                    _state.value.hydrogenLabelLayoutCache
                ) else null

                val rect = getAtomLabelLayout(
                    symbolLayout = symbolLayout,
                    hydrogenLayout = hydrogenLayout,
                    isReversed = atom.isLabelReversed,
                    atomOffset = atom.offsetPx(),
                ).boundingRect


                val isWithinBounds =
                    x in (rect.left - ATOM_HIT_TOLERANCE)..(rect.right + ATOM_HIT_TOLERANCE) &&
                            y in (rect.top - ATOM_HIT_TOLERANCE)..(rect.bottom + ATOM_HIT_TOLERANCE)

                if (isWithinBounds) {
                    return Pair(
                        moleculeIndex,
                        atomIndex
                    )
                }
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

    private fun findReactionArrowByPosition(x: Float, y: Float): ReactionArrow? {
        _state.value.document.reactionArrows.fastForEach { arrow ->
            if (isArrowHit(x, y, arrow)) {
                return arrow
            }
        }
        return null
    }

    private fun isArrowHit(
        px: Float, py: Float,
        arrow: ReactionArrow,
        tolerance: Float = ARROW_HIT_TOLERANCE
    ): Boolean {
        return when (arrow) {
            is ReactionArrow.ElectronPushing -> {
                if (arrow.curveBow == 0f) {
                    isBondHit(
                        px,
                        py,
                        Offset(arrow.startX, arrow.startY),
                        Offset(arrow.endX, arrow.endY),
                        tolerance
                    )
                } else {
                    isCurveHit(px, py, arrow, tolerance)
                }
            }

            else -> isBondHit(
                px,
                py,
                Offset(arrow.startX, arrow.startY),
                Offset(arrow.endX, arrow.endY),
                tolerance
            )
        }
    }

    /** Approximates the curve as line segments and hit-tests each, since it's a quadratic Bezier. */
    private fun isCurveHit(
        px: Float,
        py: Float,
        arrow: ReactionArrow.ElectronPushing,
        width: Float
    ): Boolean {
        val start = Offset(arrow.startX, arrow.startY)
        val end = Offset(arrow.endX, arrow.endY)
        val mid = Offset((start.x + end.x) / 2f, (start.y + end.y) / 2f)
        val dx = end.x - start.x
        val dy = end.y - start.y
        val len = sqrt(dx * dx + dy * dy)
        if (len == 0f) return false
        val perpX = -dy / len
        val perpY = dx / len
        val control = Offset(mid.x + perpX * arrow.curveBow, mid.y + perpY * arrow.curveBow)

        val segments = 12
        var prev = start
        for (i in 1..segments) {
            val t = i / segments.toFloat()
            val oneMinusT = 1f - t
            val point = Offset(
                oneMinusT * oneMinusT * start.x + 2 * oneMinusT * t * control.x + t * t * end.x,
                oneMinusT * oneMinusT * start.y + 2 * oneMinusT * t * control.y + t * t * end.y
            )
            if (isBondHit(px, py, prev, point, width)) return true
            prev = point
        }
        return false
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

            val editedMolecules = replaceMolecule(
                document = _state.value.document,
                idx = moleculeIdx,
                newMolecule = newMolecule
            ).molecules
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

            val editedMolecules = replaceMolecule(
                document = _state.value.document,
                idx = moleculeIdx,
                newMolecule = newMolecule
            ).molecules
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
            val editedMolecules = replaceMolecule(
                document = _state.value.document,
                idx = moleculeIdx,
                newMolecule = newMolecule
            ).molecules
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

            val editedMolecules = replaceMolecule(
                document = _state.value.document,
                idx = moleculeIdx,
                newMolecule = newMolecule
            ).molecules
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

            val editedMolecules = replaceMolecule(
                document = _state.value.document,
                idx = moleculeIdx,
                newMolecule = newMolecule
            ).molecules
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

            val editedMolecules = replaceMolecule(
                document = _state.value.document,
                idx = moleculeIdx,
                newMolecule = newMolecule
            ).molecules
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

            val editedMolecules = replaceMolecule(
                document = _state.value.document,
                idx = moleculeIdx,
                newMolecule = newMolecule
            ).molecules
            commitEdit(_state.value.document.copy(molecules = editedMolecules))
        }
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

    private fun handleCreateMoleculeFromEthane(
        bondType: Bond.BondType,
        bondDir: Bond.BondDir = Bond.BondDir.NONE,
        xAngstrom: Double,
        yAngstrom: Double
    ) {
        viewModelScope.launch {
            val newMolecule = cheminformaticsDataSource.createMoleculeFromEthane(
                bondType,
                bondDir,
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