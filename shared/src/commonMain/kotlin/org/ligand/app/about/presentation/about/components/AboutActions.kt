package org.ligand.app.about.presentation.about.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ligand.shared.generated.resources.Res
import ligand.shared.generated.resources.code
import ligand.shared.generated.resources.gavel
import ligand.shared.generated.resources.language
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.vectorResource

private data class Action(
    val icon: DrawableResource,
    val label: String,
    val onClick: () -> Unit,
)

@Composable
fun AboutActions(onNavigateToLicenses: () -> Unit) {
    val uriHandler = LocalUriHandler.current

    val actions = listOf(
        Action(
            icon = Res.drawable.code,
            label = "Source code"
        ) { uriHandler.openUri("https://github.com/mohammadmansour200/ligand") },
        Action(
            icon = Res.drawable.language,
            label = "Website"
        ) { uriHandler.openUri("https://mohammadmansour200.github.io/ligand/") },
        Action(
            icon = Res.drawable.gavel,
            label = "Open source licenses",
            onClick = onNavigateToLicenses
        ),
    )

    actions.chunked(3).forEach { rowItems ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
        ) {
            rowItems.forEach { action ->
                AboutActionItem(
                    action = action,
                    onClick = action.onClick,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun AboutActionItem(
    action: Action,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(100.dp))
                .clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = vectorResource(action.icon),
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = action.label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
        }
    }
}