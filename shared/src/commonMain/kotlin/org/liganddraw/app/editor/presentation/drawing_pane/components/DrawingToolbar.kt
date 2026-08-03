package org.liganddraw.app.editor.presentation.drawing_pane.components

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
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
import liganddraw.shared.generated.resources.benzene_tool
import liganddraw.shared.generated.resources.cyclobutane_tool
import liganddraw.shared.generated.resources.cycloheptane_tool
import liganddraw.shared.generated.resources.cyclohexane_tool
import liganddraw.shared.generated.resources.cyclooctane_tool
import liganddraw.shared.generated.resources.cyclopentane_tool
import liganddraw.shared.generated.resources.cyclopropane_tool
import liganddraw.shared.generated.resources.hashed_wedge_bond
import liganddraw.shared.generated.resources.pan_tool
import liganddraw.shared.generated.resources.single_bond
import liganddraw.shared.generated.resources.structure_select_tool
import liganddraw.shared.generated.resources.wedge_bond
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.vectorResource
import org.liganddraw.app.core.presentation.IconWithTooltip
import org.liganddraw.app.core.presentation.modifier.onRightClick
import org.liganddraw.app.editor.domain.DrawingPaneConstants.BENZENE
import org.liganddraw.app.editor.domain.DrawingPaneConstants.CYCLOBUTANE
import org.liganddraw.app.editor.domain.DrawingPaneConstants.CYCLOHEPTANE
import org.liganddraw.app.editor.domain.DrawingPaneConstants.CYCLOHEXANE
import org.liganddraw.app.editor.domain.DrawingPaneConstants.CYCLOOCTANE
import org.liganddraw.app.editor.domain.DrawingPaneConstants.CYCLOPENTANE
import org.liganddraw.app.editor.domain.DrawingPaneConstants.CYCLOPROPANE
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
            checked = selectedTool == Tool.StructureSelect,
            onClick = { onSelectTool(Tool.StructureSelect) },
            icon = Res.drawable.structure_select_tool,
            tooltipText = "Structure Selection"
        )
        SingleBondToolGroup(selectedTool, onSelectTool = { onSelectTool(it) })
        ToolbarButton(
            checked = selectedTool == Tool.Template(BENZENE),
            onClick = { onSelectTool(Tool.Template(BENZENE)) },
            icon = Res.drawable.benzene_tool,
            tooltipText = "Benzene"
        )
        CycloAlkaneToolGroup(selectedTool, onSelectTool)
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
                modifier = Modifier.size(24.dp).align(Alignment.BottomEnd)
                    .offset(x = 4.dp, y = 4.dp),
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

val cycloAlkaneOptions = listOf(
    Triple(Tool.Template(CYCLOHEXANE), Res.drawable.cyclohexane_tool, "Cyclohexane"),
    Triple(Tool.Template(CYCLOPROPANE), Res.drawable.cyclopropane_tool, "Cyclopropane"),
    Triple(Tool.Template(CYCLOBUTANE), Res.drawable.cyclobutane_tool, "Cyclobutane"),
    Triple(Tool.Template(CYCLOPENTANE), Res.drawable.cyclopentane_tool, "Cyclopentane"),
    Triple(Tool.Template(CYCLOHEPTANE), Res.drawable.cycloheptane_tool, "Cycloheptane"),
    Triple(Tool.Template(CYCLOOCTANE), Res.drawable.cyclooctane_tool, "Cyclooctane"),
)

@Composable
private fun CycloAlkaneToolGroup(
    selectedTool: Tool,
    onSelectTool: (Tool) -> Unit
) {
    var showDropdownMenu by remember { mutableStateOf(false) }

    var primaryTool by remember { mutableStateOf(cycloAlkaneOptions[0]) }

    Box {
        ToolbarButton(
            checked = selectedTool in cycloAlkaneOptions.map { it.first },
            onClick = { onSelectTool(primaryTool.first) },
            onLongClick = { showDropdownMenu = true },
            icon = primaryTool.second,
            tooltipText = primaryTool.third
        )

        DropdownMenu(
            expanded = showDropdownMenu,
            onDismissRequest = { showDropdownMenu = false }
        ) {
            Row {
                cycloAlkaneOptions.forEach { (tool, iconRes, label) ->
                    ToolbarButton(
                        checked = selectedTool == tool,
                        onClick = {
                            primaryTool = Triple(tool, iconRes, label)
                            showDropdownMenu = false
                            onSelectTool(tool)
                        },
                        icon = iconRes,
                        tooltipText = label
                    )
                }
            }
        }
    }
}