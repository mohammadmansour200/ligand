package org.ligand.app.editor.presentation.molecule_pane.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ligand.shared.generated.resources.Res
import ligand.shared.generated.resources.chevron_left
import ligand.shared.generated.resources.chevron_right
import ligand.shared.generated.resources.close
import org.jetbrains.compose.resources.vectorResource
import org.ligand.app.core.presentation.IconWithTooltip
import org.ligand.app.editor.presentation.drawing_pane.DrawingPaneState

@Composable
fun MoleculePaneTopBar(
    drawingPaneState: DrawingPaneState,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onClose: () -> Unit,
    showCloseButton: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row {
            IconButton(
                onClick = onPrevious,
                enabled = drawingPaneState.followedMoleculeHasPreviousMolecule
            ) {
                IconWithTooltip(
                    icon = vectorResource(Res.drawable.chevron_left),
                    text = "Previous molecule"
                )
            }
            IconButton(
                onClick = onNext,
                enabled = drawingPaneState.followedMoleculeHasNextMolecule
            ) {
                IconWithTooltip(
                    icon = vectorResource(Res.drawable.chevron_right),
                    text = "Next molecule"
                )
            }
        }
        if (showCloseButton) {
            IconButton(onClick = onClose) {
                IconWithTooltip(
                    icon = vectorResource(Res.drawable.close),
                    text = "Close",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}