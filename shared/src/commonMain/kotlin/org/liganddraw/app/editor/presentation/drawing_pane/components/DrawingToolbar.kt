package org.liganddraw.app.editor.presentation.drawing_pane.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import liganddraw.shared.generated.resources.Res
import liganddraw.shared.generated.resources.drag_pan
import org.jetbrains.compose.resources.vectorResource

@Composable
fun DrawingToolbar() {
    Column(modifier = Modifier.fillMaxHeight()) {
        IconButton(onClick = {}) {
            Icon(vectorResource(Res.drawable.drag_pan), contentDescription = null)
        }
        IconButton(onClick = {}) {
            Icon(vectorResource(Res.drawable.drag_pan), contentDescription = null)
        }
        IconButton(onClick = {}) {
            Icon(vectorResource(Res.drawable.drag_pan), contentDescription = null)
        }
        IconButton(onClick = {}) {
            Icon(vectorResource(Res.drawable.drag_pan), contentDescription = null)
        }
        IconButton(onClick = {}) {
            Icon(vectorResource(Res.drawable.drag_pan), contentDescription = null)
        }
    }
}
