package org.ligand.app.editor.presentation.molecule_pane

import io.github.vinceglb.filekit.PlatformFile
import org.ligand.app.core.domain.DataError

sealed interface MoleculePaneEvent {
    data class ExportError(val error: DataError) : MoleculePaneEvent
    data class ExportSuccess(val file: PlatformFile) : MoleculePaneEvent
}