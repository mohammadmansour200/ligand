package org.liganddraw.app.editor.presentation.drawing_pane.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.extension
import io.github.vinceglb.filekit.readString
import kotlinx.coroutines.launch
import liganddraw.shared.generated.resources.Res
import liganddraw.shared.generated.resources.menu
import org.jetbrains.compose.resources.vectorResource
import org.liganddraw.app.core.presentation.IconWithTooltip
import org.liganddraw.app.editor.domain.DrawingPaneConstants.SUPPORTED_LOWERCASE_EXTENSIONS
import org.liganddraw.app.editor.presentation.drawing_pane.DrawingPaneAction

// FileKit is case-sensitive, so I support both uppercase and lowercase extensions
val supportedExtensions =
    SUPPORTED_LOWERCASE_EXTENSIONS + SUPPORTED_LOWERCASE_EXTENSIONS.map { it.uppercase() }

@Composable
fun DrawingPaneTopBar(onAction: (DrawingPaneAction) -> Unit) {
    val scope = rememberCoroutineScope()
    val filePickerLauncher = rememberFilePickerLauncher(
        mode = FileKitMode.Single,
        type = FileKitType.File(supportedExtensions)
    ) { file ->
        if (file == null) return@rememberFilePickerLauncher
        scope.launch {
            onAction(DrawingPaneAction.OnFilePick(file.readString(), file.extension))
        }
    }

    Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
        Row(modifier = Modifier.fillMaxWidth()) {
            var expanded by remember { mutableStateOf(false) }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
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
                        text = { Text("Open File") },
                        onClick = { filePickerLauncher.launch() }
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("") },
                        onClick = { /* Do something... */ }
                    )
                    DropdownMenuItem(
                        text = { Text("") },
                        onClick = { /* Do something... */ }
                    )
                }
            }
        }
    }
}