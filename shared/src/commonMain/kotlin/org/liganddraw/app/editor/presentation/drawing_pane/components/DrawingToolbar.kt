package org.liganddraw.app.editor.presentation.drawing_pane.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import liganddraw.shared.generated.resources.Res
import liganddraw.shared.generated.resources.pan_tool
import liganddraw.shared.generated.resources.selection_tool
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.vectorResource
import org.liganddraw.app.core.presentation.IconWithTooltip
import org.liganddraw.app.editor.domain.Tool

@Composable
fun DrawingToolbar(selectedTool: Tool, onSelectTool: (Tool) -> Unit) {
    Column(
        modifier = Modifier.fillMaxHeight().background(MaterialTheme.colorScheme.surfaceContainer)
    ) {
        ToolbarButton(
            checked = selectedTool == Tool.PAN,
            onClick = { onSelectTool(Tool.PAN) },
            icon = Res.drawable.pan_tool,
            tooltipText = "Pan"
        )

        ToolbarButton(
            checked = selectedTool == Tool.SELECT,
            onClick = { onSelectTool(Tool.SELECT) },
            icon = Res.drawable.selection_tool,
            tooltipText = "Rectangle Selection"
        )
    }
}

@Composable
private fun ToolbarButton(
    checked: Boolean,
    onClick: () -> Unit,
    icon: DrawableResource,
    tooltipText: String
) {
    val containerColor = if (checked) MaterialTheme.colorScheme.primary else Color.Unspecified
    val color =
        if (checked) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    IconButton(
        onClick = { onClick() },
        shape = RoundedCornerShape(10.dp),
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = containerColor,
        )
    ) {
        IconWithTooltip(icon = vectorResource(icon), text = tooltipText, tint = color)
    }
}
