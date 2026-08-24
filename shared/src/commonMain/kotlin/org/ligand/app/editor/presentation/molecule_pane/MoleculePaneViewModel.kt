package org.ligand.app.editor.presentation.molecule_pane

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.erkko68.filament.Engine
import io.github.erkko68.filament.Filament
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ligand.shared.generated.resources.Res
import org.ligand.app.core.domain.onError
import org.ligand.app.core.domain.onSuccess
import org.ligand.app.editor.domain.Atom
import org.ligand.app.editor.domain.CheminformaticsDataSource
import org.ligand.app.editor.domain.EPMMeshData
import org.ligand.app.editor.domain.Molecule
import org.ligand.app.editor.domain.PubchemDataSource
import org.ligand.app.editor.domain.computeEpmMeshData

class MoleculePaneViewModel(
    private val cheminformaticsDataSource: CheminformaticsDataSource,
    private val pubchemDataSource: PubchemDataSource
) :
    ViewModel() {
    private val _state = MutableStateFlow(MoleculePaneState())
    val state = _state.asStateFlow()

    private val _filamentResources = MutableStateFlow<FilamentResources>(FilamentResources.Loading)
    val filamentResources = _filamentResources.asStateFlow()

    init {
        viewModelScope.launch {
            Filament.init()
            val engine = Engine.create(Engine.Backend.DEFAULT)
            val ibl = Res.readBytes("files/lightroom_ibl.ktx")
            val solidColor = Res.readBytes("files/lit_color.filamat")
            val epm = Res.readBytes("files/epm.filamat")
            _filamentResources.value = FilamentResources.Ready(engine, ibl, solidColor, epm)
        }
    }

    override fun onCleared() {
        (_filamentResources.value as? FilamentResources.Ready)?.engine?.destroy()
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
                    val epmMeshData = handleComputeEpmMeshData(mol.atoms)
                    _state.update {
                        it.copy(
                            epmMeshData = epmMeshData,
                            conformer = mol,
                            conformerError = null
                        )
                    }

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
                        .onError { error ->
                            _state.update {
                                it.copy(
                                    properties = it.properties?.copy(iupacName = null),
                                    isIupacLoading = false,
                                    iupacError = error
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

    private suspend fun handleComputeEpmMeshData(atoms: List<Atom>): EPMMeshData =
        withContext(Dispatchers.Default) {
            return@withContext computeEpmMeshData(
                atoms = atoms,
            )
        }
}