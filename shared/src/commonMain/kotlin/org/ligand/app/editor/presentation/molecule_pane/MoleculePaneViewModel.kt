package org.ligand.app.editor.presentation.molecule_pane

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ligand.shared.generated.resources.Res
import org.ligand.app.core.domain.onError
import org.ligand.app.core.domain.onSuccess
import org.ligand.app.editor.domain.CheminformaticsDataSource
import org.ligand.app.editor.domain.Molecule
import org.ligand.app.editor.domain.PubchemDataSource

class MoleculePaneViewModel(
    private val cheminformaticsDataSource: CheminformaticsDataSource,
    private val pubchemDataSource: PubchemDataSource
) :
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

    private var previousGenerateConformerMolecule: Molecule? = null
    private fun generateConformer(molecule: Molecule?) {
        if (previousGenerateConformerMolecule == molecule) return
        previousGenerateConformerMolecule = molecule
        if (molecule == null) {
            _state.update { it.copy(conformer = molecule, conformerError = null) }
            return
        }
        viewModelScope.launch {
            cheminformaticsDataSource.generate3DConformer(molecule)
                .onSuccess { mol ->
                    _state.update { it.copy(conformer = mol, conformerError = null) }
                }.onError { error ->
                    _state.update {
                        it.copy(conformerError = error)
                    }
                }
        }
    }

    private var previousCalcPropertiesMolecule: Molecule? = null
    private var iupacNameJob: Job? = null
    private fun calcProperties(molecule: Molecule?) {
        if (previousCalcPropertiesMolecule == molecule) return
        previousCalcPropertiesMolecule = molecule

        if (molecule == null) {
            _state.update { it.copy(properties = null, propertiesError = null) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isIupacLoading = true, propertiesError = null) }

            cheminformaticsDataSource.calcProperties(molecule)
                .onSuccess { properties ->
                    _state.update { it.copy(properties = properties, propertiesError = null) }
                    fetchIupacName(molecule)
                }
                .onError { error ->
                    _state.update { it.copy(propertiesError = error, isIupacLoading = false) }
                }
        }
    }

    private fun fetchIupacName(molecule: Molecule) {
        iupacNameJob?.cancel()
        iupacNameJob = viewModelScope.launch {
            cheminformaticsDataSource.getInchiKey(molecule)
                .onSuccess { key ->
                    pubchemDataSource.getIupacName(key)
                        .onSuccess { name ->
                            _state.update {
                                it.copy(
                                    properties = it.properties?.copy(iupacName = name),
                                    isIupacLoading = false
                                )
                            }
                        }
                        .onError {
                            _state.update {
                                it.copy(
                                    properties = it.properties?.copy(iupacName = null),
                                    isIupacLoading = false
                                )
                            }
                        }
                }
                .onError {
                    _state.update {
                        it.copy(
                            properties = it.properties?.copy(iupacName = null),
                            isIupacLoading = false
                        )
                    }
                }
        }
    }
}