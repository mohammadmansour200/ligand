package org.liganddraw.app.editor.data.network

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.liganddraw.app.core.data.safeCall
import org.liganddraw.app.core.domain.DataError
import org.liganddraw.app.core.domain.Result
import org.liganddraw.app.editor.data.mappers.IupacNamePropertyResponseDto
import org.liganddraw.app.editor.domain.PubchemDataSource

const val BASE_URL =
    "https://pubchem.ncbi.nlm.nih.gov/rest/pug/compound/inchikey"

class KtorPubchemDataSource(val httpClient: HttpClient) : PubchemDataSource {
    override suspend fun getIupacName(inchiKey: String): Result<String, DataError> =
        withContext(Dispatchers.IO) {
            when (
                val result = safeCall<IupacNamePropertyResponseDto> {
                    httpClient.get(
                        "$BASE_URL/$inchiKey/property/IUPACName/JSON"
                    )
                }
            ) {
                is Result.Success -> {
                    val iupacName = result.data.propertyTable.properties.firstOrNull()?.iupacName
                    if (iupacName != null) {
                        Result.Success(iupacName)
                    } else {
                        Result.Error(DataError.Unknown)
                    }
                }

                is Result.Error -> Result.Error(result.error)
            }
        }
}