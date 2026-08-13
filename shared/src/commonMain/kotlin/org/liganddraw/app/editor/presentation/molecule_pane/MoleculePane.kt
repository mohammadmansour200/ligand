package org.liganddraw.app.editor.presentation.molecule_pane

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
import org.liganddraw.app.editor.presentation.drawing_pane.DrawingPaneAction
import org.liganddraw.app.editor.presentation.drawing_pane.DrawingPaneState
import org.liganddraw.app.editor.presentation.drawing_pane.DrawingPaneViewModel
import org.liganddraw.app.editor.presentation.molecule_pane.components.Molecule3DViewer
import org.liganddraw.app.editor.presentation.molecule_pane.components.MoleculePaneTopBar
import org.liganddraw.app.editor.presentation.molecule_pane.components.MoleculePropertiesCard
import org.liganddraw.app.editor.presentation.molecule_pane.components.NoMoleculeNotice

@Composable
fun MoleculePaneRoot(
    moleculePaneViewModel: MoleculePaneViewModel = koinViewModel(),
    drawingPaneViewModel: DrawingPaneViewModel = koinViewModel(),
    onClose: (() -> Unit),
    showCloseButton: Boolean
) {
    val moleculePaneState by moleculePaneViewModel.state.collectAsStateWithLifecycle()
    val drawingPaneState by drawingPaneViewModel.state.collectAsStateWithLifecycle()
    MoleculePane(
        moleculePaneState = moleculePaneState,
        drawingPaneState = drawingPaneState,
        drawingPaneOnAction = drawingPaneViewModel::onAction,
        onClose = onClose,
        showCloseButton = showCloseButton
    )
}

@Composable
fun MoleculePane(
    moleculePaneState: MoleculePaneState,
    drawingPaneState: DrawingPaneState,
    drawingPaneOnAction: (DrawingPaneAction) -> Unit,
    onClose: (() -> Unit),
    showCloseButton: Boolean
) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(modifier = Modifier.fillMaxSize()) {
            MoleculePaneTopBar(
                drawingPaneState = drawingPaneState,
                onPrevious = { drawingPaneOnAction(DrawingPaneAction.OnSelectPreviousMolecule) },
                onNext = { drawingPaneOnAction(DrawingPaneAction.OnSelectNextMolecule) },
                onClose = onClose,
                showCloseButton = showCloseButton
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when {
                    moleculePaneState.conformer == null -> NoMoleculeNotice(
                        modifier = Modifier.align(
                            Alignment.Center
                        )
                    )

                    else -> {
                        Molecule3DViewer(
                            conformer = moleculePaneState.conformer,
                            iblBytes = moleculePaneState.iblBytes,
                            solidColorMaterialBytes = moleculePaneState.solidColorMaterialBytes,
                            epmMaterialBytes = moleculePaneState.epmMaterialBytes,
                            error = moleculePaneState.conformerError
                        )
                        MoleculePropertiesCard(moleculePaneState)
                    }
                }
            }
        }
    }
}
