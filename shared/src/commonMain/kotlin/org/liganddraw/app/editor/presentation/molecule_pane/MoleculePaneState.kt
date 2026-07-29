package org.liganddraw.app.editor.presentation.molecule_pane

import androidx.compose.runtime.Immutable
import org.liganddraw.app.editor.domain.Molecule
import org.liganddraw.app.editor.domain.MoleculeProperties

@Immutable
data class MoleculePaneState(
    val conformer: Molecule? = null,
    val properties: MoleculeProperties? = null,
    val iblBytes: ByteArray = byteArrayOf(),
    val solidColorMaterialBytes: ByteArray = byteArrayOf(),
    val epmMaterialBytes: ByteArray = byteArrayOf()
)
