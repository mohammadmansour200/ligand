package org.liganddraw.app.core.domain

sealed interface DataError : Error {
    object FileCorrupted : DataError
}