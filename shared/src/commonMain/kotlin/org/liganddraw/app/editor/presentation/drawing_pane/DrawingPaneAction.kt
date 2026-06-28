package org.liganddraw.app.editor.presentation.drawing_pane

sealed interface DrawingPaneAction {
    /**
     * Action triggered when a file is successfully picked by the user.
     * File is parsed using Cheminformatics engine. parsed molecules are then saved in state
     * @param content The content of the picked file, represented as a String.
     * @param extension The extension of the picked file. This is typically used to determine the appropriate parsing method.
     */
    data class OnFilePick(val content: String, val extension: String) : DrawingPaneAction
}