package org.ligand.app.core.domain

sealed interface NetworkError : Error {
    object RequestTimeout : NetworkError
    object TooManyRequests : NetworkError
    object NoInternet : NetworkError
    object Server : NetworkError
    object Serialization : NetworkError
    object Unknown : NetworkError
}