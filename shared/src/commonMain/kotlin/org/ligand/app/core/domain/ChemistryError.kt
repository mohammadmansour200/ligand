package org.ligand.app.core.domain

sealed interface ChemistryError : Error {
    object SanitizationFailed : ChemistryError
    object NoConformation : ChemistryError
}