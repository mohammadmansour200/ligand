package org.ligand.app.editor.presentation.molecule_pane

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import org.ligand.app.editor.presentation.drawing_pane.DrawingPaneAction
import org.ligand.app.editor.presentation.drawing_pane.DrawingPaneState
import org.ligand.app.editor.presentation.drawing_pane.DrawingPaneViewModel
import org.ligand.app.editor.presentation.molecule_pane.components.Molecule3DViewer
import org.ligand.app.editor.presentation.molecule_pane.components.MoleculePaneTopBar
import org.ligand.app.editor.presentation.molecule_pane.components.MoleculePropertiesCard
import org.ligand.app.editor.presentation.molecule_pane.components.NoMoleculeNotice

@Composable
fun MoleculePaneRoot(
    moleculePaneViewModel: MoleculePaneViewModel = koinViewModel(),
    drawingPaneViewModel: DrawingPaneViewModel = koinViewModel(),
    onClose: (() -> Unit),
) {
    val filamentResources by moleculePaneViewModel.filamentResources.collectAsStateWithLifecycle()
    val moleculePaneState by moleculePaneViewModel.state.collectAsStateWithLifecycle()
    val drawingPaneState by drawingPaneViewModel.state.collectAsStateWithLifecycle()
    MoleculePane(
        filamentResources = filamentResources,
        moleculePaneState = moleculePaneState,
        drawingPaneState = drawingPaneState,
        drawingPaneOnAction = drawingPaneViewModel::onAction,
        onClose = onClose,
    )
}

@Composable
fun MoleculePane(
    filamentResources: FilamentResources,
    moleculePaneState: MoleculePaneState,
    drawingPaneState: DrawingPaneState,
    drawingPaneOnAction: (DrawingPaneAction) -> Unit,
    onClose: (() -> Unit),
) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(modifier = Modifier.fillMaxSize()) {
            MoleculePaneTopBar(
                drawingPaneState = drawingPaneState,
                onPrevious = { drawingPaneOnAction(DrawingPaneAction.OnSelectPreviousMolecule) },
                onNext = { drawingPaneOnAction(DrawingPaneAction.OnSelectNextMolecule) },
                onClose = onClose,
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {

                when {
                    filamentResources !is FilamentResources.Ready || moleculePaneState.epmMeshData == null || moleculePaneState.conformer == null -> NoMoleculeNotice(
                        modifier = Modifier.align(
                            Alignment.Center
                        )
                    )

                    else -> {
                        Molecule3DViewer(
                            engine = filamentResources.engine,
                            epmMeshData = moleculePaneState.epmMeshData,
                            conformer = moleculePaneState.conformer,
                            iblBytes = filamentResources.iblBytes,
                            solidColorMaterialBytes = filamentResources.solidColorMaterialBytes,
                            epmMaterialBytes = filamentResources.epmMaterialBytes,
                            error = moleculePaneState.conformerError
                        )
                        MoleculePropertiesCard(moleculePaneState)
                    }
                }
            }
        }
    }
}
