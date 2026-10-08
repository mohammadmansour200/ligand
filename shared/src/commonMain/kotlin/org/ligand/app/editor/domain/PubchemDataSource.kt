package org.ligand.app.editor.domain

import org.ligand.app.core.domain.DataError
import org.ligand.app.core.domain.Result

data class MoleculeNames(
    val iupacName: String?,
    val synonym: String?
)

interface PubchemDataSource {
    suspend fun getMoleculeNames(
        inchiKey: String
    ): Result<MoleculeNames, DataError>
}