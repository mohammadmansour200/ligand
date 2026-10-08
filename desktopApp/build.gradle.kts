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

        buildTypes.release {
            proguard {
                isEnabled.set(false)
            }
        }

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

tasks.register<Zip>("packagePortableZip") {
    group = "compose desktop"
    description = "Packages a portable Windows ZIP release containing the .exe and runtime."

    dependsOn("createReleaseDistributable")

    val appName = libs.versions.app.name.get()
    val versionName = libs.versions.app.version.name.get()

    archiveFileName.set("${appName}_${versionName}_portable.zip")
    destinationDirectory.set(layout.buildDirectory.dir("compose/binaries/main-release/zip"))

    from(layout.buildDirectory.dir("compose/binaries/main-release/app/$appName"))
}