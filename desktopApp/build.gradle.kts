import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

dependencies {
    implementation(projects.shared)

    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutinesSwing)
    implementation(compose.components.resources)

    implementation(libs.compose.uiToolingPreview)

    implementation(libs.filekit.core)
}

compose.desktop {
    application {
        mainClass = "org.ligand.app.MainKt"

        nativeDistributions {
            targetFormats(
                TargetFormat.Msi,
                TargetFormat.Exe,
                TargetFormat.Deb,
                TargetFormat.Rpm,
            )
            packageName = libs.versions.app.name.get()
            packageVersion = libs.versions.app.version.name.get()
            vendor = libs.versions.app.vendor.get()
            description = libs.versions.app.description.get()

            appResourcesRootDir.set(project.layout.projectDirectory.dir("libs"))

            linux {
                iconFile.set(project.file("src/main/resources/icons/icon.png"))
                modules("jdk.security.auth")
            }
            windows {
                iconFile.set(project.file("src/main/resources/icons/icon.ico"))
            }
        }
    }
}