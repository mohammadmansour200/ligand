package org.liganddraw.app.core.domain

sealed interface ChemistryError : Error {
    data class ExceedsMaxValence(
        val element: String,
        val currentValence: Int,
        val maxValence: Int,
    ) : ChemistryError
}