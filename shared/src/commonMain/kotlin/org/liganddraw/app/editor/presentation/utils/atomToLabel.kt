package org.liganddraw.app.editor.presentation.utils

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import org.liganddraw.app.editor.domain.Atom
import org.liganddraw.app.editor.presentation.drawing_pane.components.subscriptStyle

fun Atom.toLabel(): AnnotatedString {
    return buildAnnotatedString {
        if (numImplicitHydrogen == 0L) append(symbol)
        else {
            val appendHydrogens = {
                append("H")
                if (numImplicitHydrogen > 1) {
                    pushStyle(
                        subscriptStyle
                    )
                    append(numImplicitHydrogen.toString())
                    pop()
                }
            }

            if (isLabelReversed) {
                appendHydrogens()
                append(symbol)
            } else {
                append(symbol)
                appendHydrogens()
            }
        }
    }
}