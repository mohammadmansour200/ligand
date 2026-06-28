package org.liganddraw.app.editor.presentation.drawing_pane

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.liganddraw.app.core.domain.onSuccess
import org.liganddraw.app.editor.domain.CheminformaticsDataSource
import kotlin.io.path.absolutePathString
import kotlin.io.path.createTempFile
import kotlin.io.path.deleteIfExists
import kotlin.io.path.writeText

class DrawingPaneViewModel(private val cheminformaticsDataSource: CheminformaticsDataSource) :
    ViewModel() {
    private val _state = MutableStateFlow(DrawingPaneState())
    val state = _state.asStateFlow()

    fun onAction(action: DrawingPaneAction) {
        when (action) {
            is DrawingPaneAction.OnFilePick -> parseFile(action.content, action.extension)
        }
    }

    private fun parseFile(content: String, extension: String) {
        viewModelScope.launch {
            // Create temporary file
            val tempFile = createTempFile()
            tempFile.writeText(content)

            // Parse file to Molecule
            val parseFile = when (extension.lowercase()) {
                "sdf" -> cheminformaticsDataSource.sdfFileToMolecule(tempFile.absolutePathString())
                "cdx" -> cheminformaticsDataSource.cdxFileToMolecule(tempFile.absolutePathString())
                else -> cheminformaticsDataSource.molFileToMolecule(tempFile.absolutePathString())
            }

            // Add molecule to existing molecules list
            val existingMols = _state.value.molecules
            parseFile.onSuccess { mols ->
                _state.update { it.copy(molecules = existingMols + mols) }
                println(mols.toString())
            }

            // Delete temporary file
            tempFile.deleteIfExists()
        }
    }
}