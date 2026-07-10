package org.liganddraw.app.editor.presentation.molecule_pane

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import org.liganddraw.app.editor.presentation.molecule_pane.components.Molecule3DViewer
import org.liganddraw.app.editor.presentation.molecule_pane.components.MoleculePropertiesTable
import org.liganddraw.app.editor.presentation.molecule_pane.components.NoSelectedMoleculeNotice

@Composable
fun MoleculePaneRoot(viewModel: MoleculePaneViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    MoleculePane(state)
}

@Composable
fun MoleculePane(state: MoleculePaneState) {
    Column(
        modifier = Modifier
            .fillMaxSize().background(MaterialTheme.colorScheme.surface),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (state.conformer == null || state.properties == null) {
            NoSelectedMoleculeNotice()
        } else {
            Molecule3DViewer(
                conformer = state.conformer,
                ibl = state.ibl,
                solidColorMaterial = state.solidColorMaterial
            )
            MoleculePropertiesTable(state.properties)
        }
    }
}




