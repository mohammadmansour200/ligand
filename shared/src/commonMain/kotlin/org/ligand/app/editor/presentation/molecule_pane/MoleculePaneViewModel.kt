package org.ligand.app.editor.presentation.molecule_pane

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.erkko68.filament.Engine
import io.github.erkko68.filament.Filament
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.ImageFormat
import io.github.vinceglb.filekit.dialogs.compose.util.encodeToByteArray
import io.github.vinceglb.filekit.dialogs.openFileSaver
import io.github.vinceglb.filekit.write
import io.github.vinceglb.filekit.writeString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ligand.shared.generated.resources.Res
import org.ligand.app.core.domain.DataError
import org.ligand.app.core.domain.onError
import org.ligand.app.core.domain.onSuccess
import org.ligand.app.editor.domain.Atom
import org.ligand.app.editor.domain.ChemicalExportFormat
import org.ligand.app.editor.domain.CheminformaticsDataSource
import org.ligand.app.editor.domain.EPMMeshData
import org.ligand.app.editor.domain.ImageExportFormat
import org.ligand.app.editor.domain.Molecule
import org.ligand.app.editor.domain.PubchemDataSource
import org.ligand.app.editor.domain.computeEpmMeshData
import kotlin.uuid.Uuid

class MoleculePaneViewModel(
    private val cheminformaticsDataSource: CheminformaticsDataSource,
    private val pubchemDataSource: PubchemDataSource
) :
    ViewModel() {
    private val _state = MutableStateFlow(MoleculePaneState())
    val state = _state.asStateFlow()

    private val _events = Channel<MoleculePaneEvent>()
    val events = _events.receiveAsFlow()

    private val _filamentResources = MutableStateFlow<FilamentResources>(FilamentResources.Loading)
    val filamentResources = _filamentResources.asStateFlow()

    init {
        viewModelScope.launch {
            Filament.init()
            val engine = Engine.create(Engine.Backend.DEFAULT)
            val ibl = Res.readBytes("files/lightroom_ibl.ktx")
            val solidColor = Res.readBytes("files/lit_color.filamat")
            val epm = Res.readBytes("files/epm.filamat")
            if (engine != null) _filamentResources.value =
                FilamentResources.Ready(engine, ibl, solidColor, epm)
        }
    }

    override fun onCleared() {
        (_filamentResources.value as? FilamentResources.Ready)?.engine?.use {
            it.close()
        }
    }

    fun onAction(action: MoleculePaneAction) {
        when (action) {
            is MoleculePaneAction.OnGenerateConformer -> generateConformer(action.molecule)
            is MoleculePaneAction.OnCalcProperties -> calcProperties(action.molecule)
            is MoleculePaneAction.OnExportImage -> exportImage(action.imageBitmap, action.format)
            is MoleculePaneAction.OnExportChemicalFile -> exportChemicalFile(
                action.molecule,
                action.format
            )
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
    private var moleculeNamesJob: Job? = null

    private fun calcProperties(molecule: Molecule?) {
        if (previousCalcPropertiesMolecule == molecule) return
        previousCalcPropertiesMolecule = molecule

        if (molecule == null) {
            _state.update { it.copy(properties = null, propertiesError = null) }
            return
        }
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isNamesLoading = true,
                    propertiesError = null
                )
            }

            cheminformaticsDataSource.calcProperties(molecule)
                .onSuccess { properties ->
                    _state.update { it.copy(properties = properties, propertiesError = null) }
                    fetchMoleculeNames(molecule)
                }
                .onError { error ->
                    _state.update {
                        it.copy(
                            propertiesError = error,
                            isNamesLoading = false,
                        )
                    }
                }
        }
    }

    private fun fetchMoleculeNames(molecule: Molecule) {
        moleculeNamesJob?.cancel()
        viewModelScope.launch {
            cheminformaticsDataSource.getInchiKey(molecule)
                .onSuccess { key ->
                    moleculeNamesJob = viewModelScope.launch {
                        pubchemDataSource.getMoleculeNames(key)
                            .onSuccess { names ->
                                _state.update {
                                    it.copy(
                                        properties = it.properties?.copy(
                                            iupacName = names.iupacName,
                                            synonym = names.synonym
                                        ),
                                        isNamesLoading = false,
                                        namesError = null,
                                    )
                                }
                            }
                            .onError { error ->
                                _state.update {
                                    it.copy(
                                        properties = it.properties?.copy(
                                            iupacName = null,
                                            synonym = null
                                        ),
                                        isNamesLoading = false,
                                        namesError = error
                                    )
                                }
                            }
                    }
                }
                .onError {
                    _state.update {
                        it.copy(
                            properties = it.properties?.copy(iupacName = null, synonym = null),
                            isNamesLoading = false,
                        )
                    }
                }
        }
    }

    private fun exportImage(imageBitmap: ImageBitmap, format: ImageExportFormat) {
        viewModelScope.launch(Dispatchers.Default) {
            // --- ENCODE BITMAP ---
            val encodeFormat = when (format) {
                ImageExportFormat.PNG -> ImageFormat.PNG
                ImageExportFormat.JPG -> ImageFormat.JPEG
            }
            val bytes = try {
                imageBitmap.encodeToByteArray(format = encodeFormat)
            } catch (e: Exception) {
                _events.send(MoleculePaneEvent.ExportError(DataError.ExportFailed(e.stackTraceToString())))
                return@launch
            }

            /// --- WRITE FILE ---
            val prefix = _state.value.properties?.let { it.iupacName ?: it.formula } ?: "molecule"
            val suffix = Uuid.random().toString().take(4)
            val fileName = "${prefix}_$suffix"
            val extension = format.name.lowercase()

            val targetFile = FileKit.openFileSaver(
                suggestedName = fileName,
                defaultExtension = extension,
            )

            if (targetFile != null) {
                targetFile.write(bytes)
                _events.send(MoleculePaneEvent.ExportSuccess(targetFile))
            }
        }
    }

    private fun exportChemicalFile(molecule: Molecule, format: ChemicalExportFormat) {
        viewModelScope.launch {
            // --- CONVERT TO SPECIFIED FORMAT ---
            cheminformaticsDataSource.convertMolecule(molecule, format).onSuccess { content ->
                /// --- WRITE FILE ---
                val prefix =
                    _state.value.properties?.let { it.iupacName ?: it.formula } ?: "molecule"
                val suffix = Uuid.random().toString().take(4)
                val fileName = "${prefix}_$suffix"
                val extension = format.name.lowercase()

                val targetFile = FileKit.openFileSaver(
                    suggestedName = fileName,
                    defaultExtension = extension,
                )

                if (targetFile != null) {
                    targetFile.writeString(content)
                    _events.send(MoleculePaneEvent.ExportSuccess(targetFile))
                }
            }.onError { error ->
                _events.send(MoleculePaneEvent.ExportError(error))
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