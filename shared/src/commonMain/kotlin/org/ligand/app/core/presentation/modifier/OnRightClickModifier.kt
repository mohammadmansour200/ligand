package org.ligand.app.core.presentation.modifier

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun Modifier.onRightClick(onClick: () -> Unit): Modifier