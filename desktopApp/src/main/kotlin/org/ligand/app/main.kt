package org.ligand.app

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.github.vinceglb.filekit.FileKit
import org.ligand.app.di.initKoin

fun main() {
    try {
        val osName = System.getProperty("os.name").lowercase()
        val resourcesDirStr =
            System.getProperty("compose.application.resources.dir") // appResourcesRootDir.set(project.layout.projectDirectory.dir("libs")) places shared libs inside this dir
        // Load Windows RDKit shared lib
        if ("windows" in osName) {
            System.load("$resourcesDirStr/GraphMolWrap.dll")
        } else {
            // Load Linux RDKit shared lib
            System.load("$resourcesDirStr/libRDKitChemDraw.so.1")
            System.load("$resourcesDirStr/libGraphMolWrap.so")
        }
    } catch (e: Exception) {
        e.printStackTrace()
        return
    }

    initKoin()
    FileKit.init(appId = "org.ligand.app")

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Ligand",
        ) {
            App()
        }
    }
}