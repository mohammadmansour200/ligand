package org.ligand.app.editor.presentation.molecule_pane

import androidx.compose.ui.graphics.ImageBitmap
import org.ligand.app.editor.domain.ChemicalExportFormat
import org.ligand.app.editor.domain.ImageExportFormat
import org.ligand.app.editor.domain.Molecule

sealed interface MoleculePaneAction {
    data class OnGenerateConformer(val molecule: Molecule?) : MoleculePaneAction
    data class OnCalcProperties(val molecule: Molecule?) : MoleculePaneAction
    data class OnExportImage(val imageBitmap: ImageBitmap, val format: ImageExportFormat) :
        MoleculePaneAction

    data class OnExportChemicalFile(val molecule: Molecule, val format: ChemicalExportFormat) :
        MoleculePaneAction
}