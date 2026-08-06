package org.liganddraw.app.editor.presentation.drawing_pane

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import org.liganddraw.app.editor.domain.Tool
import org.liganddraw.app.editor.presentation.drawing_pane.components.DrawingCanvas
import org.liganddraw.app.editor.presentation.drawing_pane.components.DrawingPaneTopBar
import org.liganddraw.app.editor.presentation.drawing_pane.components.DrawingToolbar
import org.liganddraw.app.editor.presentation.drawing_pane.components.ElementPalette
import org.liganddraw.app.editor.presentation.drawing_pane.components.ValenceViolationExplanationDialog

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
            DrawingPaneTopBar(onAction = { onAction(it) })
        },
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(it)) {
            DrawingCanvas(state = state, onAction = { action -> onAction(action) })

            DrawingToolbar(
                selectedTool = state.selectedTool,
                onSelectTool = { tool -> onAction(DrawingPaneAction.OnSelectTool(tool)) }
            )
            val selectedElement =
                if (state.selectedTool is Tool.Element) state.selectedTool.symbol else null
            ElementPalette(
                onAtomSelected = { symbol ->
                    onAction(
                        DrawingPaneAction.OnSelectTool(
                            Tool.Element(
                                symbol
                            )
                        )
                    )
                },
                selectedAtom = selectedElement
            )
        }
        state.valenceViolationExplanationAtom?.let { atom ->
            ValenceViolationExplanationDialog(
                atom = atom,
                onDismiss = { onAction(DrawingPaneAction.OnDismissValenceViolationDialog) })
        }
    }
}
