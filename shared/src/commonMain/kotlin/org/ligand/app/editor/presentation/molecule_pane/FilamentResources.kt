package org.ligand.app.editor.presentation.molecule_pane

import io.github.erkko68.filament.Engine

sealed interface FilamentResources {
    data object Loading : FilamentResources

    data class Ready(
        val engine: Engine,
        val iblBytes: ByteArray,
        val solidColorMaterialBytes: ByteArray,
        val epmMaterialBytes: ByteArray
    ) : FilamentResources
}