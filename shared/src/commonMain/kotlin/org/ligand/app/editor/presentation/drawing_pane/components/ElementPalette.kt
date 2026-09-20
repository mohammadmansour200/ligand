package org.ligand.app.editor.presentation.drawing_pane.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.ligand.app.editor.presentation.drawing_pane.modifier.fadingEdges

@Composable
fun BoxScope.ElementPalette(
    selectedElement: String?,
    onElementSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val topElements = listOf("H", "C", "N", "O", "S")
    val middleElements = listOf("P", "F", "Cl", "Br", "I")
    val bottomElements = listOf("B", "Al", "Li", "Mg", "Fe")

    val scrollState = rememberScrollState()
    val background = MaterialTheme.colorScheme.surfaceContainerLow
    Column(
        modifier = modifier
            .align(Alignment.CenterEnd)
            .padding(end = 8.dp)
            .heightIn(max = 260.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .fadingEdges(
                scrollState = scrollState,
                isVertical = true,
                edgeColor = background,
            )
            .verticalScroll(scrollState)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        topElements.forEach { element ->
            ElementItem(
                element = element,
                isSelected = element == selectedElement,
                onSelect = { onElementSelected(element) }
            )
        }

        HorizontalDivider(
            modifier = Modifier
                .width(24.dp)
                .padding(vertical = 4.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )

        bottomElements.forEach { element ->
            ElementItem(
                element = element,
                isSelected = element == selectedElement,
                onSelect = { onElementSelected(element) }
            )
        }

        HorizontalDivider(
            modifier = Modifier
                .width(24.dp)
                .padding(vertical = 4.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )

        middleElements.forEach { element ->
            ElementItem(
                element = element,
                isSelected = element == selectedElement,
                onSelect = { onElementSelected(element) }
            )
        }
    }
}

@Composable
private fun ElementItem(
    element: String,
    isSelected: Boolean,
    onSelect: (String) -> Unit
) {
    val backgroundColor =
        if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
    val textColor =
        if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .clickable { onSelect(element) }
    ) {
        Text(
            text = element,
            color = textColor,
            fontSize = 20.sp,
            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
        )
    }
}