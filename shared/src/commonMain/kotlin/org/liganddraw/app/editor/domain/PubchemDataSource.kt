package org.liganddraw.app.editor.domain

import org.liganddraw.app.core.domain.DataError
import org.liganddraw.app.core.domain.Result

interface PubchemDataSource {
    suspend fun getIupacName(
        inchiKey: String
    ): Result<String, DataError>
}