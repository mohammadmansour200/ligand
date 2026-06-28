package org.liganddraw.app

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.VerticalDragHandle
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.SupportingPaneScaffold
import androidx.compose.material3.adaptive.layout.defaultDragHandleSemantics
import androidx.compose.material3.adaptive.layout.rememberPaneExpansionState
import androidx.compose.material3.adaptive.navigation.rememberSupportingPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel
import org.liganddraw.app.editor.presentation.drawing_pane.DrawingPaneRoot
import org.liganddraw.app.editor.presentation.drawing_pane.DrawingPaneViewModel
import org.liganddraw.app.ui.theme.AppTheme

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
@Preview
fun App() {
    AppTheme {
        val drawingPaneViewModel = koinViewModel<DrawingPaneViewModel>()

        val navigator = rememberSupportingPaneScaffoldNavigator()
        SupportingPaneScaffold(
            directive = navigator.scaffoldDirective,
            value = navigator.scaffoldValue,
            supportingPane = {
                AnimatedPane {

                }
            }, mainPane = {
                AnimatedPane {
                    DrawingPaneRoot()
                }
            },
            paneExpansionState = rememberPaneExpansionState(navigator.scaffoldValue),
            paneExpansionDragHandle = { state ->
                val interactionSource =
                    remember { MutableInteractionSource() }
                VerticalDragHandle(
                    modifier =
                        Modifier.paneExpansionDraggable(
                            state,
                            LocalMinimumInteractiveComponentSize.current,
                            interactionSource, state.defaultDragHandleSemantics()
                        ), interactionSource = interactionSource
                )
            })
    }
}