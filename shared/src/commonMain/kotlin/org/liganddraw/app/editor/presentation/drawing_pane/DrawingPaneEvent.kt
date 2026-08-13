package org.liganddraw.app.editor.presentation.drawing_pane

import org.liganddraw.app.editor.domain.Molecule

sealed interface DrawingPaneEvent {
    data class CalculateMolecule3DAndProperties(val molecule: Molecule?) : DrawingPaneEvent
}