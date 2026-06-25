package org.liganddraw.app

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.liganddraw.app.di.initKoin

fun main() {
    initKoin()

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "LigandDraw",
        ) {
            App()
        }
    }
}