package org.liganddraw.app.core.presentation

import org.liganddraw.app.core.domain.ChemistryError

data class ErrorText(
    val title: String,
    val body: String,
    val hint: String?
)

fun ChemistryError.toErrorText(): ErrorText {
    return when (this) {
        ChemistryError.SanitizationFailed -> ErrorText(
            title = "This structure isn't chemically valid",
            body = "Check the highlighted bonds and atoms and correct them.",
            hint = "Long-press the highlighted atom to see why."
        )

        ChemistryError.NoConformation -> ErrorText(
            title = "Couldn't generate a 3D structure",
            body = "This molecule's geometry couldn't be resolved.",
            hint = null
        )
    }
}