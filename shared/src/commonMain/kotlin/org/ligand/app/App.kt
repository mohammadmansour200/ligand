package org.ligand.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.ligand.app.about.presentation.about.AboutRoot
import org.ligand.app.about.presentation.licenses.LicensesRoot
import org.ligand.app.editor.presentation.EditorRoot
import org.ligand.app.ui.theme.AppTheme

@Serializable
data object EditorRoute : NavKey

@Serializable
data object AboutRoute : NavKey

@Serializable
data object LicensesRoute : NavKey

private val navConfig = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(EditorRoute::class, EditorRoute.serializer())
            subclass(AboutRoute::class, AboutRoute.serializer())
            subclass(LicensesRoute::class, LicensesRoute.serializer())
        }
    }
}

@Composable
@Preview
fun App() {
    AppTheme {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            val backStack = rememberNavBackStack(navConfig, EditorRoute)

            NavDisplay(
                backStack = backStack,
                onBack = { backStack.removeLastOrNull() },
                entryProvider = entryProvider {
                    entry<EditorRoute> {
                        EditorRoot(
                            onNavigateToAbout = { backStack.add(AboutRoute) },
                        )
                    }
                    entry<AboutRoute> {
                        AboutRoot(
                            onNavigateBack = { backStack.removeLastOrNull() },
                            onNavigateToLicenses = { backStack.add(LicensesRoute) },
                        )
                    }
                    entry<LicensesRoute> {
                        LicensesRoot(
                            onNavigateBack = { backStack.removeLastOrNull() },
                        )
                    }
                },
            )
        }
    }
}