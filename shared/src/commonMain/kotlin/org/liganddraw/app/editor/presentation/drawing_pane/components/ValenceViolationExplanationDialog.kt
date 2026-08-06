package org.liganddraw.app.editor.presentation.drawing_pane.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import org.liganddraw.app.editor.domain.Atom

data class ValenceExplanation(
    val title: String,
    val body: String,
)

private val explanations: Map<String, Map<Int, ValenceExplanation>> = mapOf(
    "C" to mapOf(
        0 to ValenceExplanation(
            title = "Carbon",
            body = """
                    Carbon has 4 valence electrons and forms exactly 4 bonds to complete its octet.
                    It can't form more than 4 — A double bond counts as 2, and a triple bond counts as 3.
                """.trimIndent()
        ),
        1 to ValenceExplanation(
            title = "Carbon (+)",
            body = """
                    A positively charged carbon has lost an electron, leaving only 3 bonds and an empty orbital.
                    This is called a carbocation — it's electron-deficient and highly reactive.
                """.trimIndent()
        ),
        -1 to ValenceExplanation(
            title = "Carbon (-)",
            body = """
                    A negatively charged carbon carries a lone pair instead of a 4th bond, forming only 3 bonds total.
                    This is called a carbanion.
                """.trimIndent()
        ),
    ),
    "N" to mapOf(
        0 to ValenceExplanation(
            title = "Nitrogen",
            body = """
                    Nitrogen has 5 valence electrons — 3 go into bonds, and the remaining pair stays as a lone pair.
                    This gives nitrogen exactly 3 bonds.
                """.trimIndent()
        ),
        1 to ValenceExplanation(
            title = "Nitrogen (+)",
            body = "A positively charged nitrogen uses its lone pair to form a 4th bond instead of keeping it."
        ),
        -1 to ValenceExplanation(
            title = "Nitrogen (-)",
            body = "A negatively charged nitrogen carries an extra lone pair, leaving only 2 bonds."
        ),
    ),
    "O" to mapOf(
        0 to ValenceExplanation(
            title = "Oxygen",
            body = """
                    Oxygen has 6 valence electrons — 2 go into bonds, and 2 lone pairs remain.
                    This gives neutral oxygen exactly 2 bonds.
                """.trimIndent()
        ),
        1 to ValenceExplanation(
            title = "Oxygen (+)",
            body = "A positively charged oxygen has one fewer lone pair, allowing a 3rd bond."
        ),
        -1 to ValenceExplanation(
            title = "Oxygen (-)",
            body = "A negatively charged oxygen carries an extra lone pair, leaving only 1 bond."
        ),
    ),
    // TODO: add S, P, halogens, etc.
)

private val fallback = ValenceExplanation(
    title = "Too many bonds",
    body = "This atom has more bonds than its element typically allows. Try removing a bond or adjusting the charge."
)

@Composable
fun ValenceViolationExplanationDialog(
    atom: Atom,
    onDismiss: () -> Unit,
) {
    val explanation = remember(atom.symbol, atom.charge) {
        explanations[atom.symbol]?.get(atom.charge) ?: fallback
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(explanation.title) },
        text = { Text(explanation.body, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Got it") }
        }
    )
}