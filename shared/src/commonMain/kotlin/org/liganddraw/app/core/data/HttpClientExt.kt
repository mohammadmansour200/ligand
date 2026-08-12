package org.liganddraw.app.core.data

import io.ktor.client.call.NoTransformationFoundException
import io.ktor.client.call.body
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.statement.HttpResponse
import io.ktor.util.network.UnresolvedAddressException
import kotlinx.coroutines.ensureActive
import org.liganddraw.app.core.domain.DataError
import org.liganddraw.app.core.domain.Result
import kotlin.coroutines.coroutineContext

suspend inline fun <reified T> safeCall(
    execute: () -> HttpResponse
): Result<T, DataError> {
    val response = try {
        execute()
    } catch (e: SocketTimeoutException) {
        return Result.Error(DataError.RequestTimeout)
    } catch (e: UnresolvedAddressException) {
        return Result.Error(DataError.NoInternet)
    } catch (e: Exception) {
        coroutineContext.ensureActive()
        return Result.Error(DataError.Unknown)
    }

    return responseToResult(response)
}

suspend inline fun <reified T> responseToResult(
    response: HttpResponse
): Result<T, DataError> {
    return when (response.status.value) {
        in 200..299 -> {
            try {
                Result.Success(response.body<T>())
            } catch (e: NoTransformationFoundException) {
                Result.Error(DataError.Serialization)
            }
        }

        408 -> Result.Error(DataError.RequestTimeout)
        429 -> Result.Error(DataError.TooManyRequests)
        in 500..599 -> Result.Error(DataError.Server)
        else -> Result.Error(DataError.Unknown)
    }
}