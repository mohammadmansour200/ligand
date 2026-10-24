package org.ligand.app.about.presentation.about.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ligand.shared.generated.resources.Res
import ligand.shared.generated.resources.cloud
import ligand.shared.generated.resources.science
import ligand.shared.generated.resources.swap_horiz
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.vectorResource

private data class Credit(
    val icon: DrawableResource,
    val name: String,
    val description: String,
    val url: String,
)

private val credits = listOf(
    Credit(
        icon = Res.drawable.science,
        name = "RDKit",
        description = "Core cheminformatics operations, 3D model generation, molecular formula, molecular weight, LogP, hydrogen bonding and rotatable bonds estimation",
        url = "https://www.rdkit.org",
    ),
    Credit(
        icon = Res.drawable.swap_horiz,
        name = "OPSIN",
        description = "Name to structure conversion",
        url = "https://opsin.ch.cam.ac.uk",
    ),
    Credit(
        icon = Res.drawable.cloud,
        name = "PubChem API",
        description = "Structure nomenclature",
        url = "https://pubchem.ncbi.nlm.nih.gov",
    ),
)

@Composable
fun AboutCredits(modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current

    Column(modifier = modifier) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        Text(
            text = "Credits",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp)
                .semantics { heading() },
        )
        Text(
            text = "Ligand is built on these projects and services.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )

        credits.forEach { credit ->
            ListItem(
                modifier = Modifier.clickable(role = Role.Button) {
                    uriHandler.openUri(credit.url)
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                leadingContent = {
                    Icon(
                        imageVector = vectorResource(credit.icon),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                },
                headlineContent = {
                    Text(
                        text = credit.name,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                },
                supportingContent = {
                    Text(
                        text = credit.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
            )
        }
    }
}