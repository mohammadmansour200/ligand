package org.liganddraw.app.editor.presentation.molecule_pane

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import org.liganddraw.app.editor.presentation.molecule_pane.components.Molecule3DViewer
import org.liganddraw.app.editor.presentation.molecule_pane.components.MoleculePropertiesCard
import org.liganddraw.app.editor.presentation.molecule_pane.components.NoMoleculeNotice

@Composable
fun MoleculePaneRoot(viewModel: MoleculePaneViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    MoleculePane(state)
}

@Composable
fun MoleculePane(state: MoleculePaneState) {
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainer)) {
        when {
            state.conformer == null -> NoMoleculeNotice(modifier = Modifier.align(Alignment.Center))
            else -> {
                Molecule3DViewer(
                    conformer = state.conformer,
                    iblBytes = state.iblBytes,
                    solidColorMaterialBytes = state.solidColorMaterialBytes,
                    epmMaterialBytes = state.epmMaterialBytes,
                    error = state.conformerError
                )
                MoleculePropertiesCard(state)
            }
        }
    }
}




