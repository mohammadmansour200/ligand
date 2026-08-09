package org.liganddraw.app.editor.presentation.drawing_pane

import androidx.compose.ui.unit.IntSize
import io.github.vinceglb.filekit.PlatformFile
import org.liganddraw.app.editor.domain.Tool

sealed interface DrawingPaneAction {
    /** Reverts the most recent committed edit, restoring the previous [DrawingDocument] state. No-op if there is nothing to undo. */
    object OnUndo : DrawingPaneAction

    /** Re-applies the most recently undone edit, restoring the [DrawingDocument] state it produced. No-op if there is nothing to redo, and is cleared whenever a new edit is committed. */
    object OnRedo : DrawingPaneAction

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

    // TODO(KDoc)
    data class OnPointerLongPress(val x: Float, val y: Float) : DrawingPaneAction

    /**
     * Action triggered when drag gesture starts.
     *
     * @property x The current X coordinate in canvas pixels.
     * @property y The current Y coordinate in canvas pixels.
     */
    data class OnDragStart(val x: Float, val y: Float) : DrawingPaneAction

    /**
     * Action triggered when moving a pointer across the canvas during an active drag gesture.
     *
     * @property x The current X coordinate in canvas pixels.
     * @property y The current Y coordinate in canvas pixels.
     */
    data class OnDrag(val x: Float, val y: Float) : DrawingPaneAction

    /**Action triggered when drag gesture ends.*/
    object OnDragEnd : DrawingPaneAction

    // TODO(KDoc)
    object OnDismissValenceViolationDialog : DrawingPaneAction
}