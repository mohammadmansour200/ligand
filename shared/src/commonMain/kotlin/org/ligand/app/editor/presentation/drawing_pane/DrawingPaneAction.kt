package org.ligand.app.editor.presentation.drawing_pane

import androidx.compose.ui.text.TextLayoutResult
import io.github.vinceglb.filekit.PlatformFile
import org.ligand.app.editor.domain.Tool

sealed interface DrawingPaneAction {
    /** Reverts the most recent committed edit, restoring the previous [DrawingDocument] state. No-op if there is nothing to undo. */
    object OnUndo : DrawingPaneAction

    /** Re-applies the most recently undone edit, restoring the [DrawingDocument] state it produced. No-op if there is nothing to redo, and is cleared whenever a new edit is committed. */
    object OnRedo : DrawingPaneAction

    /**
     * Action triggered when a file is successfully picked by the user.
     * File is parsed using Cheminformatics engine. parsed molecules are then saved in state
     * @property file File from rememberFilePickerLauncher
     */
    data class OnFilePick(val file: PlatformFile) : DrawingPaneAction

    /**
     * Action triggered as effect to prevent repetitive TextMeasurer calls.
     * @property symbolLayouts Measured text layouts for symbols, keyed by symbol (C, N, H etc.).
     * @property hydrogenLayouts Measured text layouts for hydrogen counts, keyed by count (1, 2, 3 etc.).
     */
    data class OnCacheLabelLayouts(
        val symbolLayouts: Map<String, TextLayoutResult>,
        val hydrogenLayouts: Map<Long, TextLayoutResult>
    ) : DrawingPaneAction

    /**
     * Action triggered as effect to prevent repetitive TextMeasurer calls.
     * @property layouts Measured text layouts for text boxes, keyed by text box id.
     */
    data class OnCacheTextBoxLayouts(
        val layouts: Map<String, TextLayoutResult>,
    ) : DrawingPaneAction

    /**
     * Action triggered when user selects a tool from toolbar.
     * @property tool selected tool.
     */
    data class OnSelectTool(val tool: Tool) : DrawingPaneAction

    /**Action triggered by Molecule pane when user clicks next molecule button*/
    object OnSelectNextMolecule : DrawingPaneAction

    /**Action triggered by Molecule pane when user clicks previous molecule button*/
    object OnSelectPreviousMolecule : DrawingPaneAction

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