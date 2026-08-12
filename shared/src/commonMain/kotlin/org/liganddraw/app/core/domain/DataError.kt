package org.liganddraw.app.core.domain

sealed interface DataError : Error {
    object FileCorrupted : DataError
    object RequestTimeout : DataError
    object TooManyRequests : DataError
    object NoInternet : DataError
    object Server : DataError
    object Serialization : DataError
    object Unknown : DataError
}