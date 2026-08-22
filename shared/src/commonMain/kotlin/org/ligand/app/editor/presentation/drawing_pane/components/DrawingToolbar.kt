package org.ligand.app.editor.presentation.drawing_pane.components

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
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
import androidx.window.core.layout.WindowHeightSizeClass
import androidx.window.core.layout.WindowWidthSizeClass
import ligand.shared.generated.resources.Res
import ligand.shared.generated.resources.arrow_drop_down
import ligand.shared.generated.resources.benzene_tool
import ligand.shared.generated.resources.chain_tool
import ligand.shared.generated.resources.cyclobutane_tool
import ligand.shared.generated.resources.cycloheptane_tool
import ligand.shared.generated.resources.cyclohexane_tool
import ligand.shared.generated.resources.cyclooctane_tool
import ligand.shared.generated.resources.cyclopentane_tool
import ligand.shared.generated.resources.cyclopropane_tool
import ligand.shared.generated.resources.double_bond_tool
import ligand.shared.generated.resources.eraser_tool
import ligand.shared.generated.resources.hashed_wedge_bond
import ligand.shared.generated.resources.hydrogen_bond_tool
import ligand.shared.generated.resources.minus_tool
import ligand.shared.generated.resources.pan_tool
import ligand.shared.generated.resources.plus_tool
import ligand.shared.generated.resources.reaction_electron_pair_pushing_arrow_tool
import ligand.shared.generated.resources.reaction_equilibrium_arrow_tool
import ligand.shared.generated.resources.reaction_forward_arrow_tool
import ligand.shared.generated.resources.reaction_resonance_arrow_tool
import ligand.shared.generated.resources.reaction_single_electron_pushing_arrow_tool
import ligand.shared.generated.resources.single_bond
import ligand.shared.generated.resources.structure_select_tool
import ligand.shared.generated.resources.triple_bond_tool
import ligand.shared.generated.resources.wedge_bond
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.vectorResource
import org.ligand.app.core.presentation.IconWithTooltip
import org.ligand.app.core.presentation.modifier.onRightClick
import org.ligand.app.editor.domain.DrawingPaneConstants.BENZENE
import org.ligand.app.editor.domain.DrawingPaneConstants.CYCLOBUTANE
import org.ligand.app.editor.domain.DrawingPaneConstants.CYCLOHEPTANE
import org.ligand.app.editor.domain.DrawingPaneConstants.CYCLOHEXANE
import org.ligand.app.editor.domain.DrawingPaneConstants.CYCLOOCTANE
import org.ligand.app.editor.domain.DrawingPaneConstants.CYCLOPENTANE
import org.ligand.app.editor.domain.DrawingPaneConstants.CYCLOPROPANE
import org.ligand.app.editor.domain.Tool
import org.ligand.app.editor.presentation.drawing_pane.modifier.fadingEdges

@Composable
fun BoxScope.DrawingToolbar(selectedTool: Tool, onSelectTool: (Tool) -> Unit) {
    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass

    val isColumn = windowSizeClass.windowWidthSizeClass != WindowWidthSizeClass.COMPACT ||
            windowSizeClass.windowHeightSizeClass == WindowHeightSizeClass.COMPACT

    DrawingToolbarLayout(isColumn = isColumn) {
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
        ToolbarButton(
            checked = selectedTool == Tool.Erase,
            onClick = { onSelectTool(Tool.Erase) },
            icon = Res.drawable.eraser_tool,
            tooltipText = "Erase"
        )

        ToolbarSeparator(isColumn = isColumn)

        ToolbarButton(
            checked = selectedTool == Tool.Plus,
            onClick = { onSelectTool(Tool.Plus) },
            icon = Res.drawable.plus_tool,
            tooltipText = "Increase Charge"
        )
        ToolbarButton(
            checked = selectedTool == Tool.Minus,
            onClick = { onSelectTool(Tool.Minus) },
            icon = Res.drawable.minus_tool,
            tooltipText = "Decrease Charge"
        )

        ToolbarSeparator(isColumn = isColumn)

        ToolGroup(
            options = singleBondOptions,
            selectedTool = selectedTool,
            onSelectTool = { onSelectTool(it) }
        )
        ToolbarButton(
            checked = selectedTool == Tool.DoubleBond,
            onClick = { onSelectTool(Tool.DoubleBond) },
            icon = Res.drawable.double_bond_tool,
            tooltipText = "Double Bond"
        )
        ToolbarButton(
            checked = selectedTool == Tool.TripleBond,
            onClick = { onSelectTool(Tool.TripleBond) },
            icon = Res.drawable.triple_bond_tool,
            tooltipText = "Triple Bond"
        )
        ToolbarButton(
            checked = selectedTool == Tool.HydrogenBond,
            onClick = { onSelectTool(Tool.HydrogenBond) },
            icon = Res.drawable.hydrogen_bond_tool,
            tooltipText = "Hydrogen Bond"
        )
        ToolbarButton(
            checked = selectedTool == Tool.Chain,
            onClick = { onSelectTool(Tool.Chain) },
            icon = Res.drawable.chain_tool,
            tooltipText = "Chain"
        )

        ToolbarSeparator(isColumn = isColumn)

        ToolGroup(
            options = reactionArrowOptions,
            selectedTool = selectedTool,
            onSelectTool = onSelectTool
        )

        ToolbarSeparator(isColumn = isColumn)

        ToolbarButton(
            checked = selectedTool == Tool.Template(BENZENE),
            onClick = { onSelectTool(Tool.Template(BENZENE)) },
            icon = Res.drawable.benzene_tool,
            tooltipText = "Benzene"
        )
        ToolGroup(
            options = cycloAlkaneOptions,
            selectedTool = selectedTool,
            onSelectTool = onSelectTool
        )
    }
}

