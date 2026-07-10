package org.liganddraw.app.di

import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module
import org.liganddraw.app.editor.data.cheminformatics.RDKitCheminformaticsDataSource
import org.liganddraw.app.editor.domain.CheminformaticsDataSource
import org.liganddraw.app.editor.presentation.drawing_pane.DrawingPaneViewModel
import org.liganddraw.app.editor.presentation.molecule_pane.MoleculePaneViewModel

val sharedModule = module {
    singleOf(::RDKitCheminformaticsDataSource).bind<CheminformaticsDataSource>()

    viewModelOf(::DrawingPaneViewModel)
    viewModelOf(::MoleculePaneViewModel)
}