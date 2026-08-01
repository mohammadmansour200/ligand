package org.liganddraw.app.editor.presentation.drawing_pane

import androidx.compose.ui.unit.IntSize
import io.github.vinceglb.filekit.PlatformFile
import org.liganddraw.app.editor.domain.Tool

sealed interface DrawingPaneAction {
    /**
     * Action triggered when a file is successfully picked by the user.
     * File is parsed using Cheminformatics engine. parsed molecules are then saved in state
     * @param file File from rememberFilePickerLauncher
     * @param extension The extension of the picked file. This is typically used to determine the appropriate parsing method.
     */
    data class OnFilePick(val file: PlatformFile) : DrawingPaneAction

    /**
     * Action triggered as effect to prevent repetitive TextMeasurer calls.
     * @param uniqueSymbols Dimensions for each symbol (deduplicated) e.g. Cl, H, N, O etc. found in molecule data classes.
     * @param uniqueHydrogenCounts Dimensions for each implicit hydrogen count converted to label: H, H2, H3, H4 found in molecule data classes.
     */
    data class OnCacheLabelDimensions(
        val uniqueSymbols: Map<String, IntSize>,
        val uniqueHydrogenCounts: Map<Long, IntSize>
    ) : DrawingPaneAction

    /**
     * Action triggered when user selects a tool from toolbar.
     * @param tool selected tool.
     */
    data class OnSelectTool(val tool: Tool) : DrawingPaneAction

    // TODO(KDoc)
    data class OnPointerMove(val x: Float, val y: Float) : DrawingPaneAction

    // TODO(KDoc)
    data class OnPointerPress(val x: Float, val y: Float) : DrawingPaneAction
}