@Composable
fun ToolbarSeparator(
    modifier: Modifier = Modifier,
    isColumn: Boolean = false,
) {
    if (isColumn) {
        HorizontalDivider(
            modifier = modifier
                .width(24.dp)
                .padding(vertical = 4.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )
    } else {
        VerticalDivider(
            modifier = modifier
                .height(24.dp)
                .padding(horizontal = 4.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}

@Composable
private fun BoxScope.DrawingToolbarLayout(
    modifier: Modifier = Modifier,
    isColumn: Boolean,
    content: @Composable () -> Unit
) {
    val background = MaterialTheme.colorScheme.surfaceContainerLow
    val scrollState = rememberScrollState()
    if (isColumn) {
        Column(
            modifier = modifier
                .align(Alignment.CenterStart)
                .padding(start = 8.dp, top = 8.dp, bottom = 8.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(background)
                .fadingEdges(scrollState = scrollState, isVertical = true, edgeColor = background)
                .verticalScroll(scrollState)
                .padding(vertical = 8.dp, horizontal = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            content()
        }
    } else {
        Row(
            modifier = modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp, start = 8.dp, end = 8.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(background)
                .fadingEdges(scrollState = scrollState, isVertical = false, edgeColor = background)
                .horizontalScroll(scrollState)
                .padding(vertical = 4.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            content()
        }
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

val singleBondOptions = listOf(
    Triple(Tool.SingleBond, Res.drawable.single_bond, "Single Bond"),
    Triple(Tool.WedgeBond, Res.drawable.wedge_bond, "Wedge Bond"),
    Triple(Tool.HashedWedgeBond, Res.drawable.hashed_wedge_bond, "Hashed Wedge Bond"),
)

val reactionArrowOptions = listOf(
    Triple(Tool.ForwardArrow, Res.drawable.reaction_forward_arrow_tool, "Forward Arrow"),
    Triple(
        Tool.ElectronPairPushingArrow,
        Res.drawable.reaction_electron_pair_pushing_arrow_tool,
        "Push Electron Pair"
    ),
    Triple(
        Tool.SingleElectronPushingArrow,
        Res.drawable.reaction_single_electron_pushing_arrow_tool,
        "Push Single Electron"
    ),
    Triple(Tool.ResonanceArrow, Res.drawable.reaction_resonance_arrow_tool, "Resonance Arrow"),
    Triple(
        Tool.EquilibriumArrow,
        Res.drawable.reaction_equilibrium_arrow_tool,
        "Equilibrium Arrow"
    ),
)

val cycloAlkaneOptions = listOf(
    Triple(Tool.Template(CYCLOHEXANE), Res.drawable.cyclohexane_tool, "Cyclohexane"),
    Triple(Tool.Template(CYCLOPROPANE), Res.drawable.cyclopropane_tool, "Cyclopropane"),
    Triple(Tool.Template(CYCLOBUTANE), Res.drawable.cyclobutane_tool, "Cyclobutane"),
    Triple(Tool.Template(CYCLOPENTANE), Res.drawable.cyclopentane_tool, "Cyclopentane"),
    Triple(Tool.Template(CYCLOHEPTANE), Res.drawable.cycloheptane_tool, "Cycloheptane"),
    Triple(Tool.Template(CYCLOOCTANE), Res.drawable.cyclooctane_tool, "Cyclooctane"),
)

@Composable
private fun ToolGroup(
    options: List<Triple<Tool, DrawableResource, String>>,
    selectedTool: Tool,
    onSelectTool: (Tool) -> Unit
) {
    var showDropdownMenu by remember { mutableStateOf(false) }

    var primaryTool by remember { mutableStateOf(options[0]) }

    Box {
        ToolbarButton(
            checked = selectedTool in options.map { it.first },
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
                options.forEach { (tool, iconRes, label) ->
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