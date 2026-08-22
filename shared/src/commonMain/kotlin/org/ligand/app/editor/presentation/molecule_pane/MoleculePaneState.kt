package org.ligand.app.editor.presentation.molecule_pane

import androidx.compose.runtime.Immutable
import org.ligand.app.core.domain.ChemistryError
import org.ligand.app.editor.domain.Molecule
import org.ligand.app.editor.domain.MoleculeProperties

@Immutable
data class MoleculePaneState(
    val conformer: Molecule? = null,
    val properties: MoleculeProperties? = null,
    val isIupacLoading: Boolean = false,
    val conformerError: ChemistryError? = null,
    val propertiesError: ChemistryError? = null,
    val iblBytes: ByteArray = byteArrayOf(),
    val solidColorMaterialBytes: ByteArray = byteArrayOf(),
    val epmMaterialBytes: ByteArray = byteArrayOf()
)
