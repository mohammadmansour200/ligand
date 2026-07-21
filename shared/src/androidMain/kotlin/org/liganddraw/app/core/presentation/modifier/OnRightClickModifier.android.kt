package org.liganddraw.app.core.presentation.modifier

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun Modifier.onRightClick(onClick: () -> Unit) = this