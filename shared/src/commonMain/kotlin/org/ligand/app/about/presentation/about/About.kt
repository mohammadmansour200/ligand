package org.ligand.app.about.presentation.about

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ligand.shared.generated.resources.Res
import ligand.shared.generated.resources.arrow_back
import org.jetbrains.compose.resources.vectorResource
import org.ligand.app.about.presentation.about.components.AboutActions
import org.ligand.app.about.presentation.about.components.AboutCredits
import org.ligand.app.about.presentation.about.components.AboutHeader
import org.ligand.app.core.presentation.IconWithTooltip

@Composable
fun AboutRoot(onNavigateBack: () -> Unit, onNavigateToLicenses: () -> Unit) {
    About(
        onNavigateBack = onNavigateBack,
        onNavigateToLicenses = onNavigateToLicenses
    )
}

@Composable
private fun About(
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit,
    onNavigateToLicenses: () -> Unit
) {
    Scaffold(topBar = {
        TopAppBar(
            title = { Text("About") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    IconWithTooltip(
                        icon = vectorResource(Res.drawable.arrow_back),
                        text = "Back",
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors()
                .copy(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        )
    }) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(it),
        ) {
            AboutHeader()

            Spacer(Modifier.height(8.dp))

            AboutActions(onNavigateToLicenses = onNavigateToLicenses)

            Spacer(Modifier.height(8.dp))

            AboutCredits()
        }
    }
}

