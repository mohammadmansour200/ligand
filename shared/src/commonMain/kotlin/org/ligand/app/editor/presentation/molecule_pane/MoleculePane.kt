package org.ligand.app.editor.presentation.molecule_pane

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.openFileWithDefaultApplication
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import org.ligand.app.core.domain.DataError
import org.ligand.app.core.presentation.utils.ObserveAsEvents
import org.ligand.app.core.presentation.utils.toClipEntry
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
        moleculePaneOnAction = moleculePaneViewModel::onAction,
        moleculePaneEvents = moleculePaneViewModel.events,
        drawingPaneState = drawingPaneState,
        drawingPaneOnAction = drawingPaneViewModel::onAction,
        onClose = onClose,
    )
}

@Composable
fun MoleculePane(
    filamentResources: FilamentResources,
    moleculePaneState: MoleculePaneState,
    moleculePaneOnAction: (MoleculePaneAction) -> Unit,
    moleculePaneEvents: Flow<MoleculePaneEvent>,
    drawingPaneState: DrawingPaneState,
    drawingPaneOnAction: (DrawingPaneAction) -> Unit,
    onClose: (() -> Unit),
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val clipboard = LocalClipboard.current

    ObserveAsEvents(moleculePaneEvents) { event ->
        when (event) {
            is MoleculePaneEvent.ExportError -> {
                scope.launch {
                    val errorMessage = when (event.error) {
                        is DataError.ExportFailed -> event.error.message
                        is DataError.FileCorrupted -> null
                    }

                    val result =
                        snackbarHostState.showSnackbar(
                            message = "Couldn't export 3D model due to an error",
                            actionLabel = if (errorMessage != null) "Copy Error" else null,
                        )

                    if (errorMessage != null && result == SnackbarResult.ActionPerformed) clipboard.setClipEntry(
                        errorMessage.toClipEntry()
                    )
                }
            }

            is MoleculePaneEvent.ExportSuccess -> {
                scope.launch {
                    val result = snackbarHostState
                        .showSnackbar(
                            message = "File saved on your device successfully",
                            actionLabel = "Open",
                        )

                    if (result == SnackbarResult.ActionPerformed) FileKit.openFileWithDefaultApplication(
                        event.file
                    )

                }
            }
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            MoleculePaneTopBar(
                drawingPaneState = drawingPaneState,
                onPrevious = { drawingPaneOnAction(DrawingPaneAction.OnSelectPreviousMolecule) },
                onNext = { drawingPaneOnAction(DrawingPaneAction.OnSelectNextMolecule) },
                onClose = onClose,
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
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
                            filamentResources = filamentResources,
                            conformer = moleculePaneState.conformer,
                            epmMeshData = moleculePaneState.epmMeshData,
                            error = moleculePaneState.conformerError,
                            onAction = { moleculePaneOnAction(it) }
                        )
                        MoleculePropertiesCard(moleculePaneState)
                    }
                }
            }
        }
    }
}
