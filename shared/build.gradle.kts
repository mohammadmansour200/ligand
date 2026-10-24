import com.mikepenz.aboutlibraries.plugin.DuplicateMode
import com.mikepenz.aboutlibraries.plugin.DuplicateRule
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.aboutLibraries)
    alias(libs.plugins.buildKonfig)
}

compose.resources {
    publicResClass = true
}

kotlin {
    jvmToolchain(22)

    jvm {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_22
        }
    }

    androidLibrary {
        namespace = "org.ligand.app.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
        androidResources {
            enable = true
        }
        withHostTest {
            isIncludeAndroidResources = true
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)

            implementation(libs.koin.android)
            implementation(libs.koin.androidx.compose)

            implementation(libs.stax.api)
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)

            implementation(libs.androidx.adaptive)
            implementation(libs.androidx.adaptive.layout)
            implementation(libs.androidx.adaptive.navigation)
            implementation(libs.jetbrains.navigation3.ui)

            api(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)

            implementation(files("libs/org.RDKit.jar"))
            implementation(files("libs/opsin-2.9.0.jar"))

            implementation(libs.filekit.core)
            implementation(libs.filekit.dialogs.compose)

            implementation(libs.filament.compose)
            implementation(libs.filament.utils)

            implementation(libs.compose.navigationevent)

            implementation(libs.bundles.ktor)

            implementation(libs.aboutlibraries.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.koin.test)
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}

aboutLibraries {
    export {
        outputPath = file("src/commonMain/composeResources/files/libraries.json")
        prettyPrint = true
    }
    library {
        duplicationMode = DuplicateMode.MERGE
        duplicationRule = DuplicateRule.SIMPLE
    }
    collect {
        configPath = file("config")
    }
}

buildkonfig {
    packageName = "org.ligand.app"

    defaultConfigs {
        buildConfigField(
            com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING,
            "APP_VERSION",
            libs.versions.app.version.name.get()
        )
    }
}