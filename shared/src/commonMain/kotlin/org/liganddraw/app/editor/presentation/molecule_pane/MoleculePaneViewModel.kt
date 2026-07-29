package org.liganddraw.app.editor.presentation.molecule_pane

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import liganddraw.shared.generated.resources.Res
import org.liganddraw.app.core.domain.onSuccess
import org.liganddraw.app.editor.domain.CheminformaticsDataSource
import org.liganddraw.app.editor.domain.Molecule

class MoleculePaneViewModel(private val cheminformaticsDataSource: CheminformaticsDataSource) :
    ViewModel() {
    private val _state = MutableStateFlow(MoleculePaneState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val iblBytes = Res.readBytes("files/lightroom_ibl.ktx")
            val solidMaterialBytes = Res.readBytes("files/lit_color.filamat")
            val epmMaterialBytes = Res.readBytes("files/epm.filamat")
            _state.update {
                it.copy(
                    iblBytes = iblBytes,
                    solidColorMaterialBytes = solidMaterialBytes,
                    epmMaterialBytes = epmMaterialBytes
                )
            }
        }
    }

    fun onAction(action: MoleculePaneAction) {
        when (action) {
            is MoleculePaneAction.OnGenerateConformer -> generateConformer(action.molecule)
            is MoleculePaneAction.OnCalcProperties -> calcProperties(action.molecule)
        }
    }

    private fun generateConformer(molecule: Molecule) {
        viewModelScope.launch {
            cheminformaticsDataSource.generate3DConformer(molecule)
                .onSuccess { mol ->
                    _state.update { it.copy(conformer = mol) }
                }
        }
    }

    private fun calcProperties(molecule: Molecule) {
        viewModelScope.launch {
            cheminformaticsDataSource.calcProperties(molecule)
                .onSuccess { properties ->
                    _state.update { it.copy(properties = properties) }
                }
        }
    }
}