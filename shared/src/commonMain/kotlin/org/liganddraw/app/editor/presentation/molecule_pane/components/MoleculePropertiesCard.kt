package org.liganddraw.app.editor.presentation.molecule_pane.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import liganddraw.shared.generated.resources.Res
import liganddraw.shared.generated.resources.close
import liganddraw.shared.generated.resources.error
import liganddraw.shared.generated.resources.info
import org.jetbrains.compose.resources.vectorResource
import org.liganddraw.app.core.utils.truncateTo
import org.liganddraw.app.editor.presentation.drawing_pane.modifier.fadingEdges
import org.liganddraw.app.editor.presentation.molecule_pane.MoleculePaneState
import org.liganddraw.app.editor.presentation.molecule_pane.modifier.shimmer


@Composable
fun BoxScope.MoleculePropertiesCard(
    state: MoleculePaneState,
) {
    var propertiesExpanded by remember { mutableStateOf(true) }

    AnimatedVisibility(
        visible = !propertiesExpanded,
        modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut()
    ) {
        FilledTonalIconButton(onClick = { propertiesExpanded = true }) {
            Icon(vectorResource(Res.drawable.info), contentDescription = "Show properties")
        }
    }

    AnimatedVisibility(
        visible = propertiesExpanded,
        modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        enter = slideInVertically { it / 2 } + fadeIn(),
        exit = slideOutVertically { it / 2 } + fadeOut()
    ) {
        ElevatedCard(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .heightIn(max = 380.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Properties",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    IconButton(onClick = { propertiesExpanded = false }) {
                        Icon(
                            vectorResource(Res.drawable.close),
                            contentDescription = "Hide properties",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                when {
                    state.propertiesError != null -> CompactErrorNotice(
                        modifier = Modifier.padding(
                            16.dp
                        )
                    )

                    state.properties == null -> Box(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator(modifier = Modifier.size(24.dp)) }

                    else -> MoleculePropertiesTable(state)
                }
            }
        }
    }
}

@Composable
private fun CompactErrorNotice(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            vectorResource(Res.drawable.error),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = "This structure isn't chemically valid",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.error
        )
    }
}


@Composable
private fun MoleculePropertiesTable(state: MoleculePaneState) {
    val scrollState = rememberScrollState()
    Box(
        modifier = Modifier
            .wrapContentSize()
            .padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fadingEdges(
                    scrollState = scrollState,
                    isVertical = true,
                    edgeColor = MaterialTheme.colorScheme.surface,
                )
                .verticalScroll(scrollState)
        ) {
            state.properties?.let { properties ->
                TableRow(
                    label = "IUPAC Name",
                    value = properties.iupacName ?: "Unavailable",
                    stacked = true,
                    isLoading = state.isIupacLoading
                )
                TableRow(
                    label = "Molecular Formula",
                    value = formulaToSubscriptAnnotatedString(properties.formula)
                )
                TableRow(
                    label = "Molecular Weight",
                    value = "${properties.molecularWeight.truncateTo(3)} g/mol"
                )
                TableRow(label = "LogP", value = "${properties.logp.truncateTo(3)}")
                TableRow(
                    label = "H-Bond Donors",
                    value = "${properties.hydrogenBondDonors}"
                )
                TableRow(
                    label = "H-Bond Acceptors",
                    value = "${properties.hydrogenBondAcceptors}"
                )
                TableRow(label = "Rotatable Bonds", value = "${properties.rotatableBonds}")
            }
        }
    }
}

@Composable
private fun TableRow(
    label: String,
    value: AnnotatedString,
    stacked: Boolean = false,
    isLoading: Boolean = false
) {
    Column {
        if (stacked) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (isLoading) Box(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(20.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmer()
                ) else
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 4.dp)
                    )
            }
        } else {
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
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    }
}

@Composable
private fun TableRow(
    label: String,
    value: String,
    stacked: Boolean = false,
    isLoading: Boolean = false
) {
    TableRow(
        label = label,
        value = AnnotatedString(value),
        stacked = stacked,
        isLoading = isLoading
    )
}

fun formulaToSubscriptAnnotatedString(
    formula: String,
    subscriptFontScale: TextUnit = .9.em
): AnnotatedString = buildAnnotatedString {
    var i = 0
    while (i < formula.length) {
        val char = formula[i]
        if (char.isDigit()) {
            val start = i
            while (i < formula.length && formula[i].isDigit()) i++
            val digits = formula.substring(start, i)
            withStyle(
                SpanStyle(
                    baselineShift = BaselineShift.Subscript,
                    fontSize = subscriptFontScale
                )
            ) {
                append(digits)
            }
        } else {
            append(char)
            i++
        }
    }
}