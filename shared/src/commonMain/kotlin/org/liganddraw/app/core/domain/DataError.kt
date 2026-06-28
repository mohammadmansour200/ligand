package org.liganddraw.app.core.domain

sealed interface DataError : Error {
    enum class Local : DataError {
        FILE_CORRUPTED,
    }
}