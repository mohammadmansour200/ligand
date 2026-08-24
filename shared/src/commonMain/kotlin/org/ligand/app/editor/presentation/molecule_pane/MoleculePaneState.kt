package org.ligand.app.editor.presentation.molecule_pane

import androidx.compose.runtime.Immutable
import org.ligand.app.core.domain.ChemistryError
import org.ligand.app.core.domain.DataError
import org.ligand.app.editor.domain.EPMMeshData
import org.ligand.app.editor.domain.Molecule
import org.ligand.app.editor.domain.MoleculeProperties

@Immutable
data class MoleculePaneState(
    val epmMeshData: EPMMeshData? = null,
    val conformer: Molecule? = null,
    val properties: MoleculeProperties? = null,
    val isIupacLoading: Boolean = false,
    val conformerError: ChemistryError? = null,
    val propertiesError: ChemistryError? = null,
    val iupacError: DataError? = null,
)
