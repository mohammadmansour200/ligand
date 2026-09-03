package org.ligand.app.editor.domain

import org.ligand.app.core.domain.DataError
import org.ligand.app.core.domain.Result

interface PubchemDataSource {
    suspend fun getIupacName(
        inchiKey: String
    ): Result<String?, DataError>
}