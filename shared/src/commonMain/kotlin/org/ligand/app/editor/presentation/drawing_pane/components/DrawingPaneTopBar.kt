package org.ligand.app.editor.presentation.drawing_pane.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import ligand.shared.generated.resources.Res
import ligand.shared.generated.resources.menu
import ligand.shared.generated.resources.redo
import ligand.shared.generated.resources.show_info
import ligand.shared.generated.resources.undo
import org.jetbrains.compose.resources.vectorResource
import org.ligand.app.core.presentation.IconWithTooltip
import org.ligand.app.editor.domain.DrawingPaneConstants.SUPPORTED_LOWERCASE_EXTENSIONS
import org.ligand.app.editor.presentation.drawing_pane.DrawingPaneAction
import org.ligand.app.editor.presentation.drawing_pane.DrawingPaneState

@Composable
fun DrawingPaneTopBar(
    state: DrawingPaneState,
    onAction: (DrawingPaneAction) -> Unit,
    onNavigateToSupporting: () -> Unit,
) {
    Surface(
        modifier = Modifier.statusBarsPadding(),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MoreOptions(onAction)

            Spacer(modifier = Modifier.weight(1f))

            IconButton(
                onClick = { onAction(DrawingPaneAction.OnUndo) },
                enabled = state.canUndo,
            ) {
                IconWithTooltip(
                    icon = vectorResource(Res.drawable.undo),
                    text = "Undo",
                )
            }
            IconButton(
                onClick = { onAction(DrawingPaneAction.OnRedo) },
                enabled = state.canRedo,
            ) {
                IconWithTooltip(
                    icon = vectorResource(Res.drawable.redo),
                    text = "Redo",
                )
            }

            VerticalDivider(
                modifier = Modifier
                    .height(24.dp)
                    .padding(horizontal = 4.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )

            IconButton(
                onClick = onNavigateToSupporting,
            ) {
                IconWithTooltip(
                    icon = vectorResource(Res.drawable.show_info),
                    text = "Show 3D model and molecular info",
                )
            }
        }
    }
}

// FileKit is case-sensitive, so I support both uppercase and lowercase extensions
val supportedExtensions =
    SUPPORTED_LOWERCASE_EXTENSIONS + SUPPORTED_LOWERCASE_EXTENSIONS.map { it.uppercase() }

@Composable
private fun MoreOptions(
    onAction: (DrawingPaneAction) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var showNameDialog by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }

    val filePickerLauncher = rememberFilePickerLauncher(
        mode = FileKitMode.Single,
        type = FileKitType.File(supportedExtensions)
    ) { file ->
        if (file == null) return@rememberFilePickerLauncher
        onAction(DrawingPaneAction.OnFilePick(file))
    }
    Box(
        modifier = Modifier
            .padding(6.dp)
    ) {
        IconButton(onClick = { expanded = true }) {
            IconWithTooltip(
                text = "",
                icon = vectorResource(Res.drawable.menu),
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Name to Structure") },
                onClick = {
                    expanded = false
                    showNameDialog = true
                }
            )
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("Open File") },
                onClick = {
                    expanded = false
                    filePickerLauncher.launch()
                }
            )
        }
    }

    if (showNameDialog) {
        AlertDialog(
            onDismissRequest = {
                showNameDialog = false
                name = ""
            },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = {
                        Text(
                            "e.g. butane, 5-fluorouracil, (S)-alanine, beta-D-glucose",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                            alpha = 0.3f
                        )
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (name.isNotBlank()) {
                            onAction(DrawingPaneAction.OnNameToStructure(name.trim()))
                            showNameDialog = false
                            name = ""
                        }
                    },
                    enabled = name.isNotBlank()
                ) {
                    Text("Convert")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showNameDialog = false
                        name = ""
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}