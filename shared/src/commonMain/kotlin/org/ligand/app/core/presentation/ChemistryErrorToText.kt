package org.ligand.app.core.presentation

import org.ligand.app.core.domain.ChemistryError

data class ErrorText(
    val title: String,
    val body: String,
    val hint: String?
)

fun ChemistryError.toErrorText(): ErrorText {
    return when (this) {
        ChemistryError.SanitizationFailed -> ErrorText(
            title = "This structure isn't chemically valid",
            body = "Long-press the highlighted atom to see why.",
            hint = null
        )

        ChemistryError.NoConformation -> ErrorText(
            title = "Couldn't generate a 3D structure",
            body = "This molecule's geometry couldn't be resolved.",
            hint = null
        )
    }
}