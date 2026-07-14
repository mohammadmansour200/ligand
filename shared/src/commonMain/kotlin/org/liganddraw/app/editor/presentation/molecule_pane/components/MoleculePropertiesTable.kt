package org.liganddraw.app.editor.presentation.molecule_pane.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.liganddraw.app.core.utils.truncateTo
import org.liganddraw.app.editor.domain.MoleculeProperties

@Composable
fun MoleculePropertiesTable(properties: MoleculeProperties) {
    Box(
        modifier = Modifier
            .wrapContentSize()
            .padding(top = 8.dp, bottom = 8.dp, end = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
        ) {
            TableRow(
                label = "Molecular Weight (MW)",
                value = "${properties.molecularWeight.truncateTo(3)} g/mol"
            )
            TableRow(label = "LogP (Octanol-Water)", value = "${properties.logp.truncateTo(3)}")
            TableRow(
                label = "Hydrogen Bond Donors (HBD)",
                value = "${properties.hydrogenBondDonors}"
            )
            TableRow(
                label = "Hydrogen Bond Acceptors (HBA)",
                value = "${properties.hydrogenBondAcceptors}"
            )
            TableRow(label = "Rotatable Bonds", value = "${properties.rotatableBonds}")
        }
    }
}

@Composable
private fun TableRow(label: String, value: String) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    }
}