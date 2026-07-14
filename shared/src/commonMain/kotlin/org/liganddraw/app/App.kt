package org.liganddraw.app

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalDragHandle
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.SupportingPaneScaffold
import androidx.compose.material3.adaptive.layout.SupportingPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.defaultDragHandleSemantics
import androidx.compose.material3.adaptive.layout.rememberPaneExpansionState
import androidx.compose.material3.adaptive.navigation.rememberSupportingPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import org.liganddraw.app.editor.presentation.drawing_pane.DrawingPaneRoot
import org.liganddraw.app.editor.presentation.drawing_pane.DrawingPaneViewModel
import org.liganddraw.app.editor.presentation.molecule_pane.MoleculePaneAction
import org.liganddraw.app.editor.presentation.molecule_pane.MoleculePaneRoot
import org.liganddraw.app.editor.presentation.molecule_pane.MoleculePaneViewModel
import org.liganddraw.app.ui.theme.AppTheme

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
@Preview
fun App() {
    AppTheme {
        val navigator = rememberSupportingPaneScaffoldNavigator()
        val scope = rememberCoroutineScope()

        NavigationBackHandler(
            state = rememberNavigationEventState(NavigationEventInfo.None),
            onBackCompleted = {
                if (navigator.canNavigateBack()) scope.launch { navigator.navigateBack() }
            }
        )

        val drawingPaneViewModel = koinViewModel<DrawingPaneViewModel>()
        val drawingPaneState by drawingPaneViewModel.state.collectAsStateWithLifecycle()

        val moleculePaneViewModel = koinViewModel<MoleculePaneViewModel>()
        // TODO("Calculate properties and generate conformer on molecule selection from canvas")
        LaunchedEffect(drawingPaneState.molecules) {
            if (drawingPaneState.molecules.isNotEmpty()) {
                moleculePaneViewModel.onAction(
                    MoleculePaneAction.OnGenerateConformer(
                        drawingPaneState.molecules.first()
                    )
                )
                moleculePaneViewModel.onAction(MoleculePaneAction.OnCalcProperties(drawingPaneState.molecules.first()))
                navigator.navigateTo(SupportingPaneScaffoldRole.Supporting)
            }
        }
        SupportingPaneScaffold(
            directive = navigator.scaffoldDirective,
            value = navigator.scaffoldValue,
            supportingPane = {
                AnimatedPane {
                    MoleculePaneRoot(moleculePaneViewModel)
                }
            }, mainPane = {
                AnimatedPane {
                    DrawingPaneRoot(drawingPaneViewModel)
                }
            },
            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainer),
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