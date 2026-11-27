package org.ligand.app.core.domain

sealed interface DataError : Error {
    object FileCorrupted : DataError
    data class ExportFailed(val message: String?) : DataError
}