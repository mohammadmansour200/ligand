package org.ligand.app.about.presentation.licenses.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mikepenz.aboutlibraries.entity.Library


@Composable
fun LibraryItem(
    library: Library,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current
    val url = library.website?.takeIf { it.isNotBlank() } ?: library.scm?.url

    ListItem(
        modifier = modifier.then(
            if (url != null) {
                Modifier.clickable(role = Role.Button) { uriHandler.openUri(url) }
            } else {
                Modifier
            }
        ),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        overlineContent = library.developers.firstOrNull()?.name?.let { developer ->
            { Text(developer) }
        },
        headlineContent = { Text(library.name) },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                library.description?.takeIf { it.isNotBlank() }?.let { description ->
                    Text(
                        text = description,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    library.licenses.forEach { license ->
                        SuggestionChip(
                            onClick = { license.url?.let(uriHandler::openUri) },
                            label = { Text(license.name) },
                        )
                    }
                }
            }
        },
        trailingContent = library.artifactVersion?.let { version ->
            {
                Text(
                    text = version,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    )
}