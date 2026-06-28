package org.liganddraw.app.editor.presentation.drawing_pane

import androidx.compose.runtime.Immutable
import org.liganddraw.app.editor.domain.Molecule

@Immutable
data class DrawingPaneState(
    val molecules: List<Molecule> = emptyList(),
)
