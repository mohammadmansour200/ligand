package org.liganddraw.app.editor.presentation.drawing_pane

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import liganddraw.shared.generated.resources.Res
import liganddraw.shared.generated.resources.menu
import org.jetbrains.compose.resources.vectorResource

@Composable
fun DrawingPaneRoot() {
    DrawingPane()
}

@Composable
fun DrawingPane() {
    Scaffold(
        topBar = {
            Surface {
                Row(modifier = Modifier.fillMaxWidth()) {
                    IconButton(onClick = {}) {
                        Icon(vectorResource(Res.drawable.menu), contentDescription = null)
                    }
                }
            }
        },
    ) {
        Row(modifier = Modifier.fillMaxSize().padding(it)) {
//            Column(modifier = Modifier.fillMaxHeight()) {
//                IconButton(onClick = {}) {
//                    Icon(vectorResource(Res.drawable.drag_pan), contentDescription = null)
//                }
//            }
            Canvas(
                modifier = Modifier.clip(RoundedCornerShape(10.dp)).fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceBright)
            ) {
                drawLine(color = Color.White, start = Offset(5f, 10f), end = Offset(10f, 15f))
            }
        }
    }
}


