@file:OptIn(ExperimentalFoundationApi::class)

package org.liganddraw.app.core.presentation.modifier

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.PointerMatcher
import androidx.compose.foundation.onClick
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerButton

@Composable
actual fun Modifier.onRightClick(onClick: () -> Unit) = then(
    onClick(
        matcher = PointerMatcher.mouse(
            PointerButton.Secondary
        ), onClick = onClick
    )
)