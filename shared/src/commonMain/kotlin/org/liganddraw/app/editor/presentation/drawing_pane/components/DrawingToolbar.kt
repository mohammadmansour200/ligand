package org.liganddraw.app.editor.presentation.drawing_pane.components

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import liganddraw.shared.generated.resources.Res
import liganddraw.shared.generated.resources.arrow_drop_down
import liganddraw.shared.generated.resources.hashed_wedge_bond
import liganddraw.shared.generated.resources.pan_tool
import liganddraw.shared.generated.resources.selection_tool
import liganddraw.shared.generated.resources.single_bond
import liganddraw.shared.generated.resources.wedge_bond
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.vectorResource
import org.liganddraw.app.core.presentation.IconWithTooltip
import org.liganddraw.app.core.presentation.modifier.onRightClick
import org.liganddraw.app.editor.domain.Tool

@Composable
fun DrawingToolbar(selectedTool: Tool, onSelectTool: (Tool) -> Unit) {
    Column(
        modifier = Modifier.fillMaxHeight().background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        ToolbarButton(
            checked = selectedTool == Tool.Pan,
            onClick = { onSelectTool(Tool.Pan) },
            icon = Res.drawable.pan_tool,
            tooltipText = "Pan"
        )

        ToolbarButton(
            checked = selectedTool == Tool.Select,
            onClick = { onSelectTool(Tool.Select) },
            icon = Res.drawable.selection_tool,
            tooltipText = "Rectangle Selection"
        )
        SingleBondToolGroup(selectedTool, onSelectTool = { onSelectTool(it) })
    }
}

@Composable
private fun ToolbarButton(
    checked: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    icon: DrawableResource,
    tooltipText: String
) {
    val containerColor = if (checked) MaterialTheme.colorScheme.primary else Color.Unspecified
    val color =
        if (checked) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

    val buttonModifier = Modifier.clip(RoundedCornerShape(10.dp))
        .size(IconButtonDefaults.smallContainerSize()).background(containerColor)
        .combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick,
            role = Role.Button
        )
    Box(
        modifier = if (onLongClick == null) buttonModifier else buttonModifier.onRightClick(
            onLongClick
        ),
        contentAlignment = Alignment.Center
    ) {
        if (onLongClick == null) IconWithTooltip(
            icon = vectorResource(icon),
            text = tooltipText,
            tint = color
        ) else {
            IconWithTooltip(
                tooltipBoxModifier = Modifier.align(Alignment.Center),
                iconModifier = Modifier.size(32.dp),
                icon = vectorResource(icon),
                text = tooltipText,
                tint = color
            )
            Icon(
                imageVector = vectorResource(Res.drawable.arrow_drop_down),
                contentDescription = "More tools",
                modifier = Modifier.size(24.dp).align(Alignment.BottomEnd),
                tint = color
            )
        }
    }
}

@Composable
private fun SingleBondToolGroup(selectedTool: Tool, onSelectTool: (Tool) -> Unit) {
    var showDropdownMenu by remember { mutableStateOf(false) }
    var primaryTool by remember { mutableStateOf<Tool>(Tool.SingleBond) }
    Box {
        ToolbarButton(
            checked = selectedTool == Tool.SingleBond || selectedTool == Tool.WedgeBond || selectedTool == Tool.HashedWedgeBond,
            onClick = { onSelectTool(primaryTool) },
            onLongClick = { showDropdownMenu = true },
            icon = when (primaryTool) {
                is Tool.SingleBond -> Res.drawable.single_bond
                is Tool.WedgeBond -> Res.drawable.wedge_bond
                else -> Res.drawable.hashed_wedge_bond
            },
            tooltipText = "Single Bond"
        )
        DropdownMenu(expanded = showDropdownMenu, onDismissRequest = { showDropdownMenu = false }) {
            Row {
                ToolbarButton(
                    checked = selectedTool == Tool.SingleBond,
                    onClick = {
                        primaryTool = Tool.SingleBond
                        showDropdownMenu = false
                        onSelectTool(Tool.SingleBond)
                    },
                    icon = Res.drawable.single_bond,
                    tooltipText = "Single Bond"
                )
                ToolbarButton(
                    checked = selectedTool == Tool.WedgeBond,
                    onClick = {
                        primaryTool = Tool.WedgeBond
                        showDropdownMenu = false
                        onSelectTool(Tool.WedgeBond)
                    },
                    icon = Res.drawable.wedge_bond,
                    tooltipText = "Wedge Bond"
                )
                ToolbarButton(
                    checked = selectedTool == Tool.HashedWedgeBond,
                    onClick = {
                        primaryTool = Tool.HashedWedgeBond
                        showDropdownMenu = false
                        onSelectTool(Tool.HashedWedgeBond)
                    },
                    icon = Res.drawable.hashed_wedge_bond,
                    tooltipText = "Hashed Wedge Bond"
                )
            }
        }
    }
}