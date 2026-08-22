package org.ligand.app.editor.presentation.drawing_pane

import org.ligand.app.editor.domain.Molecule

sealed interface DrawingPaneEvent {
    data class CalculateMolecule3DAndProperties(val molecule: Molecule?) : DrawingPaneEvent
}