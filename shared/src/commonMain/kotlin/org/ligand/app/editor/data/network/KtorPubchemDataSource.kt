package org.ligand.app.editor.data.network

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.ligand.app.core.data.safeCall
import org.ligand.app.core.domain.DataError
import org.ligand.app.core.domain.Result
import org.ligand.app.core.domain.map
import org.ligand.app.editor.data.mappers.IupacNamePropertyResponseDto
import org.ligand.app.editor.domain.PubchemDataSource

const val BASE_URL =
    "https://pubchem.ncbi.nlm.nih.gov/rest/pug/compound/inchikey"

class KtorPubchemDataSource(val httpClient: HttpClient) : PubchemDataSource {
    override suspend fun getIupacName(inchiKey: String): Result<String?, DataError> =
        withContext(Dispatchers.IO) {
            safeCall<IupacNamePropertyResponseDto> {
                httpClient.get(
                    "$BASE_URL/$inchiKey/property/IUPACName/JSON"
                )
            }.map { response ->
                response.propertyTable.properties.firstOrNull()?.iupacName
            }
        }
}