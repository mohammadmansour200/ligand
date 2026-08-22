package org.ligand.app.core.presentation

import org.ligand.app.core.domain.DataError

fun DataError.toErrorText(): String? {
    return when (this) {
        DataError.FileCorrupted -> "This file seems to be damaged. Try uploading it again."
        DataError.RequestTimeout -> "The request took too long. Please try again."
        DataError.TooManyRequests -> "Too many requests at once. Wait a moment and try again."
        DataError.NoInternet -> "You're offline. Check your connection and try again."
        DataError.Server -> "Something went wrong on our end. Please try again shortly."
        DataError.Serialization,
        DataError.Unknown -> null
    }
}