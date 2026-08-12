package org.liganddraw.app.di

import io.ktor.client.engine.cio.CIO
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module
import org.liganddraw.app.core.data.HttpClientFactory
import org.liganddraw.app.editor.data.cheminformatics.RDKitCheminformaticsDataSource
import org.liganddraw.app.editor.data.network.KtorPubchemDataSource
import org.liganddraw.app.editor.domain.CheminformaticsDataSource
import org.liganddraw.app.editor.domain.PubchemDataSource
import org.liganddraw.app.editor.presentation.drawing_pane.DrawingPaneViewModel
import org.liganddraw.app.editor.presentation.molecule_pane.MoleculePaneViewModel

val sharedModule = module {
    single { HttpClientFactory.create(CIO.create()) }
    singleOf(::RDKitCheminformaticsDataSource).bind<CheminformaticsDataSource>()
    singleOf(::KtorPubchemDataSource).bind<PubchemDataSource>()

    viewModelOf(::DrawingPaneViewModel)
    viewModelOf(::MoleculePaneViewModel)
}