package org.liganddraw.app.editor.presentation.drawing_pane

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import org.liganddraw.app.editor.presentation.drawing_pane.components.DrawingPaneTopBar

@Composable
fun DrawingPaneRoot(viewModel: DrawingPaneViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    DrawingPane(state, viewModel::onAction)
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DrawingPane(state: DrawingPaneState, onAction: (DrawingPaneAction) -> Unit) {


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


