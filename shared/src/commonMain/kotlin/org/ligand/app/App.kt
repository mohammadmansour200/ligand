package org.ligand.app

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
import androidx.compose.material3.adaptive.navigation.BackNavigationBehavior
import androidx.compose.material3.adaptive.navigation.rememberSupportingPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import org.ligand.app.core.presentation.utils.ObserveAsEvents
import org.ligand.app.editor.presentation.drawing_pane.DrawingPaneEvent
import org.ligand.app.editor.presentation.drawing_pane.DrawingPaneRoot
import org.ligand.app.editor.presentation.drawing_pane.DrawingPaneViewModel
import org.ligand.app.editor.presentation.molecule_pane.MoleculePaneAction
import org.ligand.app.editor.presentation.molecule_pane.MoleculePaneRoot
import org.ligand.app.editor.presentation.molecule_pane.MoleculePaneViewModel
import org.ligand.app.ui.theme.AppTheme

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
@Preview
fun App() {
    AppTheme {
        val navigator = rememberSupportingPaneScaffoldNavigator()
        val scope = rememberCoroutineScope()
        val backNavigationBehavior = BackNavigationBehavior.PopUntilScaffoldValueChange

        NavigationBackHandler(
            isBackEnabled = navigator.canNavigateBack(),
            state = rememberNavigationEventState(NavigationEventInfo.None),
            onBackCompleted = {
                scope.launch { navigator.navigateBack() }
            }
        )

        val drawingPaneViewModel = koinViewModel<DrawingPaneViewModel>()
        val moleculePaneViewModel = koinViewModel<MoleculePaneViewModel>()

        ObserveAsEvents(drawingPaneViewModel.events) {
            when (it) {
                is DrawingPaneEvent.CalculateMolecule3DAndProperties -> {
                    moleculePaneViewModel.onAction(
                        MoleculePaneAction.OnGenerateConformer(it.molecule)
                    )
                    moleculePaneViewModel.onAction(
                        MoleculePaneAction.OnCalcProperties(it.molecule)
                    )
                }
            }
        }

        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            SupportingPaneScaffold(
                directive = navigator.scaffoldDirective,
                value = navigator.scaffoldValue,
                supportingPane = {
                    AnimatedPane {
                        MoleculePaneRoot(
                            moleculePaneViewModel = moleculePaneViewModel,
                            drawingPaneViewModel = drawingPaneViewModel,
                            onClose = { scope.launch { navigator.navigateBack(backNavigationBehavior) } },
                        )
                    }
                }, mainPane = {
                    AnimatedPane {
                        DrawingPaneRoot(
                            viewModel = drawingPaneViewModel,
                            onNavigateToSupporting = {
                                scope.launch { navigator.navigateTo(SupportingPaneScaffoldRole.Supporting) }
                            },
                        )
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
}