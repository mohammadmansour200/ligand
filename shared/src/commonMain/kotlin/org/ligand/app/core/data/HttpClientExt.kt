package org.ligand.app.core.data

import io.ktor.client.call.NoTransformationFoundException
import io.ktor.client.call.body
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.statement.HttpResponse
import io.ktor.util.network.UnresolvedAddressException
import kotlinx.coroutines.ensureActive
import org.ligand.app.core.domain.NetworkError
import org.ligand.app.core.domain.Result
import kotlin.coroutines.coroutineContext

suspend inline fun <reified T> safeCall(
    execute: () -> HttpResponse
): Result<T, NetworkError> {
    val response = try {
        execute()
    } catch (e: SocketTimeoutException) {
        return Result.Error(NetworkError.RequestTimeout)
    } catch (e: UnresolvedAddressException) {
        return Result.Error(NetworkError.NoInternet)
    } catch (e: Exception) {
        coroutineContext.ensureActive()
        return Result.Error(NetworkError.Unknown)
    }

    return responseToResult(response)
}

suspend inline fun <reified T> responseToResult(
    response: HttpResponse
): Result<T, NetworkError> {
    return when (response.status.value) {
        in 200..299 -> {
            try {
                Result.Success(response.body<T>())
            } catch (e: NoTransformationFoundException) {
                Result.Error(NetworkError.Serialization)
            }
        }

        408 -> Result.Error(NetworkError.RequestTimeout)
        429 -> Result.Error(NetworkError.TooManyRequests)
        in 500..599 -> Result.Error(NetworkError.Server)
        else -> Result.Error(NetworkError.Unknown)
    }
}